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
import org.bhargav.pansariwala.domain.model.PartnerDailyEarning
import org.bhargav.pansariwala.domain.model.PartnerEarnings
import org.bhargav.pansariwala.domain.model.PartnerProfile
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
data class UpdateProfileRequest(
    val name: String,
    val address: String,
    val locality: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class SaveAddressRequest(
    val line: String,
    val locality: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class CustomerLocationRequest(val lat: Double, val lng: Double)

@Serializable
data class SavedAddressDto(
    val id: String,
    val line: String,
    val locality: String = "",
    val lat: Double,
    val lng: Double,
    val isDefault: Boolean = false,
)

@Serializable
data class CustomerDto(
    val id: String,
    val phone: String,
    val name: String,
    val address: String,
    val locality: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val addresses: List<SavedAddressDto> = emptyList(),
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
    val deliveryRadiusKm: Double = 20.0,
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
    val addressId: String? = null,
    val userLat: Double? = null,
    val userLng: Double? = null,
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
    val dropoffInstructions: String? = null,
    val deliveryOtp: String? = null,
    val pickupPhotoUrls: List<String> = emptyList(),
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerPhone: String? = null,
    val partnerVehicleReg: String? = null,
    val paymentId: String? = null,
    val paymentMethod: String = "ONLINE",
    val customerPhone: String? = null,
    val shopAddress: String? = null,
    val shopLat: Double? = null,
    val shopLng: Double? = null,
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val totalDistanceKm: Double? = null,
    val deliveryDurationMin: Int? = null,
    val partnerPayoutInr: Double? = null,
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
    val imageUrl: String? = null,
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
    val shopAddress: String? = null,
    val shopDistanceKm: Double,
    val dropAddress: String,
    val dropDistanceKm: Double,
    val totalDistanceKm: Double = 0.0,
    val payoutInr: Double,
    val expiresAtEpochMs: Long,
    val status: String,
    val acceptedByPartnerId: String? = null,
    val shopLat: Double,
    val shopLng: Double,
    val shopRating: Double = 0.0,
    val customerName: String? = null,
    val estimatedMinutes: Int = 0,
)

@Serializable
data class PartnerRegisterRequest(
    val name: String,
    val email: String,
    val address: String,
    val phone: String,
    val vehicleReg: String,
    val vehiclePhotoBase64: String,
    val profilePhotoBase64: String = "",
    val dlPhotoBase64: String = "",
    val idPhotoBase64: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class PartnerOnlineRequest(val online: Boolean)

@Serializable
data class PartnerLocationRequest(val lat: Double, val lng: Double)

@Serializable
data class PartnerProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val vehicleReg: String,
    val verified: Boolean,
    val online: Boolean,
    val joinedAtEpochMs: Long,
    val todayEarnings: Double,
    val totalEarnings: Double,
    val deliveredCount: Int,
    val profilePhoto: String = "",
)

@Serializable
data class PartnerDailyEarningDto(val dayLabel: String, val amount: Double)

@Serializable
data class PartnerEarningsDto(
    val todayEarnings: Double,
    val totalEarnings: Double,
    val deliveredCount: Int,
    val acceptanceRatePercent: Int,
    val weeklyEarnings: List<PartnerDailyEarningDto>,
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
    deliveryRadiusKm = deliveryRadiusKm,
)

fun SavedAddressDto.toModel() = org.bhargav.pansariwala.domain.model.SavedAddress(
    id = id,
    line = line,
    locality = locality,
    location = GeoPoint(lat, lng),
    isDefault = isDefault,
)

fun CustomerDto.toModel() = CustomerProfile(
    id = id,
    phone = phone,
    name = name,
    address = address,
    locality = locality,
    location = if (lat != null && lng != null) GeoPoint(lat, lng) else null,
    addresses = addresses.map { it.toModel() },
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
            imageUrl = it.imageUrl,
        )
    },
    cancelReason = cancelReason,
    channel = runCatching { OrderChannel.valueOf(channel) }.getOrDefault(OrderChannel.ONLINE),
    customerId = customerId,
    deliveryAddress = deliveryAddress,
    dropoffInstructions = dropoffInstructions,
    deliveryOtp = deliveryOtp,
    pickupPhotoUrls = pickupPhotoUrls,
    partnerId = partnerId,
    partnerName = partnerName,
    partnerPhone = partnerPhone,
    partnerVehicleReg = partnerVehicleReg,
    rating = ratingStars?.let { OrderRating(it, ratingComment, createdAtEpochMs) },
    quote = quote?.toModel(),
    paymentId = paymentId,
    paymentMethod = paymentMethod,
    customerPhone = customerPhone,
    shopName = shopName,
    shopAddress = shopAddress,
    shopLat = shopLat,
    shopLng = shopLng,
    customerLat = customerLat,
    customerLng = customerLng,
    totalDistanceKm = totalDistanceKm,
    deliveryDurationMin = deliveryDurationMin,
    partnerPayoutInr = partnerPayoutInr,
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
    shopAddress = shopAddress,
    dropAddress = dropAddress,
    dropDistanceKm = dropDistanceKm,
    shopDistanceKm = shopDistanceKm,
    totalDistanceKm = totalDistanceKm,
    payoutInr = payoutInr,
    expiresAtEpochMs = expiresAtEpochMs,
    status = DeliveryOfferStatus.fromName(status),
    acceptedByPartnerId = acceptedByPartnerId,
    customerName = customerName,
    estimatedMinutes = estimatedMinutes,
)

fun PartnerDashboardDto.toModel() = PartnerDashboard(
    deliveredCount = deliveredCount,
    acceptedCount = acceptedCount,
    earnings = earnings,
    fromEpochMs = fromEpochMs,
    toEpochMs = toEpochMs,
)

fun PartnerProfileDto.toModel() = PartnerProfile(
    id = id,
    name = name,
    email = email,
    phone = phone,
    address = address,
    vehicleReg = vehicleReg,
    verified = verified,
    online = online,
    joinedAtEpochMs = joinedAtEpochMs,
    todayEarnings = todayEarnings,
    totalEarnings = totalEarnings,
    deliveredCount = deliveredCount,
    profilePhoto = profilePhoto,
)

fun PartnerEarningsDto.toModel() = PartnerEarnings(
    todayEarnings = todayEarnings,
    totalEarnings = totalEarnings,
    deliveredCount = deliveredCount,
    acceptanceRatePercent = acceptanceRatePercent,
    weeklyEarnings = weeklyEarnings.map { PartnerDailyEarning(it.dayLabel, it.amount) },
)

fun TxnDto.toModel() = MoneyTxn(id, orderId, amount, title, createdAtEpochMs)
