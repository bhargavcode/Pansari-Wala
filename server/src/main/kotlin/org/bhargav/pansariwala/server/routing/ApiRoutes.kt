package org.bhargav.pansariwala.server.routing

import com.auth0.jwt.interfaces.Payload
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.http.content.staticFiles
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.readRemaining
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.dto.AdminLoginRequest
import org.bhargav.pansariwala.server.dto.AdminOrderActionRequest
import org.bhargav.pansariwala.server.dto.AdminPartnerPatch
import org.bhargav.pansariwala.server.dto.AdminShopCreate
import org.bhargav.pansariwala.server.dto.AdminShopPatch
import org.bhargav.pansariwala.server.dto.AdminUserPatch
import org.bhargav.pansariwala.server.dto.CreateRazorpayRequest
import org.bhargav.pansariwala.server.dto.CustomerLocationRequest
import org.bhargav.pansariwala.server.dto.DeliverRequest
import org.bhargav.pansariwala.server.dto.FirebaseAuthRequest
import org.bhargav.pansariwala.server.dto.IncomingOfferResponse
import org.bhargav.pansariwala.server.dto.OkResponse
import org.bhargav.pansariwala.server.dto.OtpRequest
import org.bhargav.pansariwala.server.dto.OtpVerifyRequest
import org.bhargav.pansariwala.server.dto.PartnerLocationRequest
import org.bhargav.pansariwala.server.dto.PartnerOnlineRequest
import org.bhargav.pansariwala.server.dto.PartnerRegisterRequest
import org.bhargav.pansariwala.server.dto.PickupRequest
import org.bhargav.pansariwala.server.dto.PlaceOrderRequest
import org.bhargav.pansariwala.server.dto.PublicConfigDto
import org.bhargav.pansariwala.server.dto.QuoteRequest
import org.bhargav.pansariwala.server.dto.RateOrderRequest
import org.bhargav.pansariwala.server.dto.SaveAddressRequest
import org.bhargav.pansariwala.server.dto.ShopActionRequest
import org.bhargav.pansariwala.server.dto.ShopLoginRequest
import org.bhargav.pansariwala.server.dto.StatusPatch
import org.bhargav.pansariwala.server.dto.SyncPushRequest
import org.bhargav.pansariwala.server.dto.UpdateProfileRequest
import org.bhargav.pansariwala.server.dto.VerifyPaymentRequest
import org.bhargav.pansariwala.server.service.AppStore
import java.io.File

fun Route.apiRoutes(config: ServerConfig, store: AppStore) {
    staticFiles("/uploads", File(config.uploadDir))
    get("/health") { call.respond(OkResponse(true)) }
    get("/config/public") {
        call.respond(
            PublicConfigDto(
                razorpayKeyId = config.razorpayKeyId,
                paymentsEnabled = config.paymentsEnabled,
                devAuth = config.devAuth,
            ),
        )
    }
    post("/auth/shop/login") {
        val body = call.receive<ShopLoginRequest>()
        call.respond(store.shopLogin(body.username, body.password))
    }
    post("/auth/admin/login") {
        val body = call.receive<AdminLoginRequest>()
        call.respond(store.adminLogin(body.username, body.password))
    }
    post("/auth/user/firebase") {
        val body = call.receive<FirebaseAuthRequest>()
        call.respond(store.loginFirebase(body.idToken))
    }
    post("/auth/otp/request") {
        val body = call.receive<OtpRequest>()
        call.respond(store.requestOtp(body.phone))
    }
    post("/auth/otp/verify") {
        val body = call.receive<OtpVerifyRequest>()
        call.respond(store.verifyOtp(body.phone, body.otp, body.sessionId))
    }
    post("/partners/register") {
        val body = call.receive<PartnerRegisterRequest>()
        call.respond(store.registerPartner(body))
    }

    authenticate("auth-jwt") {
        get("/me") { call.respond(store.me(call.userId())) }
        put("/me/profile") {
            val body = call.receive<UpdateProfileRequest>()
            call.respond(store.updateProfile(call.userId(), body.name, body.address, body.locality, body.lat, body.lng))
        }
        post("/me/addresses") {
            val body = call.receive<SaveAddressRequest>()
            call.respond(store.saveAddress(call.userId(), body))
        }
        post("/me/addresses/{id}/select") {
            call.respond(store.selectAddress(call.userId(), call.parameters["id"]!!))
        }
        post("/me/location") {
            val body = call.receive<CustomerLocationRequest>()
            store.updateCustomerLocation(call.userId(), body.lat, body.lng)
            call.respond(OkResponse())
        }
        get("/me/transactions") {
            call.respond(withContext(Dispatchers.IO) { store.transactions(call.userId()) })
        }
        get("/shops") {
            val lat = call.parameters["lat"]?.toDoubleOrNull() ?: error("lat required")
            val lng = call.parameters["lng"]?.toDoubleOrNull() ?: error("lng required")
            val radius = call.parameters["radiusKm"]?.toDoubleOrNull() ?: 20.0
            val query = call.parameters["query"].orEmpty()
            call.respond(store.nearbyShops(lat, lng, radius, query))
        }
        get("/shops/{id}/catalog") {
            call.respond(store.catalog(call.parameters["id"]!!))
        }
        get("/shops/{id}/ratings") {
            call.respond(withContext(Dispatchers.IO) { store.shopRatings(call.parameters["id"]!!) })
        }
        get("/shops/{id}/offers") {
            call.respond(store.offers(call.parameters["id"]!!))
        }
        post("/orders/quote") {
            call.respond(store.quote(call.userId(), call.receive()))
        }
        post("/orders/validate") {
            val body = call.receive<PlaceOrderRequest>()
            store.validateCheckout(call.userId(), body)
            call.respond(OkResponse())
        }
        post("/payments/razorpay/order") {
            call.respond(store.createRazorpay(call.userId(), call.receive<CreateRazorpayRequest>()))
        }
        post("/payments/razorpay/verify") {
            val body = call.receive<VerifyPaymentRequest>()
            val ok = store.verifyPayment(body.razorpayOrderId, body.razorpayPaymentId, body.razorpaySignature)
            if (!ok) call.respond(HttpStatusCode.BadRequest, OkResponse(false)) else call.respond(OkResponse(true))
        }
        post("/orders") {
            call.respond(store.placeOrder(call.userId(), call.receive<PlaceOrderRequest>()))
        }
        get("/orders/mine") {
            call.respond(withContext(Dispatchers.IO) { store.customerOrders(call.userId()) })
        }
        get("/orders/{id}") { call.respond(store.getOrder(call.parameters["id"]!!)) }
        post("/orders/{id}/rating") {
            val body = call.receive<RateOrderRequest>()
            call.respond(store.rateOrder(call.userId(), call.parameters["id"]!!, body.stars, body.comment))
        }

        get("/shop/orders") {
            call.respond(withContext(Dispatchers.IO) { store.shopOrders(call.requireShopId()) })
        }
        post("/shop/orders/{id}/accept") {
            call.respond(store.acceptShopOrder(call.requireShopId(), call.parameters["id"]!!))
        }
        post("/shop/orders/{id}/reject") {
            val body = call.receive<ShopActionRequest>()
            call.respond(store.rejectShopOrder(call.requireShopId(), call.parameters["id"]!!, body.rejectedProductIds, body.reason))
        }
        post("/shop/orders/{id}/status") {
            val body = call.receive<StatusPatch>()
            call.respond(store.setStatus(call.requireShopId(), call.parameters["id"]!!, body.status))
        }
        post("/shop/orders/{id}/delivery") {
            call.respond(store.requestDelivery(call.requireShopId(), call.parameters["id"]!!))
        }
        post("/shop/orders/{id}/cancel") {
            val body = call.receive<ShopActionRequest>()
            call.respond(store.cancelShopOrder(call.requireShopId(), call.parameters["id"]!!, body.reason))
        }
        get("/sync/pull") { call.respond(store.pullSync(call.requireShopId())) }
        post("/sync/push") { store.pushSync(call.requireShopId(), call.receive<SyncPushRequest>()); call.respond(OkResponse()) }

        get("/partners/dashboard") {
            val from = call.parameters["from"]?.toLongOrNull() ?: startOfToday()
            val to = call.parameters["to"]?.toLongOrNull() ?: (from + 86_400_000)
            call.respond(store.partnerDashboard(call.userId(), from, to))
        }
        get("/partners/profile") {
            call.respond(withContext(Dispatchers.IO) { store.partnerProfile(call.userId()) })
        }
        get("/partners/earnings") {
            call.respond(withContext(Dispatchers.IO) { store.partnerEarnings(call.userId()) })
        }
        post("/partners/online") {
            val body = call.receive<PartnerOnlineRequest>()
            store.setPartnerOnline(call.userId(), body.online)
            call.respond(OkResponse())
        }
        post("/partners/location") {
            val body = call.receive<PartnerLocationRequest>()
            store.updatePartnerLocation(call.userId(), body.lat, body.lng)
            call.respond(OkResponse())
        }
        get("/partners/offers/incoming") {
            call.respond(IncomingOfferResponse(store.incomingForPartner(call.userId())))
        }
        get("/partners/offers/available") {
            call.respond(withContext(Dispatchers.IO) { store.availableOffersForPartner(call.userId()) })
        }
        get("/partners/offers/{id}") {
            call.respond(store.offerById(call.parameters["id"]!!, call.userId()))
        }
        post("/partners/offers/{id}/accept") {
            runCatching { store.acceptOffer(call.userId(), call.parameters["id"]!!) }
                .onSuccess { call.respond(it) }
                .onFailure {
                    if (it.message == "ALREADY_TAKEN") {
                        // 200 with TAKEN_BY_OTHER so clients can branch without treating it as a transport error.
                        call.respond(store.offerById(call.parameters["id"]!!, call.userId()))
                    } else throw it
                }
        }
        post("/partners/offers/{id}/reject") {
            call.respond(store.rejectOffer(call.userId(), call.parameters["id"]!!))
        }
        get("/partners/jobs/accepted") {
            call.respond(withContext(Dispatchers.IO) { store.partnerJobs(call.userId(), delivered = false) })
        }
        get("/partners/jobs/{id}") {
            val id = call.parameters["id"]!!
            call.respond(withContext(Dispatchers.IO) { store.partnerJob(call.userId(), id) })
        }
        get("/partners/jobs/delivered") {
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(withContext(Dispatchers.IO) { store.partnerJobs(call.userId(), delivered = true, from, to) })
        }
        post("/partners/jobs/{id}/cancel") {
            call.respond(store.cancelPickup(call.userId(), call.parameters["id"]!!))
        }
        post("/partners/jobs/{id}/pickup") {
            val body = call.receive<PickupRequest>()
            call.respond(store.submitPickup(call.userId(), call.parameters["id"]!!, body.photoOneBase64, body.photoTwoBase64))
        }
        post("/partners/jobs/{id}/arrived-store") {
            call.respond(store.arrivedAtStore(call.userId(), call.parameters["id"]!!))
        }
        post("/partners/jobs/{id}/verify-bags") {
            val body = runCatching { call.receive<PickupRequest>() }.getOrNull()
            call.respond(
                store.verifyBags(
                    call.userId(),
                    call.parameters["id"]!!,
                    body?.photoOneBase64.orEmpty(),
                    body?.photoTwoBase64.orEmpty(),
                ),
            )
        }
        post("/partners/jobs/{id}/arrived-customer") {
            call.respond(store.arrivedAtCustomer(call.userId(), call.parameters["id"]!!))
        }
        post("/partners/jobs/{id}/deliver") {
            val body = call.receive<DeliverRequest>()
            call.respond(store.deliver(call.userId(), call.parameters["id"]!!, body.otp))
        }

        get("/admin/shops") {
            call.requireRole("ADMIN")
            call.respond(store.listAdminShops())
        }
        post("/admin/shops") {
            call.requireRole("ADMIN")
            val body = call.receive<AdminShopCreate>()
            call.respond(
                store.createShopAdmin(
                    name = body.name,
                    shopType = body.shopType,
                    address = body.address,
                    lat = body.lat,
                    lng = body.lng,
                    active = body.active,
                    imageUrl = body.imageUrl,
                    ownerName = body.ownerName,
                    ownerPhone = body.ownerPhone,
                    ownerEmail = body.ownerEmail,
                    city = body.city,
                    state = body.state,
                    zip = body.zip,
                    country = body.country,
                    registrationNumber = body.registrationNumber,
                    taxId = body.taxId,
                    operatingHours = body.operatingHours,
                    features = body.features,
                ),
            )
        }
        get("/admin/shops/{id}") {
            call.requireRole("ADMIN")
            call.respond(store.adminShopDetail(call.parameters["id"]!!))
        }
        post("/admin/shops/{id}") {
            call.requireRole("ADMIN")
            val body = call.receive<AdminShopPatch>()
            store.patchShop(
                shopId = call.parameters["id"]!!,
                active = body.active,
                payments = body.paymentsEnabled,
                features = body.features,
                imageUrl = body.imageUrl,
                name = body.name,
                address = body.address,
                shopType = body.shopType,
                ownerName = body.ownerName,
                ownerPhone = body.ownerPhone,
                ownerEmail = body.ownerEmail,
                city = body.city,
                state = body.state,
                zip = body.zip,
                country = body.country,
                registrationNumber = body.registrationNumber,
                taxId = body.taxId,
                lat = body.lat,
                lng = body.lng,
                operatingHours = body.operatingHours,
            )
            call.respond(OkResponse())
        }
        get("/admin/dashboard") {
            call.requireRole("ADMIN")
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(store.adminDashboard(from, to))
        }
        get("/admin/orders") {
            call.requireRole("ADMIN")
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(store.listAdminTransactions(from, to))
        }
        get("/admin/orders/{id}") {
            call.requireRole("ADMIN")
            call.respond(store.adminOrderDetail(call.parameters["id"]!!))
        }
        post("/admin/orders/{id}/cancel") {
            call.requireRole("ADMIN")
            val body = runCatching { call.receive<AdminOrderActionRequest>() }.getOrNull()
            call.respond(store.adminCancelOrder(call.parameters["id"]!!, body?.reason))
        }
        post("/admin/orders/{id}/refund") {
            call.requireRole("ADMIN")
            call.respond(store.adminRefundOrder(call.parameters["id"]!!))
        }
        get("/admin/users") {
            call.requireRole("ADMIN")
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(store.listAdminUsers(from, to))
        }
        get("/admin/users/{id}") {
            call.requireRole("ADMIN")
            call.respond(store.adminUserDetail(call.parameters["id"]!!))
        }
        post("/admin/users/{id}") {
            call.requireRole("ADMIN")
            val body = call.receive<AdminUserPatch>()
            store.patchAdminUser(call.parameters["id"]!!, body.active)
            call.respond(OkResponse())
        }
        get("/admin/partners") {
            call.requireRole("ADMIN")
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(store.listAdminPartners(from, to))
        }
        get("/admin/partners/{id}") {
            call.requireRole("ADMIN")
            call.respond(store.adminPartnerDetail(call.parameters["id"]!!))
        }
        post("/admin/partners/{id}") {
            call.requireRole("ADMIN")
            val body = call.receive<AdminPartnerPatch>()
            store.patchAdminPartner(call.parameters["id"]!!, body.active)
            call.respond(OkResponse())
        }

        get("/master/categories") { call.respond(store.masterCategories()) }
        get("/master/products") { call.respond(store.masterProducts()) }
        get("/master/shop-types") { call.respond(store.shopTypes()) }

        post("/admin/master/categories") {
            call.requireRole("ADMIN")
            call.respond(store.upsertMasterCategory(call.receive()))
        }
        delete("/admin/master/categories/{id}") {
            call.requireRole("ADMIN")
            store.deleteMasterCategory(call.parameters["id"]!!)
            call.respond(OkResponse())
        }
        post("/admin/master/products") {
            call.requireRole("ADMIN")
            call.respond(store.upsertMasterProduct(call.receive()))
        }
        delete("/admin/master/products/{id}") {
            call.requireRole("ADMIN")
            store.deleteMasterProduct(call.parameters["id"]!!)
            call.respond(OkResponse())
        }
        post("/admin/master/shop-types") {
            call.requireRole("ADMIN")
            call.respond(store.upsertShopType(call.receive()))
        }
        delete("/admin/master/shop-types/{id}") {
            call.requireRole("ADMIN")
            store.deleteShopType(call.parameters["id"]!!)
            call.respond(OkResponse())
        }
        post("/admin/uploads") {
            call.requireRole("ADMIN")
            val prefix = call.request.queryParameters["prefix"] ?: "master/product-images"
            val multipart = call.receiveMultipart()
            var fileName = "upload.bin"
            var bytes: ByteArray? = null
            var contentType = "application/octet-stream"
            while (true) {
                val part = multipart.readPart() ?: break
                if (part is PartData.FileItem) {
                    fileName = part.originalFileName ?: fileName
                    contentType = part.contentType?.toString() ?: contentType
                    bytes = part.provider().readRemaining().readByteArray()
                }
            }
            val data = bytes ?: error("file required")
            call.respond(store.uploadAsset(prefix, fileName, data, contentType))
        }

        webSocket("/ws/delivery") {
            val partnerId = call.userId()
            store.registerSocket(partnerId, this)
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text && frame.readText() == "ping") {
                        send(Frame.Text("pong"))
                    }
                }
            } finally {
                store.unregisterSocket(partnerId, this)
                close(CloseReason(CloseReason.Codes.GOING_AWAY, "bye"))
            }
        }
    }
}

private fun ApplicationCall.payload(): Payload =
    principal<JWTPrincipal>()?.payload ?: error("Unauthorized")

private fun ApplicationCall.userId(): String = payload().subject

private fun ApplicationCall.requireShopId(): String =
    payload().getClaim("shopId").asString() ?: error("Shop role required")

private fun ApplicationCall.requireRole(role: String) {
    if (payload().getClaim("role").asString() != role) error("Forbidden")
}

private fun startOfToday(): Long {
    val now = System.currentTimeMillis()
    return now - (now % 86_400_000)
}
