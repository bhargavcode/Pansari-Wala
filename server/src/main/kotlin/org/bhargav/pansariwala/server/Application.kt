package org.bhargav.pansariwala.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.deflate
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.db.connectMongo
import org.bhargav.pansariwala.server.dto.ApiErrorBody
import org.bhargav.pansariwala.server.routing.apiRoutes
import org.bhargav.pansariwala.server.security.Security
import org.bhargav.pansariwala.server.service.AppStore
import org.bhargav.pansariwala.server.service.PushService
import org.bhargav.pansariwala.server.service.SupportService
import org.bhargav.pansariwala.server.storage.AssetKeyMigration
import org.bhargav.pansariwala.server.storage.AssetRefs
import org.bhargav.pansariwala.server.storage.AssetStorageFactory
import org.slf4j.event.Level

private const val OFFER_SWEEP_INTERVAL_MS = 30_000L
private const val ACCOUNT_PURGE_INTERVAL_MS = 10 * 60_000L

fun main() {
    val config = ServerConfig.fromEnv()
    val storage = AssetStorageFactory.create(config)
    val assetRefs = AssetRefs(storage, config.legacyAssetBaseUrls)
    println("Asset storage: ${storage.description}")
    val security = Security(config)
    val mongo = connectMongo(config, security)
    Runtime.getRuntime().addShutdownHook(Thread { mongo.client.close() })
    val migrated = AssetKeyMigration(mongo.db, assetRefs).run()
    if (migrated > 0) println("Asset refs: normalized $migrated documents to storage keys")
    val push = PushService(config, mongo)
    println(if (push.enabled) "Push: FCM HTTP v1 enabled" else "Push: disabled (set FCM_SERVICE_ACCOUNT_FILE or FCM_SERVICE_ACCOUNT_JSON)")
    val store = AppStore(config, security, mongo, storage, assetRefs, push)
    val support = SupportService(mongo, assetRefs, push)
    val credit = org.bhargav.pansariwala.server.service.CreditService(mongo, assetRefs)

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        // CORS first so preflight + error responses always include ACAO (web localhost → API).
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Accept)
            allowNonSimpleContentTypes = true
            anyHost()
            maxAgeInSeconds = 86_400
        }
        // Store uses the blocking Mongo driver; keep Netty's small call group free for I/O.
        intercept(ApplicationCallPipeline.Setup) {
            withContext(Dispatchers.IO) { proceed() }
        }
        install(Compression) {
            gzip { priority = 1.0; minimumSize(1024) }
            deflate { priority = 0.9; minimumSize(1024) }
        }
        install(CallLogging) { level = Level.DEBUG }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false; isLenient = true })
        }
        install(WebSockets)
        install(StatusPages) {
            exception<org.bhargav.pansariwala.server.service.OtpException> { call, cause ->
                cause.retryAfterSec?.let { call.response.headers.append("Retry-After", it.toString()) }
                call.respond(
                    HttpStatusCode.fromValue(cause.status),
                    ApiErrorBody(cause.message ?: "OTP error", cause.code),
                )
            }
            exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, ApiErrorBody(cause.message ?: "Invalid request body"))
            }
            exception<IllegalStateException> { call, cause ->
                val message = cause.message ?: "Bad request"
                val status = when {
                    message.contains("Invalid credentials") || message.contains("Invalid OTP") -> HttpStatusCode.Unauthorized
                    message.startsWith("Forbidden") -> HttpStatusCode.Forbidden
                    else -> HttpStatusCode.BadRequest
                }
                call.respond(status, ApiErrorBody(message))
            }
            exception<IllegalArgumentException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, ApiErrorBody(cause.message ?: "Bad request"))
            }
            exception<Throwable> { call, cause ->
                val message = cause.message ?: "Server error"
                val status = when {
                    message.contains("Unauthorized") -> HttpStatusCode.Unauthorized
                    message.contains("Forbidden") -> HttpStatusCode.Forbidden
                    message.contains("not found", ignoreCase = true) -> HttpStatusCode.NotFound
                    message.contains("Invalid credentials") -> HttpStatusCode.Unauthorized
                    else -> HttpStatusCode.InternalServerError
                }
                call.respond(status, ApiErrorBody(message))
            }
        }
        install(Authentication) {
            jwt("auth-jwt") {
                verifier(security.verifier())
                validate { credential ->
                    if (credential.payload.getClaim("role").asString().isNullOrBlank()) null
                    else io.ktor.server.auth.jwt.JWTPrincipal(credential.payload)
                }
                challenge { _, _ ->
                    call.respond(HttpStatusCode.Unauthorized, ApiErrorBody("Token missing or invalid"))
                }
            }
        }
        routing {
            apiRoutes(config, store, push, support, credit)
        }
        launch(Dispatchers.IO) {
            var lastAccountPurge = 0L
            while (isActive) {
                if (System.currentTimeMillis() - lastAccountPurge >= ACCOUNT_PURGE_INTERVAL_MS) {
                    lastAccountPurge = System.currentTimeMillis()
                    runCatching { store.purgeDeletedCustomers() }
                        .onSuccess { if (it > 0) println("Account deletion: purged $it customer accounts") }
                        .onFailure { println("Account purge failed: ${it.message}") }
                }
                runCatching { store.expireStaleOffers() }
                    .onSuccess { if (it > 0) println("Delivery offers: expired $it unpicked offers") }
                    .onFailure { println("Delivery offer sweep failed: ${it.message}") }
                runCatching { store.expirePaymentIntents() }
                    .onFailure { println("Payment sweep failed: ${it.message}") }
                delay(OFFER_SWEEP_INTERVAL_MS)
            }
        }
    }.start(wait = true)
}
