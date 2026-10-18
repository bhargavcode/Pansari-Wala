package org.bhargav.pansariwala.api

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

interface PansariApi {
    /** Best-effort connection pre-warm; never throws. */
    suspend fun warmUp() {}

    suspend fun publicConfig(): PublicConfigDto

    suspend fun shopLogin(username: String, password: String): TokenResponse
    suspend fun loginWithFirebase(idToken: String): TokenResponse
    suspend fun requestOtp(request: OtpRequest): OtpSessionResponse
    suspend fun otpConfig(): OtpConfigDto
    suspend fun verifyOtp(phone: String, otp: String, sessionId: String?): TokenResponse
    suspend fun updateProfile(
        name: String,
        address: String,
        locality: String?,
        lat: Double?,
        lng: Double?,
    ): CustomerProfile
    suspend fun saveAddress(line: String, locality: String, lat: Double, lng: Double): CustomerProfile
    suspend fun selectAddress(addressId: String): CustomerProfile
    suspend fun updateCustomerLocation(lat: Double, lng: Double)
    suspend fun me(): CustomerProfile

    suspend fun nearbyShops(lat: Double, lng: Double, radiusKm: Double, query: String): List<MarketplaceShop>
    suspend fun shopCatalog(shopId: String): List<Product>
    suspend fun shopInfo(shopId: String, lat: Double, lng: Double): MarketplaceShop
    suspend fun catalogSections(
        shopId: String,
        query: String,
        previewSize: Int,
    ): List<org.bhargav.pansariwala.domain.model.CatalogSection>
    suspend fun catalogPage(
        shopId: String,
        category: org.bhargav.pansariwala.domain.model.ProductCategory?,
        query: String,
        page: Int,
        pageSize: Int,
    ): org.bhargav.pansariwala.domain.model.ProductPage
    suspend fun shopProductDetail(shopId: String, productId: String): org.bhargav.pansariwala.domain.model.ProductDetail
    suspend fun masterCategories(): List<MasterCategoryDto>
    /** Platform master catalog; [query] matches name, Hindi name, brand or exact barcode. */
    suspend fun masterProductPage(query: String, categoryId: String?, page: Int, pageSize: Int): MasterProductPageDto
    suspend fun shopRatings(shopId: String): List<org.bhargav.pansariwala.domain.model.ShopReview>
    suspend fun shopOffers(shopId: String): List<ShopOffer>
    suspend fun quote(request: QuoteRequest): QuoteDto
    /** Non-payment readiness check (profile, cart, stock). Call before Razorpay. */
    suspend fun validateOrder(request: PlaceOrderRequest)
    suspend fun createRazorpayOrder(shopId: String, amountPaise: Long): RazorpayOrderDto
    suspend fun verifyPayment(request: VerifyPaymentRequest): Boolean
    suspend fun placeOrder(request: PlaceOrderRequest): Order
    suspend fun myOrders(): List<Order>
    suspend fun myOrdersPage(page: Int, pageSize: Int): OrderPage
    suspend fun order(orderId: String): Order
    suspend fun myTransactions(): List<MoneyTxn>
    suspend fun rateOrder(orderId: String, stars: Int, comment: String?): Order

    suspend fun shopOnlineOrders(fromMs: Long? = null, toMs: Long? = null): List<Order>
    /** [statuses] empty → every status. */
    suspend fun shopOnlineOrdersPage(statuses: List<OrderStatus>, query: String, page: Int, pageSize: Int): OrderPage
    suspend fun shopPaymentInfo(): ShopPaymentInfoDto
    suspend fun registerPushToken(token: String, platform: String, deviceId: String)
    suspend fun unregisterPushToken(token: String)
    suspend fun acceptOrder(orderId: String): Order
    suspend fun rejectOrder(orderId: String, rejectedProductIds: List<String>, reason: String?): Order
    suspend fun setOrderStatus(orderId: String, status: String): Order
    suspend fun requestDelivery(orderId: String, packedProductIds: List<String>): DeliveryOffer
    suspend fun startSelfDelivery(orderId: String, packedProductIds: List<String>): Order
    suspend fun completeSelfDelivery(orderId: String, otp: String): Order
    suspend fun shopPartners(): List<ShopPartnerDto>
    suspend fun addShopPartner(request: ShopPartnerRequest): ShopPartnerDto
    suspend fun removeShopPartner(id: String)
    suspend fun assignShopPartner(orderId: String, shopPartnerId: String, packedProductIds: List<String>): Order
    suspend fun cancelShopOrder(orderId: String, reason: String?): Order

    suspend fun shopProfile(): ShopProfileDto
    suspend fun updateShopProfile(patch: ShopProfilePatch): ShopProfileDto
    suspend fun shopStaff(): List<ShopStaffDto>
    suspend fun saveShopStaff(staffId: String?, body: ShopStaffUpsert): ShopStaffDto
    suspend fun deleteShopStaff(staffId: String)
    /** [all] includes inactive/expired offers (needs OFFERS permission); otherwise live offers for billing. */
    suspend fun shopManagedOffers(all: Boolean): List<ShopOfferDto>
    suspend fun saveShopOffer(offerId: String?, body: ShopOfferDto): ShopOfferDto
    suspend fun deleteShopOffer(offerId: String)
    suspend fun shopOwnRatings(): List<org.bhargav.pansariwala.domain.model.ShopReview>
    suspend fun replyToRating(orderId: String, reply: String): org.bhargav.pansariwala.domain.model.ShopReview
    suspend fun shopVisitors(fromEpochMs: Long?, toEpochMs: Long?): VisitorStatsDto

    suspend fun shopSubscription(): ShopSubscriptionDto
    suspend fun subscriptionCheckout(request: SubscriptionCheckoutRequest): SubscriptionCheckoutDto
    suspend fun confirmSubscription(request: SubscriptionConfirmRequest): ShopSubscriptionDto
    suspend fun commissionSummary(fromEpochMs: Long?, toEpochMs: Long?): CommissionSummaryDto
    /** [day] is `yyyy-MM-dd` in shop time. */
    suspend fun commissionDay(day: String): CommissionDayOrdersDto

    suspend fun registerPartner(request: PartnerRegisterRequest): OtpSessionResponse
    suspend fun partnerProfile(): PartnerProfile
    suspend fun updatePartnerProfilePhoto(profilePhotoUrl: String)
    suspend fun partnerDashboard(fromEpochMs: Long, toEpochMs: Long): PartnerDashboard
    suspend fun partnerEarnings(fromMs: Long? = null, toMs: Long? = null): PartnerEarnings
    suspend fun setPartnerOnline(online: Boolean)
    suspend fun updatePartnerLocation(lat: Double, lng: Double)
    suspend fun incomingOffer(): DeliveryOffer?
    suspend fun availableOffers(): List<DeliveryOffer>
    suspend fun acceptOffer(offerId: String): DeliveryOffer
    suspend fun rejectOffer(offerId: String): DeliveryOffer
    suspend fun partnerJob(orderId: String): Order
    suspend fun acceptedJobs(): List<Order>
    suspend fun deliveredJobs(fromEpochMs: Long, toEpochMs: Long): List<Order>
    suspend fun cancelPickup(orderId: String): Order
    suspend fun arrivedAtStore(orderId: String): Order
    suspend fun verifyBags(orderId: String, photoOne: String = "", photoTwo: String = ""): Order
    suspend fun submitPickup(orderId: String, photoOne: String, photoTwo: String): Order
    suspend fun arrivedAtCustomer(orderId: String): Order
    suspend fun deliverOrder(orderId: String, otp: String): Order
    suspend fun rateCustomer(orderId: String, stars: Int, comment: String?): Order
    /** Shop staff: partner ratings of [customerId]; [stars] empty → all. */
    suspend fun customerRatings(customerId: String, stars: Set<Int>, page: Int, size: Int): CustomerRatingPageDto

    suspend fun pullSync(): SyncPullResponse
    suspend fun pushSync(request: SyncPushRequest): SyncPushResponse
    suspend fun syncOrders(since: Long, afterId: String, limit: Int): SyncOrdersPage

    /**
     * Multipart file upload to S3 (or local) URL. Prefix must be an allowed S3 prefix.
     * When [thumbnailBytes] is sent, the server stores both JPEGs as-is (client already compressed).
     */
    suspend fun uploadImage(
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        thumbnailBytes: ByteArray? = null,
    ): UploadResultDto

    /** Pre-auth upload for partner/user onboarding photos (partners/ and users/ prefixes only). */
    suspend fun uploadImageGuest(
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        thumbnailBytes: ByteArray? = null,
    ): UploadResultDto

    /** Products + variants / stock from an .xlsx, .xls or .csv sheet; [dryRun] only validates. */
    suspend fun importShopProducts(fileName: String, bytes: ByteArray, dryRun: Boolean): ProductImportReportDto

    /** Absolute one-time download URL of this shop's products as a sheet. */
    suspend fun shopProductsExportUrl(): String

    /** Public sample sheet showing products with variants. */
    val productTemplateUrl: String
}
