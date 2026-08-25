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
)

@Serializable
data class ShopLoginRequest(val username: String, val password: String)

@Serializable
data class AdminLoginRequest(val username: String, val password: String)

@Serializable
data class FirebaseAuthRequest(val idToken: String)

@Serializable
data class OtpRequest(val phone: String)

@Serializable
data class OtpVerifyRequest(val phone: String, val otp: String, val sessionId: String? = null)

@Serializable
data class OtpSessionResponse(
    val sessionId: String,
    /** Present when SMS is not sent (dev / no SMS provider). Client may show as hint. */
    val devOtp: String? = null,
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
    val offerCount: Int = 0,
    val discountPercent: Double = 0.0,
    val upiId: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val shopType: String = "GENERAL_STORE",
    val active: Boolean = true,
    val paymentsEnabled: Boolean = true,
    val address: String = "",
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
)

@Serializable
data class ShopFeaturesDto(
    val voiceSearch: Boolean = true,
    val barcodeSearch: Boolean = true,
    val reportGeneration: Boolean = true,
    val onlineOrders: Boolean = true,
    val inventoryAlerts: Boolean = true,
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

@Serializable
data class MasterProductVariantDto(
    val name: String,
    val sku: String = "",
    val price: Double = 0.0,
)

@Serializable
data class ShopReviewDto(
    val id: String,
    val customerName: String,
    val stars: Int,
    val comment: String? = null,
    val createdAtEpochMs: Long,
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
data class OrderItemDto(
    val productId: String,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val imageUrl: String? = null,
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
    val partnerProgress: String = "",
    val refundId: String? = null,
)

@Serializable
data class RateOrderRequest(val stars: Int, val comment: String? = null)

@Serializable
data class ShopActionRequest(val rejectedProductIds: List<String> = emptyList(), val reason: String? = null)

@Serializable
data class StatusPatch(val status: String)

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
    val platePhotoBase64: String = "",
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
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
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
    val variants: List<MasterProductVariantDto> = emptyList(),
)

@Serializable
data class MasterProductUpsert(
    val id: String? = null,
    val name: String,
    val nameHi: String = "",
    val categoryId: String,
    val unit: String = "KG",
    val barcode: String? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
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
    val variants: List<MasterProductVariantDto> = emptyList(),
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
data class AdminShopPatch(
    val active: Boolean? = null,
    val paymentsEnabled: Boolean? = null,
    val features: ShopFeaturesDto? = null,
    val imageUrl: String? = null,
    val name: String? = null,
    val address: String? = null,
    val shopType: String? = null,
)
