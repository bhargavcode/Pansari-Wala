package org.bhargav.pansariwala.server.routing

import com.auth0.jwt.interfaces.Payload
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
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
import org.bhargav.pansariwala.server.dto.DeliveryRequest
import org.bhargav.pansariwala.server.dto.FirebaseAuthRequest
import org.bhargav.pansariwala.server.dto.IncomingOfferResponse
import org.bhargav.pansariwala.server.dto.OkResponse
import org.bhargav.pansariwala.server.dto.OtpRequest
import org.bhargav.pansariwala.server.dto.OtpVerifyRequest
import org.bhargav.pansariwala.server.dto.PartnerLocationRequest
import org.bhargav.pansariwala.server.dto.PartnerOnlineRequest
import org.bhargav.pansariwala.server.dto.PartnerProfilePhotoRequest
import org.bhargav.pansariwala.server.dto.PartnerRegisterRequest
import org.bhargav.pansariwala.server.dto.PickupRequest
import org.bhargav.pansariwala.server.dto.PlaceOrderRequest
import org.bhargav.pansariwala.server.dto.PublicConfigDto
import org.bhargav.pansariwala.server.dto.PushRegisterRequest
import org.bhargav.pansariwala.server.dto.PushUnregisterRequest
import org.bhargav.pansariwala.server.dto.QuoteRequest
import org.bhargav.pansariwala.server.dto.RateOrderRequest
import org.bhargav.pansariwala.server.dto.RatingReplyRequest
import org.bhargav.pansariwala.server.dto.ShopOfferDto
import org.bhargav.pansariwala.server.dto.ShopStaffUpsert
import org.bhargav.pansariwala.server.service.ShopPerm
import org.bhargav.pansariwala.server.dto.SaveAddressRequest
import org.bhargav.pansariwala.server.dto.ShopActionRequest
import org.bhargav.pansariwala.server.dto.ShopLoginRequest
import org.bhargav.pansariwala.server.dto.StatusPatch
import org.bhargav.pansariwala.server.dto.SyncPushRequest
import org.bhargav.pansariwala.server.dto.UpdateProfileRequest
import org.bhargav.pansariwala.server.dto.VerifyPaymentRequest
import org.bhargav.pansariwala.server.service.AppStore
import org.bhargav.pansariwala.server.service.PushService

fun Route.apiRoutes(config: ServerConfig, store: AppStore, push: PushService) {
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
    post("/uploads/guest") {
        val prefix = (call.request.queryParameters["prefix"] ?: "").trim().trim('/') + "/"
        val allowed = prefix.startsWith("partners/") || prefix.startsWith("users/")
        if (!allowed) error("Guest upload only allowed for partners/ or users/ prefixes")
        call.respond(receiveUpload(call, store))
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
    // Public: called after logout/session expiry when no JWT remains; the FCM token itself identifies the device.
    post("/push/unregister") {
        val body = call.receive<PushUnregisterRequest>()
        push.unregister(body.token)
        call.respond(OkResponse())
    }

    authenticate("auth-jwt") {
        post("/push/register") {
            val body = call.receive<PushRegisterRequest>()
            val payload = call.payload()
            push.register(
                token = body.token,
                principalId = payload.subject,
                role = payload.getClaim("role").asString().orEmpty(),
                shopId = payload.getClaim("shopId").asString(),
                platform = body.platform,
                deviceId = body.deviceId,
            )
            call.respond(OkResponse())
        }
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
            val categories = call.parameters["category"].orEmpty()
                .split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
            call.respond(store.nearbyShops(lat, lng, radius, query, categories))
        }
        get("/shops/{id}/catalog") {
            val shopId = call.parameters["id"]!!
            if (call.payload().getClaim("role").asString() == "CUSTOMER") {
                withContext(Dispatchers.IO) { store.recordShopVisit(shopId, call.userId()) }
            }
            call.respond(store.catalog(shopId))
        }
        get("/shops/{id}/info") {
            val lat = call.parameters["lat"]?.toDoubleOrNull() ?: error("lat required")
            val lng = call.parameters["lng"]?.toDoubleOrNull() ?: error("lng required")
            call.respond(withContext(Dispatchers.IO) { store.shopForCustomer(call.parameters["id"]!!, lat, lng) })
        }
        get("/shops/{id}/catalog/sections") {
            val shopId = call.parameters["id"]!!
            val query = call.parameters["query"].orEmpty()
            val preview = call.parameters["preview"]?.toIntOrNull() ?: 5
            call.respond(
                withContext(Dispatchers.IO) {
                    if (query.isBlank() && call.payload().getClaim("role").asString() == "CUSTOMER") {
                        store.recordShopVisit(shopId, call.userId())
                    }
                    store.catalogSections(shopId, query, preview)
                },
            )
        }
        get("/shops/{id}/products") {
            val shopId = call.parameters["id"]!!
            val category = call.parameters["category"]?.takeIf { it.isNotBlank() }
            val query = call.parameters["query"].orEmpty()
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val size = call.parameters["size"]?.toIntOrNull() ?: 10
            call.respond(withContext(Dispatchers.IO) { store.catalogPage(shopId, category, query, page, size) })
        }
        get("/shops/{id}/products/{productId}") {
            val shopId = call.parameters["id"]!!
            val productId = call.parameters["productId"]!!
            call.respond(withContext(Dispatchers.IO) { store.shopProductDetail(shopId, productId) })
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

        get("/shop/payment-info") {
            call.respond(withContext(Dispatchers.IO) { store.shopPaymentInfo(call.requireShopId()) })
        }
        get("/shop/orders") {
            call.respond(withContext(Dispatchers.IO) { store.shopOrders(call.requireShopId()) })
        }
        post("/shop/orders/{id}/accept") {
            call.respond(store.acceptShopOrder(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!))
        }
        post("/shop/orders/{id}/reject") {
            val body = call.receive<ShopActionRequest>()
            call.respond(store.rejectShopOrder(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body.rejectedProductIds, body.reason))
        }
        post("/shop/orders/{id}/status") {
            val body = call.receive<StatusPatch>()
            call.respond(store.setStatus(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body.status))
        }
        post("/shop/orders/{id}/delivery") {
            val body = runCatching { call.receive<DeliveryRequest>() }.getOrNull()
            call.respond(store.requestDelivery(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body?.packedProductIds))
        }
        post("/shop/orders/{id}/self-delivery") {
            val body = runCatching { call.receive<DeliveryRequest>() }.getOrNull()
            call.respond(store.startSelfDelivery(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body?.packedProductIds))
        }
        post("/shop/orders/{id}/self-delivery/complete") {
            val body = call.receive<DeliverRequest>()
            call.respond(store.completeSelfDelivery(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body.otp))
        }
        post("/shop/orders/{id}/cancel") {
            val body = call.receive<ShopActionRequest>()
            call.respond(store.cancelShopOrder(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body.reason))
        }

        get("/shop/profile") {
            call.requireShopId()
            call.respond(withContext(Dispatchers.IO) { store.shopProfile(call.userId()) })
        }
        put("/shop/profile") {
            call.requireShopId()
            val body = call.receive<AdminShopPatch>()
            call.respond(withContext(Dispatchers.IO) { store.updateShopProfile(call.userId(), body) })
        }
        get("/shop/staff") {
            call.requireShopId()
            call.respond(withContext(Dispatchers.IO) { store.listStaff(call.userId()) })
        }
        post("/shop/staff") {
            call.requireShopId()
            val body = call.receive<ShopStaffUpsert>()
            call.respond(withContext(Dispatchers.IO) { store.upsertStaff(call.userId(), null, body) })
        }
        put("/shop/staff/{id}") {
            call.requireShopId()
            val body = call.receive<ShopStaffUpsert>()
            call.respond(withContext(Dispatchers.IO) { store.upsertStaff(call.userId(), call.parameters["id"]!!, body) })
        }
        delete("/shop/staff/{id}") {
            call.requireShopId()
            withContext(Dispatchers.IO) { store.deleteStaff(call.userId(), call.parameters["id"]!!) }
            call.respond(OkResponse())
        }
        get("/shop/offers") {
            call.requireShopId()
            val all = call.parameters["all"] == "true"
            call.respond(withContext(Dispatchers.IO) { store.shopOffers(call.userId(), includeInactive = all) })
        }
        post("/shop/offers") {
            call.requireShopId()
            val body = call.receive<ShopOfferDto>()
            call.respond(withContext(Dispatchers.IO) { store.saveOffer(call.userId(), null, body) })
        }
        put("/shop/offers/{id}") {
            call.requireShopId()
            val body = call.receive<ShopOfferDto>()
            call.respond(withContext(Dispatchers.IO) { store.saveOffer(call.userId(), call.parameters["id"]!!, body) })
        }
        delete("/shop/offers/{id}") {
            call.requireShopId()
            withContext(Dispatchers.IO) { store.deleteOffer(call.userId(), call.parameters["id"]!!) }
            call.respond(OkResponse())
        }
        get("/shop/ratings") {
            call.requireShopId()
            call.respond(withContext(Dispatchers.IO) { store.shopRatingsForStaff(call.userId()) })
        }
        post("/shop/ratings/{orderId}/reply") {
            call.requireShopId()
            val body = call.receive<RatingReplyRequest>()
            call.respond(withContext(Dispatchers.IO) { store.replyToRatingAsStaff(call.userId(), call.parameters["orderId"]!!, body.reply) })
        }
        get("/shop/visitors") {
            call.requireShopId()
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(withContext(Dispatchers.IO) { store.visitorStats(call.userId(), from, to) })
        }
        get("/sync/pull") { call.respond(store.pullSync(call.requireShopId())) }
        post("/sync/push") {
            val shopId = call.requireShopId()
            val body = call.receive<SyncPushRequest>()
            call.respond(withContext(Dispatchers.IO) { store.pushSync(shopId, body) })
        }
        get("/sync/orders") {
            val shopId = call.requireShopId()
            val since = call.parameters["since"]?.toLongOrNull() ?: -1L
            val afterId = call.parameters["afterId"].orEmpty()
            val limit = call.parameters["limit"]?.toIntOrNull() ?: 200
            call.respond(withContext(Dispatchers.IO) { store.posOrdersSince(shopId, since, afterId, limit) })
        }

        get("/partners/dashboard") {
            val from = call.parameters["from"]?.toLongOrNull() ?: startOfToday()
            val to = call.parameters["to"]?.toLongOrNull() ?: (from + 86_400_000)
            call.respond(store.partnerDashboard(call.userId(), from, to))
        }
        get("/partners/profile") {
            call.respond(withContext(Dispatchers.IO) { store.partnerProfile(call.userId()) })
        }
        post("/partners/profile/photo") {
            val body = call.receive<PartnerProfilePhotoRequest>()
            withContext(Dispatchers.IO) {
                store.updatePartnerProfilePhoto(call.userId(), body.profilePhotoUrl)
            }
            call.respond(OkResponse())
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
            call.respond(store.submitPickup(call.userId(), call.parameters["id"]!!, body.photoOneUrl, body.photoTwoUrl))
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
                    body?.photoOneUrl.orEmpty(),
                    body?.photoTwoUrl.orEmpty(),
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
                    loginUsername = body.loginUsername,
                    loginPassword = body.loginPassword,
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
                loginUsername = body.loginUsername,
                loginPassword = body.loginPassword,
                upiId = body.upiId,
                deliveryRadiusKm = body.deliveryRadiusKm,
                discountPercent = body.discountPercent,
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
        get("/master/products/item/{id}") {
            call.respond(store.masterProduct(call.parameters["id"]!!))
        }
        get("/master/products/page") {
            val params = call.request.queryParameters
            call.respond(
                store.masterProductPage(
                    query = params["q"].orEmpty(),
                    categoryId = params["categoryId"],
                    page = params["page"]?.toIntOrNull() ?: 0,
                    pageSize = params["pageSize"]?.toIntOrNull() ?: 30,
                ),
            )
        }
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
            call.respond(receiveUpload(call, store))
        }
        post("/uploads") {
            call.respond(receiveUpload(call, store))
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

private fun ApplicationCall.requireShopPerm(store: AppStore, permission: String): String {
    val shopId = requireShopId()
    if (store.requireShopPermission(userId(), permission) != shopId) error("Forbidden")
    return shopId
}

private fun ApplicationCall.requireRole(role: String) {
    if (payload().getClaim("role").asString() != role) error("Forbidden")
}

private suspend fun receiveUpload(call: ApplicationCall, store: AppStore): org.bhargav.pansariwala.server.dto.UploadResultDto {
    val prefix = call.request.queryParameters["prefix"] ?: "master/product-images"
    val multipart = call.receiveMultipart()
    var fileName = "upload.bin"
    var bytes: ByteArray? = null
    var thumbnail: ByteArray? = null
    var contentType = "application/octet-stream"
    while (true) {
        val part = multipart.readPart() ?: break
        if (part is PartData.FileItem && part.name == "thumbnail") {
            thumbnail = part.provider().readRemaining().readByteArray()
            part.dispose()
        } else if (part is PartData.FileItem) {
            fileName = part.originalFileName ?: fileName
            contentType = part.contentType?.toString() ?: contentType
            bytes = part.provider().readRemaining().readByteArray()
            part.dispose()
        } else {
            part.dispose()
        }
    }
    val data = bytes ?: error("file required")
    return store.uploadAsset(prefix, fileName, data, contentType, thumbnail)
}

private fun startOfToday(): Long {
    val now = System.currentTimeMillis()
    return now - (now % 86_400_000)
}
