package org.bhargav.pansariwala.master

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.api.createPlatformHttpClient

class MasterApi(private val baseUrl: String = ApiRuntime.baseUrl) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val client = createPlatformHttpClient().config {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
    }

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    suspend fun login(username: String, password: String): TokenDto =
        client.post(url("/auth/admin/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginBody(username, password))
        }.body()

    suspend fun dashboard(token: String, from: Long? = null, to: Long? = null): AdminDashboardDto =
        client.get(url("/admin/dashboard")) {
            bearer(token)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }.body()

    suspend fun products(token: String): List<ProductDto> =
        client.get(url("/master/products")) { bearer(token) }.body()

    suspend fun categories(token: String): List<CategoryDto> =
        client.get(url("/master/categories")) { bearer(token) }.body()

    suspend fun shopTypes(token: String): List<ShopTypeDto> =
        client.get(url("/master/shop-types")) { bearer(token) }.body()

    suspend fun shops(token: String): List<ShopDto> =
        client.get(url("/admin/shops")) { bearer(token) }.body()

    suspend fun shopDetail(token: String, id: String): ShopDetailDto =
        client.get(url("/admin/shops/$id")) { bearer(token) }.body()

    suspend fun saveProduct(token: String, body: ProductUpsert): ProductDto =
        client.post(url("/admin/master/products")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteProduct(token: String, id: String) {
        client.delete(url("/admin/master/products/$id")) { bearer(token) }
    }

    suspend fun saveCategory(token: String, body: CategoryUpsert): CategoryDto =
        client.post(url("/admin/master/categories")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteCategory(token: String, id: String) {
        client.delete(url("/admin/master/categories/$id")) { bearer(token) }
    }

    suspend fun saveShopType(token: String, body: ShopTypeUpsert): ShopTypeDto =
        client.post(url("/admin/master/shop-types")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteShopType(token: String, id: String) {
        client.delete(url("/admin/master/shop-types/$id")) { bearer(token) }
    }

    suspend fun patchShop(
        token: String,
        id: String,
        active: Boolean? = null,
        paymentsEnabled: Boolean? = null,
        features: ShopFeaturesDto? = null,
        imageUrl: String? = null,
        name: String? = null,
        address: String? = null,
        shopType: String? = null,
    ) {
        client.post(url("/admin/shops/$id")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(ShopPatch(active, paymentsEnabled, features, imageUrl, name, address, shopType))
        }
    }

    suspend fun createShop(token: String, body: ShopCreate): ShopDto =
        client.post(url("/admin/shops")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun transactions(token: String, from: Long? = null, to: Long? = null): TxnSummaryDto =
        client.get(url("/admin/orders")) {
            bearer(token)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }.body()

    suspend fun orderDetail(token: String, id: String): TxnDto =
        client.get(url("/admin/orders/$id")) { bearer(token) }.body()

    suspend fun cancelOrder(token: String, id: String, reason: String? = null) {
        client.post(url("/admin/orders/$id/cancel")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(OrderActionBody(reason))
        }
    }

    suspend fun refundOrder(token: String, id: String) {
        client.post(url("/admin/orders/$id/refund")) { bearer(token) }
    }

    suspend fun users(token: String, from: Long? = null, to: Long? = null): List<UserDto> =
        client.get(url("/admin/users")) {
            bearer(token)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }.body()

    suspend fun userDetail(token: String, id: String): UserDetailDto =
        client.get(url("/admin/users/$id")) { bearer(token) }.body()

    suspend fun patchUser(token: String, id: String, active: Boolean) {
        client.post(url("/admin/users/$id")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(ActivePatch(active))
        }
    }

    suspend fun partners(token: String, from: Long? = null, to: Long? = null): List<PartnerDto> =
        client.get(url("/admin/partners")) {
            bearer(token)
            from?.let { parameter("from", it) }
            to?.let { parameter("to", it) }
        }.body()

    suspend fun partnerDetail(token: String, id: String): PartnerDetailDto =
        client.get(url("/admin/partners/$id")) { bearer(token) }.body()

    suspend fun patchPartner(token: String, id: String, active: Boolean) {
        client.post(url("/admin/partners/$id")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(ActivePatch(active))
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        headers.append("Authorization", "Bearer $token")
    }
}

@Serializable data class LoginBody(val username: String, val password: String)
@Serializable data class TokenDto(val accessToken: String, val role: String = "ADMIN")
@Serializable data class ActivePatch(val active: Boolean? = null)
@Serializable data class OrderActionBody(val reason: String? = null)

@Serializable data class AdminChartPointDto(val label: String, val value: Double)

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
data class ProductVariantDto(val name: String, val sku: String = "", val price: Double = 0.0)

@Serializable
data class ProductDto(
    val id: String,
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
data class ProductUpsert(
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
    val variants: List<ProductVariantDto> = emptyList(),
)

@Serializable data class CategoryDto(val id: String, val name: String, val parentId: String? = null)
@Serializable data class CategoryUpsert(val id: String? = null, val name: String, val parentId: String? = null)
@Serializable data class ShopTypeDto(val id: String, val name: String, val active: Boolean = true)
@Serializable data class ShopTypeUpsert(val id: String? = null, val name: String, val active: Boolean = true)

@Serializable
data class ShopFeaturesDto(
    val voiceSearch: Boolean = true,
    val barcodeSearch: Boolean = true,
    val reportGeneration: Boolean = true,
    val onlineOrders: Boolean = true,
    val inventoryAlerts: Boolean = true,
)

@Serializable
data class ShopHoursDayDto(
    val day: String,
    val start: String = "09:00",
    val end: String = "21:00",
    val closed: Boolean = false,
)

@Serializable
data class ShopDto(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val paymentsEnabled: Boolean = true,
    val shopType: String = "GENERAL_STORE",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
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
data class ShopDetailDto(
    val shop: ShopDto,
    val transactions: List<TxnDto> = emptyList(),
    val orderCount: Int = 0,
    val uniqueCustomers: Int = 0,
)

@Serializable
data class ShopPatch(
    val active: Boolean? = null,
    val paymentsEnabled: Boolean? = null,
    val features: ShopFeaturesDto? = null,
    val imageUrl: String? = null,
    val name: String? = null,
    val address: String? = null,
    val shopType: String? = null,
)

@Serializable
data class ShopCreate(
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
data class TxnItemDto(
    val productId: String = "",
    val productName: String = "",
    val unit: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val imageUrl: String? = null,
)

@Serializable
data class TxnDto(
    val orderId: String,
    val transactionNo: String = "",
    val createdAtEpochMs: Long = 0L,
    val status: String = "",
    val itemsSummary: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val customerId: String? = null,
    val shopId: String = "",
    val shopName: String = "",
    val offers: Double = 0.0,
    val charges: Double = 0.0,
    val total: Double = 0.0,
    val paid: Double = 0.0,
    val paymentMethod: String = "ONLINE",
    val refundId: String? = null,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val items: List<TxnItemDto> = emptyList(),
    val deliveryDurationMin: Int? = null,
    val partnerVehicleReg: String? = null,
)

@Serializable
data class TxnSummaryDto(
    val amount: Double = 0.0,
    val count: Int = 0,
    val transactions: List<TxnDto> = emptyList(),
)

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val phone: String,
    val address: String = "",
    val imageUrl: String? = null,
    val active: Boolean = true,
    val joinedAtEpochMs: Long = 0L,
)

@Serializable
data class UserDetailDto(val user: UserDto, val orders: List<TxnDto> = emptyList())

@Serializable
data class PartnerDto(
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
data class PartnerDetailDto(
    val partner: PartnerDto,
    val acceptedOrders: List<TxnDto> = emptyList(),
    val cancelledOrders: List<TxnDto> = emptyList(),
    val totalDeliveredOrders: Int = 0,
    val totalEarnings: Double = 0.0,
)
