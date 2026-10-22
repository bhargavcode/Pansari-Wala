package org.bhargav.pansariwala.server.routing

import com.auth0.jwt.interfaces.Payload
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.PartData
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import org.bhargav.pansariwala.server.service.ProductSheet
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.dto.PaymentIntentDto
import org.bhargav.pansariwala.server.dto.VendorIdRequest
import kotlinx.io.readByteArray
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.dto.AdminLoginRequest
import org.bhargav.pansariwala.server.dto.ShopCategoryOrderDto
import org.bhargav.pansariwala.server.dto.AdminOrderActionRequest
import org.bhargav.pansariwala.server.dto.AdminPasswordChange
import org.bhargav.pansariwala.server.dto.PlatformSettingsDto
import org.bhargav.pansariwala.server.dto.SubmitUtrRequest
import org.bhargav.pansariwala.server.dto.AdminPartnerPatch
import org.bhargav.pansariwala.server.dto.AdminShopCreate
import org.bhargav.pansariwala.server.dto.AdminShopPatch
import org.bhargav.pansariwala.server.dto.AdminUserPatch
import org.bhargav.pansariwala.server.dto.AddFromMasterRequest
import org.bhargav.pansariwala.server.dto.ShopProductActivePatch
import org.bhargav.pansariwala.server.dto.ShopProductEdit
import org.bhargav.pansariwala.server.dto.StockUpdateRequest
import org.bhargav.pansariwala.server.dto.CreateRazorpayRequest
import org.bhargav.pansariwala.server.dto.AdminBillingUpdate
import org.bhargav.pansariwala.server.dto.BillingSettingsDto
import org.bhargav.pansariwala.server.dto.SubscriptionCheckoutRequest
import org.bhargav.pansariwala.server.dto.SubscriptionConfirmRequest
import org.bhargav.pansariwala.server.dto.SubscriptionPlanDto
import org.bhargav.pansariwala.server.dto.CustomerLocationRequest
import org.bhargav.pansariwala.server.dto.DeliverRequest
import org.bhargav.pansariwala.server.dto.DeliveryRequest
import org.bhargav.pansariwala.server.dto.FirebaseAuthRequest
import org.bhargav.pansariwala.server.dto.IncomingOfferResponse
import org.bhargav.pansariwala.server.dto.OkResponse
import org.bhargav.pansariwala.server.dto.OtpRequest
import org.bhargav.pansariwala.server.dto.OtpVerifyRequest
import org.bhargav.pansariwala.server.security.OtpAppProof
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
import org.bhargav.pansariwala.server.dto.ShopPartnerRequest
import org.bhargav.pansariwala.server.dto.AssignShopPartnerRequest
import org.bhargav.pansariwala.server.service.ShopPerm
import org.bhargav.pansariwala.server.dto.SaveAddressRequest
import org.bhargav.pansariwala.server.dto.ChangePhoneRequest
import org.bhargav.pansariwala.server.dto.CustomerPhotoRequest
import org.bhargav.pansariwala.server.dto.ShopActionRequest
import org.bhargav.pansariwala.server.dto.ShopLoginRequest
import org.bhargav.pansariwala.server.dto.ReturnRequest
import org.bhargav.pansariwala.server.dto.StatusPatch
import org.bhargav.pansariwala.server.dto.SyncPushRequest
import org.bhargav.pansariwala.server.dto.UpdateProfileRequest
import org.bhargav.pansariwala.server.dto.VerifyPaymentRequest
import org.bhargav.pansariwala.server.service.AppStore
import org.bhargav.pansariwala.server.service.PushService
import org.bhargav.pansariwala.server.service.SupportService
import org.bhargav.pansariwala.server.dto.AppInstallRequest
import org.bhargav.pansariwala.server.dto.IssueReportCreate
import org.bhargav.pansariwala.server.dto.IssueResolveRequest

private const val PAYMENT_RETURN_HTML = """<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Pansari Wala</title></head>
<body style="font-family:sans-serif;text-align:center;padding:48px 16px">
<h2>Payment submitted</h2><p>You can close this page and return to the Pansari Wala app.</p></body></html>"""

/** User app deep link (`pansariwala://payment/<intentId>`), registered in the user Android/iOS apps. */
private const val APP_PAYMENT_LINK = "pansariwala://payment/"

private fun paymentReturnHtml(intentId: String): String {
    val link = APP_PAYMENT_LINK + intentId
    return """<!doctype html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>Pansari Wala</title></head>
<body style="font-family:sans-serif;text-align:center;padding:48px 16px">
<h2>Returning to Pansari Wala…</h2><p>Your payment status will be shown in the app.</p>
<p><a href="$link" style="display:inline-block;padding:14px 28px;background:#2e7d32;color:#fff;border-radius:24px;text-decoration:none;font-weight:bold">Open Pansari Wala</a></p>
<script>window.location.replace("$link");</script></body></html>"""
}

fun Route.apiRoutes(config: ServerConfig, store: AppStore, push: PushService, support: SupportService) {
    get("/health") { call.respond(OkResponse(true)) }
    imageProxyRoutes()
    get("/config/public") {
        call.respond(
            PublicConfigDto(
                razorpayKeyId = config.razorpayKeyId,
                paymentsEnabled = config.paymentsEnabled,
                devAuth = config.devAuth,
                minUserSearchRadiusKm = store.platformSettings().minUserSearchRadiusKm,
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
        val ip = call.clientIp()
        val proof = OtpAppProof(body.playIntegrityToken, body.recaptchaToken, body.androidPackage, body.androidCertSha1)
        call.respond(withContext(Dispatchers.IO) { store.requestOtp(body.phone, ip, proof) })
    }
    get("/auth/otp/config") {
        call.respond(withContext(Dispatchers.IO) { store.otpConfig() })
    }
    post("/auth/otp/verify") {
        val body = call.receive<OtpVerifyRequest>()
        call.respond(withContext(Dispatchers.IO) { store.verifyOtp(body.phone, body.otp, body.sessionId) })
    }
    post("/partners/register") {
        val body = call.receive<PartnerRegisterRequest>()
        call.respond(withContext(Dispatchers.IO) { store.registerPartner(body) })
    }
    // Public: called after logout/session expiry when no JWT remains; the FCM token itself identifies the device.
    post("/push/unregister") {
        val body = call.receive<PushUnregisterRequest>()
        push.unregister(body.token)
        call.respond(OkResponse())
    }
    // Public: the sample has no shop data; exports are opened through a one-time token from an authenticated call.
    get("/files/product-template.xlsx") {
        call.respondSheet("pansari-products-sample.xlsx", ProductSheet.template())
    }
    get("/files/products-export/{token}") {
        val bytes = withContext(Dispatchers.IO) { store.consumeExportLink(call.parameters["token"]!!) }
        if (bytes == null) call.respond(HttpStatusCode.NotFound, OkResponse(ok = false))
        else call.respondSheet("pansari-products.xlsx", bytes)
    }

    // Public: Cashfree server-to-server callback (HMAC-signed) and the browser page after paying.
    post("/payments/cashfree/webhook") {
        val raw = call.receiveText()
        val ok = store.handleCashfreeWebhook(
            timestamp = call.request.headers["x-webhook-timestamp"].orEmpty(),
            rawBody = raw,
            signature = call.request.headers["x-webhook-signature"].orEmpty(),
        )
        call.respond(if (ok) HttpStatusCode.OK else HttpStatusCode.Unauthorized, OkResponse(ok))
    }
    get("/payments/cashfree/return") {
        val intentId = call.request.queryParameters["order_id"]?.let { store.cashfreeReturn(it) }
        call.respondText(intentId?.let(::paymentReturnHtml) ?: PAYMENT_RETURN_HTML, ContentType.Text.Html)
    }

    authenticate("auth-jwt") {
        post("/shop/products/import") {
            val shopId = call.requireShopPerm(store, ShopPerm.INVENTORY)
            val dryRun = call.request.queryParameters["dryRun"] == "true"
            val (name, bytes) = receiveSheet(call)
            call.respond(withContext(Dispatchers.IO) { store.importShopProducts(shopId, bytes, name, dryRun) })
        }
        post("/shop/products/export-link") {
            val shopId = call.requireShopPerm(store, ShopPerm.INVENTORY)
            call.respond(store.createExportLink(shopId))
        }
        get("/shop/category-order") {
            val shopId = call.requireShopPerm(store, ShopPerm.INVENTORY)
            call.respond(withContext(Dispatchers.IO) { store.shopCategoryOrder(shopId) })
        }
        put("/shop/category-order") {
            val shopId = call.requireShopPerm(store, ShopPerm.INVENTORY)
            val body = call.receive<ShopCategoryOrderDto>()
            call.respond(withContext(Dispatchers.IO) { store.updateShopCategoryOrder(shopId, body) })
        }
        post("/admin/master/products/import") {
            call.requireRole("ADMIN")
            val dryRun = call.request.queryParameters["dryRun"] == "true"
            val (name, bytes) = receiveSheet(call)
            call.respond(withContext(Dispatchers.IO) { store.importMasterProducts(bytes, name, dryRun) })
        }
        post("/admin/master/products/export-link") {
            call.requireRole("ADMIN")
            call.respond(store.createExportLink(null))
        }
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
        post("/app/install") {
            val body = call.receive<AppInstallRequest>()
            val payload = call.payload()
            support.recordInstall(
                principalId = payload.subject,
                role = payload.getClaim("role").asString().orEmpty(),
                shopId = payload.getClaim("shopId").asString(),
                body = body,
            )
            call.respond(OkResponse())
        }
        post("/issues") {
            val body = call.receive<IssueReportCreate>()
            val payload = call.payload()
            call.respond(
                support.createIssue(
                    principalId = payload.subject,
                    role = payload.getClaim("role").asString().orEmpty(),
                    shopId = payload.getClaim("shopId").asString(),
                    body = body,
                ),
            )
        }
        get("/issues/mine") {
            call.respond(support.myIssues(call.userId(), call.request.queryParameters["status"]))
        }
        get("/admin/issues") {
            call.requireRole("ADMIN")
            val params = call.request.queryParameters
            call.respond(support.adminIssues(params["status"], params["app"]))
        }
        get("/admin/issues/counts") {
            call.requireRole("ADMIN")
            call.respond(support.issueCounts())
        }
        post("/admin/issues/{id}/resolve") {
            call.requireRole("ADMIN")
            call.respond(support.resolveIssue(call.parameters["id"]!!, call.receive<IssueResolveRequest>()))
        }
        post("/admin/issues/{id}/reopen") {
            call.requireRole("ADMIN")
            call.respond(support.reopenIssue(call.parameters["id"]!!))
        }
        get("/admin/app-usage") {
            call.requireRole("ADMIN")
            val params = call.request.queryParameters
            call.respond(support.usageReport(params["app"], params["platform"], params["version"], params["paymentMethod"]))
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
        put("/me/photo") {
            val body = call.receive<CustomerPhotoRequest>()
            call.respond(store.updateCustomerPhoto(call.userId(), body.imageUrl))
        }
        post("/me/phone") {
            val body = call.receive<ChangePhoneRequest>()
            call.respond(withContext(Dispatchers.IO) { store.changeCustomerPhone(call.userId(), body.phone, body.otp, body.sessionId) })
        }
        get("/me/spend") {
            val from = call.parameters["from"]?.toLongOrNull() ?: error("from required")
            val to = call.parameters["to"]?.toLongOrNull() ?: error("to required")
            call.respond(withContext(Dispatchers.IO) { store.customerSpend(call.userId(), from, to) })
        }
        post("/me/deletion") {
            call.respond(store.requestAccountDeletion(call.userId()))
        }
        delete("/me/deletion") {
            call.respond(store.cancelAccountDeletion(call.userId()))
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
            val categoryId = call.parameters["categoryId"]?.takeIf { it.isNotBlank() }
            val query = call.parameters["query"].orEmpty()
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val size = call.parameters["size"]?.toIntOrNull() ?: 10
            call.respond(withContext(Dispatchers.IO) { store.catalogPage(shopId, categoryId, query, page, size) })
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
        get("/orders/mine/page") {
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val size = call.parameters["size"]?.toIntOrNull() ?: 20
            call.respond(withContext(Dispatchers.IO) { store.customerOrdersPage(call.userId(), page, size) })
        }
        get("/orders/{id}") { call.respond(store.getOrder(call.parameters["id"]!!)) }
        post("/payments/intents") {
            call.respond(store.createPaymentIntent(call.userId(), call.receive<PlaceOrderRequest>()))
        }
        get("/payments/intents") {
            call.respond(withContext(Dispatchers.IO) { store.customerPaymentIntents(call.userId()) })
        }
        get("/payments/intents/{id}") {
            call.respond(withContext(Dispatchers.IO) { store.paymentIntent(call.userId(), call.parameters["id"]!!) })
        }
        post("/payments/intents/{id}/utr") {
            val body = call.receive<SubmitUtrRequest>()
            call.respond(withContext(Dispatchers.IO) { store.submitIntentUtr(call.userId(), call.parameters["id"]!!, body.utr) })
        }
        post("/payments/intents/{id}/refresh") {
            call.respond(store.refreshPaymentIntent(call.userId(), call.parameters["id"]!!))
        }
        post("/orders/{id}/rating") {
            val body = call.receive<RateOrderRequest>()
            call.respond(store.rateOrder(call.userId(), call.parameters["id"]!!, body.stars, body.comment))
        }

        get("/shop/payment-info") {
            call.respond(withContext(Dispatchers.IO) { store.shopPaymentInfo(call.requireShopId()) })
        }
        get("/shop/orders") {
            val from = call.request.queryParameters["from"]?.toLongOrNull()
            val to = call.request.queryParameters["to"]?.toLongOrNull()
            call.respond(withContext(Dispatchers.IO) { store.shopOrders(call.requireShopId(), from, to) })
        }
        get("/shop/partners") {
            call.respond(withContext(Dispatchers.IO) { store.shopPartners(call.requireShopId()) })
        }
        post("/shop/partners") {
            val shopId = call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS)
            val body = call.receive<ShopPartnerRequest>()
            call.respond(withContext(Dispatchers.IO) { store.addShopPartner(shopId, body) })
        }
        delete("/shop/partners/{id}") {
            val shopId = call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS)
            withContext(Dispatchers.IO) { store.removeShopPartner(shopId, call.parameters["id"]!!) }
            call.respond(OkResponse())
        }
        post("/shop/orders/{id}/assign-partner") {
            val shopId = call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS)
            val body = call.receive<AssignShopPartnerRequest>()
            call.respond(
                withContext(Dispatchers.IO) {
                    store.assignShopPartner(shopId, call.parameters["id"]!!, body.shopPartnerId, body.packedProductIds)
                },
            )
        }
        get("/shop/orders/page") {
            val shopId = call.requireShopId()
            val statuses = call.parameters["status"].orEmpty()
                .split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            val query = call.parameters["query"].orEmpty()
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val size = call.parameters["size"]?.toIntOrNull() ?: 20
            call.respond(withContext(Dispatchers.IO) { store.shopOrdersPage(shopId, statuses, query, page, size) })
        }
        get("/shop/payment-intents") {
            call.respond(withContext(Dispatchers.IO) { store.shopPaymentIntents(call.requireShopId()) })
        }
        post("/shop/payment-intents/{id}/confirm") {
            val shopId = call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS)
            call.respond(store.shopConfirmIntent(shopId, call.parameters["id"]!!))
        }
        post("/shop/payment-intents/{id}/reject") {
            val shopId = call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS)
            call.respond(withContext(Dispatchers.IO) { store.shopRejectIntent(shopId, call.parameters["id"]!!) })
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
        post("/shop/orders/{id}/accept-return") {
            val body = runCatching { call.receive<ReturnRequest>() }.getOrNull()
            call.respond(store.acceptReturn(call.requireShopPerm(store, ShopPerm.ONLINE_ORDERS), call.parameters["id"]!!, body?.reason))
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
        get("/shop/customers/{customerId}/ratings") {
            val shopId = call.requireShopId()
            val stars = call.parameters["stars"].orEmpty()
                .split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..5 }.toSet()
            val page = call.parameters["page"]?.toIntOrNull() ?: 0
            val size = call.parameters["size"]?.toIntOrNull() ?: 20
            call.respond(
                withContext(Dispatchers.IO) {
                    store.customerRatingsForShop(shopId, call.parameters["customerId"]!!, stars, page, size)
                },
            )
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
        get("/shop/subscription") {
            val shopId = call.requireShopId()
            call.respond(withContext(Dispatchers.IO) { store.billing.shopSubscription(shopId) })
        }
        post("/shop/subscription/checkout") {
            val shopId = call.requireShopPerm(store, ShopPerm.SHOP_PROFILE)
            call.respond(store.billing.checkout(shopId, call.receive<SubscriptionCheckoutRequest>()))
        }
        post("/shop/subscription/confirm") {
            val shopId = call.requireShopPerm(store, ShopPerm.SHOP_PROFILE)
            val body = call.receive<SubscriptionConfirmRequest>()
            call.respond(withContext(Dispatchers.IO) { store.billing.confirm(shopId, call.userId(), body) })
        }
        get("/shop/subscription/commission") {
            val shopId = call.requireShopId()
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(withContext(Dispatchers.IO) { store.billing.commissionSummary(shopId, from, to) })
        }
        get("/shop/subscription/commission/{day}") {
            val shopId = call.requireShopId()
            call.respond(withContext(Dispatchers.IO) { store.billing.commissionDay(shopId, call.parameters["day"]!!) })
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
            val from = call.request.queryParameters["from"]?.toLongOrNull()
            val to = call.request.queryParameters["to"]?.toLongOrNull()
            call.respond(withContext(Dispatchers.IO) { store.partnerEarnings(call.userId(), from, to) })
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
        post("/partners/jobs/{id}/return-to-shop") {
            val body = runCatching { call.receive<ReturnRequest>() }.getOrNull()
            call.respond(store.returnToShop(call.userId(), call.parameters["id"]!!, body?.reason))
        }
        post("/partners/jobs/{id}/deliver") {
            val body = call.receive<DeliverRequest>()
            call.respond(store.deliver(call.userId(), call.parameters["id"]!!, body.otp))
        }
        post("/partners/jobs/{id}/customer-rating") {
            val body = call.receive<RateOrderRequest>()
            call.respond(
                withContext(Dispatchers.IO) {
                    store.rateCustomer(call.userId(), call.parameters["id"]!!, body.stars, body.comment)
                },
            )
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
        get("/admin/shops/{id}/products") {
            call.requireRole("ADMIN")
            val params = call.request.queryParameters
            call.respond(
                store.adminShopProductPage(
                    shopId = call.parameters["id"]!!,
                    query = params["q"].orEmpty(),
                    category = params["category"],
                    status = params["status"],
                    lowStockOnly = params["lowStock"].toBoolean(),
                    page = params["page"]?.toIntOrNull() ?: 0,
                    pageSize = params["pageSize"]?.toIntOrNull() ?: 25,
                ),
            )
        }
        get("/admin/shops/{id}/stock-summary") {
            call.requireRole("ADMIN")
            call.respond(store.adminShopStockSummary(call.parameters["id"]!!))
        }
        get("/admin/shops/{id}/master-links") {
            call.requireRole("ADMIN")
            val ids = call.request.queryParameters["ids"].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            call.respond(store.adminShopMasterLinks(call.parameters["id"]!!, ids))
        }
        post("/admin/shops/{id}/products/from-master") {
            call.requireRole("ADMIN")
            call.respond(store.adminAddShopProductFromMaster(call.parameters["id"]!!, call.receive<AddFromMasterRequest>()))
        }
        post("/admin/shops/{id}/products/{productId}") {
            call.requireRole("ADMIN")
            call.respond(
                store.adminEditShopProduct(call.parameters["id"]!!, call.parameters["productId"]!!, call.receive<ShopProductEdit>()),
            )
        }
        post("/admin/shops/{id}/products/{productId}/stock") {
            call.requireRole("ADMIN")
            call.respond(
                store.adminUpdateShopProductStock(
                    call.parameters["id"]!!,
                    call.parameters["productId"]!!,
                    call.receive<StockUpdateRequest>(),
                ),
            )
        }
        post("/admin/shops/{id}/products/{productId}/active") {
            call.requireRole("ADMIN")
            val body = call.receive<ShopProductActivePatch>()
            call.respond(store.adminSetShopProductActive(call.parameters["id"]!!, call.parameters["productId"]!!, body.active))
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
                deliveryUntil = body.deliveryUntil,
                homeDeliveryEnabled = body.homeDeliveryEnabled,
                minOrderValueInr = body.minOrderValueInr,
                freeDeliveryEnabled = body.freeDeliveryEnabled,
                freeDeliveryAboveInr = body.freeDeliveryAboveInr,
            )
            call.respond(OkResponse())
        }
        get("/admin/subscription-plans") {
            call.requireRole("ADMIN")
            call.respond(withContext(Dispatchers.IO) { store.billing.plans(includeInactive = true) })
        }
        post("/admin/subscription-plans") {
            call.requireRole("ADMIN")
            val body = call.receive<SubscriptionPlanDto>()
            call.respond(withContext(Dispatchers.IO) { store.billing.savePlan(body.id, body) })
        }
        delete("/admin/subscription-plans/{id}") {
            call.requireRole("ADMIN")
            withContext(Dispatchers.IO) { store.billing.deletePlan(call.parameters["id"]!!) }
            call.respond(OkResponse())
        }
        get("/admin/billing-settings") {
            call.requireRole("ADMIN")
            call.respond(withContext(Dispatchers.IO) { store.billing.settings() })
        }
        post("/admin/billing-settings") {
            call.requireRole("ADMIN")
            val body = call.receive<BillingSettingsDto>()
            call.respond(withContext(Dispatchers.IO) { store.billing.updateSettings(body) })
        }
        get("/admin/shops/{id}/subscription") {
            call.requireRole("ADMIN")
            call.respond(withContext(Dispatchers.IO) { store.billing.shopSubscription(call.parameters["id"]!!) })
        }
        post("/admin/shops/{id}/billing") {
            call.requireRole("ADMIN")
            val body = call.receive<AdminBillingUpdate>()
            call.respond(
                withContext(Dispatchers.IO) { store.billing.adminUpdateBilling(call.parameters["id"]!!, call.userId(), body) },
            )
        }
        get("/admin/shops/{id}/commission") {
            call.requireRole("ADMIN")
            val from = call.parameters["from"]?.toLongOrNull()
            val to = call.parameters["to"]?.toLongOrNull()
            call.respond(withContext(Dispatchers.IO) { store.billing.commissionSummary(call.parameters["id"]!!, from, to) })
        }
        get("/admin/shops/{id}/commission/{day}") {
            call.requireRole("ADMIN")
            call.respond(
                withContext(Dispatchers.IO) { store.billing.commissionDay(call.parameters["id"]!!, call.parameters["day"]!!) },
            )
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

        get("/admin/platform-settings") {
            call.requireRole("ADMIN")
            call.respond(store.platformSettings())
        }
        post("/admin/platform-settings") {
            call.requireRole("ADMIN")
            call.respond(store.savePlatformSettings(call.receive<PlatformSettingsDto>()))
        }
        post("/admin/payment-settings") {
            call.requireRole("ADMIN")
            val body = call.receive<PlatformSettingsDto>()
            call.respond(store.savePaymentSettings(body.paymentMode, body.paymentWindowMinutes))
        }
        post("/admin/shops/{id}/cashfree-vendor") {
            call.requireRole("ADMIN")
            val body = call.receive<VendorIdRequest>()
            withContext(Dispatchers.IO) { store.setShopVendorId(call.parameters["id"]!!, body.vendorId) }
            call.respond(OkResponse())
        }
        post("/admin/partners/{id}/cashfree-vendor") {
            call.requireRole("ADMIN")
            val body = call.receive<VendorIdRequest>()
            withContext(Dispatchers.IO) { store.setPartnerVendorId(call.parameters["id"]!!, body.vendorId) }
            call.respond(OkResponse())
        }
        post("/admin/account/password") {
            call.requireRole("ADMIN")
            val body = call.receive<AdminPasswordChange>()
            store.changeAdminPassword(call.userId(), body.currentPassword, body.newPassword)
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

        // Live status of one payment transaction: current state first, then every change until closed.
        webSocket("/ws/payments/{id}") {
            val intentId = call.parameters["id"]!!
            val initial = runCatching { withContext(Dispatchers.IO) { store.paymentIntent(call.userId(), intentId) } }.getOrNull()
            if (initial == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "not found"))
                return@webSocket
            }
            val json = Json { encodeDefaults = true }
            send(Frame.Text(json.encodeToString(PaymentIntentDto.serializer(), initial)))
            val relay = launch {
                store.paymentEvents.filter { it.id == intentId }.collect { dto ->
                    send(Frame.Text(json.encodeToString(PaymentIntentDto.serializer(), dto)))
                }
            }
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text && frame.readText() == "ping") send(Frame.Text("pong"))
                }
            } finally {
                relay.cancel()
            }
        }
    }
}

private fun ApplicationCall.payload(): Payload =
    principal<JWTPrincipal>()?.payload ?: error("Unauthorized")

private fun ApplicationCall.userId(): String = payload().subject

/** nginx sets X-Real-IP; X-Forwarded-For's first hop is the fallback, then the socket peer. */
private fun ApplicationCall.clientIp(): String? =
    request.headers["X-Real-IP"]?.trim()?.takeIf { it.isNotEmpty() }
        ?: request.headers["X-Forwarded-For"]?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
        ?: request.local.remoteAddress

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

private const val SHEET_MAX_BYTES = 10 * 1024 * 1024
private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

/** First multipart file part: (file name, bytes). */
private suspend fun receiveSheet(call: ApplicationCall): Pair<String, ByteArray> {
    val multipart = call.receiveMultipart()
    var result: Pair<String, ByteArray>? = null
    while (true) {
        val part = multipart.readPart() ?: break
        if (part is PartData.FileItem && result == null) {
            result = (part.originalFileName ?: "products.xlsx") to part.provider().readRemaining().readByteArray()
        }
        part.dispose()
    }
    val sheet = result ?: error("file required")
    require(sheet.second.size <= SHEET_MAX_BYTES) { "File is too large (max 10 MB)" }
    return sheet
}

private suspend fun ApplicationCall.respondSheet(fileName: String, bytes: ByteArray) {
    response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"$fileName\"")
    respondBytes(bytes, ContentType.parse(XLSX_MIME))
}

private fun startOfToday(): Long {
    val now = System.currentTimeMillis()
    return now - (now % 86_400_000)
}
