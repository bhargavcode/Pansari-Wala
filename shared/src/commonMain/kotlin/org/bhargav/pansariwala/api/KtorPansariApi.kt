package org.bhargav.pansariwala.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.DeliveryOffer
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
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
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                val response = (cause as? ResponseException)?.response ?: return@handleResponseExceptionWithRequest
                val text = runCatching { response.bodyAsText() }.getOrDefault("")
                val body = runCatching { json.decodeFromString<ApiErrorBody>(text) }.getOrNull()
                val serverMessage = body?.error?.takeIf { it.isNotBlank() } ?: response.status.description
                throw ApiException(
                    status = response.status.value,
                    serverMessage = serverMessage,
                    code = body?.code?.takeIf { it.isNotBlank() },
                    retryAfterSec = response.headers["Retry-After"]?.trim()?.toIntOrNull(),
                )
            }
        }
        if (shouldInstallHttpLogging()) {
            install(Logging) { installPansariHttpLogging() }
        }
        install(Auth) {
            bearer {
                loadTokens { currentJwtTokens() }
                refreshTokens {
                    // No refresh-token API: a 401 means the JWT is unusable.
                    SessionExpiredBus.onRefreshFailed(preferences)
                    null
                }
                sendWithoutRequest { request ->
                    val path = request.url.pathSegments.joinToString("/")
                    val public = path.contains("auth/") ||
                        path.contains("config/public") ||
                        path.endsWith("health") ||
                        path.contains("partners/register") ||
                        path.contains("push/unregister") ||
                        path.contains("uploads/guest")
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

    override suspend fun warmUp() {
        runCatching { client.get(AppConstants.API_HEALTH_PATH) }
    }

    override suspend fun publicConfig(): PublicConfigDto =
        client.get("config/public").body()

    override suspend fun shopLogin(username: String, password: String): TokenResponse =
        client.post("auth/shop/login") { setBody(ShopLoginRequest(username, password)) }.body()

    override suspend fun loginWithFirebase(idToken: String): TokenResponse =
        client.post("auth/user/firebase") { setBody(FirebaseAuthRequest(idToken)) }.body()

    override suspend fun requestOtp(request: OtpRequest): OtpSessionResponse =
        client.post("auth/otp/request") {
            otpTimeout()
            setBody(request)
        }.body()

    override suspend fun otpConfig(): OtpConfigDto = client.get("auth/otp/config").body()

    private fun io.ktor.client.request.HttpRequestBuilder.otpTimeout() {
        timeout {
            requestTimeoutMillis = AppConstants.OTP_REQUEST_TIMEOUT_MS
            socketTimeoutMillis = AppConstants.OTP_REQUEST_TIMEOUT_MS
        }
    }

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

    override suspend fun shopInfo(shopId: String, lat: Double, lng: Double): MarketplaceShop =
        client.get("shops/$shopId/info") {
            parameter("lat", lat)
            parameter("lng", lng)
        }.body<ShopDto>().toModel()

    override suspend fun catalogSections(
        shopId: String,
        query: String,
        previewSize: Int,
    ): List<org.bhargav.pansariwala.domain.model.CatalogSection> =
        client.get("shops/$shopId/catalog/sections") {
            if (query.isNotBlank()) parameter("query", query)
            parameter("preview", previewSize)
        }.body<List<CatalogSectionDto>>().map { it.toModel() }

    override suspend fun catalogPage(
        shopId: String,
        category: org.bhargav.pansariwala.domain.model.ProductCategory?,
        query: String,
        page: Int,
        pageSize: Int,
    ): org.bhargav.pansariwala.domain.model.ProductPage =
        client.get("shops/$shopId/products") {
            category?.let { parameter("category", it.name) }
            if (query.isNotBlank()) parameter("query", query)
            parameter("page", page)
            parameter("size", pageSize)
        }.body<ProductPageDto>().toModel()

    override suspend fun shopProductDetail(
        shopId: String,
        productId: String,
    ): org.bhargav.pansariwala.domain.model.ProductDetail =
        client.get("shops/$shopId/products/$productId").body<ProductDetailDto>().toModel()

    override suspend fun masterCategories(): List<MasterCategoryDto> = client.get("master/categories").body()

    override suspend fun masterProductPage(
        query: String,
        categoryId: String?,
        page: Int,
        pageSize: Int,
    ): MasterProductPageDto =
        client.get("master/products/page") {
            if (query.isNotBlank()) parameter("q", query.trim())
            categoryId?.let { parameter("categoryId", it) }
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()

    override suspend fun shopRatings(shopId: String): List<org.bhargav.pansariwala.domain.model.ShopReview> =
        client.get("shops/$shopId/ratings").body<List<ShopReviewDto>>().map { it.toModel() }

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

    override suspend fun myOrdersPage(page: Int, pageSize: Int): OrderPage =
        client.get("orders/mine/page") {
            parameter("page", page)
            parameter("size", pageSize)
        }.body<OrderPageDto>().toModel()

    override suspend fun order(orderId: String): Order =
        client.get("orders/$orderId").body<OrderDto>().toModel()

    override suspend fun myTransactions(): List<MoneyTxn> =
        client.get("me/transactions").body<List<TxnDto>>().map { it.toModel() }

    override suspend fun rateOrder(orderId: String, stars: Int, comment: String?): Order =
        client.post("orders/$orderId/rating") { setBody(RateOrderRequest(stars, comment)) }
            .body<OrderDto>().toModel()

    override suspend fun shopOnlineOrders(fromMs: Long?, toMs: Long?): List<Order> =
        client.get("shop/orders") {
            fromMs?.let { parameter("from", it) }
            toMs?.let { parameter("to", it) }
        }.body<List<OrderDto>>().map { it.toModel() }

    override suspend fun shopOnlineOrdersPage(
        statuses: List<OrderStatus>,
        query: String,
        page: Int,
        pageSize: Int,
    ): OrderPage =
        client.get("shop/orders/page") {
            if (statuses.isNotEmpty()) parameter("status", statuses.joinToString(",") { it.name })
            if (query.isNotBlank()) parameter("query", query.trim())
            parameter("page", page)
            parameter("size", pageSize)
        }.body<OrderPageDto>().toModel()

    override suspend fun shopPaymentInfo(): ShopPaymentInfoDto =
        client.get("shop/payment-info").body()

    override suspend fun registerPushToken(token: String, platform: String, deviceId: String) {
        client.post("push/register") { setBody(PushRegisterRequest(token, platform, deviceId)) }.body<OkResponse>()
    }

    override suspend fun unregisterPushToken(token: String) {
        client.post("push/unregister") { setBody(PushUnregisterRequest(token)) }.body<OkResponse>()
    }

    override suspend fun acceptOrder(orderId: String): Order =
        client.post("shop/orders/$orderId/accept").body<OrderDto>().toModel()

    override suspend fun rejectOrder(orderId: String, rejectedProductIds: List<String>, reason: String?): Order =
        client.post("shop/orders/$orderId/reject") {
            setBody(ShopActionRequest(rejectedProductIds, reason))
        }.body<OrderDto>().toModel()

    override suspend fun setOrderStatus(orderId: String, status: String): Order =
        client.post("shop/orders/$orderId/status") { setBody(StatusPatch(status)) }
            .body<OrderDto>().toModel()

    override suspend fun requestDelivery(orderId: String, packedProductIds: List<String>): DeliveryOffer =
        client.post("shop/orders/$orderId/delivery") {
            setBody(DeliveryRequestDto(packedProductIds))
        }.body<DeliveryOfferDto>().toModel()

    override suspend fun startSelfDelivery(orderId: String, packedProductIds: List<String>): Order =
        client.post("shop/orders/$orderId/self-delivery") {
            setBody(DeliveryRequestDto(packedProductIds))
        }.body<OrderDto>().toModel()

    override suspend fun completeSelfDelivery(orderId: String, otp: String): Order =
        client.post("shop/orders/$orderId/self-delivery/complete") {
            setBody(DeliverRequest(otp))
        }.body<OrderDto>().toModel()

    override suspend fun shopPartners(): List<ShopPartnerDto> =
        client.get("shop/partners").body()

    override suspend fun addShopPartner(request: ShopPartnerRequest): ShopPartnerDto =
        client.post("shop/partners") { setBody(request) }.body()

    override suspend fun removeShopPartner(id: String) {
        client.delete("shop/partners/$id").body<OkResponse>()
    }

    override suspend fun assignShopPartner(orderId: String, shopPartnerId: String, packedProductIds: List<String>): Order =
        client.post("shop/orders/$orderId/assign-partner") {
            setBody(AssignShopPartnerRequest(shopPartnerId, packedProductIds.takeIf { it.isNotEmpty() }))
        }.body<OrderDto>().toModel()

    override suspend fun cancelShopOrder(orderId: String, reason: String?): Order =
        client.post("shop/orders/$orderId/cancel") {
            setBody(ShopActionRequest(reason = reason))
        }.body<OrderDto>().toModel()

    override suspend fun shopProfile(): ShopProfileDto = client.get("shop/profile").body()

    override suspend fun updateShopProfile(patch: ShopProfilePatch): ShopProfileDto =
        client.put("shop/profile") { setBody(patch) }.body()

    override suspend fun shopStaff(): List<ShopStaffDto> = client.get("shop/staff").body()

    override suspend fun saveShopStaff(staffId: String?, body: ShopStaffUpsert): ShopStaffDto =
        if (staffId == null) {
            client.post("shop/staff") { setBody(body) }.body()
        } else {
            client.put("shop/staff/$staffId") { setBody(body) }.body()
        }

    override suspend fun deleteShopStaff(staffId: String) {
        client.delete("shop/staff/$staffId").body<OkResponse>()
    }

    override suspend fun shopManagedOffers(all: Boolean): List<ShopOfferDto> =
        client.get("shop/offers") { parameter("all", all) }.body()

    override suspend fun saveShopOffer(offerId: String?, body: ShopOfferDto): ShopOfferDto =
        if (offerId == null) {
            client.post("shop/offers") { setBody(body) }.body()
        } else {
            client.put("shop/offers/$offerId") { setBody(body) }.body()
        }

    override suspend fun deleteShopOffer(offerId: String) {
        client.delete("shop/offers/$offerId").body<OkResponse>()
    }

    override suspend fun shopOwnRatings(): List<org.bhargav.pansariwala.domain.model.ShopReview> =
        client.get("shop/ratings").body<List<ShopReviewDto>>().map { it.toModel() }

    override suspend fun replyToRating(orderId: String, reply: String): org.bhargav.pansariwala.domain.model.ShopReview =
        client.post("shop/ratings/$orderId/reply") { setBody(RatingReplyRequest(reply)) }
            .body<ShopReviewDto>().toModel()

    override suspend fun shopVisitors(fromEpochMs: Long?, toEpochMs: Long?): VisitorStatsDto =
        client.get("shop/visitors") {
            fromEpochMs?.let { parameter("from", it) }
            toEpochMs?.let { parameter("to", it) }
        }.body()

    override suspend fun registerPartner(request: PartnerRegisterRequest): OtpSessionResponse =
        client.post("partners/register") {
            otpTimeout()
            setBody(request)
        }.body()

    override suspend fun partnerProfile(): PartnerProfile =
        client.get("partners/profile").body<PartnerProfileDto>().toModel()

    override suspend fun updatePartnerProfilePhoto(profilePhotoUrl: String) {
        client.post("partners/profile/photo") {
            setBody(PartnerProfilePhotoRequest(profilePhotoUrl))
        }.body<OkResponse>()
    }

    override suspend fun partnerDashboard(fromEpochMs: Long, toEpochMs: Long): PartnerDashboard =
        client.get("partners/dashboard") {
            parameter("from", fromEpochMs)
            parameter("to", toEpochMs)
        }.body<PartnerDashboardDto>().toModel()

    override suspend fun partnerEarnings(fromMs: Long?, toMs: Long?): PartnerEarnings =
        client.get("partners/earnings") {
            fromMs?.let { parameter("from", it) }
            toMs?.let { parameter("to", it) }
        }.body<PartnerEarningsDto>().toModel()

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

    override suspend fun verifyBags(orderId: String, photoOne: String, photoTwo: String): Order =
        client.post("partners/jobs/$orderId/verify-bags") {
            setBody(PickupRequest(photoOne, photoTwo))
        }.body<OrderDto>().toModel()

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

    override suspend fun rateCustomer(orderId: String, stars: Int, comment: String?): Order =
        client.post("partners/jobs/$orderId/customer-rating") {
            setBody(RateOrderRequest(stars, comment))
        }.body<OrderDto>().toModel()

    override suspend fun customerRatings(customerId: String, stars: Set<Int>, page: Int, size: Int): CustomerRatingPageDto =
        client.get("shop/customers/$customerId/ratings") {
            if (stars.isNotEmpty()) parameter("stars", stars.sorted().joinToString(","))
            parameter("page", page)
            parameter("size", size)
        }.body()

    override suspend fun pullSync(): SyncPullResponse = client.get("sync/pull").body()

    override suspend fun pushSync(request: SyncPushRequest): SyncPushResponse =
        client.post("sync/push") { setBody(request) }.body()

    override suspend fun syncOrders(since: Long, afterId: String, limit: Int): SyncOrdersPage =
        client.get("sync/orders") {
            parameter("since", since)
            parameter("afterId", afterId)
            parameter("limit", limit)
        }.body()

    override suspend fun uploadImage(
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        thumbnailBytes: ByteArray?,
    ): UploadResultDto = postUpload("uploads", prefix, fileName, bytes, contentType, thumbnailBytes)

    override suspend fun uploadImageGuest(
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        thumbnailBytes: ByteArray?,
    ): UploadResultDto = postUpload("uploads/guest", prefix, fileName, bytes, contentType, thumbnailBytes)

    private suspend fun postUpload(
        path: String,
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        thumbnailBytes: ByteArray?,
    ): UploadResultDto {
        return client.post(path) {
            parameter("prefix", prefix.trim('/'))
            timeout {
                requestTimeoutMillis = AppConstants.HTTP_UPLOAD_TIMEOUT_MS
                socketTimeoutMillis = AppConstants.HTTP_UPLOAD_TIMEOUT_MS
            }
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                        if (thumbnailBytes != null && thumbnailBytes.isNotEmpty()) {
                            append(
                                "thumbnail",
                                thumbnailBytes,
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/jpeg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"thumb.jpg\"")
                                },
                            )
                        }
                    },
                ),
            )
        }.body()
    }
}
