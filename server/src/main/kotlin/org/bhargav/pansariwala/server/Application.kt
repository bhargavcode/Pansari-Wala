package org.bhargav.pansariwala.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
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
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.db.connectMongo
import org.bhargav.pansariwala.server.dto.ApiErrorBody
import org.bhargav.pansariwala.server.routing.apiRoutes
import org.bhargav.pansariwala.server.security.Security
import org.bhargav.pansariwala.server.service.AppStore
import org.slf4j.event.Level
import java.io.File

fun main() {
    val config = ServerConfig.fromEnv()
    File(config.uploadDir).mkdirs()
    val security = Security(config)
    val mongo = connectMongo(config, security)
    Runtime.getRuntime().addShutdownHook(Thread { mongo.client.close() })
    val store = AppStore(config, security, mongo)

    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        install(CallLogging) { level = Level.INFO }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false })
        }
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            anyHost()
        }
        install(WebSockets)
        install(StatusPages) {
            exception<IllegalStateException> { call, cause ->
                call.respond(HttpStatusCode.BadRequest, ApiErrorBody(cause.message ?: "Bad request"))
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
            }
        }
        routing {
            apiRoutes(config, store)
        }
    }.start(wait = true)
}
