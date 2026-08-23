package org.bhargav.pansariwala.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.DeliveryOffer
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.PartnerDashboard
import org.bhargav.pansariwala.domain.model.PartnerEarnings
import org.bhargav.pansariwala.domain.model.PartnerProfile
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.util.AppConstants

class KtorPansariApi(
    engineClient: HttpClient,
    private val baseUrl: String,
    private val preferences: AppPreferences,
) : PansariApi {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = engineClient.config {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = AppConstants.HTTP_CONNECT_TIMEOUT_MS
            requestTimeoutMillis = AppConstants.HTTP_REQUEST_TIMEOUT_MS
            socketTimeoutMillis = AppConstants.HTTP_SOCKET_TIMEOUT_MS
        }
        install(ContentNegotiation) { json(json) }
        install(Logging) { level = LogLevel.HEADERS }
        install(Auth) {
            bearer {
                loadTokens { currentJwtTokens() }
                refreshTokens { currentJwtTokens() }
                sendWithoutRequest { request ->
                    val path = request.url.pathSegments.joinToString("/")
                    val public = path.contains("auth/") ||
                        path.contains("config/public") ||
                        path.endsWith("health") ||
                        path.contains("partners/register")
                    !public
                }
            }
        }
        defaultRequest {
            url(baseUrl.trimEnd('/') + "/")
            contentType(ContentType.Application.Json)
        }
    }.also { http ->
        JwtAuthCache.onInvalidate = {
            http.authProviders
                .filterIsInstance<BearerAuthProvider>()
                .forEach { it.clearToken() }
        }
    }

    private suspend fun currentJwtTokens(): BearerTokens? {
        val token = preferences.getAccessToken() ?: return null
        if (!token.startsWith(AppConstants.JWT_PREFIX)) return null
        return BearerTokens(accessToken = token, refreshToken = token)
    }

    override suspend fun publicConfig(): PublicConfigDto =
        client.get("config/public").body()

    override suspend fun shopLogin(username: String, password: String): TokenResponse =
        client.post("auth/shop/login") { setBody(ShopLoginRequest(username, password)) }.body()

    override suspend fun loginWithFirebase(idToken: String): TokenResponse =
        client.post("auth/user/firebase") { setBody(FirebaseAuthRequest(idToken)) }.body()

    override suspend fun requestOtp(phone: String): OtpSessionResponse =
        client.post("auth/otp/request") { setBody(OtpRequest(phone)) }.body()

    override suspend fun verifyOtp(phone: String, otp: String, sessionId: String?): TokenResponse =
        client.post("auth/otp/verify") { setBody(OtpVerifyRequest(phone, otp, sessionId)) }.body()

    override suspend fun updateProfile(
        name: String,
        address: String,
        locality: String?,
        lat: Double?,
        lng: Double?,
    ): CustomerProfile =
        client.put("me/profile") {
            setBody(UpdateProfileRequest(name, address, locality, lat, lng))
        }.body<CustomerDto>().toModel()

    override suspend fun saveAddress(line: String, locality: String, lat: Double, lng: Double): CustomerProfile =
        client.post("me/addresses") {
            setBody(SaveAddressRequest(line, locality, lat, lng))
        }.body<CustomerDto>().toModel()

    override suspend fun selectAddress(addressId: String): CustomerProfile =
        client.post("me/addresses/$addressId/select").body<CustomerDto>().toModel()

    override suspend fun updateCustomerLocation(lat: Double, lng: Double) {
        client.post("me/location") { setBody(CustomerLocationRequest(lat, lng)) }
    }

    override suspend fun me(): CustomerProfile =
        client.get("me").body<CustomerDto>().toModel()

    override suspend fun nearbyShops(lat: Double, lng: Double, radiusKm: Double, query: String): List<MarketplaceShop> =
        client.get("shops") {
            parameter("lat", lat)
            parameter("lng", lng)
            parameter("radiusKm", radiusKm)
            parameter("query", query)
        }.body<List<ShopDto>>().map { it.toModel() }

    override suspend fun shopCatalog(shopId: String): List<Product> =
        client.get("shops/$shopId/catalog").body<List<ProductDto>>().map { it.toModel() }

    override suspend fun shopOffers(shopId: String): List<ShopOffer> =
        client.get("shops/$shopId/offers").body<List<OfferDto>>().map { it.toModel() }

    override suspend fun quote(request: QuoteRequest): QuoteDto =
        client.post("orders/quote") { setBody(request) }.body()

    override suspend fun validateOrder(request: PlaceOrderRequest) {
        client.post("orders/validate") { setBody(request) }.body<OkResponse>()
    }

    override suspend fun createRazorpayOrder(shopId: String, amountPaise: Long): RazorpayOrderDto =
        client.post("payments/razorpay/order") {
            setBody(CreateRazorpayRequest(shopId, amountPaise))
        }.body()

    override suspend fun verifyPayment(request: VerifyPaymentRequest): Boolean {
        val body = client.post("payments/razorpay/verify") { setBody(request) }.body<OkResponse>()
        return body.ok
    }

    override suspend fun placeOrder(request: PlaceOrderRequest): Order =
        client.post("orders") { setBody(request) }.body<OrderDto>().toModel()

    override suspend fun myOrders(): List<Order> =
        client.get("orders/mine").body<List<OrderDto>>().map { it.toModel() }

    override suspend fun order(orderId: String): Order =
        client.get("orders/$orderId").body<OrderDto>().toModel()

    override suspend fun myTransactions(): List<MoneyTxn> =
        client.get("me/transactions").body<List<TxnDto>>().map { it.toModel() }

    override suspend fun rateOrder(orderId: String, stars: Int, comment: String?): Order =
        client.post("orders/$orderId/rating") { setBody(RateOrderRequest(stars, comment)) }
            .body<OrderDto>().toModel()

    override suspend fun shopOnlineOrders(): List<Order> =
        client.get("shop/orders").body<List<OrderDto>>().map { it.toModel() }

    override suspend fun acceptOrder(orderId: String): Order =
        client.post("shop/orders/$orderId/accept").body<OrderDto>().toModel()

    override suspend fun rejectOrder(orderId: String, rejectedProductIds: List<String>, reason: String?): Order =
        client.post("shop/orders/$orderId/reject") {
            setBody(ShopActionRequest(rejectedProductIds, reason))
        }.body<OrderDto>().toModel()

    override suspend fun setOrderStatus(orderId: String, status: String): Order =
        client.post("shop/orders/$orderId/status") { setBody(StatusPatch(status)) }
            .body<OrderDto>().toModel()

    override suspend fun requestDelivery(orderId: String): DeliveryOffer =
        client.post("shop/orders/$orderId/delivery").body<DeliveryOfferDto>().toModel()

    override suspend fun cancelShopOrder(orderId: String, reason: String?): Order =
        client.post("shop/orders/$orderId/cancel") {
            setBody(ShopActionRequest(reason = reason))
        }.body<OrderDto>().toModel()

    override suspend fun registerPartner(request: PartnerRegisterRequest): String =
        client.post("partners/register") { setBody(request) }.body<OtpSessionResponse>().sessionId

    override suspend fun partnerProfile(): PartnerProfile =
        client.get("partners/profile").body<PartnerProfileDto>().toModel()

    override suspend fun partnerDashboard(fromEpochMs: Long, toEpochMs: Long): PartnerDashboard =
        client.get("partners/dashboard") {
            parameter("from", fromEpochMs)
            parameter("to", toEpochMs)
        }.body<PartnerDashboardDto>().toModel()

    override suspend fun partnerEarnings(): PartnerEarnings =
        client.get("partners/earnings").body<PartnerEarningsDto>().toModel()

    override suspend fun setPartnerOnline(online: Boolean) {
        client.post("partners/online") { setBody(PartnerOnlineRequest(online)) }
    }

    override suspend fun updatePartnerLocation(lat: Double, lng: Double) {
        client.post("partners/location") { setBody(PartnerLocationRequest(lat, lng)) }
    }

    override suspend fun incomingOffer(): DeliveryOffer? =
        client.get("partners/offers/incoming").body<IncomingOfferResponse>().offer?.toModel()

    override suspend fun availableOffers(): List<DeliveryOffer> =
        client.get("partners/offers/available").body<List<DeliveryOfferDto>>().map { it.toModel() }

    override suspend fun acceptOffer(offerId: String): DeliveryOffer =
        client.post("partners/offers/$offerId/accept").body<DeliveryOfferDto>().toModel()

    override suspend fun rejectOffer(offerId: String): DeliveryOffer =
        client.post("partners/offers/$offerId/reject").body<DeliveryOfferDto>().toModel()

    override suspend fun partnerJob(orderId: String): Order =
        client.get("partners/jobs/$orderId").body<OrderDto>().toModel()

    override suspend fun acceptedJobs(): List<Order> =
        client.get("partners/jobs/accepted").body<List<OrderDto>>().map { it.toModel() }

    override suspend fun deliveredJobs(fromEpochMs: Long, toEpochMs: Long): List<Order> =
        client.get("partners/jobs/delivered") {
            parameter("from", fromEpochMs)
            parameter("to", toEpochMs)
        }.body<List<OrderDto>>().map { it.toModel() }

    override suspend fun cancelPickup(orderId: String): Order =
        client.post("partners/jobs/$orderId/cancel").body<OrderDto>().toModel()

    override suspend fun arrivedAtStore(orderId: String): Order =
        client.post("partners/jobs/$orderId/arrived-store").body<OrderDto>().toModel()

    override suspend fun submitPickup(orderId: String, photoOne: String, photoTwo: String): Order =
        client.post("partners/jobs/$orderId/pickup") {
            setBody(PickupRequest(photoOne, photoTwo))
        }.body<OrderDto>().toModel()

    override suspend fun arrivedAtCustomer(orderId: String): Order =
        client.post("partners/jobs/$orderId/arrived-customer").body<OrderDto>().toModel()

    override suspend fun deliverOrder(orderId: String, otp: String): Order =
        client.post("partners/jobs/$orderId/deliver") {
            setBody(DeliverRequest(otp))
        }.body<OrderDto>().toModel()

    override suspend fun pullSync(): SyncPullResponse = client.get("sync/pull").body()

    override suspend fun pushSync(request: SyncPushRequest) {
        client.post("sync/push") { setBody(request) }
    }
}
