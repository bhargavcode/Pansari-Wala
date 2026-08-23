package org.bhargav.pansariwala.api

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

interface PansariApi {
    suspend fun publicConfig(): PublicConfigDto

    suspend fun shopLogin(username: String, password: String): TokenResponse
    suspend fun loginWithFirebase(idToken: String): TokenResponse
    suspend fun requestOtp(phone: String): OtpSessionResponse
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
    suspend fun shopOffers(shopId: String): List<ShopOffer>
    suspend fun quote(request: QuoteRequest): QuoteDto
    /** Non-payment readiness check (profile, cart, stock). Call before Razorpay. */
    suspend fun validateOrder(request: PlaceOrderRequest)
    suspend fun createRazorpayOrder(shopId: String, amountPaise: Long): RazorpayOrderDto
    suspend fun verifyPayment(request: VerifyPaymentRequest): Boolean
    suspend fun placeOrder(request: PlaceOrderRequest): Order
    suspend fun myOrders(): List<Order>
    suspend fun order(orderId: String): Order
    suspend fun myTransactions(): List<MoneyTxn>
    suspend fun rateOrder(orderId: String, stars: Int, comment: String?): Order

    suspend fun shopOnlineOrders(): List<Order>
    suspend fun acceptOrder(orderId: String): Order
    suspend fun rejectOrder(orderId: String, rejectedProductIds: List<String>, reason: String?): Order
    suspend fun setOrderStatus(orderId: String, status: String): Order
    suspend fun requestDelivery(orderId: String): DeliveryOffer
    suspend fun cancelShopOrder(orderId: String, reason: String?): Order

    suspend fun registerPartner(request: PartnerRegisterRequest): String
    suspend fun partnerProfile(): PartnerProfile
    suspend fun partnerDashboard(fromEpochMs: Long, toEpochMs: Long): PartnerDashboard
    suspend fun partnerEarnings(): PartnerEarnings
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
    suspend fun submitPickup(orderId: String, photoOne: String, photoTwo: String): Order
    suspend fun arrivedAtCustomer(orderId: String): Order
    suspend fun deliverOrder(orderId: String, otp: String): Order

    suspend fun pullSync(): SyncPullResponse
    suspend fun pushSync(request: SyncPushRequest)
}
