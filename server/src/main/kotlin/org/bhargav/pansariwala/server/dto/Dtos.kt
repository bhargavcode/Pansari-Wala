package org.bhargav.pansariwala.server.dto

import kotlinx.serialization.Serializable

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
    /** Shop logins only: SHOP (admin) or EXECUTIVE. */
    val shopRole: String? = null,
    val permissions: List<String> = emptyList(),
)

@Serializable
data class ShopLoginRequest(val username: String, val password: String)

@Serializable
data class AdminLoginRequest(val username: String, val password: String)

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

/** Tells clients which app proof `/auth/otp/request` needs. [mode] is `dev` or `firebase`. */
@Serializable
data class OtpConfigResponse(
    val mode: String,
    val recaptchaSiteKey: String = "",
    val authDomain: String = "",
)

@Serializable
data class OtpVerifyRequest(val phone: String, val otp: String, val sessionId: String? = null)

@Serializable
data class OtpSessionResponse(
    val sessionId: String,
    /** Present when SMS is not sent (dev / no SMS provider). Client may show as hint. */
    val devOtp: String? = null,
    val expiresInSec: Int = 300,
    val resendAfterSec: Int = 30,
    val maskedPhone: String = "",
)

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
    /** Live offers only (active and inside their start/end window). */
    val offerCount: Int = 0,
    /** Titles of the biggest live offers, largest first. */
    val topOffers: List<String> = emptyList(),
    val discountPercent: Double = 0.0,
    val upiId: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val shopType: String = "GENERAL_STORE",
    val active: Boolean = true,
    val paymentsEnabled: Boolean = true,
    val address: String = "",
    val deliversToYou: Boolean = true,
    val deliveryUntil: String = "",
    /** False after [deliveryUntil]: orders are pickup-only with no delivery charge. */
    val deliveryAvailable: Boolean = true,
)

@Serializable
data class ShopHoursDayDto(
    val day: String,
    val start: String = "09:00",
    val end: String = "21:00",
    val closed: Boolean = false,
)

@Serializable
data class AdminShopCreate(
    val name: String,
    val shopType: String = "GENERAL_STORE",
    val address: String = "",
    val lat: Double = 28.6139,
    val lng: Double = 77.2090,
    val active: Boolean = true,
    val imageUrl: String? = null,
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "India",
    val registrationNumber: String = "",
    val taxId: String = "",
    val operatingHours: List<ShopHoursDayDto> = emptyList(),
    val features: ShopFeaturesDto? = null,
    /** POS login for the owner; defaults to [ownerPhone] when blank. */
    val loginUsername: String = "",
    val loginPassword: String = "",
)

@Serializable
data class ShopFeaturesDto(
    val voiceSearch: Boolean = true,
    val barcodeSearch: Boolean = true,
    val reportGeneration: Boolean = true,
    val onlineOrders: Boolean = true,
    val inventoryAlerts: Boolean = true,
    val barcodeScanner: Boolean = false,
)

@Serializable
data class AdminShopDto(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isOpen: Boolean = true,
    val active: Boolean = true,
    val paymentsEnabled: Boolean = true,
    val discountPercent: Double = 0.0,
    val upiId: String = "",
    val address: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val shopType: String = "GENERAL_STORE",
    val joinedAtEpochMs: Long = 0L,
    val features: ShopFeaturesDto = ShopFeaturesDto(),
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "India",
    val registrationNumber: String = "",
    val taxId: String = "",
    val operatingHours: List<ShopHoursDayDto> = emptyList(),
    val loginUsername: String = "",
    val deliveryUntil: String = "",
    val homeDeliveryEnabled: Boolean = true,
)

@Serializable
data class AdminShopDetailDto(
    val shop: AdminShopDto,
    val transactions: List<AdminTxnDto> = emptyList(),
    val orderCount: Int = 0,
    val uniqueCustomers: Int = 0,
)

@Serializable
data class AdminChartPointDto(val label: String, val value: Double)

@Serializable
data class AdminDashboardDto(
    val shopCount: Int,
    val productCount: Int,
    val transactionAmount: Double,
    val transactionCount: Int,
    val userCount: Int,
    val partnerCount: Int,
    val salesByWeekday: List<AdminChartPointDto> = emptyList(),
    val txnTrendByMonth: List<AdminChartPointDto> = emptyList(),
)

@Serializable
data class AdminTxnDto(
    val orderId: String,
    val transactionNo: String,
    val createdAtEpochMs: Long,
    val status: String,
    val itemsSummary: String,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerId: String? = null,
    val shopId: String,
    val shopName: String = "",
    val offers: Double = 0.0,
    val charges: Double = 0.0,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val paymentMethod: String = "ONLINE",
    val refundId: String? = null,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val items: List<OrderItemDto> = emptyList(),
    val deliveryDurationMin: Int? = null,
    val partnerVehicleReg: String? = null,
)

@Serializable
data class AdminTxnSummaryDto(
    val amount: Double,
    val count: Int,
    val transactions: List<AdminTxnDto>,
)

@Serializable
data class AdminUserDto(
    val id: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val imageUrl: String? = null,
    val active: Boolean = true,
    val joinedAtEpochMs: Long = 0L,
)

@Serializable
data class AdminUserDetailDto(
    val user: AdminUserDto,
    val orders: List<AdminTxnDto> = emptyList(),
)

@Serializable
data class AdminUserPatch(val active: Boolean? = null)

@Serializable
data class AdminPartnerDto(
    val id: String,
    val name: String,
    val phone: String,
    val email: String = "",
    val address: String = "",
    val idImageUrl: String = "",
    val vehicleImageUrl: String = "",
    val profileImageUrl: String = "",
    val vehicleNumber: String = "",
    val vehicleName: String = "",
    val vehicleBrand: String = "",
    val vehicleColor: String = "",
    val vehicleType: String = "SCOOTY",
    val active: Boolean = true,
    val verified: Boolean = false,
    val joinedAtEpochMs: Long = 0L,
)

@Serializable
data class AdminPartnerDetailDto(
    val partner: AdminPartnerDto,
    val acceptedOrders: List<AdminTxnDto> = emptyList(),
    val cancelledOrders: List<AdminTxnDto> = emptyList(),
    val totalDeliveredOrders: Int = 0,
    val totalEarnings: Double = 0.0,
)

@Serializable
data class AdminPartnerPatch(val active: Boolean? = null)

@Serializable
data class AdminOrderActionRequest(val reason: String? = null)

/** Mirrors `PlatformSettingsDoc`; see it for zero-means-off fields. */
@Serializable
data class PlatformSettingsDto(
    val platformFeeInr: Double = 10.0,
    val deliveryBasePerKmInr: Double = 8.0,
    val deliverySurchargePercent: Double = 30.0,
    val minDeliveryChargeInr: Double = 0.0,
    val maxDeliveryChargeInr: Double = 0.0,
    val freeDeliveryAboveInr: Double = 0.0,
    val minOrderValueInr: Double = 0.0,
    val partnerPayoutPerKmInr: Double = 8.0,
    val partnerRingKm: Double = 8.0,
    val offerTtlMinutes: Int = 5,
    val defaultShopDeliveryRadiusKm: Double = 20.0,
    val onlineOrderingEnabled: Boolean = true,
    val orderingPausedMessage: String = "",
    val partnerRegistrationEnabled: Boolean = true,
    val updatedAt: Long = 0L,
)

@Serializable
data class AdminPasswordChange(val currentPassword: String, val newPassword: String)

/** Mirrors `ProductVariantDoc`; see it for [sellMode] semantics. */
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
data class MasterProductPageDto(
    val items: List<MasterProductDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean,
)

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
data class RatingReplyRequest(val reply: String)

@Serializable
data class ShopStaffDto(
    val id: String,
    val username: String,
    val displayName: String,
    val role: String,
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
)

@Serializable
data class ShopStaffUpsert(
    val username: String = "",
    val displayName: String = "",
    val password: String = "",
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
)

@Serializable
data class ShopProfileDto(
    val shop: AdminShopDto,
    val me: ShopStaffDto,
)

/** Shop's customer-catalog category order. [sort]: PRIORITY | NAME. */
@Serializable
data class ShopCategoryOrderDto(
    val sort: String = "PRIORITY",
    val items: List<ShopCategoryOrderItemDto> = emptyList(),
)

/** One root category used by the shop; [priority] null = unranked (after ranked ones, by name). */
@Serializable
data class ShopCategoryOrderItemDto(
    val id: String,
    val name: String = "",
    val priority: Int? = null,
    val productCount: Int = 0,
)

@Serializable
data class ShopOfferDto(
    val id: String = "",
    val title: String,
    val description: String = "",
    val scope: String = "BILL",
    val discountType: String = "PERCENT",
    val value: Double,
    val productIds: List<String> = emptyList(),
    val minBillAmount: Double = 0.0,
    val active: Boolean = true,
    val startsAtEpochMs: Long? = null,
    val endsAtEpochMs: Long? = null,
)

@Serializable
data class VisitorStatsDto(
    val uniqueVisitors: Int,
    val visitDays: Int,
    val daily: List<AdminChartPointDto> = emptyList(),
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
    /** Master category (root) and optional sub-category ids; [category] stays the legacy fixed bucket. */
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    /** Best live PRODUCT offer for this item (read-only; ignored on writes). */
    val offer: ProductOfferDto? = null,
)

@Serializable
data class ProductOfferDto(
    val id: String,
    val title: String,
    /** PERCENT or FLAT (₹ off each unit). */
    val discountType: String,
    val value: Double,
)

@Serializable
data class AddFromMasterRequest(
    val masterProductId: String,
    val category: String? = null,
    val sellingPrice: Double? = null,
    val costPrice: Double? = null,
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double? = null,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
)

/** Exactly one of [delta] (may be negative) or [set] is applied; [set] wins when both are sent. */
@Serializable
data class StockUpdateRequest(val delta: Double? = null, val set: Double? = null)

@Serializable
data class ShopProductActivePatch(val active: Boolean)

@Serializable
data class ShopProductEdit(
    val name: String,
    val nameHi: String = "",
    val category: String,
    val brandName: String = "",
    val barcode: String? = null,
    val sellingPrice: Double,
    val costPrice: Double,
    val lowStockThreshold: Double,
)

@Serializable
data class ShopStockSummaryDto(
    val total: Int,
    val active: Int,
    val inactive: Int,
    val outOfStock: Int,
    val lowStock: Int,
)

@Serializable
data class CatalogSectionDto(
    /** Master category id, or "uncategorized" for products without one. */
    val category: String,
    val totalCount: Int,
    val preview: List<ProductDto>,
    val categoryName: String = "",
)

@Serializable
data class ProductPageDto(
    val items: List<ProductDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean,
)

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
    /** Master category name; blank when the product has none. */
    val categoryName: String = "",
)

@Serializable
data class PricePointDto(val at: Long, val price: Double)

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
    /** PRODUCT offers: the active products it applies to, priced with their best live offer. */
    val products: List<ProductDto> = emptyList(),
)

@Serializable
data class PlaceOrderItemDto(
    val productId: String,
    /** PACK/BUNDLE: number of packs. LOOSE: amount in the variant's base unit (0.25 = 250 g). */
    val quantity: Double,
    val variantId: String? = null,
)

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
    /** Customer collects at the shop: no delivery charge, no radius check, no partner. */
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
data class OkResponse(val ok: Boolean = true)

@Serializable
data class ProductImportErrorDto(val row: Int, val message: String)

/** Result of a product / inventory spreadsheet import; [dryRun] = checked only, nothing saved. */
@Serializable
data class ProductImportReportDto(
    val dryRun: Boolean,
    val productsCreated: Int = 0,
    val productsUpdated: Int = 0,
    val variantsCreated: Int = 0,
    val variantsUpdated: Int = 0,
    val errors: List<ProductImportErrorDto> = emptyList(),
)

/** Short-lived public download path (relative to the API base), e.g. a product export. */
@Serializable
data class FileLinkDto(val path: String)

@Serializable
data class ShopPaymentInfoDto(val shopName: String, val upiId: String = "")

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
data class ShopPartnerDto(
    val id: String,
    val name: String,
    val phone: String,
    val vehicleReg: String = "",
    /** True when the rider has a partner-app account and will get assigned orders as jobs. */
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
    val items: List<OrderDto>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val hasMore: Boolean,
    /** Orders still waiting for the shop to accept (RECEIVED), independent of the page filter. */
    val newCount: Int = 0,
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
    val channel: String = "ONLINE",
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
    /** Delivery partner's rating of the customer for this order. */
    val customerRatingStars: Int? = null,
    val customerRatingComment: String? = null,
    /** Customer's average rating from delivery partners across all their orders. */
    val customerRatingAvg: Double? = null,
    val customerRatingCount: Int = 0,
    val cancelReason: String? = null,
    val partnerProgress: String = "",
    val refundId: String? = null,
    val packedProductIds: List<String> = emptyList(),
    val deliveryMode: String = "PARTNER",
    val discount: Double = 0.0,
)

@Serializable
data class RateOrderRequest(val stars: Int, val comment: String? = null)

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
    val items: List<CustomerRatingDto>,
    val page: Int,
    /** Matching the star filter. */
    val totalCount: Int,
    val hasMore: Boolean,
    /** Unfiltered summary. */
    val average: Double,
    val ratingCount: Int,
    val starCounts: Map<Int, Int> = emptyMap(),
)

@Serializable
data class ShopActionRequest(val rejectedProductIds: List<String> = emptyList(), val reason: String? = null)

@Serializable
data class StatusPatch(val status: String)

@Serializable
data class DeliveryRequest(val packedProductIds: List<String> = emptyList())

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
data class IncomingOfferResponse(val offer: DeliveryOfferDto? = null)

@Serializable
data class PartnerRegisterRequest(
    val name: String,
    val email: String,
    val address: String,
    val phone: String,
    val vehicleReg: String,
    val platePhotoUrl: String = "",
    val vehiclePhotoUrl: String = "",
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
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val brandName: String = "",
    val companyName: String = "",
    val subcategoryId: String? = null,
    val salePrice: Double = 0.0,
    val cost: Double = 0.0,
    val active: Boolean = true,
    val addedAtEpochMs: Long = 0L,
    val description: String = "",
    val sku: String = "",
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 0.0,
    val tags: String = "",
    val weightKg: Double = 0.0,
    val dimensions: String = "",
    val variants: List<ProductVariantDto> = emptyList(),
)

@Serializable
data class MasterProductUpsert(
    val id: String? = null,
    val name: String,
    val nameHi: String = "",
    val categoryId: String,
    val unit: String = "PIECE",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val brandName: String = "",
    val companyName: String = "",
    val subcategoryId: String? = null,
    val salePrice: Double = 0.0,
    val cost: Double = 0.0,
    val active: Boolean = true,
    val description: String = "",
    val sku: String = "",
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 0.0,
    val tags: String = "",
    val weightKg: Double = 0.0,
    val dimensions: String = "",
    val variants: List<ProductVariantDto> = emptyList(),
)

@Serializable
data class MasterCategoryUpsert(
    val id: String? = null,
    val name: String,
    val parentId: String? = null,
)

@Serializable
data class ShopTypeDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
)

@Serializable
data class ShopTypeUpsert(
    val id: String? = null,
    val name: String,
    val active: Boolean = true,
)

@Serializable
data class UploadResultDto(
    val url: String,
    val thumbnailUrl: String,
    val key: String = "",
    val thumbnailKey: String = "",
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
    val orders: List<OrderDto>,
    val nextSince: Long,
    val nextAfterId: String,
    val hasMore: Boolean,
)

@Serializable
data class SyncPullResponse(
    val products: List<ProductDto>,
    val onlineOrders: List<OrderDto>,
    val masterProducts: List<MasterProductDto>,
)

@Serializable
data class AdminShopPatch(
    val active: Boolean? = null,
    val paymentsEnabled: Boolean? = null,
    val features: ShopFeaturesDto? = null,
    val imageUrl: String? = null,
    val name: String? = null,
    val address: String? = null,
    val shopType: String? = null,
    val ownerName: String? = null,
    val ownerPhone: String? = null,
    val ownerEmail: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val country: String? = null,
    val registrationNumber: String? = null,
    val taxId: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val operatingHours: List<ShopHoursDayDto>? = null,
    val loginUsername: String? = null,
    val loginPassword: String? = null,
    val upiId: String? = null,
    val deliveryRadiusKm: Double? = null,
    val discountPercent: Double? = null,
    /** "HH:mm"; empty string clears the cutoff. */
    val deliveryUntil: String? = null,
    val homeDeliveryEnabled: Boolean? = null,
)
