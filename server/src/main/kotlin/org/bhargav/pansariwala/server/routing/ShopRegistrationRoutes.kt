package org.bhargav.pansariwala.server.routing

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.server.dto.ShopRegistrationDraftDto
import org.bhargav.pansariwala.server.dto.ShopRegistrationSubmit
import org.bhargav.pansariwala.server.dto.ShopRegistrationVerifyRequest
import org.bhargav.pansariwala.server.service.AppStore
import org.bhargav.pansariwala.server.service.ShopRegistrationService

/**
 * Self-service shop onboarding. The OTP is requested through `/auth/otp/request`; verifying it here returns a
 * registrant token that only opens the `/shop-register` endpoints (see the `auth-registrant` provider).
 */
fun Route.shopRegistrationRoutes(registration: ShopRegistrationService, store: AppStore) {
    post("/auth/shop-register/verify") {
        val body = call.receive<ShopRegistrationVerifyRequest>()
        call.respond(withContext(Dispatchers.IO) { registration.verify(body.phone, body.otp, body.sessionId) })
    }
    authenticate("auth-registrant") {
        get("/shop-register/draft") {
            call.respond(withContext(Dispatchers.IO) { registration.draft(call.registrantPhone()) })
        }
        put("/shop-register/draft") {
            val body = call.receive<ShopRegistrationDraftDto>()
            call.respond(withContext(Dispatchers.IO) { registration.saveDraft(call.registrantPhone(), body) })
        }
        get("/shop-register/shop-types") {
            call.respond(withContext(Dispatchers.IO) { store.shopTypes() })
        }
        get("/shop-register/username-available") {
            val username = call.request.queryParameters["username"].orEmpty()
            call.respond(withContext(Dispatchers.IO) { store.registrationUsernameAvailability(username) })
        }
        post("/shop-register/submit") {
            val body = call.receive<ShopRegistrationSubmit>()
            call.respond(withContext(Dispatchers.IO) { registration.submit(call.registrantPhone(), body) })
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.registrantPhone(): String =
    principal<JWTPrincipal>()?.payload?.subject ?: error("Unauthorized")
