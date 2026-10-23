package org.bhargav.pansariwala.master

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_network_error
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.bhargav.pansariwala.util.AppConstants
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.api.CommissionSummaryDto
import org.bhargav.pansariwala.api.ShopSubscriptionDto
import org.bhargav.pansariwala.api.SubscriptionPlanDto
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.api.installPansariHttpLogging
import org.bhargav.pansariwala.api.shouldInstallHttpLogging

/** Server error with the API's `{"error": "..."}` message instead of Ktor's raw request dump. */
class MasterApiException(message: String, val status: Int) : Exception(message)

/** Session hooks shared by every screen; the shell sets [onExpired] to drop back to login on 401. */
object MasterSession {
    var onExpired: (() -> Unit)? = null

    /** True when the JWT `exp` claim is in the past (or the token is unreadable). */
    @OptIn(ExperimentalEncodingApi::class)
    fun isExpired(token: String, nowEpochMs: Long): Boolean {
        val payload = token.split('.').getOrNull(1) ?: return true
        val exp = runCatching {
            val bytes = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL).decode(payload)
            masterJson.parseToJsonElement(bytes.decodeToString()).jsonObject["exp"]?.jsonPrimitive?.longOrNull
        }.getOrNull() ?: return true
        return exp * 1_000L <= nowEpochMs
    }
}

@Serializable private data class ApiErrorBody(val error: String = "")

private val masterJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** One client for all screens: avoids a fresh connection pool per screen. */
private val masterClient: HttpClient by lazy {
    createPlatformHttpClient().config {
        expectSuccess = true
        install(ContentNegotiation) { json(masterJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = AppConstants.MasterWeb.CONNECT_TIMEOUT_MS
            requestTimeoutMillis = AppConstants.MasterWeb.REQUEST_TIMEOUT_MS
            socketTimeoutMillis = AppConstants.MasterWeb.REQUEST_TIMEOUT_MS
        }
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, request ->
                val response = (cause as? ResponseException)?.response
                if (response == null) {
                    if (cause is CancellationException && cause !is HttpRequestTimeoutException) throw cause
                    throw MasterApiException(getString(Res.string.master_network_error), 0)
                }
                val status = response.status
                if (status == HttpStatusCode.Unauthorized &&
                    !request.url.encodedPath.endsWith(AppConstants.MasterWeb.LOGIN_PATH)
                ) {
                    MasterSession.onExpired?.invoke()
                }
                val text = runCatching { response.bodyAsText() }.getOrDefault("")
                val message = runCatching { masterJson.decodeFromString<ApiErrorBody>(text).error }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: status.description
                throw MasterApiException(message, status.value)
            }
        }
        if (shouldInstallHttpLogging()) {
            install(Logging) { installPansariHttpLogging() }
        }
    }
}

class MasterApi(private val baseUrl: String = ApiRuntime.baseUrl) {
    private val client get() = masterClient

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

    suspend fun productPage(
        token: String,
        query: String,
        categoryId: String?,
        page: Int,
        pageSize: Int,
        shopTypeId: String? = null,
        subcategoryId: String? = null,
    ): ProductPageDto =
        client.get(url("/master/products/page")) {
            bearer(token)
            parameter("q", query)
            categoryId?.takeIf { it.isNotBlank() }?.let { parameter("categoryId", it) }
            shopTypeId?.takeIf { it.isNotBlank() }?.let { parameter("shopTypeId", it) }
            subcategoryId?.takeIf { it.isNotBlank() }?.let { parameter("subcategoryId", it) }
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()

    suspend fun product(token: String, id: String): ProductDto =
        client.get(url("/master/products/item/$id")) { bearer(token) }.body()

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

    suspend fun uploadFile(
        token: String,
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
    ): UploadResultDto =
        client.post(url("/admin/uploads")) {
            bearer(token)
            timeout {
                requestTimeoutMillis = AppConstants.HTTP_UPLOAD_TIMEOUT_MS
                socketTimeoutMillis = AppConstants.HTTP_UPLOAD_TIMEOUT_MS
            }
            parameter("prefix", prefix.trim('/'))
            setBody(
                io.ktor.client.request.forms.MultiPartFormDataContent(
                    io.ktor.client.request.forms.formData {
                        append(
                            "file",
                            bytes,
                            io.ktor.http.Headers.build {
                                append(io.ktor.http.HttpHeaders.ContentType, contentType)
                                append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                    },
                ),
            )
        }.body()

    val productTemplateUrl: String get() = url("/" + AppConstants.ProductSheet.TEMPLATE_PATH)

    suspend fun importProducts(
        token: String,
        fileName: String,
        bytes: ByteArray,
        dryRun: Boolean,
    ): org.bhargav.pansariwala.api.ProductImportReportDto =
        client.post(url("/admin/master/products/import")) {
            bearer(token)
            timeout {
                requestTimeoutMillis = AppConstants.HTTP_UPLOAD_TIMEOUT_MS
                socketTimeoutMillis = AppConstants.HTTP_UPLOAD_TIMEOUT_MS
            }
            parameter("dryRun", dryRun)
            setBody(
                io.ktor.client.request.forms.MultiPartFormDataContent(
                    io.ktor.client.request.forms.formData {
                        append(
                            "file",
                            bytes,
                            io.ktor.http.Headers.build {
                                append(io.ktor.http.HttpHeaders.ContentType, AppConstants.ProductSheet.MIME_XLSX)
                                append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                    },
                ),
            )
        }.body()

    /** One-time link (no auth header needed) so the browser can download the export. */
    suspend fun productsExportUrl(token: String): String =
        url("/" + client.post(url("/admin/master/products/export-link")) { bearer(token) }
            .body<org.bhargav.pansariwala.api.FileLinkDto>().path.trimStart('/'))

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
        ownerName: String? = null,
        ownerPhone: String? = null,
        ownerEmail: String? = null,
        city: String? = null,
        state: String? = null,
        zip: String? = null,
        country: String? = null,
        registrationNumber: String? = null,
        taxId: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        operatingHours: List<ShopHoursDayDto>? = null,
        loginUsername: String? = null,
        loginPassword: String? = null,
        upiId: String? = null,
        deliveryRadiusKm: Double? = null,
        discountPercent: Double? = null,
        deliveryUntil: String? = null,
        homeDeliveryEnabled: Boolean? = null,
        minOrderValueInr: Double? = null,
        freeDeliveryEnabled: Boolean? = null,
        freeDeliveryAboveInr: Double? = null,
    ) {
        client.post(url("/admin/shops/$id")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(
                ShopPatch(
                    active = active, paymentsEnabled = paymentsEnabled, features = features, imageUrl = imageUrl,
                    name = name, address = address, shopType = shopType,
                    ownerName = ownerName, ownerPhone = ownerPhone, ownerEmail = ownerEmail,
                    city = city, state = state, zip = zip, country = country,
                    registrationNumber = registrationNumber, taxId = taxId, lat = lat, lng = lng,
                    operatingHours = operatingHours, loginUsername = loginUsername, loginPassword = loginPassword,
                    upiId = upiId, deliveryRadiusKm = deliveryRadiusKm, discountPercent = discountPercent,
                    deliveryUntil = deliveryUntil, homeDeliveryEnabled = homeDeliveryEnabled,
                    minOrderValueInr = minOrderValueInr, freeDeliveryEnabled = freeDeliveryEnabled,
                    freeDeliveryAboveInr = freeDeliveryAboveInr,
                ),
            )
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
        client.get(url("/admin/orders/${id.encodeURLPathPart()}")) { bearer(token) }.body()

    suspend fun cancelOrder(token: String, id: String, reason: String? = null) {
        client.post(url("/admin/orders/${id.encodeURLPathPart()}/cancel")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(OrderActionBody(reason))
        }
    }

    suspend fun refundOrder(token: String, id: String) {
        client.post(url("/admin/orders/${id.encodeURLPathPart()}/refund")) { bearer(token) }
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

    suspend fun setShopVendorId(token: String, shopId: String, vendorId: String) {
        client.post(url("/admin/shops/$shopId/cashfree-vendor")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(VendorIdBody(vendorId))
        }
    }

    suspend fun setPartnerVendorId(token: String, partnerId: String, vendorId: String) {
        client.post(url("/admin/partners/$partnerId/cashfree-vendor")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(VendorIdBody(vendorId))
        }
    }

    suspend fun patchPartner(token: String, id: String, active: Boolean) {
        client.post(url("/admin/partners/$id")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(ActivePatch(active))
        }
    }

    suspend fun platformSettings(token: String): PlatformSettingsDto =
        client.get(url("/admin/platform-settings")) { bearer(token) }.body()

    suspend fun savePlatformSettings(token: String, body: PlatformSettingsDto): PlatformSettingsDto =
        client.post(url("/admin/platform-settings")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun savePaymentSettings(token: String, body: PlatformSettingsDto): PlatformSettingsDto =
        client.post(url("/admin/payment-settings")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun subscriptionPlans(token: String): List<SubscriptionPlanDto> =
        client.get(url("/admin/subscription-plans")) { bearer(token) }.body()

    suspend fun saveSubscriptionPlan(token: String, body: SubscriptionPlanDto): SubscriptionPlanDto =
        client.post(url("/admin/subscription-plans")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun deleteSubscriptionPlan(token: String, id: String) {
        client.delete(url("/admin/subscription-plans/$id")) { bearer(token) }
    }

    suspend fun billingSettings(token: String): BillingSettingsDto =
        client.get(url("/admin/billing-settings")) { bearer(token) }.body()

    suspend fun saveBillingSettings(token: String, body: BillingSettingsDto): BillingSettingsDto =
        client.post(url("/admin/billing-settings")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun shopSubscription(token: String, shopId: String): ShopSubscriptionDto =
        client.get(url("/admin/shops/$shopId/subscription")) { bearer(token) }.body()

    suspend fun updateShopBilling(token: String, shopId: String, body: AdminBillingUpdate): ShopSubscriptionDto =
        client.post(url("/admin/shops/$shopId/billing")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun shopCommission(token: String, shopId: String): CommissionSummaryDto =
        client.get(url("/admin/shops/$shopId/commission")) { bearer(token) }.body()

    suspend fun changePassword(token: String, currentPassword: String, newPassword: String) {
        client.post(url("/admin/account/password")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(PasswordChangeBody(currentPassword, newPassword))
        }
    }

    suspend fun shopProducts(
        token: String,
        shopId: String,
        query: String,
        category: String?,
        status: String,
        lowStock: Boolean,
        page: Int,
        pageSize: Int,
    ): ShopProductPageDto =
        client.get(url("/admin/shops/${shopId.encodeURLPathPart()}/products")) {
            bearer(token)
            if (query.isNotBlank()) parameter("q", query.trim())
            category?.let { parameter("category", it) }
            parameter("status", status)
            if (lowStock) parameter("lowStock", true)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()

    suspend fun shopStockSummary(token: String, shopId: String): ShopStockSummaryDto =
        client.get(url("/admin/shops/${shopId.encodeURLPathPart()}/stock-summary")) { bearer(token) }.body()

    suspend fun shopMasterLinks(token: String, shopId: String, masterIds: List<String>): Map<String, String> =
        if (masterIds.isEmpty()) emptyMap() else client.get(url("/admin/shops/${shopId.encodeURLPathPart()}/master-links")) {
            bearer(token)
            parameter("ids", masterIds.joinToString(","))
        }.body()

    suspend fun addShopProductFromMaster(token: String, shopId: String, body: AddFromMasterBody): ShopProductDto =
        client.post(url("/admin/shops/${shopId.encodeURLPathPart()}/products/from-master")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun updateShopProductStock(token: String, shopId: String, productId: String, body: StockUpdateBody): ShopProductDto =
        client.post(url("/admin/shops/${shopId.encodeURLPathPart()}/products/${productId.encodeURLPathPart()}/stock")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun setShopProductActive(token: String, shopId: String, productId: String, active: Boolean): ShopProductDto =
        client.post(url("/admin/shops/${shopId.encodeURLPathPart()}/products/${productId.encodeURLPathPart()}/active")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(ShopProductActiveBody(active))
        }.body()

    suspend fun saveMasterTranslations(token: String, body: ProductTranslationsBody): ProductTranslationsResultDto =
        client.post(url("/admin/master/products/translations")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun saveProductTranslation(token: String, productId: String, body: ProductFieldsTranslationBody): ProductDto =
        client.post(url("/admin/master/products/${productId.encodeURLPathPart()}/translation")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun saveCategoryTranslation(token: String, categoryId: String, body: CategoryTranslationBody): CategoryDto =
        client.post(url("/admin/master/categories/${categoryId.encodeURLPathPart()}/translation")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun serverMessages(token: String): List<ServerMessageDto> =
        client.get(url("/admin/messages")) { bearer(token) }.body()

    suspend fun saveServerMessageTranslations(token: String, body: ProductTranslationsBody): ProductTranslationsResultDto =
        client.post(url("/admin/messages/translations")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun saveShopTranslations(token: String, shopId: String, body: ProductTranslationsBody): ProductTranslationsResultDto =
        client.post(url("/admin/shops/${shopId.encodeURLPathPart()}/products/translations")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun editShopProduct(token: String, shopId: String, productId: String, body: ShopProductEditBody): ShopProductDto =
        client.post(url("/admin/shops/${shopId.encodeURLPathPart()}/products/${productId.encodeURLPathPart()}")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun appUsage(
        token: String,
        app: String?,
        platform: String?,
        version: String?,
        paymentMethod: String?,
    ): AppUsageReportDto =
        client.get(url("/admin/app-usage")) {
            bearer(token)
            app?.let { parameter("app", it) }
            platform?.let { parameter("platform", it) }
            version?.let { parameter("version", it) }
            paymentMethod?.let { parameter("paymentMethod", it) }
        }.body()

    suspend fun issues(token: String, status: String?, app: String?): List<AdminIssueDto> =
        client.get(url("/admin/issues")) {
            bearer(token)
            status?.let { parameter("status", it) }
            app?.let { parameter("app", it) }
        }.body()

    suspend fun resolveIssue(token: String, id: String, body: IssueResolveBody): AdminIssueDto =
        client.post(url("/admin/issues/${id.encodeURLPathPart()}/resolve")) {
            bearer(token)
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun reopenIssue(token: String, id: String): AdminIssueDto =
        client.post(url("/admin/issues/${id.encodeURLPathPart()}/reopen")) { bearer(token) }.body()

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        headers.append("Authorization", "Bearer $token")
    }
}

@Serializable
data class AppVersionCountDto(
    val app: String,
    val platform: String,
    val versionName: String,
    val versionCode: Long = 0L,
    val users: Int,
)

@Serializable
data class PaymentMethodUsageDto(val method: String, val users: Int, val orders: Int)

@Serializable
data class AppUserRowDto(
    val principalId: String,
    val role: String,
    val app: String,
    val platform: String,
    val versionName: String,
    val versionCode: Long = 0L,
    val name: String = "",
    val phone: String = "",
    val lastSeenAt: Long = 0L,
    val paymentMethods: List<String> = emptyList(),
)

@Serializable
data class AppUsageReportDto(
    val totalUsers: Int = 0,
    val versions: List<AppVersionCountDto> = emptyList(),
    val paymentMethods: List<PaymentMethodUsageDto> = emptyList(),
    val users: List<AppUserRowDto> = emptyList(),
)

@Serializable
data class AdminIssueDto(
    val id: String,
    val app: String,
    val reporterId: String = "",
    val reporterRole: String = "",
    val reporterName: String = "",
    val reporterPhone: String = "",
    val feature: String = "",
    val featureLabel: String = "",
    val issueType: String = "",
    val issueLabel: String = "",
    val description: String = "",
    val attachments: List<String> = emptyList(),
    val platform: String = "",
    val appVersion: String = "",
    val status: String = "",
    val createdAt: Long = 0L,
    val resolvedAt: Long? = null,
    val fixedInVersion: String = "",
    val resolutionNote: String = "",
)

@Serializable data class IssueResolveBody(val fixedInVersion: String = "", val note: String = "")

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

typealias ProductVariantDto = org.bhargav.pansariwala.api.ProductVariantDto

/** A product in one shop's inventory (not the master catalog item). */
typealias ShopProductDto = org.bhargav.pansariwala.api.ProductDto

@Serializable
data class ShopProductPageDto(
    val items: List<ShopProductDto> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class ShopStockSummaryDto(
    val total: Int = 0,
    val active: Int = 0,
    val inactive: Int = 0,
    val outOfStock: Int = 0,
    val lowStock: Int = 0,
)

@Serializable
data class AddFromMasterBody(
    val masterProductId: String,
    val category: String,
    val sellingPrice: Double,
    val costPrice: Double,
    val stockQty: Double,
    val lowStockThreshold: Double,
)

@Serializable data class StockUpdateBody(val delta: Double? = null, val set: Double? = null)
@Serializable data class ShopProductActiveBody(val active: Boolean)

@Serializable
data class ShopProductEditBody(
    val name: String,
    val nameHi: String,
    val category: String,
    val brandName: String,
    val barcode: String?,
    val sellingPrice: Double,
    val costPrice: Double,
    val lowStockThreshold: Double,
)

/** One language column of product names; a blank [ProductTranslationItemBody.name] clears it. */
@Serializable
data class ProductTranslationsBody(val language: String, val items: List<ProductTranslationItemBody>)

@Serializable
data class ProductTranslationItemBody(val id: String, val name: String)

@Serializable
data class ProductTranslationsResultDto(val updated: Int = 0)

@Serializable
data class ProductPageDto(
    val items: List<ProductDto> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 0,
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
)

@Serializable
data class ProductDto(
    val id: String,
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
    val addedAtEpochMs: Long = 0L,
    val description: String = "",
    val sku: String = "",
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 0.0,
    val tags: String = "",
    val weightKg: Double = 0.0,
    val dimensions: String = "",
    val variants: List<ProductVariantDto> = emptyList(),
    val translations: Map<String, String> = emptyMap(),
    val brandTranslations: Map<String, String> = emptyMap(),
    val companyTranslations: Map<String, String> = emptyMap(),
    val descriptionTranslations: Map<String, String> = emptyMap(),
)

/** Every translatable field of one product in [language]; blank clears that field's translation. */
@Serializable
data class ProductFieldsTranslationBody(
    val language: String,
    val name: String,
    val brandName: String,
    val companyName: String,
    val description: String,
)

@Serializable
data class CategoryTranslationBody(val language: String, val name: String)

/** [id] is the English text; `{0}`, `{1}` are runtime values the server fills in. */
@Serializable
data class ServerMessageDto(val id: String, val translations: Map<String, String> = emptyMap())

@Serializable
data class ProductUpsert(
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
data class UploadResultDto(
    val url: String,
    val thumbnailUrl: String,
)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val translations: Map<String, String> = emptyMap(),
    val shopTypeIds: List<String> = emptyList(),
)
@Serializable data class CategoryUpsert(
    val id: String? = null,
    val name: String,
    val parentId: String? = null,
    val shopTypeIds: List<String>? = null,
)
@Serializable data class ShopTypeDto(val id: String, val name: String, val active: Boolean = true)
@Serializable data class ShopTypeUpsert(val id: String? = null, val name: String, val active: Boolean = true)
@Serializable private data class PasswordChangeBody(val currentPassword: String, val newPassword: String)

/** Zero means "off" for max/free-delivery/min-order fields. */
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
    val minUserSearchRadiusKm: Double = 1.0,
    val onlineOrderingEnabled: Boolean = true,
    val orderingPausedMessage: String = "",
    val partnerRegistrationEnabled: Boolean = true,
    /** DIRECT_UPI, CASHFREE or RAZORPAY. */
    val paymentMode: String = "DIRECT_UPI",
    val paymentWindowMinutes: Int = 5,
    val cashfreeConfigured: Boolean = false,
    val razorpayConfigured: Boolean = false,
    val updatedAt: Long = 0L,
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
data class BillingSettingsDto(
    val defaultCommissionPercent: Double = 1.5,
    val expiryWarningDays: Int = 7,
)

@Serializable
data class AdminBillingUpdate(
    val model: String,
    val planId: String? = null,
    val commissionPercent: Double? = null,
    val expiresAtEpochMs: Long? = null,
    val freeUntilEpochMs: Long? = null,
    val note: String = "",
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
    val loginUsername: String = "",
    val isOpen: Boolean = true,
    val upiId: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val discountPercent: Double = 0.0,
    val deliveryUntil: String = "",
    val homeDeliveryEnabled: Boolean = true,
    val minOrderValueInr: Double = AppConstants.DEFAULT_SHOP_MIN_ORDER_INR,
    val freeDeliveryEnabled: Boolean = false,
    val freeDeliveryAboveInr: Double = AppConstants.DEFAULT_SHOP_FREE_DELIVERY_ABOVE_INR,
    val cashfreeVendorId: String = "",
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
    val minOrderValueInr: Double? = null,
    val freeDeliveryEnabled: Boolean? = null,
    val freeDeliveryAboveInr: Double? = null,
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
    val loginUsername: String = "",
    val loginPassword: String = "",
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
    val cashfreeVendorId: String = "",
)

@Serializable
data class VendorIdBody(val vendorId: String)

@Serializable
data class PartnerDetailDto(
    val partner: PartnerDto,
    val acceptedOrders: List<TxnDto> = emptyList(),
    val cancelledOrders: List<TxnDto> = emptyList(),
    val totalDeliveredOrders: Int = 0,
    val totalEarnings: Double = 0.0,
)
