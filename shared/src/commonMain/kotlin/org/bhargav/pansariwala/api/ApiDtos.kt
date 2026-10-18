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
import org.bhargav.pansariwala.domain.model.DiscountType
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductOffer
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ProductVariant
import org.bhargav.pansariwala.domain.model.SellMode
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.domain.model.ShopType
import org.bhargav.pansariwala.util.AppConstants

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
    val shopRole: String? = null,
    val permissions: List<String> = emptyList(),
)

@Serializable
data class ShopLoginRequest(val username: String, val password: String)

@Serializable
data class FirebaseAuthRequest(val idToken: String)

@Serializable
data class PushRegisterRequest(val token: String, val platform: String, val deviceId: String? = null)

@Serializable
data class PushUnregisterRequest(val token: String)

@Serializable
data class OtpRequest(
    val phone: String,
    val playIntegrityToken: String? = null,
    val recaptchaToken: String? = null,
    val androidPackage: String? = null,
    val androidCertSha1: String? = null,
)

/** Server OTP mode: `dev` (fixed code) or `firebase` (needs app proof from [OtpAppVerifier]). */
@Serializable
data class OtpConfigDto(
    val mode: String = AppConstants.OTP_MODE_DEV,
    val recaptchaSiteKey: String = "",
    val authDomain: String = "",
)

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
    val topOffers: List<String> = emptyList(),
    val discountPercent: Double = 0.0,
    val upiId: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val shopType: String = "GENERAL_STORE",
    val deliversToYou: Boolean = true,
    val address: String = "",
    val deliveryUntil: String = "",
    val deliveryAvailable: Boolean = true,
)

@Serializable
data class CatalogSectionDto(
    val category: String,
    val totalCount: Int,
    val preview: List<ProductDto> = emptyList(),
    val categoryName: String = "",
)

@Serializable
data class ProductPageDto(
    val items: List<ProductDto> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class ShopPartnerDto(
    val id: String,
    val name: String,
    val phone: String,
    val vehicleReg: String = "",
    val onPartnerApp: Boolean = false,
)

@Serializable
data class ShopPartnerRequest(
    val name: String,
    val phone: String,
    val vehicleReg: String = "",
)

@Serializable
data class AssignShopPartnerRequest(
    val shopPartnerId: String,
    val packedProductIds: List<String>? = null,
)

@Serializable
data class OrderPageDto(
    val items: List<OrderDto> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val newCount: Int = 0,
)

data class OrderPage(
    val items: List<Order>,
    val page: Int,
    val totalCount: Int,
    val hasMore: Boolean,
    val newCount: Int,
)

fun OrderPageDto.toModel() = OrderPage(items.map { it.toModel() }, page, totalCount, hasMore, newCount)

@Serializable
data class ProductVariantDto(
    val name: String,
    val sku: String = "",
    val price: Double = 0.0,
    val id: String = "",
    val sellMode: String = "PACK",
    val attributes: Map<String, String> = emptyMap(),
    val mrp: Double = 0.0,
    val baseUnit: String = "",
    val minQty: Double = 0.0,
    val maxQty: Double = 0.0,
    val stepQty: Double = 0.0,
    val quickPicks: List<Double> = emptyList(),
    val bundleCount: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val stockQty: Double = 0.0,
    val barcode: String? = null,
    val active: Boolean = true,
    val packSize: Double = 0.0,
    val packUnit: String = "",
    val baseVariantId: String = "",
)

@Serializable
data class ProductImportErrorDto(val row: Int, val message: String)

/** Mirrors the server import report; [dryRun] = checked only, nothing saved. */
@Serializable
data class ProductImportReportDto(
    val dryRun: Boolean,
    val productsCreated: Int = 0,
    val productsUpdated: Int = 0,
    val variantsCreated: Int = 0,
    val variantsUpdated: Int = 0,
    val errors: List<ProductImportErrorDto> = emptyList(),
)

@Serializable
data class FileLinkDto(val path: String)

@Serializable
data class ProductDetailDto(
    val product: ProductDto,
    val brandName: String = "",
    val companyName: String = "",
    val description: String = "",
    val sku: String = "",
    val tags: String = "",
    val weightKg: Double = 0.0,
    val dimensions: String = "",
    val variants: List<ProductVariantDto> = emptyList(),
    val priceHistory: List<PricePointDto> = emptyList(),
    val categoryName: String = "",
)

@Serializable
data class PricePointDto(val at: Long, val price: Double)

@Serializable
data class ShopReviewDto(
    val id: String,
    val customerName: String,
    val stars: Int,
    val comment: String? = null,
    val createdAtEpochMs: Long,
    val reply: String? = null,
    val replyAtEpochMs: Long? = null,
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
    val imageUrls: List<String> = emptyList(),
    val variants: List<ProductVariantDto> = emptyList(),
    val brandName: String = "",
    val masterProductId: String? = null,
    val active: Boolean = true,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    /** Best live PRODUCT offer, attached by the server on catalog reads. */
    val offer: ProductOfferDto? = null,
)

@Serializable
data class ProductOfferDto(
    val id: String,
    val title: String,
    val discountType: String,
    val value: Double,
)

@Serializable
data class OfferDto(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
    val scope: String = "BILL",
    val discountType: String = "PERCENT",
    val value: Double = discountPercent,
    val minBillAmount: Double = 0.0,
    val endsAtEpochMs: Long? = null,
    val products: List<ProductDto> = emptyList(),
)

@Serializable
data class CustomerRatingDto(
    val orderId: String,
    val stars: Int,
    val comment: String? = null,
    val partnerName: String = "",
    val createdAtEpochMs: Long,
)

@Serializable
data class CustomerRatingPageDto(
    val items: List<CustomerRatingDto> = emptyList(),
    val page: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val average: Double = 0.0,
    val ratingCount: Int = 0,
    val starCounts: Map<Int, Int> = emptyMap(),
)

@Serializable
data class PlaceOrderItemDto(val productId: String, val quantity: Double, val variantId: String? = null)

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
    val selfPickup: Boolean = false,
)

@Serializable
data class QuoteRequest(
    val shopId: String,
    val items: List<PlaceOrderItemDto>,
    val userLat: Double,
    val userLng: Double,
    val selfPickup: Boolean = false,
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
data class ShopPaymentInfoDto(val shopName: String, val upiId: String = "")

@Serializable
data class OtpSessionResponse(
    val sessionId: String,
    val devOtp: String? = null,
    val expiresInSec: Int = AppConstants.OTP_DEFAULT_TTL_SEC,
    val resendAfterSec: Int = AppConstants.OTP_DEFAULT_RESEND_SEC,
    val maskedPhone: String = "",
)

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
    val selfPickup: Boolean = false,
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
    val customerRatingStars: Int? = null,
    val customerRatingComment: String? = null,
    val customerRatingAvg: Double? = null,
    val customerRatingCount: Int = 0,
    val cancelReason: String? = null,
    val partnerProgress: String = "",
    val packedProductIds: List<String> = emptyList(),
    val deliveryMode: String = AppConstants.DELIVERY_MODE_PARTNER,
    /** POS bill discount in INR. Online orders keep theirs on [quote]. */
    val discount: Double = 0.0,
)

@Serializable
data class DeliveryRequestDto(val packedProductIds: List<String>)

@Serializable
data class OrderItemDto(
    val productId: String,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val imageUrl: String? = null,
    val variantId: String? = null,
    val variantLabel: String? = null,
    val sellMode: String? = null,
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
    val vehiclePhotoUrl: String,
    val profilePhotoUrl: String = "",
    val dlPhotoUrl: String = "",
    val idPhotoUrl: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)

@Serializable
data class PartnerOnlineRequest(val online: Boolean)

@Serializable
data class PartnerLocationRequest(val lat: Double, val lng: Double)

@Serializable
data class PartnerProfilePhotoRequest(val profilePhotoUrl: String)

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
    val rangeEarnings: Double = 0.0,
    val rangeDeliveredCount: Int = 0,
)

@Serializable
data class PickupRequest(val photoOneUrl: String, val photoTwoUrl: String)

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
    val imageUrls: List<String> = emptyList(),
    val brandName: String = "",
    val subcategoryId: String? = null,
    val salePrice: Double = 0.0,
    val variants: List<ProductVariantDto> = emptyList(),
    val cost: Double = 0.0,
    val active: Boolean = true,
    val lowStockThreshold: Double = 0.0,
)

@Serializable
data class MasterProductPageDto(
    val items: List<MasterProductDto> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class UploadResultDto(
    val url: String,
    val thumbnailUrl: String,
)

@Serializable
data class OfflineOrderSyncDto(
    val revision: Long,
    val order: OrderDto,
)

@Serializable
data class OfflineOrderAck(
    val id: String,
    val revision: Long,
)

@Serializable
data class OfflineProductSyncDto(
    val revision: Long,
    val product: ProductDto,
)

@Serializable
data class SyncPushRequest(
    val products: List<ProductDto> = emptyList(),
    val orders: List<OrderDto> = emptyList(),
    val offlineOrders: List<OfflineOrderSyncDto> = emptyList(),
    val offlineProducts: List<OfflineProductSyncDto> = emptyList(),
)

@Serializable
data class SyncPushResponse(
    val accepted: List<OfflineOrderAck> = emptyList(),
    val acceptedProducts: List<OfflineOrderAck> = emptyList(),
)

@Serializable
data class SyncOrdersPage(
    val orders: List<OrderDto> = emptyList(),
    val nextSince: Long,
    val nextAfterId: String = "",
    val hasMore: Boolean = false,
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
    topOffers = topOffers,
    discountPercent = discountPercent,
    deliveryRadiusKm = deliveryRadiusKm,
    shopType = ShopType.fromApi(shopType),
    deliversToYou = deliversToYou,
    address = address,
    deliveryUntil = deliveryUntil,
    deliveryAvailable = deliveryAvailable,
)

fun CatalogSectionDto.toModel() = org.bhargav.pansariwala.domain.model.CatalogSection(
    category = org.bhargav.pansariwala.domain.model.CatalogCategory(id = category, name = categoryName),
    totalCount = totalCount,
    preview = preview.map { it.toModel() },
)

fun ProductPageDto.toModel() = org.bhargav.pansariwala.domain.model.ProductPage(
    items = items.map { it.toModel() },
    page = page,
    totalCount = totalCount,
    hasMore = hasMore,
)

fun ProductDetailDto.toModel() = org.bhargav.pansariwala.domain.model.ProductDetail(
    product = product.toModel().let { p ->
        if (p.variants.isNotEmpty()) p else p.copy(variants = variants.filter { it.active }.map { it.toModel() })
    },
    brandName = brandName,
    companyName = companyName,
    description = description,
    sku = sku,
    tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    weightKg = weightKg,
    dimensions = dimensions,
    priceHistory = priceHistory.map { org.bhargav.pansariwala.domain.model.PricePoint(it.at, it.price) },
    categoryName = categoryName,
)

fun ShopReviewDto.toModel() = org.bhargav.pansariwala.domain.model.ShopReview(
    id = id,
    customerName = customerName,
    stars = stars,
    comment = comment,
    createdAtEpochMs = createdAtEpochMs,
    reply = reply,
    replyAtEpochMs = replyAtEpochMs,
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
    imageUrls = imageUrls.take(AppConstants.PHOTO_MAX_PRODUCT_IMAGES),
    variants = variants.filter { it.active }.map { it.toModel() },
    brandName = brandName,
    masterProductId = masterProductId,
    isActive = active,
    offer = offer?.toModel(),
    categoryId = categoryId,
    subCategoryId = subCategoryId,
)

fun ProductOfferDto.toModel() = ProductOffer(
    id = id,
    title = title,
    type = DiscountType.fromName(discountType),
    value = value,
)

fun ProductVariantDto.toModel() = ProductVariant(
    id = id,
    label = name,
    sellMode = SellMode.fromName(sellMode),
    price = price,
    mrp = mrp,
    attributes = attributes,
    baseUnit = ProductUnit.fromName(baseUnit),
    minQty = minQty,
    maxQty = maxQty,
    stepQty = stepQty,
    quickPicks = quickPicks,
    bundleCount = bundleCount,
    imageUrls = imageUrls,
    stockQty = stockQty,
    sku = sku,
    barcode = barcode,
    active = active,
    packSize = packSize,
    packUnit = if (packSize > 0.0) ProductUnit.fromName(packUnit) else ProductUnit.PIECE,
    baseVariantId = baseVariantId,
)

fun ProductVariant.toDto() = ProductVariantDto(
    name = label,
    sku = sku,
    price = price,
    id = id,
    sellMode = sellMode.name,
    attributes = attributes,
    mrp = mrp,
    baseUnit = if (isLoose) baseUnit.name else "",
    minQty = minQty,
    maxQty = maxQty,
    stepQty = stepQty,
    quickPicks = quickPicks,
    bundleCount = bundleCount,
    imageUrls = imageUrls,
    stockQty = stockQty,
    barcode = barcode,
    active = active,
    packSize = packSize,
    packUnit = if (packSize > 0.0) packUnit.name else "",
    baseVariantId = baseVariantId,
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
    imageUrls = imageUrls.take(AppConstants.PHOTO_MAX_PRODUCT_IMAGES),
    variants = variants.map { it.toDto() },
    brandName = brandName,
    masterProductId = masterProductId,
    active = isActive,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
)

fun Order.toSyncDto() = OrderDto(
    id = id,
    shopId = shopId,
    createdAtEpochMs = createdAtEpochMs,
    status = status.name,
    customerName = customerName,
    channel = OrderChannel.POS.name,
    paymentMethod = paymentMethod,
    cancelReason = cancelReason,
    discount = discount,
    items = items.map { line ->
        OrderItemDto(
            productId = line.productId,
            productName = line.productName,
            unit = line.unit.name,
            quantity = line.quantity,
            unitPrice = line.unitPrice,
            imageUrl = line.imageUrl,
            variantId = line.variantId,
            variantLabel = line.variantLabel,
        )
    },
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
            variantId = it.variantId,
            variantLabel = it.variantLabel,
        )
    },
    cancelReason = cancelReason,
    channel = runCatching { OrderChannel.valueOf(channel) }.getOrDefault(OrderChannel.ONLINE),
    customerId = customerId,
    deliveryAddress = deliveryAddress,
    dropoffInstructions = dropoffInstructions,
    deliveryOtp = deliveryOtp,
    pickupPhotoUrls = pickupPhotoUrls,
    selfPickup = selfPickup,
    partnerId = partnerId,
    partnerName = partnerName,
    partnerPhone = partnerPhone,
    partnerVehicleReg = partnerVehicleReg,
    rating = ratingStars?.let { OrderRating(it, ratingComment, createdAtEpochMs) },
    customerRatingByPartner = customerRatingStars?.let { OrderRating(it, customerRatingComment, createdAtEpochMs) },
    customerRatingAvg = customerRatingAvg,
    customerRatingCount = customerRatingCount,
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
    partnerProgress = partnerProgress,
    packedProductIds = packedProductIds.toSet(),
    isSelfDelivery = deliveryMode == AppConstants.DELIVERY_MODE_SELF,
    discount = discount,
)

fun OfferDto.toModel() = ShopOffer(
    id = id,
    title = title,
    description = description,
    discountPercent = discountPercent,
    type = org.bhargav.pansariwala.domain.model.DiscountType.fromName(discountType),
    value = value,
    minBillAmount = minBillAmount,
    endsAtEpochMs = endsAtEpochMs,
    products = products.filter { it.active }.map { it.toModel() },
)

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
    rangeEarnings = rangeEarnings,
    rangeDeliveredCount = rangeDeliveredCount,
)

fun TxnDto.toModel() = MoneyTxn(id, orderId, amount, title, createdAtEpochMs)
