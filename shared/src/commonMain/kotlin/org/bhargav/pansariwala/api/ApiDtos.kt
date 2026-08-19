package org.bhargav.pansariwala.api

import kotlinx.serialization.Serializable
import org.bhargav.pansariwala.domain.model.CheckoutQuote
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.DeliveryOffer
import org.bhargav.pansariwala.domain.model.DeliveryOfferStatus
import org.bhargav.pansariwala.domain.model.DeliveryPartner
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.OrderRating
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.PartnerDashboard
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ShopOffer

@Serializable
data class ApiErrorBody(val error: String, val code: String? = null)

@Serializable
data class PublicConfigDto(
    val razorpayKeyId: String,
    val paymentsEnabled: Boolean,
    val devAuth: Boolean,
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val role: String,
    val userId: String,
    val shopId: String? = null,
    val displayName: String? = null,
    val profileComplete: Boolean = true,
)

@Serializable
data class ShopLoginRequest(val username: String, val password: String)

@Serializable
data class FirebaseAuthRequest(val idToken: String)

@Serializable
data class OtpRequest(val phone: String)

@Serializable
data class OtpVerifyRequest(val phone: String, val otp: String, val sessionId: String? = null)

@Serializable
data class UpdateProfileRequest(val name: String, val address: String, val lat: Double? = null, val lng: Double? = null)

@Serializable
data class CustomerDto(
    val id: String,
    val phone: String,
    val name: String,
    val address: String,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class NearbyShopsRequest(val lat: Double, val lng: Double, val radiusKm: Double, val query: String = "")

@Serializable
data class ShopDto(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val rating: Double,
    val ratingCount: Int,
    val distanceKm: Double,
    val isOpen: Boolean,
    val lat: Double,
    val lng: Double,
    val offerCount: Int = 0,
    val discountPercent: Double = 0.0,
    val upiId: String = "",
)

@Serializable
data class ProductDto(
    val id: String,
    val shopId: String,
    val name: String,
    val nameHi: String,
    val category: String,
    val unit: String,
    val barcode: String? = null,
    val sellingPrice: Double,
    val costPrice: Double,
    val stockQty: Double,
    val lowStockThreshold: Double,
    val voiceAlias: String? = null,
)

@Serializable
data class OfferDto(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
)

@Serializable
data class PlaceOrderItemDto(val productId: String, val quantity: Double)

@Serializable
data class PlaceOrderRequest(
    val shopId: String,
    val items: List<PlaceOrderItemDto>,
    val razorpayPaymentId: String? = null,
    val razorpayOrderId: String? = null,
    val razorpaySignature: String? = null,
)

@Serializable
data class QuoteRequest(
    val shopId: String,
    val items: List<PlaceOrderItemDto>,
    val userLat: Double,
    val userLng: Double,
)

@Serializable
data class QuoteDto(
    val itemsSubtotal: Double,
    val discount: Double,
    val platformFee: Double,
    val deliveryCharge: Double,
    val payable: Double,
    val deliveryDistanceKm: Double,
)

@Serializable
data class CreateRazorpayRequest(val shopId: String, val amountPaise: Long)

@Serializable
data class StatusPatch(val status: String)

@Serializable
data class IncomingOfferResponse(val offer: DeliveryOfferDto? = null)

@Serializable
data class OkResponse(val ok: Boolean = true)

@Serializable
data class OtpSessionResponse(val sessionId: String)

@Serializable
data class RazorpayOrderDto(
    val orderId: String,
    val amountPaise: Long,
    val currency: String,
    val keyId: String,
)

@Serializable
data class VerifyPaymentRequest(
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String,
)

@Serializable
data class OrderDto(
    val id: String,
    val shopId: String,
    val shopName: String? = null,
    val createdAtEpochMs: Long,
    val status: String,
    val customerName: String? = null,
    val customerId: String? = null,
    val channel: String = OrderChannel.ONLINE.name,
    val deliveryAddress: String? = null,
    val deliveryOtp: String? = null,
    val pickupPhotoUrls: List<String> = emptyList(),
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerPhone: String? = null,
    val partnerVehicleReg: String? = null,
    val paymentId: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val quote: QuoteDto? = null,
    val ratingStars: Int? = null,
    val ratingComment: String? = null,
    val cancelReason: String? = null,
)

@Serializable
data class OrderItemDto(
    val productId: String,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
)

@Serializable
data class RateOrderRequest(val stars: Int, val comment: String? = null)

@Serializable
data class ShopActionRequest(val rejectedProductIds: List<String> = emptyList(), val reason: String? = null)

@Serializable
data class DeliveryOfferDto(
    val id: String,
    val orderId: String,
    val shopId: String,
    val shopName: String,
    val shopImageUrl: String? = null,
    val shopDistanceKm: Double,
    val dropAddress: String,
    val dropDistanceKm: Double,
    val payoutInr: Double,
    val expiresAtEpochMs: Long,
    val status: String,
    val acceptedByPartnerId: String? = null,
    val shopLat: Double,
    val shopLng: Double,
    val shopRating: Double = 0.0,
)

@Serializable
data class PartnerRegisterRequest(
    val name: String,
    val email: String,
    val address: String,
    val phone: String,
    val vehicleReg: String,
    val platePhotoBase64: String,
    val vehiclePhotoBase64: String,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class PickupRequest(val photoOneBase64: String, val photoTwoBase64: String)

@Serializable
data class DeliverRequest(val otp: String)

@Serializable
data class PartnerDashboardDto(
    val deliveredCount: Int,
    val acceptedCount: Int,
    val earnings: Double,
    val fromEpochMs: Long,
    val toEpochMs: Long,
)

@Serializable
data class TxnDto(
    val id: String,
    val orderId: String,
    val amount: Double,
    val title: String,
    val createdAtEpochMs: Long,
)

@Serializable
data class MasterCategoryDto(val id: String, val name: String, val parentId: String? = null)

@Serializable
data class MasterProductDto(
    val id: String,
    val name: String,
    val nameHi: String,
    val categoryId: String,
    val unit: String,
    val barcode: String? = null,
)

@Serializable
data class SyncPushRequest(
    val products: List<ProductDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
)

@Serializable
data class SyncPullResponse(
    val products: List<ProductDto>,
    val onlineOrders: List<OrderDto>,
    val masterProducts: List<MasterProductDto>,
)

@Serializable
data class AdminShopPatch(val active: Boolean? = null, val paymentsEnabled: Boolean? = null)

fun ShopDto.toModel() = MarketplaceShop(
    id = id,
    name = name,
    imageUrl = imageUrl,
    rating = rating,
    ratingCount = ratingCount,
    distanceKm = distanceKm,
    isOpen = isOpen,
    location = GeoPoint(lat, lng),
    offerCount = offerCount,
    discountPercent = discountPercent,
)

fun CustomerDto.toModel() = CustomerProfile(
    id = id,
    phone = phone,
    name = name,
    address = address,
    location = if (lat != null && lng != null) GeoPoint(lat, lng) else null,
)

fun QuoteDto.toModel() = CheckoutQuote(
    itemsSubtotal = itemsSubtotal,
    discount = discount,
    platformFee = platformFee,
    deliveryCharge = deliveryCharge,
    payable = payable,
    deliveryDistanceKm = deliveryDistanceKm,
)

fun CheckoutQuote.toDto() = QuoteDto(
    itemsSubtotal = itemsSubtotal,
    discount = discount,
    platformFee = platformFee,
    deliveryCharge = deliveryCharge,
    payable = payable,
    deliveryDistanceKm = deliveryDistanceKm,
)

fun ProductDto.toModel() = Product(
    id = id,
    shopId = shopId,
    name = name,
    nameHi = nameHi,
    category = ProductCategory.fromName(category),
    unit = ProductUnit.fromName(unit),
    barcode = barcode,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    voiceAlias = voiceAlias,
)

fun Product.toDto() = ProductDto(
    id = id,
    shopId = shopId,
    name = name,
    nameHi = nameHi,
    category = category.name,
    unit = unit.name,
    barcode = barcode,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    voiceAlias = voiceAlias,
)

fun OrderDto.toModel() = Order(
    id = id,
    shopId = shopId,
    createdAtEpochMs = createdAtEpochMs,
    status = OrderStatus.fromName(status),
    customerName = customerName,
    items = items.map {
        OrderItem(
            productId = it.productId,
            productName = it.productName,
            unit = ProductUnit.fromName(it.unit),
            quantity = it.quantity,
            unitPrice = it.unitPrice,
        )
    },
    cancelReason = cancelReason,
    channel = runCatching { OrderChannel.valueOf(channel) }.getOrDefault(OrderChannel.ONLINE),
    customerId = customerId,
    deliveryAddress = deliveryAddress,
    deliveryOtp = deliveryOtp,
    pickupPhotoUrls = pickupPhotoUrls,
    partnerId = partnerId,
    partnerName = partnerName,
    partnerPhone = partnerPhone,
    partnerVehicleReg = partnerVehicleReg,
    rating = ratingStars?.let { OrderRating(it, ratingComment, createdAtEpochMs) },
    quote = quote?.toModel(),
    paymentId = paymentId,
    shopName = shopName,
)

fun OfferDto.toModel() = ShopOffer(id, title, description, discountPercent)

fun DeliveryOfferDto.toModel() = DeliveryOffer(
    id = id,
    orderId = orderId,
    shop = MarketplaceShop(
        id = shopId,
        name = shopName,
        imageUrl = shopImageUrl,
        rating = shopRating,
        ratingCount = 0,
        distanceKm = shopDistanceKm,
        isOpen = true,
        location = GeoPoint(shopLat, shopLng),
    ),
    dropAddress = dropAddress,
    dropDistanceKm = dropDistanceKm,
    shopDistanceKm = shopDistanceKm,
    payoutInr = payoutInr,
    expiresAtEpochMs = expiresAtEpochMs,
    status = DeliveryOfferStatus.fromName(status),
    acceptedByPartnerId = acceptedByPartnerId,
)

fun PartnerDashboardDto.toModel() = PartnerDashboard(
    deliveredCount = deliveredCount,
    acceptedCount = acceptedCount,
    earnings = earnings,
    fromEpochMs = fromEpochMs,
    toEpochMs = toEpochMs,
)

fun TxnDto.toModel() = MoneyTxn(id, orderId, amount, title, createdAtEpochMs)
