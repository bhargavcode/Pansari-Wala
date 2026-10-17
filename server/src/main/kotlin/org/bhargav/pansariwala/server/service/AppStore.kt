package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.Filters.gt
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Updates.inc
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.nin
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Projections.exclude
import com.mongodb.client.model.Projections.include
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bson.Document
import org.bson.conversions.Bson
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.db.AdminUserDoc
import org.bhargav.pansariwala.server.db.CategoryDoc
import org.bhargav.pansariwala.server.dto.OtpConfigResponse
import org.bhargav.pansariwala.server.security.OtpAppProof
import org.bhargav.pansariwala.server.db.CustomerAddressDoc
import org.bhargav.pansariwala.server.db.CustomerDoc
import org.bhargav.pansariwala.server.db.DeliveryOfferDoc
import org.bhargav.pansariwala.server.db.MasterProductDoc
import org.bhargav.pansariwala.server.db.ProductVariantDoc
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.db.OfferDoc
import org.bhargav.pansariwala.server.db.OrderDoc
import org.bhargav.pansariwala.server.db.OtpDoc
import org.bhargav.pansariwala.server.db.PartnerDoc
import org.bhargav.pansariwala.server.db.PlatformSettingsDoc
import org.bhargav.pansariwala.server.dto.PlatformSettingsDto
import org.bhargav.pansariwala.server.dto.ProductOfferDto
import org.bhargav.pansariwala.server.db.ProductDoc
import org.bhargav.pansariwala.server.db.ShopDoc
import org.bhargav.pansariwala.server.db.ShopFeaturesDoc
import org.bhargav.pansariwala.server.db.ShopHoursDayDoc
import org.bhargav.pansariwala.server.db.ShopTypeDoc
import org.bhargav.pansariwala.server.db.ShopUserDoc
import org.bhargav.pansariwala.server.db.ShopPartnerDoc
import org.bhargav.pansariwala.server.dto.ShopPartnerDto
import org.bhargav.pansariwala.server.dto.ShopPartnerRequest
import org.bhargav.pansariwala.server.db.ShopVisitDoc
import org.bhargav.pansariwala.server.db.TxnDoc
import org.bhargav.pansariwala.server.dto.AdminShopPatch
import org.bhargav.pansariwala.server.dto.ShopOfferDto
import org.bhargav.pansariwala.server.dto.ShopProfileDto
import org.bhargav.pansariwala.server.dto.ShopStaffDto
import org.bhargav.pansariwala.server.dto.ShopStaffUpsert
import org.bhargav.pansariwala.server.dto.VisitorStatsDto
import org.bhargav.pansariwala.server.dto.AdminChartPointDto
import org.bhargav.pansariwala.server.dto.AdminDashboardDto
import org.bhargav.pansariwala.server.dto.AdminPartnerDetailDto
import org.bhargav.pansariwala.server.dto.AdminPartnerDto
import org.bhargav.pansariwala.server.dto.AdminShopDetailDto
import org.bhargav.pansariwala.server.dto.AdminShopDto
import org.bhargav.pansariwala.server.dto.AdminTxnDto
import org.bhargav.pansariwala.server.dto.AdminTxnSummaryDto
import org.bhargav.pansariwala.server.dto.AdminUserDetailDto
import org.bhargav.pansariwala.server.dto.AdminUserDto
import org.bhargav.pansariwala.server.dto.CreateRazorpayRequest
import org.bhargav.pansariwala.server.dto.CustomerDto
import org.bhargav.pansariwala.server.dto.DeliveryOfferDto
import org.bhargav.pansariwala.server.dto.MasterCategoryDto
import org.bhargav.pansariwala.server.dto.MasterCategoryUpsert
import org.bhargav.pansariwala.server.dto.MasterProductDto
import org.bhargav.pansariwala.server.dto.MasterProductUpsert
import org.bhargav.pansariwala.server.dto.ProductVariantDto
import org.bhargav.pansariwala.server.dto.MasterProductPageDto
import org.bhargav.pansariwala.server.dto.OfferDto
import org.bhargav.pansariwala.server.dto.OrderDto
import org.bhargav.pansariwala.server.dto.OrderPageDto
import org.bhargav.pansariwala.server.dto.OrderItemDto
import org.bhargav.pansariwala.server.dto.OtpSessionResponse
import org.bhargav.pansariwala.server.dto.PartnerDailyEarningDto
import org.bhargav.pansariwala.server.dto.ShopHoursDayDto
import org.bhargav.pansariwala.server.dto.PartnerDashboardDto
import org.bhargav.pansariwala.server.dto.PartnerEarningsDto
import org.bhargav.pansariwala.server.dto.PartnerProfileDto
import org.bhargav.pansariwala.server.dto.PartnerRegisterRequest
import org.bhargav.pansariwala.server.dto.PlaceOrderItemDto
import org.bhargav.pansariwala.server.dto.PlaceOrderRequest
import org.bhargav.pansariwala.server.dto.ProductDto
import org.bhargav.pansariwala.server.dto.CatalogSectionDto
import org.bhargav.pansariwala.server.dto.ProductDetailDto
import org.bhargav.pansariwala.server.dto.PricePointDto
import org.bhargav.pansariwala.server.db.PriceHistoryDoc
import org.bhargav.pansariwala.server.dto.ProductPageDto
import org.bhargav.pansariwala.server.dto.AddFromMasterRequest
import org.bhargav.pansariwala.server.dto.ShopProductEdit
import org.bhargav.pansariwala.server.dto.ShopStockSummaryDto
import org.bhargav.pansariwala.server.dto.StockUpdateRequest
import org.bhargav.pansariwala.server.dto.QuoteDto
import org.bhargav.pansariwala.server.dto.QuoteRequest
import org.bhargav.pansariwala.server.dto.RazorpayOrderDto
import org.bhargav.pansariwala.server.dto.SaveAddressRequest
import org.bhargav.pansariwala.server.dto.SavedAddressDto
import org.bhargav.pansariwala.server.dto.ShopDto
import org.bhargav.pansariwala.server.dto.ShopFeaturesDto
import org.bhargav.pansariwala.server.dto.ShopReviewDto
import org.bhargav.pansariwala.server.dto.ShopTypeDto
import org.bhargav.pansariwala.server.dto.ShopTypeUpsert
import org.bhargav.pansariwala.server.dto.OfflineOrderAck
import org.bhargav.pansariwala.server.dto.SyncPullResponse
import org.bhargav.pansariwala.server.dto.SyncPushRequest
import org.bhargav.pansariwala.server.dto.SyncPushResponse
import org.bhargav.pansariwala.server.dto.SyncOrdersPage
import org.bhargav.pansariwala.server.dto.ShopPaymentInfoDto
import org.bhargav.pansariwala.server.dto.TokenResponse
import org.bhargav.pansariwala.server.dto.TxnDto
import org.bhargav.pansariwala.server.dto.UploadResultDto
import org.bhargav.pansariwala.server.security.Security
import org.bhargav.pansariwala.server.storage.AssetRefs
import org.bhargav.pansariwala.server.storage.AssetStorage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates

private const val QUERY_MAX_MS = 8_000L
private const val PRICE_HISTORY_WINDOW_MS = 366L * 24 * 60 * 60 * 1000
private const val POS_SYNC_PAGE_MAX = 500
private const val MAX_CATALOG_PAGE_SIZE = 50
private const val DEFAULT_SHOP_CATEGORY = "GENERAL_GROCERY"
private const val MAX_ORDER_PAGE_SIZE = 20
private const val MAX_MASTER_PAGE_SIZE = 100
private const val VARIANT_PACK = "PACK"
private const val VARIANT_LOOSE = "LOOSE"
private const val VARIANT_BUNDLE = "BUNDLE"
private val VARIANT_SELL_MODES = setOf(VARIANT_PACK, VARIANT_LOOSE, VARIANT_BUNDLE)
private const val DEFAULT_LOOSE_STEP = 0.05
private const val QTY_EPSILON = 1e-6
private val LOOSE_UNITS = setOf("KG", "LITRE", "GRAM")
private const val PLATFORM_SETTINGS_ID = "global"
private const val PLATFORM_SETTINGS_CACHE_MS = 30_000L
private const val DEFAULT_MAP_LAT = 28.6139
private const val DEFAULT_MAP_LNG = 77.2090
private const val MIN_SHOP_PASSWORD = 4
private const val MIN_ADMIN_PASSWORD = 6
private const val DAY_MS = 86_400_000L
private val LOGIN_USERNAME_REGEX = Regex("^[a-z0-9._@-]{3,40}$")
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun PartnerDoc.hasGpsFix(): Boolean {
    val latitude = lat ?: return false
    val longitude = lng ?: return false
    return latitude != 0.0 || longitude != 0.0
}

class AppStore(
    private val config: ServerConfig,
    private val security: Security,
    mongo: MongoApp,
    storage: AssetStorage,
    private val assets: AssetRefs,
    private val push: PushService,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val sockets = ConcurrentHashMap<String, WebSocketSession>()
    private val acceptLocks = ConcurrentHashMap<String, Any>()
    private val shopCol = mongo.db.getCollection<ShopDoc>("shops")
    private val shopUserCol = mongo.db.getCollection<ShopUserDoc>("shop_users")
    private val shopPartnerCol = mongo.db.getCollection<ShopPartnerDoc>("shop_partners")
    private val customerCol = mongo.db.getCollection<CustomerDoc>("customers")
    private val partnerCol = mongo.db.getCollection<PartnerDoc>("partners")
    private val categoryCol = mongo.db.getCollection<CategoryDoc>("master_categories")
    private val masterProductCol = mongo.db.getCollection<MasterProductDoc>("master_products")
    private val shopTypeCol = mongo.db.getCollection<ShopTypeDoc>("master_shop_types")
    private val assetStore = AssetStore(storage)
    private val productCol = mongo.db.getCollection<ProductDoc>("products")
    private val priceHistoryCol = mongo.db.getCollection<PriceHistoryDoc>("price_history")
    private val offerCol = mongo.db.getCollection<OfferDoc>("offers")
    private val offerCountCol = mongo.db.getCollection<Document>("offers")
    private val orderCol = mongo.db.getCollection<OrderDoc>("orders")
    /** Raw orders collection for id-only projections (OrderDoc codec rejects partial docs). */
    private val orderIdCol = mongo.db.getCollection<Document>("orders")
    private val txnCol = mongo.db.getCollection<TxnDoc>("transactions")
    private val deliveryOfferCol = mongo.db.getCollection<DeliveryOfferDoc>("delivery_offers")
    private val offerPayoutCol = mongo.db.getCollection<Document>("delivery_offers")
    private val otpCol = mongo.db.getCollection<OtpDoc>("otp_challenges")
    private val otp = OtpService(config, security, otpCol)
    private val adminUserCol = mongo.db.getCollection<AdminUserDoc>("admin_users")
    private val shopVisitCol = mongo.db.getCollection<ShopVisitDoc>("shop_visits")
    private val platformSettingsCol = mongo.db.getCollection<PlatformSettingsDoc>("platform_settings")
    @Volatile private var platformCache: Pair<Long, PlatformSettingsDoc>? = null
    private val partnerLiteProjection = exclude(
        "platePhoto",
        "vehiclePhoto",
        "profilePhoto",
        "dlPhoto",
        "idPhoto",
    )
    /** List endpoints: pickup photo refs are only needed on the order detail. */
    private val orderListProjection = exclude("pickupPhotos")
    /** Home/profile: keep avatar, drop document/vehicle photo refs. */
    private val partnerProfileProjection = exclude(
        "platePhoto",
        "vehiclePhoto",
        "dlPhoto",
        "idPhoto",
    )

    fun registerSocket(partnerId: String, session: WebSocketSession) {
        sockets[partnerId] = session
    }

    fun unregisterSocket(partnerId: String, session: WebSocketSession) {
        sockets.remove(partnerId, session)
    }

    fun shopLogin(username: String, password: String): TokenResponse {
        val key = username.trim()
        require(key.isNotEmpty() && password.isNotBlank()) { "Username and password are required" }
        val row = shopUserCol.find(eq("username", key)).firstOrNull()
            ?: shopUserCol.find(eq("username", key.lowercase())).firstOrNull()
            ?: shopUserCol.find(usernameFilter(key)).firstOrNull()
            ?: error("Invalid credentials")
        verifyAndMigratePassword(row.passwordHash, password) { hash ->
            shopUserCol.updateOne(eq("_id", row.id), set("passwordHash", hash))
        }
        require(row.active) { "This login is disabled. Contact your shop admin." }
        val token = security.issueJwt(row.id, "SHOP", row.shopId, row.displayName)
        return TokenResponse(
            token, null, "SHOP", row.id, row.shopId, row.displayName,
            shopRole = row.role,
            permissions = effectivePermissions(row),
        )
    }

    fun adminLogin(username: String, password: String): TokenResponse {
        val key = username.trim()
        require(key.isNotEmpty() && password.isNotBlank()) { "Username and password are required" }
        val row = adminUserCol.find(eq("username", key)).firstOrNull()
            ?: adminUserCol.find(usernameFilter(key)).firstOrNull()
            ?: error("Invalid credentials")
        verifyAndMigratePassword(row.passwordHash, password) { hash ->
            adminUserCol.updateOne(eq("_id", row.id), set("passwordHash", hash))
        }
        val token = security.issueJwt(row.id, "ADMIN", displayName = "Admin")
        return TokenResponse(token, null, "ADMIN", row.id, null, "Admin")
    }

    fun changeAdminPassword(adminId: String, currentPassword: String, newPassword: String) {
        val row = adminUserCol.find(eq("_id", adminId)).firstOrNull() ?: error("Admin not found")
        require(security.passwordMatches(currentPassword.trim(), row.passwordHash)) { "Current password is incorrect" }
        val next = newPassword.trim()
        require(next.length >= MIN_ADMIN_PASSWORD) { "New password must be at least $MIN_ADMIN_PASSWORD characters" }
        adminUserCol.updateOne(eq("_id", adminId), set("passwordHash", security.hashPassword(next)))
    }

    /** Cached for [PLATFORM_SETTINGS_CACHE_MS]; quote/checkout hit this on every request. */
    private fun platform(): PlatformSettingsDoc {
        val now = System.currentTimeMillis()
        platformCache?.let { (at, doc) -> if (now - at < PLATFORM_SETTINGS_CACHE_MS) return doc }
        val doc = platformSettingsCol.find(eq("_id", PLATFORM_SETTINGS_ID)).firstOrNull() ?: PlatformSettingsDoc()
        platformCache = now to doc
        return doc
    }

    fun platformSettings(): PlatformSettingsDto = platform().toDto()

    fun savePlatformSettings(body: PlatformSettingsDto): PlatformSettingsDto {
        fun money(v: Double, label: String) = require(v in 0.0..100_000.0) { "$label must be 0-100000" }
        money(body.platformFeeInr, "Platform fee")
        money(body.deliveryBasePerKmInr, "Delivery per km")
        money(body.minDeliveryChargeInr, "Min delivery charge")
        money(body.maxDeliveryChargeInr, "Max delivery charge")
        money(body.freeDeliveryAboveInr, "Free delivery above")
        money(body.minOrderValueInr, "Min order value")
        money(body.partnerPayoutPerKmInr, "Partner payout per km")
        require(body.deliverySurchargePercent in 0.0..500.0) { "Surcharge must be 0-500%" }
        require(body.maxDeliveryChargeInr == 0.0 || body.maxDeliveryChargeInr >= body.minDeliveryChargeInr) {
            "Max delivery charge must be at least the min"
        }
        require(body.partnerRingKm in 1.0..100.0) { "Partner range must be 1-100 km" }
        require(body.offerTtlMinutes in 1..120) { "Offer expiry must be 1-120 minutes" }
        require(body.defaultShopDeliveryRadiusKm in 1.0..100.0) { "Delivery radius must be 1-100 km" }
        val doc = PlatformSettingsDoc(
            id = PLATFORM_SETTINGS_ID,
            platformFeeInr = body.platformFeeInr,
            deliveryBasePerKmInr = body.deliveryBasePerKmInr,
            deliverySurchargePercent = body.deliverySurchargePercent,
            minDeliveryChargeInr = body.minDeliveryChargeInr,
            maxDeliveryChargeInr = body.maxDeliveryChargeInr,
            freeDeliveryAboveInr = body.freeDeliveryAboveInr,
            minOrderValueInr = body.minOrderValueInr,
            partnerPayoutPerKmInr = body.partnerPayoutPerKmInr,
            partnerRingKm = body.partnerRingKm,
            offerTtlMinutes = body.offerTtlMinutes,
            defaultShopDeliveryRadiusKm = body.defaultShopDeliveryRadiusKm,
            onlineOrderingEnabled = body.onlineOrderingEnabled,
            orderingPausedMessage = body.orderingPausedMessage.trim().take(200),
            partnerRegistrationEnabled = body.partnerRegistrationEnabled,
            updatedAt = System.currentTimeMillis(),
        )
        platformSettingsCol.replaceOne(eq("_id", PLATFORM_SETTINGS_ID), doc, ReplaceOptions().upsert(true))
        platformCache = System.currentTimeMillis() to doc
        return doc.toDto()
    }

    private fun PlatformSettingsDoc.toDto() = PlatformSettingsDto(
        platformFeeInr = platformFeeInr,
        deliveryBasePerKmInr = deliveryBasePerKmInr,
        deliverySurchargePercent = deliverySurchargePercent,
        minDeliveryChargeInr = minDeliveryChargeInr,
        maxDeliveryChargeInr = maxDeliveryChargeInr,
        freeDeliveryAboveInr = freeDeliveryAboveInr,
        minOrderValueInr = minOrderValueInr,
        partnerPayoutPerKmInr = partnerPayoutPerKmInr,
        partnerRingKm = partnerRingKm,
        offerTtlMinutes = offerTtlMinutes,
        defaultShopDeliveryRadiusKm = defaultShopDeliveryRadiusKm,
        onlineOrderingEnabled = onlineOrderingEnabled,
        orderingPausedMessage = orderingPausedMessage,
        partnerRegistrationEnabled = partnerRegistrationEnabled,
        updatedAt = updatedAt,
    )

    private fun PlatformSettingsDoc.deliveryCharge(distanceKm: Double, subtotalAfterDiscount: Double): Double {
        if (freeDeliveryAboveInr > 0 && subtotalAfterDiscount >= freeDeliveryAboveInr) return 0.0
        val base = deliveryBasePerKmInr * distanceKm
        val charge = (base + base * deliverySurchargePercent / 100.0).coerceAtLeast(minDeliveryChargeInr)
        return if (maxDeliveryChargeInr > 0) charge.coerceAtMost(maxDeliveryChargeInr) else charge
    }

    private val PlatformSettingsDoc.offerTtlMs: Long get() = offerTtlMinutes * 60_000L

    private fun usernameFilter(username: String) =
        regex("username", "^${Regex.escape(username.trim())}$", "i")

    private fun verifyAndMigratePassword(
        storedHash: String,
        password: String,
        persist: (String) -> Unit,
    ) {
        val raw = password.trim()
        if (!security.passwordMatches(raw, storedHash)) error("Invalid credentials")
        val current = security.hashPassword(raw)
        if (storedHash != current) persist(current)
    }

    fun loginFirebase(idToken: String): TokenResponse {
        val phone = security.verifyFirebaseOrDev(idToken)
        val partner = partnerCol.find(eq("phone", phone)).projection(partnerLiteProjection).firstOrNull()
        if (partner != null) {
            partnerCol.updateOne(eq("_id", partner.id), set("verified", true))
            val token = security.issueJwt(partner.id, "PARTNER", displayName = partner.name)
            return TokenResponse(token, null, "PARTNER", partner.id, null, partner.name, true)
        }
        return upsertCustomerToken(phone)
    }

    fun requestOtp(phone: String, clientIp: String? = null, proof: OtpAppProof = OtpAppProof()): OtpSessionResponse =
        otp.request(phone, clientIp, proof)

    fun otpConfig(): OtpConfigResponse = otp.config()

    fun verifyOtp(phone: String, code: String, sessionId: String?): TokenResponse {
        val normalized = otp.verify(phone, code, sessionId)
        val partner = partnerCol.find(eq("phone", normalized)).firstOrNull()
        if (partner != null) {
            partnerCol.updateOne(eq("_id", partner.id), set("verified", true))
            val token = security.issueJwt(partner.id, "PARTNER", displayName = partner.name)
            return TokenResponse(token, null, "PARTNER", partner.id, null, partner.name, true)
        }
        return upsertCustomerToken(normalized)
    }

    private fun upsertCustomerToken(phone: String): TokenResponse {
        val existingPartner = partnerCol.find(eq("phone", phone)).firstOrNull()
        require(existingPartner == null) { "Phone registered as delivery partner, use partner login" }
        val existing = customerCol.find(eq("phone", phone)).firstOrNull()
        val id = existing?.id ?: security.randomId("cust")
        if (existing == null) {
            customerCol.insertOne(CustomerDoc(id, phone, "", "", joinedAt = System.currentTimeMillis()))
        }
        val name = existing?.name.orEmpty()
        val complete = name.isNotBlank() && existing?.address?.isNotBlank() == true
        val token = security.issueJwt(id, "CUSTOMER", displayName = name.ifBlank { phone })
        return TokenResponse(token, null, "CUSTOMER", id, null, name.ifBlank { phone }, complete)
    }

    fun me(userId: String): CustomerDto {
        val row = customerCol.find(eq("_id", userId)).firstOrNull() ?: error("Customer not found")
        return row.toCustomerDto()
    }

    fun updateProfile(
        userId: String,
        name: String,
        address: String,
        locality: String?,
        lat: Double?,
        lng: Double?,
    ): CustomerDto {
        require(name.trim().length >= 2) { "Full name is required" }
        require(address.isNotBlank()) { "Address is required" }
        if (lat != null || lng != null) {
            require((lat ?: 0.0) in -90.0..90.0 && (lng ?: 0.0) in -180.0..180.0) { "Invalid location" }
        }
        val current = customerCol.find(eq("_id", userId)).firstOrNull() ?: error("Customer not found")
        val loc = locality.orEmpty()
        val addresses = upsertDefaultAddress(current.addresses, address, loc, lat, lng)
        customerCol.replaceOne(
            eq("_id", userId),
            current.copy(
                name = name,
                address = address,
                locality = loc,
                lat = lat ?: current.lat,
                lng = lng ?: current.lng,
                addresses = addresses,
            ),
        )
        return me(userId)
    }

    fun saveAddress(userId: String, request: SaveAddressRequest): CustomerDto {
        require(request.line.isNotBlank() && request.locality.isNotBlank()) { "Address is required" }
        val current = customerCol.find(eq("_id", userId)).firstOrNull() ?: error("Customer not found")
        val id = security.randomId("addr")
        val next = CustomerAddressDoc(
            id = id,
            line = request.line,
            locality = request.locality,
            lat = request.lat,
            lng = request.lng,
            isDefault = true,
        )
        val addresses = current.addresses.map { it.copy(isDefault = false) } + next
        customerCol.replaceOne(
            eq("_id", userId),
            current.copy(
                address = request.line,
                locality = request.locality,
                lat = request.lat,
                lng = request.lng,
                addresses = addresses,
            ),
        )
        return me(userId)
    }

    fun selectAddress(userId: String, addressId: String): CustomerDto {
        val current = customerCol.find(eq("_id", userId)).firstOrNull() ?: error("Customer not found")
        val chosen = current.addresses.firstOrNull { it.id == addressId } ?: error("Address not found")
        val addresses = current.addresses.map { it.copy(isDefault = it.id == addressId) }
        customerCol.replaceOne(
            eq("_id", userId),
            current.copy(
                address = chosen.line,
                locality = chosen.locality,
                lat = chosen.lat,
                lng = chosen.lng,
                addresses = addresses,
            ),
        )
        return me(userId)
    }

    fun updateCustomerLocation(userId: String, lat: Double, lng: Double) {
        customerCol.updateOne(
            eq("_id", userId),
            combine(set("lat", lat), set("lng", lng)),
        )
    }

    fun nearbyShops(
        lat: Double,
        lng: Double,
        radiusKm: Double,
        query: String,
        categories: Set<String> = emptySet(),
    ): List<ShopDto> {
        val disabledTypes = shopTypeCol.find(eq("active", false)).toList().map { it.id }.toSet()
        val allowedTypes = categories - disabledTypes
        if (categories.isNotEmpty() && allowedTypes.isEmpty()) return emptyList()
        val shopFilter = when {
            allowedTypes.isNotEmpty() -> and(eq("active", true), `in`("shopType", allowedTypes))
            disabledTypes.isNotEmpty() -> and(eq("active", true), nin("shopType", disabledTypes))
            else -> eq("active", true)
        }
        val offerCounts = offerCountCol.aggregate(
            listOf(Aggregates.group("\$shopId", Accumulators.sum("n", 1))),
        ).toList().associate { it.getString("_id") to (it.getInteger("n") ?: 0) }
        // Customer radius decides visibility; the shop's own delivery radius only flags deliverability,
        // otherwise a shop with a small delivery radius silently disappears from a wider search.
        return shopCol.find(shopFilter).toList()
            .map { it.toCustomerDto(lat, lng, offerCounts[it.id] ?: 0) }
            .filter { it.distanceKm <= radiusKm }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.distanceKm }
    }

    fun shopForCustomer(shopId: String, lat: Double, lng: Double): ShopDto {
        val offerCount = offerCol.countDocuments(eq("shopId", shopId)).toInt()
        return shopById(shopId).toCustomerDto(lat, lng, offerCount)
    }

    private fun ShopDoc.toCustomerDto(lat: Double, lng: Double, offerCount: Int): ShopDto {
        val dist = haversine(lat, lng, this.lat, this.lng)
        return ShopDto(
            id = id,
            name = name,
            imageUrl = assets.toUrlOrNull(imageUrl),
            rating = rating,
            ratingCount = ratingCount,
            distanceKm = dist,
            isOpen = isOpen,
            lat = this.lat,
            lng = this.lng,
            offerCount = offerCount,
            discountPercent = discountPercent,
            upiId = upiId.ifBlank { config.defaultShopUpi },
            deliveryRadiusKm = deliveryRadiusKm,
            shopType = shopType,
            deliversToYou = dist <= deliveryRadiusKm,
        )
    }

    /** [includeInactive] is for the shop's own devices; customers only see active products. */
    fun catalog(shopId: String, includeInactive: Boolean = false): List<ProductDto> {
        val filter = if (includeInactive) eq("shopId", shopId) else and(eq("shopId", shopId), ne("active", false))
        return productCol.find(filter).toList().map { it.toDto() }.withOffers(shopId)
    }

    /** Attaches each product's best live PRODUCT offer so every client can show the sale price. */
    private fun List<ProductDto>.withOffers(shopId: String): List<ProductDto> {
        if (isEmpty()) return this
        val offers = liveOffers(shopId).filter { it.scope == OfferScope.PRODUCT }
        if (offers.isEmpty()) return this
        return map { product ->
            val best = offers.bestProductOffer(product.id, product.sellingPrice) ?: return@map product
            product.copy(offer = ProductOfferDto(best.id, best.title, best.discountType, best.value))
        }
    }

    private fun productFilter(shopId: String, category: String?, query: String): Bson {
        val parts = mutableListOf(eq("shopId", shopId), ne("active", false))
        if (!category.isNullOrBlank()) parts += eq("category", category)
        val q = query.trim()
        if (q.isNotEmpty()) {
            val pattern = java.util.regex.Pattern.quote(q)
            parts += or(
                regex("name", pattern, "i"),
                regex("nameHi", pattern, "i"),
                regex("voiceAlias", pattern, "i"),
                regex("brandName", pattern, "i"),
                eq("barcode", q),
            )
        }
        return and(parts)
    }

    /** Per-category totals with a small preview, so the shop home never loads the full catalog. */
    fun catalogSections(shopId: String, query: String, previewSize: Int): List<CatalogSectionDto> {
        val filter = productFilter(shopId, null, query)
        val limit = previewSize.coerceIn(1, MAX_CATALOG_PAGE_SIZE)
        return productCol.aggregate(
            listOf(Aggregates.match(filter), Aggregates.group("\$category", Accumulators.sum("n", 1))),
            Document::class.java,
        ).toList().mapNotNull { row ->
            val category = row.getString("_id") ?: return@mapNotNull null
            val preview = productCol.find(productFilter(shopId, category, query))
                .sort(Sorts.ascending("name", "_id"))
                .limit(limit)
                .toList()
                .map { it.toDto() }
                .withOffers(shopId)
            CatalogSectionDto(category, row.getInteger("n") ?: preview.size, preview)
        }.sortedByDescending { it.totalCount }
    }

    fun catalogPage(shopId: String, category: String?, query: String, page: Int, pageSize: Int): ProductPageDto {
        val size = pageSize.coerceIn(1, MAX_CATALOG_PAGE_SIZE)
        val safePage = page.coerceAtLeast(0)
        val filter = productFilter(shopId, category, query)
        val total = productCol.countDocuments(filter).toInt()
        val items = productCol.find(filter)
            .sort(Sorts.ascending("name", "_id"))
            .skip(safePage * size)
            .limit(size)
            .toList()
            .map { it.toDto() }
            .withOffers(shopId)
        return ProductPageDto(
            items = items,
            page = safePage,
            pageSize = size,
            totalCount = total,
            hasMore = (safePage + 1) * size < total,
        )
    }

    /** Admin view of one shop's inventory, including inactive products. [status]: all | active | inactive. */
    fun adminShopProductPage(
        shopId: String,
        query: String,
        category: String?,
        status: String?,
        lowStockOnly: Boolean,
        page: Int,
        pageSize: Int,
    ): ProductPageDto {
        val size = pageSize.coerceIn(1, MAX_CATALOG_PAGE_SIZE)
        val safePage = page.coerceAtLeast(0)
        val parts = mutableListOf(eq("shopId", shopId))
        if (!category.isNullOrBlank()) parts += eq("category", category)
        when (status?.lowercase()) {
            "active" -> parts += ne("active", false)
            "inactive" -> parts += eq("active", false)
        }
        if (lowStockOnly) parts += lowStockExpr()
        val q = query.trim()
        if (q.isNotEmpty()) {
            val pattern = java.util.regex.Pattern.quote(q)
            parts += or(
                eq("_id", q),
                eq("barcode", q),
                eq("masterProductId", q),
                regex("name", pattern, "i"),
                regex("nameHi", pattern, "i"),
                regex("brandName", pattern, "i"),
                regex("voiceAlias", pattern, "i"),
            )
        }
        val filter = and(parts)
        val total = productCol.countDocuments(filter).toInt()
        val items = productCol.find(filter)
            .sort(Sorts.ascending("name", "_id"))
            .skip(safePage * size)
            .limit(size)
            .toList()
            .map { it.toDto() }
        return ProductPageDto(items, safePage, size, total, (safePage + 1) * size < total)
    }

    fun adminShopStockSummary(shopId: String): ShopStockSummaryDto {
        val shop = eq("shopId", shopId)
        val inactive = productCol.countDocuments(and(shop, eq("active", false))).toInt()
        val total = productCol.countDocuments(shop).toInt()
        return ShopStockSummaryDto(
            total = total,
            active = total - inactive,
            inactive = inactive,
            outOfStock = productCol.countDocuments(and(shop, lte("stockQty", 0.0))).toInt(),
            lowStock = productCol.countDocuments(and(shop, gt("stockQty", 0.0), lowStockExpr())).toInt(),
        )
    }

    /** masterProductId → shop productId for the given master ids already in [shopId]. */
    fun adminShopMasterLinks(shopId: String, masterIds: List<String>): Map<String, String> {
        if (masterIds.isEmpty()) return emptyMap()
        return productCol.find(and(eq("shopId", shopId), `in`("masterProductId", masterIds.take(MAX_CATALOG_PAGE_SIZE))))
            .toList()
            .mapNotNull { p -> p.masterProductId?.let { it to p.id } }
            .toMap()
    }

    fun adminAddShopProductFromMaster(shopId: String, body: AddFromMasterRequest): ProductDto {
        shopCol.find(eq("_id", shopId)).firstOrNull() ?: error("Shop not found")
        val master = masterProductCol.find(eq("_id", body.masterProductId)).firstOrNull()
            ?: error("Master product not found")
        if (productCol.find(and(eq("shopId", shopId), eq("masterProductId", master.id))).firstOrNull() != null) {
            error("${master.name} is already in this shop")
        }
        val doc = ProductDoc(
            id = "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}",
            shopId = shopId,
            name = master.name,
            nameHi = master.nameHi,
            category = body.category?.takeIf { it.isNotBlank() } ?: DEFAULT_SHOP_CATEGORY,
            unit = master.unit,
            barcode = master.barcode?.takeIf { it.isNotBlank() },
            sellingPrice = (body.sellingPrice ?: master.salePrice).coerceAtLeast(0.0),
            costPrice = (body.costPrice ?: master.cost).coerceAtLeast(0.0),
            stockQty = body.stockQty.coerceAtLeast(0.0),
            lowStockThreshold = (body.lowStockThreshold ?: master.lowStockThreshold).coerceAtLeast(0.0),
            imageUrls = normalizeProductImageUrls(master.imageUrls, master.imageUrl, master.thumbnailUrl),
            variants = master.variants.filter { it.active },
            brandName = master.brandName,
            masterProductId = master.id,
        )
        productCol.insertOne(doc)
        recordPriceChange(null, doc)
        return doc.toDto()
    }

    fun adminUpdateShopProductStock(shopId: String, productId: String, body: StockUpdateRequest): ProductDto {
        val byId = and(eq("_id", productId), eq("shopId", shopId))
        val set = body.set
        val delta = body.delta
        when {
            set != null -> {
                require(set >= 0.0) { "Stock can't be negative" }
                if (productCol.updateOne(byId, set("stockQty", set)).matchedCount == 0L) error("Product not found")
            }
            delta != null && delta != 0.0 -> {
                // Atomic: a stock-out only applies while enough stock remains.
                val guard = if (delta < 0.0) and(byId, gte("stockQty", -delta)) else byId
                if (productCol.updateOne(guard, inc("stockQty", delta)).matchedCount == 0L) {
                    productCol.find(byId).firstOrNull() ?: error("Product not found")
                    error("Not enough stock to remove ${-delta}")
                }
            }
            else -> error("Nothing to update")
        }
        return productCol.find(byId).firstOrNull()?.toDto() ?: error("Product not found")
    }

    fun adminSetShopProductActive(shopId: String, productId: String, active: Boolean): ProductDto {
        val byId = and(eq("_id", productId), eq("shopId", shopId))
        if (productCol.updateOne(byId, set("active", active)).matchedCount == 0L) error("Product not found")
        return productCol.find(byId).firstOrNull()!!.toDto()
    }

    fun adminEditShopProduct(shopId: String, productId: String, body: ShopProductEdit): ProductDto {
        val byId = and(eq("_id", productId), eq("shopId", shopId))
        val existing = productCol.find(byId).firstOrNull() ?: error("Product not found")
        require(body.name.isNotBlank()) { "Name is required" }
        require(body.sellingPrice >= 0.0 && body.costPrice >= 0.0 && body.lowStockThreshold >= 0.0) {
            "Prices and low-stock level can't be negative"
        }
        val next = existing.copy(
            name = body.name.trim(),
            nameHi = body.nameHi.trim(),
            category = body.category.ifBlank { existing.category },
            brandName = body.brandName.trim(),
            barcode = body.barcode?.trim()?.takeIf { it.isNotEmpty() },
            sellingPrice = body.sellingPrice,
            costPrice = body.costPrice,
            lowStockThreshold = body.lowStockThreshold,
        )
        productCol.replaceOne(byId, next)
        recordPriceChange(existing, next)
        return next.toDto()
    }

    private fun lowStockExpr(): Bson = Document("\$expr", Document("\$lte", listOf("\$stockQty", "\$lowStockThreshold")))

    fun shopProductDetail(shopId: String, productId: String): ProductDetailDto {
        val product = productCol.find(and(eq("_id", productId), eq("shopId", shopId), ne("active", false)))
            .firstOrNull() ?: error("Product not found")
        val master = product.masterProductId?.let { masterProductCol.find(eq("_id", it)).firstOrNull() }
            ?: product.barcode?.takeIf { it.isNotBlank() }
                ?.let { masterProductCol.find(eq("barcode", it)).firstOrNull() }
            ?: masterProductCol.find(
                regex("name", "^${java.util.regex.Pattern.quote(product.name.trim())}$", "i"),
            ).firstOrNull()
        val dto = listOf(product.toDto()).withOffers(shopId).first()
        val images = dto.imageUrls.ifEmpty {
            master?.let { assets.toUrls(normalizeProductImageUrls(it.imageUrls, it.imageUrl, it.thumbnailUrl)) }
                .orEmpty()
        }
        return ProductDetailDto(
            product = dto.copy(imageUrls = images),
            brandName = master?.brandName.orEmpty(),
            companyName = master?.companyName.orEmpty(),
            description = master?.description.orEmpty(),
            sku = master?.sku.orEmpty(),
            tags = master?.tags.orEmpty(),
            weightKg = master?.weightKg ?: 0.0,
            dimensions = master?.dimensions.orEmpty(),
            variants = dto.variants,
            priceHistory = priceHistory(product),
        )
    }

    private fun priceHistory(product: ProductDoc, now: Long = System.currentTimeMillis()): List<PricePointDto> {
        val since = now - PRICE_HISTORY_WINDOW_MS
        val baseline = priceHistoryCol.find(and(eq("productId", product.id), lt("at", since)))
            .sort(Sorts.descending("at")).limit(1).firstOrNull()
        val inWindow = priceHistoryCol.find(and(eq("productId", product.id), gte("at", since)))
            .sort(Sorts.ascending("at")).toList()
        val points = listOfNotNull(baseline?.let { PricePointDto(since, it.price) }) +
            inWindow.map { PricePointDto(it.at, it.price) }
        val current = product.displayPrice()
        return if (points.lastOrNull()?.price == current) points else points + PricePointDto(now, current)
    }

    private fun ProductDoc.displayPrice(): Double =
        variants.filter { it.active && it.price > 0.0 }.minOfOrNull { it.price } ?: sellingPrice

    private fun recordPriceChange(previous: ProductDoc?, next: ProductDoc) {
        val price = next.displayPrice()
        if (previous != null && previous.displayPrice() == price) return
        priceHistoryCol.insertOne(
            PriceHistoryDoc(security.randomId("ph"), next.shopId, next.id, price, System.currentTimeMillis()),
        )
    }

    fun offers(shopId: String): List<OfferDto> =
        liveOffers(shopId).map {
            OfferDto(it.id, it.title, it.description, if (it.discountType == OfferType.PERCENT) it.value else 0.0)
        }

    private fun liveOffers(shopId: String, now: Long = System.currentTimeMillis()): List<OfferDoc> =
        offerCol.find(eq("shopId", shopId)).toList().filter { it.isLive(now) }

    fun quote(userId: String, request: QuoteRequest): QuoteDto {
        me(userId)
        val shop = shopById(request.shopId)
        val lines = resolveLines(request.shopId, request.items).map { PricedLine(it.product.id, it.unitPrice, it.quantity) }
        val subtotal = lines.sumOf { it.lineTotal }
        val dist = haversine(request.userLat, request.userLng, shop.lat, shop.lng)
        val discount = offerDiscount(liveOffers(request.shopId), lines, shop.discountPercent)
        val settings = platform()
        val delivery = settings.deliveryCharge(dist, subtotal - discount)
        val fee = settings.platformFeeInr
        return QuoteDto(subtotal, discount, fee, delivery, (subtotal - discount + fee + delivery).coerceAtLeast(0.0), dist)
    }

    /** Non-payment checks (profile, cart, stock, delivery radius). Call before creating a Razorpay order. */
    fun validateCheckout(userId: String, request: PlaceOrderRequest): CustomerDto {
        require(request.items.isNotEmpty()) { "Cart is empty" }
        val settings = platform()
        require(settings.onlineOrderingEnabled) {
            settings.orderingPausedMessage.ifBlank { "Online ordering is paused. Please try again later." }
        }
        val customer = me(userId)
        require(customer.name.isNotBlank() && customer.address.isNotBlank()) { "Complete your profile" }
        val shop = shopById(request.shopId)
        val drop = resolveDrop(customer, request)
        val dist = haversine(drop.lat, drop.lng, shop.lat, shop.lng)
        require(dist <= shop.deliveryRadiusKm) { "Out of shop delivery range" }
        val lines = resolveLines(request.shopId, request.items)
        if (settings.minOrderValueInr > 0) {
            require(lines.sumOf { it.lineTotal } + QTY_EPSILON >= settings.minOrderValueInr) {
                "Minimum order value is ₹${settings.minOrderValueInr.toLong()}"
            }
        }
        lines
            .groupBy { it.product.id to it.variant?.id }
            .values
            .forEach { group ->
                val first = group.first()
                require(first.available + QTY_EPSILON >= group.sumOf { it.quantity }) { "${first.displayName} is out of stock" }
            }
        return customer
    }

    private data class ResolvedLine(val product: ProductDoc, val variant: ProductVariantDoc?, val quantity: Double) {
        val unitPrice: Double get() = variant?.price ?: product.sellingPrice
        val lineTotal: Double get() = unitPrice * quantity
        val available: Double get() = variant?.stockQty ?: product.stockQty
        val displayName: String get() = variant?.let { "${product.name} (${it.name})" } ?: product.name
    }

    /** Prices every cart line from the shop's own catalog; [PlaceOrderItemDto.variantId] picks the option. */
    private fun resolveLines(shopId: String, items: List<PlaceOrderItemDto>): List<ResolvedLine> {
        val ids = items.map { it.productId }.distinct()
        val products = productCol.find(and(eq("shopId", shopId), `in`("_id", ids))).toList().associateBy { it.id }
        return items.map { line ->
            val product = products[line.productId] ?: error("Unknown product")
            require(product.active) { "${product.name}: currently unavailable" }
            val variant = line.variantId?.takeIf { it.isNotBlank() }?.let { vid ->
                product.variants.firstOrNull { it.id == vid && it.active } ?: error("${product.name}: option unavailable")
            }
            require(line.quantity > 0) { "Invalid quantity" }
            val loose = variant?.sellMode == VARIANT_LOOSE || (variant == null && isLooseUnit(product.unit))
            if (loose && variant != null) {
                require(line.quantity + QTY_EPSILON >= variant.minQty) { "${product.name}: below minimum quantity" }
                require(variant.maxQty <= 0 || line.quantity <= variant.maxQty + QTY_EPSILON) {
                    "${product.name}: above maximum quantity"
                }
            } else if (!loose) {
                require(kotlin.math.abs(line.quantity - kotlin.math.round(line.quantity)) < QTY_EPSILON) { "Invalid quantity" }
            }
            ResolvedLine(product, variant, line.quantity)
        }
    }

    private fun isLooseUnit(unit: String): Boolean = unit.uppercase() in LOOSE_UNITS

    private fun deductStock(lines: List<ResolvedLine>) {
        lines.groupBy { it.product.id to it.variant?.id }.values.forEach { group ->
            val first = group.first()
            val left = (first.available - group.sumOf { it.quantity }).coerceAtLeast(0.0)
            val variant = first.variant
            if (variant == null) {
                productCol.updateOne(eq("_id", first.product.id), set("stockQty", left))
            } else {
                productCol.updateOne(
                    and(eq("_id", first.product.id), eq("variants.id", variant.id)),
                    set("variants.$.stockQty", left),
                )
            }
        }
    }

    suspend fun createRazorpay(userId: String, request: CreateRazorpayRequest): RazorpayOrderDto {
        require(request.amountPaise > 0) { "Invalid amount" }
        val shop = shopById(request.shopId)
        require(shop.paymentsEnabled) { "Payments are disabled for this shop" }
        val shopUpi = shop.upiId.ifBlank { config.defaultShopUpi }
        val orderId = security.createRazorpayOrder(
            request.amountPaise,
            "u_${userId.take(8)}_${System.currentTimeMillis()}",
            notes = mapOf(
                "shop_id" to shop.id,
                "shop_upi" to shopUpi,
            ),
        )
        return RazorpayOrderDto(orderId, request.amountPaise, "INR", config.razorpayKeyId.ifBlank { "rzp_test_dev" })
    }

    fun verifyPayment(orderId: String, paymentId: String, signature: String): Boolean =
        security.verifyRazorpaySignature(orderId, paymentId, signature)

    suspend fun placeOrder(userId: String, request: PlaceOrderRequest): OrderDto {
        if (config.paymentsEnabled) {
            val orderId = request.razorpayOrderId ?: error("Missing Razorpay order id")
            val paymentId = request.razorpayPaymentId ?: error("Missing Razorpay payment id")
            val signature = request.razorpaySignature ?: error("Missing Razorpay signature")
            require(security.verifyRazorpaySignature(orderId, paymentId, signature)) { "Payment signature mismatch" }
        } else if (!config.devAuth) {
            error("Payments are not configured")
        }
        val customer = validateCheckout(userId, request)
        val drop = resolveDrop(customer, request)
        val shop = shopById(request.shopId)
        val shopUpi = shop.upiId.ifBlank { config.defaultShopUpi }
        val q = quote(userId, QuoteRequest(request.shopId, request.items, drop.lat, drop.lng))
        val resolved = resolveLines(request.shopId, request.items)
        val items = resolved.map { line ->
            val image = (line.variant?.imageUrls.orEmpty() + line.product.imageUrls).firstOrNull { assets.isValidRef(it) }
            OrderItemDto(
                productId = line.product.id,
                productName = line.product.name,
                unit = line.variant?.baseUnit?.takeIf { line.variant.sellMode == VARIANT_LOOSE } ?: line.product.unit,
                quantity = line.quantity,
                unitPrice = line.unitPrice,
                imageUrl = image?.let { assets.toUrl(it) },
                variantId = line.variant?.id,
                variantLabel = line.variant?.name,
                sellMode = line.variant?.sellMode,
            )
        }
        val id = security.newOrderId()
        val otp = security.deliveryOtp()
        val now = System.currentTimeMillis()
        val paymentMethod = if (request.razorpayPaymentId.isNullOrBlank()) "COD" else "ONLINE"
        deductStock(resolved)
        orderCol.insertOne(
            OrderDoc(
                id = id,
                shopId = request.shopId,
                customerId = userId,
                status = "RECEIVED",
                channel = "ONLINE",
                customerName = customer.name,
                customerPhone = customer.phone,
                customerLat = drop.lat,
                customerLng = drop.lng,
                deliveryAddress = drop.formatted,
                dropoffInstructions = "Drop at doorstep unless customer says otherwise",
                deliveryOtp = otp,
                paymentId = request.razorpayPaymentId ?: "pay_dev",
                paymentMethod = paymentMethod,
                razorpayOrderId = request.razorpayOrderId,
                shopUpi = shopUpi,
                createdAt = now,
                items = items,
                quote = q,
            ),
        )
        txnCol.insertOne(TxnDoc(security.randomId("txn"), id, userId, q.payable, "Order $id", now))
        publishOrderUpdate(id, previousStatus = null)
        if (config.paymentsEnabled) {
            runCatching {
                security.transferToShopUpi(
                    shopName = shop.name,
                    upiId = shopUpi,
                    amountPaise = (q.payable * 100).toLong(),
                    paymentId = request.razorpayPaymentId.orEmpty(),
                )
            }
        }
        return getOrder(id)
    }

    fun getOrder(orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        return row.toDto()
    }

    /**
     * Pushes the order's new state to its shop + customer (replaces client polling). No-op when the
     * status did not change unless [always] (partner progress updates shown on the customer screen).
     */
    private fun publishOrderUpdate(
        orderId: String,
        previousStatus: String?,
        partnerTimeout: Boolean = false,
        always: Boolean = false,
    ) {
        if (!push.enabled) return
        val row = orderCol.find(eq("_id", orderId)).projection(orderListProjection).firstOrNull() ?: return
        if (row.channel != "ONLINE") return
        if (row.status == previousStatus && !partnerTimeout && !always) return
        push.orderUpdated(
            OrderUpdate(
                orderId = row.id,
                shopId = row.shopId,
                customerId = row.customerId,
                previousStatus = previousStatus,
                status = row.status,
                shopName = shopCol.find(eq("_id", row.shopId)).firstOrNull()?.name.orEmpty(),
                customerName = row.customerName.orEmpty(),
                partnerTimeout = partnerTimeout,
            ),
        )
    }

    fun customerOrders(userId: String): List<OrderDto> =
        mapOrders(
            orderCol.find(eq("customerId", userId))
                .sort(Sorts.descending("createdAt"))
                .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
                .toList(),
        )

    fun transactions(userId: String): List<TxnDto> =
        txnCol.find(eq("customerId", userId))
            .sort(Sorts.descending("createdAt"))
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
            .map { TxnDto(it.id, it.orderId, it.amount, it.title, it.createdAt) }

    fun rateOrder(userId: String, orderId: String, stars: Int, comment: String?): OrderDto {
        require(stars in 1..5) { "Stars must be 1-5" }
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.customerId == userId) { "Forbidden" }
        require(row.status in listOf("DELIVERED", "COMPLETED")) { "Rate after delivery" }
        orderCol.replaceOne(eq("_id", orderId), row.copy(ratingStars = stars, ratingComment = comment))
        recalculateShopRating(row.shopId)
        return getOrder(orderId)
    }

    fun shopRatings(shopId: String): List<ShopReviewDto> =
        orderCol.find(
            and(
                eq("shopId", shopId),
                exists("ratingStars", true),
            ),
        )
            .sort(Sorts.descending("createdAt"))
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
            .mapNotNull { row ->
                val stars = row.ratingStars ?: return@mapNotNull null
                ShopReviewDto(
                    id = row.id,
                    customerName = row.customerName?.takeIf { it.isNotBlank() } ?: "Customer",
                    stars = stars,
                    comment = row.ratingComment,
                    createdAtEpochMs = row.createdAt,
                    reply = row.ratingReply,
                    replyAtEpochMs = row.ratingReplyAt,
                )
            }

    /** One-way: the shop may set/replace its reply; customers cannot respond to it. */
    fun replyToRating(shopId: String, orderId: String, reply: String): ShopReviewDto {
        val text = reply.trim()
        require(text.isNotEmpty()) { "Reply cannot be empty" }
        require(text.length <= MAX_RATING_REPLY) { "Reply is too long" }
        val row = orderCol.find(and(eq("_id", orderId), eq("shopId", shopId))).firstOrNull()
            ?: error("Rating not found")
        require(row.ratingStars != null) { "Order has no rating" }
        orderCol.updateOne(
            eq("_id", orderId),
            combine(set("ratingReply", text), set("ratingReplyAt", System.currentTimeMillis())),
        )
        return shopRatings(shopId).first { it.id == orderId }
    }

    private fun recalculateShopRating(shopId: String) {
        val rated = orderCol.find(and(eq("shopId", shopId), exists("ratingStars", true))).toList()
        val count = rated.size
        val avg = if (count == 0) 0.0 else rated.mapNotNull { it.ratingStars }.average()
        shopCol.updateOne(
            eq("_id", shopId),
            combine(set("rating", avg), set("ratingCount", count)),
        )
    }

    /** [fromMs] inclusive / [toMs] exclusive bound `createdAt` so reports don't pull the full order history. */
    fun shopOrders(shopId: String, fromMs: Long? = null, toMs: Long? = null): List<OrderDto> =
        mapOrders(
            orderCol.find(
                and(
                    listOfNotNull(
                        eq("shopId", shopId),
                        eq("channel", "ONLINE"),
                        fromMs?.let { com.mongodb.client.model.Filters.gte("createdAt", it) },
                        toMs?.let { com.mongodb.client.model.Filters.lt("createdAt", it) },
                    ),
                ),
            )
                .sort(Sorts.descending("createdAt"))
                .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
                .toList(),
        )

    /**
     * Paged online orders for the POS. [statuses] empty → all; [query] matches order id, customer
     * name or phone (case-insensitive).
     */
    fun shopOrdersPage(
        shopId: String,
        statuses: List<String>,
        query: String,
        page: Int,
        pageSize: Int,
    ): OrderPageDto {
        val size = pageSize.coerceIn(1, MAX_ORDER_PAGE_SIZE)
        val safePage = page.coerceAtLeast(0)
        val base = and(eq("shopId", shopId), eq("channel", "ONLINE"))
        val parts = mutableListOf(base)
        if (statuses.isNotEmpty()) parts += `in`("status", statuses)
        val q = query.trim()
        if (q.isNotEmpty()) {
            val rx = java.util.regex.Pattern.quote(q)
            parts += com.mongodb.client.model.Filters.or(
                com.mongodb.client.model.Filters.regex("_id", rx, "i"),
                com.mongodb.client.model.Filters.regex("customerName", rx, "i"),
                com.mongodb.client.model.Filters.regex("customerPhone", rx, "i"),
            )
        }
        val filter = and(parts)
        val total = orderCol.countDocuments(filter).toInt()
        val rows = orderCol.find(filter)
            .sort(Sorts.descending("createdAt"))
            .skip(safePage * size)
            .limit(size)
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
        val newCount = orderCol.countDocuments(and(base, eq("status", "RECEIVED"))).toInt()
        return OrderPageDto(
            items = mapOrders(rows),
            page = safePage,
            pageSize = size,
            totalCount = total,
            hasMore = (safePage + 1) * size < total,
            newCount = newCount,
        )
    }

    /** Paged customer order history, newest first. */
    fun customerOrdersPage(userId: String, page: Int, pageSize: Int): OrderPageDto {
        val size = pageSize.coerceIn(1, MAX_ORDER_PAGE_SIZE)
        val safePage = page.coerceAtLeast(0)
        val filter = eq("customerId", userId)
        val total = orderCol.countDocuments(filter).toInt()
        val rows = orderCol.find(filter)
            .sort(Sorts.descending("createdAt"))
            .skip(safePage * size)
            .limit(size)
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
        return OrderPageDto(mapOrders(rows), safePage, size, total, (safePage + 1) * size < total)
    }

    suspend fun acceptShopOrder(shopId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        require(row.status == "RECEIVED" || row.status == "ACCEPTED") { "Cannot accept" }
        orderCol.updateOne(eq("_id", orderId), set("status", "ACCEPTED"))
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    fun rejectShopOrder(shopId: String, orderId: String, rejectedIds: List<String>, reason: String?): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        val status: String
        val items = if (rejectedIds.isEmpty()) {
            status = "REJECTED"
            row.items
        } else {
            val kept = row.items.filterNot { it.productId in rejectedIds }
            status = if (kept.isEmpty()) "REJECTED" else "ACCEPTED"
            kept
        }
        orderCol.replaceOne(eq("_id", orderId), row.copy(status = status, items = items, cancelReason = reason))
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    fun setStatus(shopId: String, orderId: String, status: String): OrderDto {
        val allowed = setOf("ACCEPTED", "PACKING", "LOOKING_FOR_PARTNER", "COMPLETED", "REJECTED")
        require(status in allowed) { "Invalid status" }
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        if (status == "PACKING") require(row.status == "ACCEPTED" || row.status == "PACKING") { "Accept first" }
        orderCol.updateOne(eq("_id", orderId), set("status", status))
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    suspend fun cancelShopOrder(shopId: String, orderId: String, reason: String?): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        require(row.status !in setOf("DELIVERED", "COMPLETED", "CANCELLED", "REJECTED")) { "Cannot cancel" }
        val reasonText = reason?.trim().orEmpty().ifBlank { "CANCELLED" }
        val refundPaise = refundAmountPaise(row)
        val refundId = runCatching {
            security.refundRazorpayPayment(
                paymentId = row.paymentId.orEmpty(),
                amountPaise = refundPaise,
                notes = mapOf("order_id" to orderId, "reason" to reasonText),
            )
        }.getOrNull()
        row.items.forEach { item ->
            val product = productCol.find(eq("_id", item.productId)).firstOrNull() ?: return@forEach
            productCol.updateOne(eq("_id", item.productId), set("stockQty", product.stockQty + item.quantity))
        }
        deliveryOfferCol.find(eq("orderId", orderId)).toList().forEach { offer ->
            if (offer.status == "RINGING" || offer.status == "ACCEPTED") {
                deliveryOfferCol.replaceOne(eq("_id", offer.id), offer.copy(status = "CANCELLED"))
            }
        }
        orderCol.replaceOne(
            eq("_id", orderId),
            row.copy(
                status = "CANCELLED",
                cancelReason = reasonText,
                refundId = refundId,
                partnerId = null,
            ),
        )
        val customerId = row.customerId
        if (customerId != null && refundPaise > 0) {
            txnCol.insertOne(
                TxnDoc(
                    security.randomId("txn"),
                    orderId,
                    customerId,
                    refundPaise / 100.0,
                    "Refund $orderId",
                    System.currentTimeMillis(),
                ),
            )
        }
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    suspend fun requestDelivery(
        shopId: String,
        orderId: String,
        packedProductIds: List<String>? = null,
    ): DeliveryOfferDto {
        val order = getOrder(orderId)
        require(order.shopId == shopId) { "Forbidden" }
        require(order.status in setOf("PACKING", "LOOKING_FOR_PARTNER")) { "Pack order first" }
        if (packedProductIds != null) {
            val orderItemIds = order.items.map { it.productId }.toSet()
            val packed = packedProductIds.filter { it in orderItemIds }.distinct()
            require(packed.isNotEmpty()) { "Pack at least one item" }
            orderCol.updateOne(eq("_id", orderId), set("packedProductIds", packed))
        }
        val shop = shopById(shopId)
        val now = System.currentTimeMillis()
        val existing = deliveryOfferCol.find(
            and(eq("orderId", orderId), eq("status", "RINGING")),
        ).firstOrNull()
        if (existing != null && existing.expiresAt >= now) {
            orderCol.updateOne(eq("_id", orderId), set("status", "LOOKING_FOR_PARTNER"))
            publishOrderUpdate(orderId, order.status)
            broadcastOffer(existing, shop, order.deliveryAddress.orEmpty())
            return toOfferDto(existing, dummyPartner())
        }
        val expires = now + platform().offerTtlMs
        val offerId = security.randomId("offr")
        val customerLat = order.customerLat ?: shop.lat
        val customerLng = order.customerLng ?: shop.lng
        // Stored drop = shop→customer. Partner→shop is computed per partner in toOfferDto.
        val dropKm = haversine(shop.lat, shop.lng, customerLat, customerLng)
        val totalKm = (dropKm * 2).coerceAtLeast(0.5)
        val payout = (platform().partnerPayoutPerKmInr * totalKm).let { kotlin.math.round(it * 100) / 100.0 }
        val offer = DeliveryOfferDoc(
            id = offerId,
            orderId = orderId,
            shopId = shopId,
            status = "RINGING",
            payout = payout,
            shopDistanceKm = 0.0,
            dropDistanceKm = dropKm,
            expiresAt = expires,
            dropAddress = order.deliveryAddress.orEmpty(),
        )
        orderCol.updateOne(eq("_id", orderId), set("status", "LOOKING_FOR_PARTNER"))
        deliveryOfferCol.insertOne(offer)
        publishOrderUpdate(orderId, order.status)
        broadcastOffer(offer, shop, order.deliveryAddress.orEmpty())
        return toOfferDto(offer, dummyPartner())
    }

    /** Shop delivers the order itself: withdraws any partner offer and marks the order on the way. */
    fun startSelfDelivery(shopId: String, orderId: String, packedProductIds: List<String>?): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        require(row.partnerId == null) { "A delivery partner already accepted this order" }
        require(row.status in setOf("ACCEPTED", "PACKING", "LOOKING_FOR_PARTNER")) { "Accept and pack the order first" }
        val packed = packedProductIds?.let { ids ->
            val orderItemIds = row.items.map { it.productId }.toSet()
            ids.filter { it in orderItemIds }.distinct().also {
                require(it.isNotEmpty()) { "Pack at least one item" }
            }
        }
        deliveryOfferCol.updateMany(
            and(eq("orderId", orderId), eq("status", "RINGING")),
            set("status", "CANCELLED"),
        )
        val updates = buildList {
            add(set("status", "ON_THE_WAY"))
            add(set("deliveryMode", "SELF"))
            add(set("partnerProgress", "SELF_DELIVERY"))
            if (packed != null) add(set("packedProductIds", packed))
            if (row.deliveryOtp.isNullOrBlank()) add(set("deliveryOtp", security.deliveryOtp()))
        }
        orderCol.updateOne(eq("_id", orderId), combine(updates))
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    fun shopPartners(shopId: String): List<ShopPartnerDto> =
        shopPartnerCol.find(eq("shopId", shopId)).sort(Sorts.ascending("name")).toList().map { it.toDto() }

    /** Adds a rider to the shop's roster; links to a partner-app account when the phone matches one. */
    fun addShopPartner(shopId: String, request: ShopPartnerRequest): ShopPartnerDto {
        val name = request.name.trim()
        val phone = Security.normalizePhone(request.phone)
        require(name.length >= 2) { "Full name is required" }
        require(phone.length == 10) { "Enter a valid 10-digit mobile number" }
        require(shopPartnerCol.find(and(eq("shopId", shopId), eq("phone", phone))).firstOrNull() == null) {
            "This delivery partner is already added"
        }
        val appPartner = partnerCol.find(eq("phone", phone)).projection(partnerLiteProjection).firstOrNull()
        val doc = ShopPartnerDoc(
            id = security.randomId("spt"),
            shopId = shopId,
            name = name,
            phone = phone,
            vehicleReg = Security.normalizeReg(request.vehicleReg),
            partnerId = appPartner?.id,
            createdAt = System.currentTimeMillis(),
        )
        shopPartnerCol.insertOne(doc)
        return doc.toDto()
    }

    fun removeShopPartner(shopId: String, id: String) {
        shopPartnerCol.deleteOne(and(eq("_id", id), eq("shopId", shopId)))
    }

    /**
     * Hands an order to one of the shop's own riders. Riders on the partner app get it as an active
     * job (PARTNER_ACCEPTED); others are tracked as shop delivery completed with the customer's OTP.
     */
    fun assignShopPartner(shopId: String, orderId: String, shopPartnerId: String, packedProductIds: List<String>?): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        require(row.partnerId == null) { "A delivery partner already accepted this order" }
        require(row.status in setOf("ACCEPTED", "PACKING", "LOOKING_FOR_PARTNER")) { "Accept and pack the order first" }
        val rider = shopPartnerCol.find(and(eq("_id", shopPartnerId), eq("shopId", shopId))).firstOrNull()
            ?: error("Delivery partner not found")
        // Re-resolve: the rider may have joined the partner app after being added.
        val appPartnerId = rider.partnerId
            ?: partnerCol.find(eq("phone", rider.phone)).projection(partnerLiteProjection).firstOrNull()?.id
                ?.also { shopPartnerCol.updateOne(eq("_id", rider.id), set("partnerId", it)) }
        val packed = packedProductIds?.let { ids ->
            val orderItemIds = row.items.map { it.productId }.toSet()
            ids.filter { it in orderItemIds }.distinct().also { require(it.isNotEmpty()) { "Pack at least one item" } }
        }
        deliveryOfferCol.updateMany(
            and(eq("orderId", orderId), eq("status", "RINGING")),
            set("status", "CANCELLED"),
        )
        val updates = buildList {
            if (appPartnerId != null) {
                add(set("status", "PARTNER_ACCEPTED"))
                add(set("partnerId", appPartnerId))
                add(set("deliveryMode", "SHOP_PARTNER"))
                add(set("partnerProgress", "TO_STORE"))
                add(set("partnerPayoutInr", 0.0))
            } else {
                add(set("status", "ON_THE_WAY"))
                add(set("deliveryMode", "SELF"))
                add(set("partnerProgress", "SELF_DELIVERY"))
            }
            add(set("ownPartnerName", rider.name))
            add(set("ownPartnerPhone", rider.phone))
            add(set("ownPartnerVehicle", rider.vehicleReg))
            if (packed != null) add(set("packedProductIds", packed))
            if (row.deliveryOtp.isNullOrBlank()) add(set("deliveryOtp", security.deliveryOtp()))
        }
        orderCol.updateOne(eq("_id", orderId), combine(updates))
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    private fun ShopPartnerDoc.toDto() = ShopPartnerDto(id, name, phone, vehicleReg, onPartnerApp = partnerId != null)

    /** Completes a self-delivered order with the customer's 4-digit delivery OTP. */
    fun completeSelfDelivery(shopId: String, orderId: String, otp: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        require(row.deliveryMode == "SELF" && row.status == "ON_THE_WAY") { "Order is not out for self delivery" }
        require(otp.trim().length == 4 && row.deliveryOtp == otp.trim()) { "Invalid delivery OTP" }
        val shop = shopById(shopId)
        val distanceKm = haversine(shop.lat, shop.lng, row.customerLat ?: shop.lat, row.customerLng ?: shop.lng)
        val durationMin = ((System.currentTimeMillis() - row.createdAt) / 60_000).toInt().coerceAtLeast(1)
        orderCol.updateOne(
            eq("_id", orderId),
            combine(
                set("status", "DELIVERED"),
                set("totalDistanceKm", distanceKm),
                set("deliveryDurationMin", durationMin),
                set("partnerPayoutInr", 0.0),
            ),
        )
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    /**
     * Marks unpicked offers past the platform offer TTL as EXPIRED and returns their orders to PACKING
     * so the shop sees "Request delivery" again. Returns the number of offers expired.
     */
    fun expireStaleOffers(now: Long = System.currentTimeMillis()): Int {
        val stale = deliveryOfferCol.find(
            and(eq("status", "RINGING"), lt("expiresAt", now)),
        ).toList()
        stale.forEach { offer ->
            val updated = deliveryOfferCol.updateOne(
                and(eq("_id", offer.id), eq("status", "RINGING")),
                set("status", "EXPIRED"),
            )
            if (updated.modifiedCount == 0L) return@forEach
            val stillRinging = deliveryOfferCol.find(
                and(eq("orderId", offer.orderId), eq("status", "RINGING"), gte("expiresAt", now)),
            ).firstOrNull()
            if (stillRinging == null) {
                val reverted = orderCol.updateOne(
                    and(eq("_id", offer.orderId), eq("status", "LOOKING_FOR_PARTNER")),
                    set("status", "PACKING"),
                )
                if (reverted.modifiedCount > 0) {
                    publishOrderUpdate(offer.orderId, "LOOKING_FOR_PARTNER", partnerTimeout = true)
                }
            }
        }
        return stale.size
    }

    fun incomingForPartner(partnerId: String): DeliveryOfferDto? =
        availableOffersForPartner(partnerId).minByOrNull { it.shopDistanceKm }

    fun availableOffersForPartner(partnerId: String): List<DeliveryOfferDto> {
        val partner = partnerCol.find(eq("_id", partnerId)).projection(partnerLiteProjection).firstOrNull() ?: return emptyList()
        // Online is only for push/socket fan-out; REST listing works while offline too.
        if (!partner.verified || !partner.hasGpsFix()) return emptyList()
        val now = System.currentTimeMillis()
        val ringing = deliveryOfferCol.find(
            and(eq("status", "RINGING"), gte("expiresAt", now)),
        ).toList()
            .filter { it.acceptedBy == null && partnerId !in it.rejectedBy }
        if (ringing.isEmpty()) return emptyList()
        val shops = shopCol.find(`in`("_id", ringing.map { it.shopId }.distinct()))
            .toList()
            .associateBy { it.id }
        val orders = orderCol.find(`in`("_id", ringing.map { it.orderId }.distinct()))
            .toList()
            .associateBy { it.id }
        return ringing
            .mapNotNull { offer ->
                val shop = shops[offer.shopId] ?: return@mapNotNull null
                toOfferDto(offer, partner, shop, orders[offer.orderId])
            }
            .filter { it.shopDistanceKm <= platform().partnerRingKm }
            .sortedBy { it.shopDistanceKm }
    }

    fun offerById(offerId: String, partnerId: String): DeliveryOfferDto {
        val now = System.currentTimeMillis()
        val offer = deliveryOfferCol.find(eq("_id", offerId)).firstOrNull() ?: error("Offer not found")
        val partner = partnerCol.find(eq("_id", partnerId)).projection(partnerLiteProjection).firstOrNull() ?: error("Partner not found")
        val status = when {
            offer.acceptedBy != null && offer.acceptedBy != partnerId -> "TAKEN_BY_OTHER"
            offer.status == "RINGING" && offer.expiresAt < now -> "EXPIRED"
            else -> offer.status
        }
        return toOfferDto(offer, partner).copy(status = status, acceptedByPartnerId = offer.acceptedBy)
    }

    fun acceptOffer(partnerId: String, offerId: String): DeliveryOfferDto {
        val lock = acceptLocks.getOrPut(offerId) { Any() }
        synchronized(lock) {
            val offer = deliveryOfferCol.find(eq("_id", offerId)).firstOrNull() ?: error("Offer not found")
            val partner = partnerCol.find(eq("_id", partnerId)).projection(partnerLiteProjection).firstOrNull() ?: error("Partner not found")
            val now = System.currentTimeMillis()
            if (offer.status != "RINGING" || offer.expiresAt < now) {
                error("ALREADY_TAKEN")
            }
            val dto = toOfferDto(offer, partner)
            require(dto.shopDistanceKm <= platform().partnerRingKm) { "Outside serving area" }
            deliveryOfferCol.updateOne(
                eq("_id", offerId),
                combine(set("status", "ACCEPTED"), set("acceptedBy", partnerId)),
            )
            orderCol.updateOne(
                eq("_id", offer.orderId),
                combine(
                    set("status", "PARTNER_ACCEPTED"),
                    set("partnerId", partnerId),
                    set("partnerPayoutInr", dto.payoutInr),
                    set("totalDistanceKm", dto.totalDistanceKm),
                    set("partnerProgress", "TO_STORE"),
                ),
            )
        }
        val orderId = deliveryOfferCol.find(eq("_id", offerId)).firstOrNull()?.orderId
        if (orderId != null) publishOrderUpdate(orderId, "LOOKING_FOR_PARTNER")
        return offerById(offerId, partnerId)
    }

    fun rejectOffer(partnerId: String, offerId: String): DeliveryOfferDto {
        val offer = deliveryOfferCol.find(eq("_id", offerId)).firstOrNull() ?: error("Offer not found")
        if (offer.status == "RINGING") {
            val rejected = (offer.rejectedBy + partnerId).distinct()
            deliveryOfferCol.replaceOne(eq("_id", offerId), offer.copy(rejectedBy = rejected))
        }
        return offerById(offerId, partnerId)
    }

    fun registerPartner(request: PartnerRegisterRequest): OtpSessionResponse {
        require(platform().partnerRegistrationEnabled) { "New partner registrations are closed right now" }
        val name = request.name.trim()
        val email = request.email.trim()
        val address = request.address.trim()
        val phone = Security.normalizePhone(request.phone)
        val reg = Security.normalizeReg(request.vehicleReg)
        require(name.length >= 2) { "Full name is required" }
        require(phone.length == 10) { "Enter a valid 10-digit mobile number" }
        require(EMAIL_REGEX.matches(email)) { "Enter a valid email address" }
        require(address.isNotEmpty()) { "Address is required" }
        require(Security.isValidVehicleReg(reg)) { "Invalid vehicle registration number (e.g. DL01AB1234)" }
        // Photos are optional; when sent they must be uploaded asset refs, not inline blobs.
        fun photo(label: String, value: String): String {
            val v = value.trim()
            if (v.isEmpty()) return ""
            require(assets.isValidRef(v)) { "$label upload is invalid, please re-upload" }
            return assets.toKey(v).take(2_048)
        }
        val plate = photo("Number plate photo", request.platePhotoUrl)
        val vehicle = photo("Vehicle photo", request.vehiclePhotoUrl)
        val profile = photo("Profile photo", request.profilePhotoUrl)
        val dl = photo("Driving licence photo", request.dlPhotoUrl)
        val idDoc = photo("ID proof photo", request.idPhotoUrl)
        if (request.lat != null || request.lng != null) {
            require((request.lat ?: 0.0) in -90.0..90.0 && (request.lng ?: 0.0) in -180.0..180.0) { "Invalid location" }
        }
        val existing = partnerCol.find(eq("phone", phone)).firstOrNull()
        // Verified partners must log in; an unverified row is a previous attempt whose OTP never completed.
        require(existing == null || !existing.verified) { "Phone already registered as partner, please log in" }
        val takenReg = partnerCol.find(eq("vehicleReg", reg)).firstOrNull()
        require(takenReg == null || takenReg.phone == phone) { "Vehicle already registered with another partner" }
        val doc = PartnerDoc(
            id = existing?.id ?: security.randomId("ptr"),
            phone = phone,
            name = name,
            email = email,
            address = address,
            vehicleReg = reg,
            platePhoto = plate,
            vehiclePhoto = vehicle,
            profilePhoto = profile,
            dlPhoto = dl,
            idPhoto = idDoc,
            lat = request.lat,
            lng = request.lng,
            verified = false,
            joinedAt = existing?.joinedAt ?: System.currentTimeMillis(),
        )
        partnerCol.replaceOne(eq("_id", doc.id), doc, ReplaceOptions().upsert(true))
        customerCol.deleteMany(eq("phone", phone))
        // Firebase needs app proof, which only /auth/otp/request carries; an empty session tells the app to call it.
        return if (config.devAuth) requestOtp(phone) else OtpSessionResponse(sessionId = "")
    }

    fun partnerProfile(partnerId: String): PartnerProfileDto {
        val partner = partnerCol.find(eq("_id", partnerId))
            .projection(partnerProfileProjection)
            .firstOrNull() ?: error("Partner not found")
        val start = startOfToday()
        val end = start + 86_400_000
        val deliveredStatuses = listOf("DELIVERED", "COMPLETED")
        val deliveredFilter = and(eq("partnerId", partnerId), `in`("status", deliveredStatuses))
        val todayFilter = and(deliveredFilter, gte("createdAt", start), lt("createdAt", end))
        val deliveredCount = orderCol.countDocuments(deliveredFilter).toInt()
        val todayOrderIds = orderIdsMatching(todayFilter)
        val todayEarnings = sumPartnerPayout(partnerId, todayOrderIds)
        val allDeliveredIds = orderIdsMatching(deliveredFilter)
        val totalEarnings = sumPartnerPayout(partnerId, allDeliveredIds)
        return PartnerProfileDto(
            id = partner.id,
            name = partner.name,
            email = partner.email,
            phone = partner.phone,
            address = partner.address,
            vehicleReg = partner.vehicleReg,
            verified = partner.verified,
            online = partner.online,
            joinedAtEpochMs = partner.joinedAt,
            todayEarnings = todayEarnings,
            totalEarnings = totalEarnings,
            deliveredCount = deliveredCount,
            profilePhoto = assets.toUrl(partner.profilePhoto),
        )
    }

    private fun orderIdsMatching(filter: Bson): List<String> =
        orderIdCol.find(filter)
            .projection(include("_id"))
            .toList()
            .mapNotNull { it.getString("_id") }

    private fun sumPartnerPayout(partnerId: String, orderIds: List<String>): Double {
        if (orderIds.isEmpty()) return 0.0
        return offerPayoutCol.find(and(eq("acceptedBy", partnerId), `in`("orderId", orderIds)))
            .projection(include("payout"))
            .toList()
            .sumOf { doc ->
                doc.getDouble("payout")
                    ?: doc.getInteger("payout")?.toDouble()
                    ?: 0.0
            }
    }

    fun setPartnerOnline(partnerId: String, online: Boolean) {
        partnerCol.updateOne(eq("_id", partnerId), set("online", online))
    }

    fun updatePartnerProfilePhoto(partnerId: String, profilePhotoUrl: String) {
        require(assets.isValidRef(profilePhotoUrl)) { "Invalid profile photo URL" }
        val result = partnerCol.updateOne(
            eq("_id", partnerId),
            set("profilePhoto", assets.toKey(profilePhotoUrl).take(2_048)),
        )
        require(result.matchedCount > 0) { "Partner not found" }
    }

    fun updatePartnerLocation(partnerId: String, lat: Double, lng: Double) {
        if (lat == 0.0 && lng == 0.0) return
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return
        partnerCol.updateOne(
            eq("_id", partnerId),
            combine(set("lat", lat), set("lng", lng)),
        )
    }

    fun partnerEarnings(partnerId: String): PartnerEarningsDto {
        val start = startOfToday()
        val delivered = orderCol.find(
            and(eq("partnerId", partnerId), `in`("status", listOf("DELIVERED", "COMPLETED"))),
        ).toList()
        val deliveredIds = delivered.map { it.id }.toSet()
        val acceptedOffers = deliveryOfferCol.find(eq("acceptedBy", partnerId)).toList()
        val totalEarnings = acceptedOffers.filter { it.orderId in deliveredIds }.sumOf { it.payout }
        val todayIds = delivered.filter { it.createdAt >= start }.map { it.id }.toSet()
        val todayEarnings = acceptedOffers.filter { it.orderId in todayIds }.sumOf { it.payout }
        val offersSeen = deliveryOfferCol.countDocuments(
            or(eq("acceptedBy", partnerId), eq("rejectedBy", partnerId)),
        ).toInt()
        val accepted = acceptedOffers.size
        val acceptanceRate = if (offersSeen == 0) 100 else ((accepted * 100) / offersSeen).coerceIn(0, 100)
        val weekly = (6 downTo 0).map { daysAgo ->
            val dayStart = start - daysAgo * 86_400_000L
            val dayEnd = dayStart + 86_400_000L
            val dayIds = delivered.filter { it.createdAt in dayStart until dayEnd }.map { it.id }.toSet()
            val amount = acceptedOffers.filter { it.orderId in dayIds }.sumOf { it.payout }
            val label = java.time.Instant.ofEpochMilli(dayStart)
                .atZone(java.time.ZoneId.systemDefault())
                .dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }
            PartnerDailyEarningDto(label, amount)
        }
        return PartnerEarningsDto(
            todayEarnings = todayEarnings,
            totalEarnings = totalEarnings,
            deliveredCount = delivered.size,
            acceptanceRatePercent = acceptanceRate,
            weeklyEarnings = weekly,
        )
    }

    fun partnerJob(partnerId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        return row.toDto().copy(deliveryOtp = null)
    }

    fun arrivedAtStore(partnerId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        require(row.status == "PARTNER_ACCEPTED") { "Invalid status" }
        if (row.partnerProgress != "AT_STORE" && row.partnerProgress != "CAPTURE") {
            orderCol.updateOne(eq("_id", orderId), set("partnerProgress", "AT_STORE"))
            publishOrderUpdate(orderId, row.status, always = true)
        }
        return getOrder(orderId)
    }

    fun verifyBags(partnerId: String, orderId: String, photoOne: String, photoTwo: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        require(row.status == "PARTNER_ACCEPTED") { "Invalid status" }
        val updates = if (assets.isValidRef(photoOne) && assets.isValidRef(photoTwo)) {
            combine(set("partnerProgress", "CAPTURE"), set("pickupPhotos", assets.toKeys(listOf(photoOne, photoTwo))))
        } else {
            set("partnerProgress", "CAPTURE")
        }
        orderCol.updateOne(eq("_id", orderId), updates)
        return getOrder(orderId)
    }

    fun arrivedAtCustomer(partnerId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        require(row.status == "ON_THE_WAY") { "Invalid status" }
        if (row.partnerProgress != "AT_CUSTOMER") {
            orderCol.updateOne(eq("_id", orderId), set("partnerProgress", "AT_CUSTOMER"))
            publishOrderUpdate(orderId, row.status, always = true)
        }
        return getOrder(orderId)
    }

    fun partnerDashboard(partnerId: String, from: Long, to: Long): PartnerDashboardDto {
        val delivered = orderCol.find(and(eq("partnerId", partnerId), `in`("status", listOf("DELIVERED", "COMPLETED"))))
            .toList()
            .filter { it.createdAt in from..to }
        val accepted = orderCol.find(
            and(eq("partnerId", partnerId), `in`("status", listOf("PARTNER_ACCEPTED", "ON_THE_WAY"))),
        ).toList()
        val deliveredIds = delivered.map { it.id }.toSet()
        val earnings = deliveryOfferCol.find(eq("acceptedBy", partnerId)).toList()
            .filter { it.orderId in deliveredIds }
            .sumOf { it.payout }
        return PartnerDashboardDto(delivered.size, accepted.size, earnings, from, to)
    }

    fun partnerJobs(partnerId: String, delivered: Boolean, from: Long? = null, to: Long? = null): List<OrderDto> {
        val statuses = if (delivered) listOf("DELIVERED", "COMPLETED") else listOf("PARTNER_ACCEPTED", "ON_THE_WAY")
        val filter = if (delivered && from != null && to != null) {
            and(
                eq("partnerId", partnerId),
                `in`("status", statuses),
                gte("createdAt", from),
                lt("createdAt", to),
            )
        } else {
            and(eq("partnerId", partnerId), `in`("status", statuses))
        }
        return mapOrders(
            orderCol.find(filter)
                .projection(orderListProjection)
                .sort(Sorts.descending("createdAt"))
                .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
                .toList(),
        )
    }

    suspend fun cancelPickup(partnerId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        orderCol.replaceOne(eq("_id", orderId), row.copy(status = "LOOKING_FOR_PARTNER", partnerId = null, partnerProgress = ""))
        publishOrderUpdate(orderId, row.status)
        deliveryOfferCol.find(eq("orderId", orderId)).toList().forEach { offer ->
            deliveryOfferCol.replaceOne(eq("_id", offer.id), offer.copy(status = "CANCELLED", acceptedBy = null))
        }
        runCatching { requestDelivery(row.shopId, orderId) }
        return getOrder(orderId)
    }

    fun submitPickup(partnerId: String, orderId: String, photoOne: String, photoTwo: String): OrderDto {
        require(assets.isValidRef(photoOne) && assets.isValidRef(photoTwo)) { "Two pickup photo URLs required" }
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        orderCol.updateOne(
            eq("_id", orderId),
            combine(
                set("status", "ON_THE_WAY"),
                set("pickupPhotos", assets.toKeys(listOf(photoOne, photoTwo))),
                set("partnerProgress", "TO_CUSTOMER"),
            ),
        )
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    fun deliver(partnerId: String, orderId: String, otp: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        require(otp.length == 4 && row.deliveryOtp == otp) { "Invalid delivery OTP" }
        val offer = deliveryOfferCol.find(and(eq("orderId", orderId), eq("acceptedBy", partnerId))).firstOrNull()
        val totalKm = row.totalDistanceKm
            ?: offer?.let { stored ->
                val partner = partnerCol.find(eq("_id", partnerId)).firstOrNull()
                if (partner != null) toOfferDto(stored, partner).totalDistanceKm
                else stored.dropDistanceKm
            }
            ?: 0.0
        val durationMin = ((System.currentTimeMillis() - row.createdAt) / 60_000).toInt().coerceAtLeast(1)
        orderCol.updateOne(
            eq("_id", orderId),
            combine(
                set("status", "DELIVERED"),
                set("totalDistanceKm", totalKm),
                set("deliveryDurationMin", durationMin),
                set("partnerPayoutInr", offer?.payout ?: row.partnerPayoutInr),
            ),
        )
        publishOrderUpdate(orderId, row.status)
        return getOrder(orderId)
    }

    fun pullSync(shopId: String): SyncPullResponse = SyncPullResponse(
        products = catalog(shopId, includeInactive = true),
        onlineOrders = shopOrders(shopId),
        masterProducts = masterProducts(),
    )

    fun pushSync(shopId: String, request: SyncPushRequest): SyncPushResponse {
        request.products.filter { it.shopId == shopId }.forEach { product ->
            val previous = productCol.find(eq("_id", product.id)).firstOrNull()
            val doc = product.toDoc()
            val stored = productCol.replaceOne(eq("_id", product.id), doc, ReplaceOptions().upsert(true))
            if (stored.wasAcknowledged()) recordPriceChange(previous, doc)
        }
        // Bills before products: a pushed product carries the device stock, which already includes its bills.
        val accepted = ArrayList<OfflineOrderAck>(request.offlineOrders.size)
        for (envelope in request.offlineOrders) {
            val order = envelope.order
            if (envelope.revision <= 0L || order.id.isBlank() || order.shopId != shopId) continue
            if (order.channel != "POS") continue
            val existing = orderCol.find(eq("_id", order.id)).firstOrNull()
            if (existing != null && (existing.channel != "POS" || existing.shopId != shopId)) continue
            val stored = orderCol.replaceOne(
                eq("_id", order.id),
                order.toPosDoc(System.currentTimeMillis()),
                ReplaceOptions().upsert(true),
            )
            if (stored.wasAcknowledged()) {
                applyPosStockDelta(shopId, existing, order)
                accepted += OfflineOrderAck(order.id, envelope.revision)
            }
        }
        val acceptedProducts = ArrayList<OfflineOrderAck>(request.offlineProducts.size)
        for (envelope in request.offlineProducts) {
            val product = envelope.product
            if (envelope.revision <= 0L || product.id.isBlank() || product.shopId != shopId) continue
            val existing = productCol.find(eq("_id", product.id)).firstOrNull()
            if (existing != null && existing.shopId != shopId) continue
            val doc = product.toDoc()
            val stored = productCol.replaceOne(eq("_id", product.id), doc, ReplaceOptions().upsert(true))
            if (stored.wasAcknowledged()) {
                recordPriceChange(existing, doc)
                acceptedProducts += OfflineOrderAck(product.id, envelope.revision)
            }
        }
        return SyncPushResponse(accepted, acceptedProducts)
    }

    /** POS bills (any device) changed after [since]/[afterId], oldest write first. */
    fun posOrdersSince(shopId: String, since: Long, afterId: String, limit: Int): SyncOrdersPage {
        val size = limit.coerceIn(1, POS_SYNC_PAGE_MAX)
        val rows = orderCol.find(
            and(
                eq("shopId", shopId),
                eq("channel", "POS"),
                or(gt("syncedAt", since), and(eq("syncedAt", since), gt("_id", afterId))),
            ),
        )
            .sort(Sorts.ascending("syncedAt", "_id"))
            .limit(size + 1)
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
        val page = rows.take(size)
        val last = page.lastOrNull()
        return SyncOrdersPage(
            orders = page.map { it.toDto() },
            nextSince = last?.syncedAt ?: since,
            nextAfterId = last?.id ?: afterId,
            hasMore = rows.size > size,
        )
    }

    private fun applyPosStockDelta(shopId: String, previous: OrderDoc?, next: OrderDto) {
        fun qty(items: List<OrderItemDto>, cancelled: Boolean): Map<String, Double> =
            if (cancelled) emptyMap() else items.groupBy { it.productId }.mapValues { (_, l) -> l.sumOf { it.quantity } }
        val before = previous?.let { qty(it.items, it.status == "CANCELLED") }.orEmpty()
        val after = qty(next.items, next.status == "CANCELLED")
        (before.keys + after.keys).forEach { productId ->
            val delta = (after[productId] ?: 0.0) - (before[productId] ?: 0.0)
            if (delta != 0.0) {
                productCol.updateOne(and(eq("_id", productId), eq("shopId", shopId)), inc("stockQty", -delta))
            }
        }
    }

    private fun OrderDto.toPosDoc(syncedAt: Long) = OrderDoc(
        id = id,
        shopId = shopId,
        customerId = customerId,
        status = status,
        channel = "POS",
        customerName = customerName,
        customerPhone = customerPhone,
        paymentId = paymentId,
        paymentMethod = paymentMethod.ifBlank { "CASH" },
        createdAt = createdAtEpochMs,
        items = items,
        quote = quote,
        cancelReason = cancelReason,
        discount = discount,
        syncedAt = syncedAt,
    )

    fun masterCategories(): List<MasterCategoryDto> =
        categoryCol.find().toList().map { MasterCategoryDto(it.id, it.name, it.parentId) }

    fun masterProducts(): List<MasterProductDto> =
        masterProductCol.find().toList().map { it.toDto() }

    fun masterProduct(id: String): MasterProductDto =
        masterProductCol.find(eq("_id", id)).firstOrNull()?.toDto() ?: error("Product not found")

    fun masterProductPage(query: String, categoryId: String?, page: Int, pageSize: Int): MasterProductPageDto {
        val size = pageSize.coerceIn(1, MAX_MASTER_PAGE_SIZE)
        val safePage = page.coerceAtLeast(0)
        val parts = mutableListOf<Bson>()
        if (!categoryId.isNullOrBlank()) parts += or(eq("categoryId", categoryId), eq("subcategoryId", categoryId))
        val q = query.trim()
        if (q.isNotEmpty()) {
            val pattern = java.util.regex.Pattern.quote(q)
            parts += or(
                regex("name", pattern, "i"),
                regex("nameHi", pattern, "i"),
                regex("brandName", pattern, "i"),
                eq("barcode", q),
                eq("variants.barcode", q),
            )
        }
        val filter = if (parts.isEmpty()) Document() else and(parts)
        val total = masterProductCol.countDocuments(filter).toInt()
        val items = masterProductCol.find(filter)
            .sort(Sorts.ascending("name", "_id"))
            .skip(safePage * size)
            .limit(size)
            .toList()
            .map { it.toDto() }
        return MasterProductPageDto(items, safePage, size, total, (safePage + 1) * size < total)
    }

    fun upsertMasterCategory(body: MasterCategoryUpsert): MasterCategoryDto {
        val id = body.id?.takeIf { it.isNotBlank() } ?: "cat_${UUID.randomUUID().toString().take(8)}"
        val doc = CategoryDoc(id, body.name.trim(), body.parentId?.takeIf { it.isNotBlank() })
        categoryCol.replaceOne(eq("_id", id), doc, ReplaceOptions().upsert(true))
        return MasterCategoryDto(doc.id, doc.name, doc.parentId)
    }

    fun deleteMasterCategory(id: String) {
        if (masterProductCol.countDocuments(eq("categoryId", id)) > 0L) {
            error("Category has products")
        }
        if (categoryCol.countDocuments(eq("parentId", id)) > 0L) {
            error("Category has subcategories")
        }
        categoryCol.deleteOne(eq("_id", id))
    }

    fun upsertMasterProduct(body: MasterProductUpsert): MasterProductDto {
        val id = body.id?.takeIf { it.isNotBlank() } ?: "mp_${UUID.randomUUID().toString().take(8)}"
        val existing = masterProductCol.find(eq("_id", id)).firstOrNull()
        val urls = normalizeProductImageUrls(body.imageUrls, body.imageUrl, body.thumbnailUrl)
        val doc = MasterProductDoc(
            id = id,
            name = body.name.trim(),
            nameHi = body.nameHi.trim(),
            categoryId = body.categoryId,
            unit = body.unit.ifBlank { "KG" },
            barcode = body.barcode?.takeIf { it.isNotBlank() },
            imageUrl = urls.firstOrNull(),
            thumbnailUrl = urls.firstOrNull(),
            imageUrls = urls,
            brandName = body.brandName.trim(),
            companyName = body.companyName.trim(),
            subcategoryId = body.subcategoryId?.takeIf { it.isNotBlank() },
            salePrice = body.salePrice,
            cost = body.cost,
            active = body.active,
            addedAt = existing?.addedAt?.takeIf { it > 0 } ?: System.currentTimeMillis(),
            description = body.description,
            sku = body.sku.trim(),
            stockQty = body.stockQty,
            lowStockThreshold = body.lowStockThreshold,
            tags = body.tags.trim(),
            weightKg = body.weightKg,
            dimensions = body.dimensions.trim(),
            variants = body.variants.toVariantDocs(),
        )
        masterProductCol.replaceOne(eq("_id", id), doc, ReplaceOptions().upsert(true))
        return doc.toDto()
    }

    fun deleteMasterProduct(id: String) {
        masterProductCol.deleteOne(eq("_id", id))
    }

    fun shopTypes(): List<ShopTypeDto> =
        shopTypeCol.find().toList().map { ShopTypeDto(it.id, it.name, it.active) }

    fun upsertShopType(body: ShopTypeUpsert): ShopTypeDto {
        val id = body.id?.takeIf { it.isNotBlank() }
            ?: body.name.trim().uppercase().replace(Regex("[^A-Z0-9]+"), "_")
        val doc = ShopTypeDoc(id, body.name.trim(), body.active)
        shopTypeCol.replaceOne(eq("_id", id), doc, ReplaceOptions().upsert(true))
        return ShopTypeDto(doc.id, doc.name, doc.active)
    }

    fun deleteShopType(id: String) {
        shopTypeCol.deleteOne(eq("_id", id))
    }

    fun uploadAsset(
        prefix: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String,
        thumbnail: ByteArray? = null,
    ): UploadResultDto = assetStore.save(prefix, fileName, bytes, contentType, thumbnail)

    fun createShopAdmin(
        name: String,
        shopType: String,
        address: String,
        lat: Double,
        lng: Double,
        active: Boolean,
        imageUrl: String? = null,
        ownerName: String = "",
        ownerPhone: String = "",
        ownerEmail: String = "",
        city: String = "",
        state: String = "",
        zip: String = "",
        country: String = "India",
        registrationNumber: String = "",
        taxId: String = "",
        operatingHours: List<ShopHoursDayDto> = emptyList(),
        features: ShopFeaturesDto? = null,
        loginUsername: String = "",
        loginPassword: String = "",
    ): AdminShopDto {
        require(name.isNotBlank()) { "Shop name is required" }
        require(shopType.isNotBlank()) { "Shop category is required" }
        require(address.isNotBlank() || city.isNotBlank()) { "Shop address is required" }
        validateShopLocation(lat, lng)
        val phone = ownerPhone.trim().takeIf { it.isNotEmpty() }?.let { Security.normalizePhone(it) }
        require(phone == null || phone.length == 10) { "Owner phone must be a 10-digit mobile number" }
        require(ownerEmail.isBlank() || EMAIL_REGEX.matches(ownerEmail.trim())) { "Owner email is invalid" }
        val username = normalizeLoginUsername(loginUsername.ifBlank { phone.orEmpty() })
        require(username.isNotEmpty()) { "Login username (or owner phone) is required" }
        require(loginPassword.trim().length >= MIN_SHOP_PASSWORD) {
            "Login password must be at least $MIN_SHOP_PASSWORD characters"
        }
        require(shopUserCol.find(eq("username", username)).firstOrNull() == null) {
            "Login username '$username' is already taken"
        }
        val id = "shop_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val composedAddress = listOf(address, city, state, zip, country)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        shopCol.insertOne(
            ShopDoc(
                id = id,
                name = name.trim(),
                imageUrl = assets.toKeyOrNull(imageUrl),
                rating = 0.0,
                ratingCount = 0,
                lat = lat,
                lng = lng,
                isOpen = true,
                active = active,
                paymentsEnabled = true,
                discountPercent = 0.0,
                deliveryRadiusKm = platform().defaultShopDeliveryRadiusKm,
                upiId = config.defaultShopUpi,
                address = composedAddress.ifBlank { address },
                shopType = shopType.ifBlank { "GENERAL_STORE" },
                joinedAt = now,
                features = features?.let {
                    ShopFeaturesDoc(
                        voiceSearch = it.voiceSearch,
                        barcodeSearch = it.barcodeSearch,
                        reportGeneration = it.reportGeneration,
                        onlineOrders = it.onlineOrders,
                        inventoryAlerts = it.inventoryAlerts,
                    )
                } ?: ShopFeaturesDoc(),
                ownerName = ownerName.trim(),
                ownerPhone = phone ?: ownerPhone.trim(),
                ownerEmail = ownerEmail.trim(),
                city = city.trim(),
                state = state.trim(),
                zip = zip.trim(),
                country = country.trim().ifBlank { "India" },
                registrationNumber = registrationNumber.trim(),
                taxId = taxId.trim(),
                operatingHours = operatingHours.map {
                    ShopHoursDayDoc(it.day, it.start, it.end, it.closed)
                },
            ),
        )
        runCatching {
            shopUserCol.insertOne(
                ShopUserDoc(
                    id = security.randomId("user"),
                    shopId = id,
                    username = username,
                    passwordHash = security.hashPassword(loginPassword),
                    displayName = ownerName.trim().ifBlank { name.trim() },
                    role = "SHOP",
                ),
            )
        }.onFailure {
            shopCol.deleteOne(eq("_id", id))
            throw it
        }
        return shopById(id).toAdminDto(username)
    }

    private fun validateShopLocation(lat: Double, lng: Double) {
        require(lat in -90.0..90.0 && lng in -180.0..180.0) { "Shop location is invalid" }
        require(lat != 0.0 || lng != 0.0) { "Pick the shop location on the map" }
    }

    private fun normalizeLoginUsername(raw: String): String {
        val key = raw.trim().lowercase()
        require(key.isEmpty() || LOGIN_USERNAME_REGEX.matches(key)) {
            "Login username may use 3-40 letters, digits, '.', '_', '-' or '@'"
        }
        return key
    }

    /** Creates or updates the owner POS login of [shopId]; password is optional on update. */
    private fun upsertShopOwnerLogin(shopId: String, rawUsername: String?, password: String?, displayName: String) {
        val pwd = password?.trim().orEmpty()
        val username = rawUsername?.let(::normalizeLoginUsername).orEmpty()
        if (username.isEmpty() && pwd.isEmpty()) return
        if (pwd.isNotEmpty()) {
            require(pwd.length >= MIN_SHOP_PASSWORD) { "Login password must be at least $MIN_SHOP_PASSWORD characters" }
        }
        val current = shopUserCol.find(and(eq("shopId", shopId), eq("role", "SHOP"))).firstOrNull()
        if (username.isNotEmpty()) {
            val taken = shopUserCol.find(eq("username", username)).firstOrNull()
            require(taken == null || taken.id == current?.id) { "Login username '$username' is already taken" }
        }
        if (current == null) {
            require(username.isNotEmpty()) { "Login username is required" }
            require(pwd.isNotEmpty()) { "Login password is required" }
            shopUserCol.insertOne(
                ShopUserDoc(security.randomId("user"), shopId, username, security.hashPassword(pwd), displayName, "SHOP"),
            )
            return
        }
        val updates = buildList {
            if (username.isNotEmpty()) add(set("username", username))
            if (pwd.isNotEmpty()) add(set("passwordHash", security.hashPassword(pwd)))
        }
        if (updates.isNotEmpty()) shopUserCol.updateOne(eq("_id", current.id), combine(updates))
    }

    private fun shopLoginUsernames(): Map<String, String> =
        shopUserCol.find(eq("role", "SHOP")).toList()
            .groupBy { it.shopId }
            .mapValues { (_, users) -> users.first().username }

    fun patchShop(
        shopId: String,
        active: Boolean?,
        payments: Boolean?,
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
    ) {
        val shop = shopById(shopId)
        require(deliveryRadiusKm == null || deliveryRadiusKm in 1.0..100.0) { "Delivery radius must be 1-100 km" }
        require(discountPercent == null || discountPercent in 0.0..90.0) { "Discount must be 0-90%" }
        if (name != null) require(name.isNotBlank()) { "Shop name is required" }
        if (lat != null || lng != null) validateShopLocation(lat ?: shop.lat, lng ?: shop.lng)
        val phone = ownerPhone?.trim()?.takeIf { it.isNotEmpty() }?.let { Security.normalizePhone(it) }
        require(phone == null || phone.length == 10) { "Owner phone must be a 10-digit mobile number" }
        require(ownerEmail.isNullOrBlank() || EMAIL_REGEX.matches(ownerEmail.trim())) { "Owner email is invalid" }
        upsertShopOwnerLogin(shopId, loginUsername, loginPassword, (ownerName ?: shop.ownerName).ifBlank { shop.name })
        val updates = buildList {
            if (active != null) add(set("active", active))
            if (payments != null) add(set("paymentsEnabled", payments))
            if (features != null) {
                add(set("features", ShopFeaturesDoc(
                    voiceSearch = features.voiceSearch,
                    barcodeSearch = features.barcodeSearch,
                    reportGeneration = features.reportGeneration,
                    onlineOrders = features.onlineOrders,
                    inventoryAlerts = features.inventoryAlerts,
                )))
            }
            if (imageUrl != null) add(set("imageUrl", assets.toKeyOrNull(imageUrl)))
            if (name != null) add(set("name", name.trim()))
            if (address != null) add(set("address", address.trim()))
            if (shopType != null) add(set("shopType", shopType.trim()))
            if (ownerName != null) add(set("ownerName", ownerName.trim()))
            if (ownerPhone != null) add(set("ownerPhone", phone ?: ownerPhone.trim()))
            if (ownerEmail != null) add(set("ownerEmail", ownerEmail.trim()))
            if (city != null) add(set("city", city.trim()))
            if (state != null) add(set("state", state.trim()))
            if (zip != null) add(set("zip", zip.trim()))
            if (country != null) add(set("country", country.trim()))
            if (registrationNumber != null) add(set("registrationNumber", registrationNumber.trim()))
            if (taxId != null) add(set("taxId", taxId.trim()))
            if (lat != null) add(set("lat", lat))
            if (lng != null) add(set("lng", lng))
            if (operatingHours != null) {
                add(set("operatingHours", operatingHours.map { ShopHoursDayDoc(it.day, it.start, it.end, it.closed) }))
            }
            if (upiId != null) add(set("upiId", upiId.trim()))
            if (deliveryRadiusKm != null) add(set("deliveryRadiusKm", deliveryRadiusKm))
            if (discountPercent != null) add(set("discountPercent", discountPercent))
        }
        if (updates.isNotEmpty()) {
            shopCol.updateOne(eq("_id", shopId), combine(updates))
        }
    }

    private fun shopUser(userId: String): ShopUserDoc =
        shopUserCol.find(eq("_id", userId)).firstOrNull() ?: error("Unauthorized")

    /** Returns the caller's shop id when its login is active and holds [permission]. */
    fun requireShopPermission(userId: String, permission: String): String {
        val user = shopUser(userId)
        if (!user.active || permission !in effectivePermissions(user)) error("Forbidden")
        return user.shopId
    }

    private fun ShopUserDoc.toStaffDto() = ShopStaffDto(
        id = id,
        username = username,
        displayName = displayName,
        role = role,
        permissions = effectivePermissions(this),
        active = active,
    )

    fun shopProfile(userId: String): ShopProfileDto {
        val user = shopUser(userId)
        if (!user.active) error("Forbidden")
        val login = shopUserCol.find(and(eq("shopId", user.shopId), eq("role", ShopRole.ADMIN)))
            .firstOrNull()?.username.orEmpty()
        return ShopProfileDto(shopById(user.shopId).toAdminDto(login), user.toStaffDto())
    }

    fun updateShopProfile(userId: String, patch: AdminShopPatch): ShopProfileDto {
        val shopId = if (patch.features != null) {
            requireShopPermission(userId, ShopPerm.TOGGLE_FEATURES)
        } else {
            shopUser(userId).shopId
        }
        if (patch.copy(features = null) != AdminShopPatch()) requireShopPermission(userId, ShopPerm.SHOP_PROFILE)
        patchShop(
            shopId = shopId,
            active = patch.active,
            payments = patch.paymentsEnabled,
            features = patch.features,
            imageUrl = patch.imageUrl,
            name = patch.name,
            address = patch.address,
            shopType = patch.shopType,
            ownerName = patch.ownerName,
            ownerPhone = patch.ownerPhone,
            ownerEmail = patch.ownerEmail,
            city = patch.city,
            state = patch.state,
            zip = patch.zip,
            country = patch.country,
            registrationNumber = patch.registrationNumber,
            taxId = patch.taxId,
            lat = patch.lat,
            lng = patch.lng,
            operatingHours = patch.operatingHours,
            loginUsername = patch.loginUsername,
            loginPassword = patch.loginPassword,
            upiId = patch.upiId,
            deliveryRadiusKm = patch.deliveryRadiusKm,
            discountPercent = patch.discountPercent,
        )
        return shopProfile(userId)
    }

    fun listStaff(userId: String): List<ShopStaffDto> {
        val shopId = requireShopPermission(userId, ShopPerm.MANAGE_STAFF)
        return shopUserCol.find(eq("shopId", shopId)).toList()
            .sortedWith(compareBy({ it.role != ShopRole.ADMIN }, { it.displayName.lowercase() }))
            .map { it.toStaffDto() }
    }

    /** Creates an EXECUTIVE login when [staffId] is null; admin (`SHOP`) logins are managed by master. */
    fun upsertStaff(userId: String, staffId: String?, body: ShopStaffUpsert): ShopStaffDto {
        val shopId = requireShopPermission(userId, ShopPerm.MANAGE_STAFF)
        val permissions = body.permissions.filter { it in ShopPerm.ALL }.distinct()
        val granterPermissions = effectivePermissions(shopUser(userId))
        if (!permissions.all { it in granterPermissions }) error("Forbidden: you can only grant access you have")
        val password = body.password.trim()
        if (password.isNotEmpty()) {
            require(password.length >= MIN_SHOP_PASSWORD) { "Password must be at least $MIN_SHOP_PASSWORD characters" }
        }
        if (staffId == null) {
            val username = normalizeLoginUsername(body.username)
            require(username.isNotEmpty()) { "Login username is required" }
            require(password.isNotEmpty()) { "Password is required" }
            require(body.displayName.isNotBlank()) { "Name is required" }
            require(shopUserCol.find(eq("username", username)).firstOrNull() == null) {
                "Login username '$username' is already taken"
            }
            val doc = ShopUserDoc(
                id = security.randomId("user"),
                shopId = shopId,
                username = username,
                passwordHash = security.hashPassword(password),
                displayName = body.displayName.trim(),
                role = ShopRole.EXECUTIVE,
                permissions = permissions,
                active = body.active,
                createdAt = System.currentTimeMillis(),
            )
            shopUserCol.insertOne(doc)
            return doc.toStaffDto()
        }
        val current = shopUserCol.find(and(eq("_id", staffId), eq("shopId", shopId))).firstOrNull()
            ?: error("Staff login not found")
        require(current.role == ShopRole.EXECUTIVE) { "Admin logins can only be changed from the master portal" }
        requireCanManage(granterPermissions, current)
        require(current.id != userId || body.active) { "You cannot disable your own login" }
        // Keep access the editor cannot see/grant; otherwise a limited manager would silently strip it.
        val retained = current.permissions.filter { it in ShopPerm.ALL && it !in granterPermissions }
        val updates = buildList {
            if (body.displayName.isNotBlank()) add(set("displayName", body.displayName.trim()))
            if (password.isNotEmpty()) add(set("passwordHash", security.hashPassword(password)))
            add(set("permissions", (retained + permissions).distinct()))
            add(set("active", body.active))
        }
        shopUserCol.updateOne(eq("_id", current.id), combine(updates))
        return shopUser(current.id).toStaffDto()
    }

    fun deleteStaff(userId: String, staffId: String) {
        val shopId = requireShopPermission(userId, ShopPerm.MANAGE_STAFF)
        require(staffId != userId) { "You cannot delete your own login" }
        val current = shopUserCol.find(and(eq("_id", staffId), eq("shopId", shopId))).firstOrNull()
            ?: error("Staff login not found")
        require(current.role == ShopRole.EXECUTIVE) { "Admin logins can only be changed from the master portal" }
        requireCanManage(effectivePermissions(shopUser(userId)), current)
        shopUserCol.deleteOne(eq("_id", staffId))
    }

    /** Executives holding full admin access can only be edited / disabled / removed by another admin. */
    private fun requireCanManage(granterPermissions: List<String>, target: ShopUserDoc) {
        if (ShopPerm.ADMIN in target.permissions && ShopPerm.ADMIN !in granterPermissions) {
            error("Forbidden: only a shop admin can change a login with full admin access")
        }
    }

    private fun OfferDoc.toShopOfferDto() = ShopOfferDto(
        id = id,
        title = title,
        description = description,
        scope = scope,
        discountType = discountType,
        value = value,
        productIds = productIds,
        minBillAmount = minBillAmount,
        active = active,
        startsAtEpochMs = startsAt,
        endsAtEpochMs = endsAt,
    )

    /** Live offers for billing (any shop login); full list incl. inactive for [ShopPerm.OFFERS]. */
    fun shopOffers(userId: String, includeInactive: Boolean): List<ShopOfferDto> {
        val shopId = if (includeInactive) requireShopPermission(userId, ShopPerm.OFFERS) else shopUser(userId).shopId
        val rows = if (includeInactive) offerCol.find(eq("shopId", shopId)).toList() else liveOffers(shopId)
        return rows.sortedByDescending { it.createdAt }.map { it.toShopOfferDto() }
    }

    fun saveOffer(userId: String, offerId: String?, body: ShopOfferDto): ShopOfferDto {
        val shopId = requireShopPermission(userId, ShopPerm.OFFERS)
        require(body.title.isNotBlank()) { "Offer title is required" }
        require(body.scope in listOf(OfferScope.BILL, OfferScope.PRODUCT)) { "Invalid offer scope" }
        require(body.discountType in listOf(OfferType.PERCENT, OfferType.FLAT)) { "Invalid discount type" }
        require(body.value > 0.0) { "Discount must be greater than 0" }
        require(body.discountType != OfferType.PERCENT || body.value <= 90.0) { "Percent discount must be at most 90" }
        require(body.scope != OfferScope.PRODUCT || body.productIds.isNotEmpty()) { "Pick at least one product" }
        require(body.minBillAmount >= 0.0) { "Minimum bill cannot be negative" }
        val start = body.startsAtEpochMs
        val end = body.endsAtEpochMs
        require(start == null || end == null || end >= start) { "Offer end must be after start" }
        val existing = offerId?.let { id ->
            offerCol.find(and(eq("_id", id), eq("shopId", shopId))).firstOrNull() ?: error("Offer not found")
        }
        val doc = OfferDoc(
            id = existing?.id ?: security.randomId("off"),
            shopId = shopId,
            title = body.title.trim(),
            description = body.description.trim(),
            discountPercent = if (body.discountType == OfferType.PERCENT) body.value else 0.0,
            scope = body.scope,
            discountType = body.discountType,
            value = body.value,
            productIds = if (body.scope == OfferScope.PRODUCT) body.productIds.distinct() else emptyList(),
            minBillAmount = if (body.scope == OfferScope.BILL) body.minBillAmount else 0.0,
            active = body.active,
            startsAt = start,
            endsAt = end,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        )
        offerCol.replaceOne(eq("_id", doc.id), doc, ReplaceOptions().upsert(true))
        return doc.toShopOfferDto()
    }

    fun deleteOffer(userId: String, offerId: String) {
        val shopId = requireShopPermission(userId, ShopPerm.OFFERS)
        offerCol.deleteOne(and(eq("_id", offerId), eq("shopId", shopId)))
    }

    fun shopRatingsForStaff(userId: String): List<ShopReviewDto> =
        shopRatings(requireShopPermission(userId, ShopPerm.RATINGS))

    fun replyToRatingAsStaff(userId: String, orderId: String, reply: String): ShopReviewDto =
        replyToRating(requireShopPermission(userId, ShopPerm.RATINGS), orderId, reply)

    /** Records a customer opening the shop catalog; idempotent per visitor per day. */
    fun recordShopVisit(shopId: String, visitorId: String, now: Long = System.currentTimeMillis()) {
        val day = now / DAY_MS
        runCatching {
            shopVisitCol.replaceOne(
                eq("_id", "$shopId:$visitorId:$day"),
                ShopVisitDoc("$shopId:$visitorId:$day", shopId, visitorId, day, now),
                ReplaceOptions().upsert(true),
            )
        }
    }

    fun visitorStats(userId: String, fromEpochMs: Long?, toEpochMs: Long?): VisitorStatsDto {
        val shopId = requireShopPermission(userId, ShopPerm.VISITORS)
        val filters = buildList {
            add(eq("shopId", shopId))
            if (fromEpochMs != null) add(gte("day", fromEpochMs / DAY_MS))
            if (toEpochMs != null) add(lte("day", toEpochMs / DAY_MS))
        }
        val rows = shopVisitCol.find(and(filters))
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
        val dayFormat = java.text.SimpleDateFormat("dd MMM", java.util.Locale.ENGLISH)
        val daily = rows.groupBy { it.day }.toSortedMap().map { (day, visits) ->
            AdminChartPointDto(dayFormat.format(java.util.Date(day * DAY_MS)), visits.map { it.visitorId }.distinct().size.toDouble())
        }
        return VisitorStatsDto(
            uniqueVisitors = rows.map { it.visitorId }.distinct().size,
            visitDays = rows.size,
            daily = daily,
        )
    }

    fun listShopsAdmin(): List<ShopDto> = listAdminShops().map { it.toLegacyShopDto() }

    fun listAdminShops(): List<AdminShopDto> {
        val logins = shopLoginUsernames()
        return shopCol.find().toList().map { it.toAdminDto(logins[it.id].orEmpty()) }
            .sortedWith(compareBy({ it.id.toIntOrNull() ?: Int.MAX_VALUE }, { it.name }))
    }

    fun adminShopDetail(shopId: String): AdminShopDetailDto {
        val login = shopUserCol.find(and(eq("shopId", shopId), eq("role", "SHOP"))).firstOrNull()?.username.orEmpty()
        val shop = shopById(shopId).toAdminDto(login)
        val txns = listAdminTransactions(shopId = shopId).transactions
        val uniqueCustomers = txns.mapNotNull { it.customerId }.distinct().size
        return AdminShopDetailDto(
            shop = shop,
            transactions = txns,
            orderCount = txns.size,
            uniqueCustomers = uniqueCustomers,
        )
    }

    fun adminDashboard(fromEpochMs: Long? = null, toEpochMs: Long? = null): AdminDashboardDto {
        val filters = buildList {
            if (fromEpochMs != null) add(gte("createdAt", fromEpochMs))
            if (toEpochMs != null) add(lte("createdAt", toEpochMs))
        }
        // Only the fields needed for totals/charts — full order docs are large and Atlas is remote.
        val rows = orderIdCol.find(if (filters.isEmpty()) Document() else and(filters))
            .projection(include("createdAt", "quote.payable", "items.quantity", "items.unitPrice", "paymentId", "status"))
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
        val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val byWeekday = DoubleArray(7)
        val byMonth = DoubleArray(12)
        var paidSum = 0.0
        val cal = java.util.Calendar.getInstance()
        rows.forEach { row ->
            val createdAt = (row["createdAt"] as? Number)?.toLong() ?: 0L
            val quotePayable = ((row["quote"] as? Document)?.get("payable") as? Number)?.toDouble()
            val total = quotePayable ?: (row["items"] as? List<*>).orEmpty().sumOf { item ->
                val doc = item as? Document ?: return@sumOf 0.0
                ((doc["quantity"] as? Number)?.toDouble() ?: 0.0) * ((doc["unitPrice"] as? Number)?.toDouble() ?: 0.0)
            }
            val status = row.getString("status").orEmpty()
            if (!row.getString("paymentId").isNullOrBlank() && status !in setOf("CANCELLED", "REJECTED")) {
                paidSum += total
            }
            cal.timeInMillis = createdAt
            byWeekday[(cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7] += total
            byMonth[cal.get(java.util.Calendar.MONTH)] += total
        }
        return AdminDashboardDto(
            shopCount = shopCol.estimatedDocumentCount().toInt(),
            productCount = masterProductCol.estimatedDocumentCount().toInt(),
            transactionAmount = paidSum,
            transactionCount = rows.size,
            userCount = customerCol.estimatedDocumentCount().toInt(),
            partnerCount = partnerCol.estimatedDocumentCount().toInt(),
            salesByWeekday = weekdayLabels.mapIndexed { i, label -> AdminChartPointDto(label, byWeekday[i]) },
            txnTrendByMonth = monthLabels.mapIndexed { i, label -> AdminChartPointDto(label, byMonth[i]) },
        )
    }

    fun listAdminTransactions(
        fromEpochMs: Long? = null,
        toEpochMs: Long? = null,
        shopId: String? = null,
    ): AdminTxnSummaryDto {
        val filters = buildList {
            if (fromEpochMs != null) add(gte("createdAt", fromEpochMs))
            if (toEpochMs != null) add(lte("createdAt", toEpochMs))
            if (shopId != null) add(eq("shopId", shopId))
        }
        val rows = orderCol.find(if (filters.isEmpty()) Document() else and(filters))
            .projection(orderListProjection)
            .sort(Sorts.descending("createdAt"))
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()
        val mapped = mapOrders(rows).map { it.toAdminTxn() }
        return AdminTxnSummaryDto(
            amount = mapped.sumOf { it.paid },
            count = mapped.size,
            transactions = mapped,
        )
    }

    fun adminOrderDetail(orderId: String): AdminTxnDto =
        getOrder(orderId).toAdminTxn()

    suspend fun adminCancelOrder(orderId: String, reason: String?): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        return cancelShopOrder(row.shopId, orderId, reason)
    }

    suspend fun adminRefundOrder(orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.refundId.isNullOrBlank()) { "Already refunded" }
        require(!row.paymentId.isNullOrBlank()) { "No payment to refund" }
        if (row.status !in setOf("DELIVERED", "COMPLETED", "CANCELLED", "REJECTED")) {
            return cancelShopOrder(row.shopId, orderId, "ADMIN_REFUND")
        }
        val refundPaise = refundAmountPaise(row)
        val refundId = runCatching {
            security.refundRazorpayPayment(
                paymentId = row.paymentId.orEmpty(),
                amountPaise = refundPaise,
                notes = mapOf("order_id" to orderId, "reason" to "ADMIN_REFUND"),
            )
        }.getOrNull() ?: error("Refund failed")
        orderCol.replaceOne(eq("_id", orderId), row.copy(refundId = refundId))
        row.customerId?.let { customerId ->
            if (refundPaise > 0) {
                txnCol.insertOne(
                    TxnDoc(
                        security.randomId("txn"),
                        orderId,
                        customerId,
                        refundPaise / 100.0,
                        "Refund $orderId",
                        System.currentTimeMillis(),
                    ),
                )
            }
        }
        return getOrder(orderId)
    }

    fun listAdminUsers(fromEpochMs: Long? = null, toEpochMs: Long? = null): List<AdminUserDto> {
        val rows = customerCol.find().toList()
        return rows.map { it.toAdminUserDto() }
            .filter { u ->
                val joined = u.joinedAtEpochMs.takeIf { it > 0 } ?: 0L
                (fromEpochMs == null || joined >= fromEpochMs) &&
                    (toEpochMs == null || joined <= toEpochMs)
            }
            .sortedByDescending { it.joinedAtEpochMs }
    }

    fun patchAdminUser(userId: String, active: Boolean?) {
        if (active != null) {
            customerCol.updateOne(eq("_id", userId), set("active", active))
        }
    }

    fun adminUserDetail(userId: String): AdminUserDetailDto {
        val user = customerCol.find(eq("_id", userId)).firstOrNull()?.toAdminUserDto()
            ?: error("User not found")
        val orders = mapOrders(
            orderCol.find(eq("customerId", userId)).sort(Sorts.descending("createdAt")).toList(),
        ).map { it.toAdminTxn() }
        return AdminUserDetailDto(user, orders)
    }

    fun listAdminPartners(fromEpochMs: Long? = null, toEpochMs: Long? = null): List<AdminPartnerDto> {
        return partnerCol.find().toList()
            .map { it.toAdminPartnerDto() }
            .filter { p ->
                (fromEpochMs == null || p.joinedAtEpochMs >= fromEpochMs) &&
                    (toEpochMs == null || p.joinedAtEpochMs <= toEpochMs)
            }
            .sortedByDescending { it.joinedAtEpochMs }
    }

    fun patchAdminPartner(partnerId: String, active: Boolean?) {
        if (active != null) {
            partnerCol.updateOne(eq("_id", partnerId), set("active", active))
        }
    }

    fun adminPartnerDetail(partnerId: String): AdminPartnerDetailDto {
        val partner = partnerCol.find(eq("_id", partnerId)).firstOrNull()?.toAdminPartnerDto()
            ?: error("Partner not found")
        val all = mapOrders(
            orderCol.find(eq("partnerId", partnerId)).sort(Sorts.descending("createdAt")).toList(),
        ).map { it.toAdminTxn() }
        val cancelled = all.filter { it.status in setOf("CANCELLED", "REJECTED") }
        val accepted = all.filter { it.status !in setOf("CANCELLED", "REJECTED") }
        val delivered = all.filter { it.status in setOf("DELIVERED", "COMPLETED") }
        val earnings = partnerProfile(partnerId).totalEarnings
        return AdminPartnerDetailDto(
            partner = partner,
            acceptedOrders = accepted,
            cancelledOrders = cancelled,
            totalDeliveredOrders = delivered.size,
            totalEarnings = earnings,
        )
    }

    fun shopPaymentInfo(shopId: String): ShopPaymentInfoDto {
        val shop = shopById(shopId)
        return ShopPaymentInfoDto(shopName = shop.name, upiId = shop.upiId.trim())
    }

    private fun shopById(shopId: String): ShopDoc =
        shopCol.find(eq("_id", shopId)).firstOrNull() ?: error("Shop not found")

    private fun MasterProductDoc.toDto() = MasterProductDto(
        id = id,
        name = name,
        nameHi = nameHi,
        categoryId = categoryId,
        unit = unit,
        barcode = barcode,
        imageUrl = assets.toUrlOrNull(imageUrls.firstOrNull() ?: imageUrl),
        thumbnailUrl = assets.toUrlOrNull(imageUrls.firstOrNull() ?: thumbnailUrl),
        imageUrls = assets.toUrls(normalizeProductImageUrls(imageUrls, imageUrl, thumbnailUrl)),
        brandName = brandName,
        companyName = companyName,
        subcategoryId = subcategoryId,
        salePrice = salePrice,
        cost = cost,
        active = active,
        addedAtEpochMs = addedAt,
        description = description,
        sku = sku,
        stockQty = stockQty,
        lowStockThreshold = lowStockThreshold,
        tags = tags,
        weightKg = weightKg,
        dimensions = dimensions,
        variants = variants.map { it.toDto() },
    )

    private fun ShopDoc.toAdminDto(loginUsername: String = "") = AdminShopDto(
        loginUsername = loginUsername,
        id = id,
        name = name,
        imageUrl = assets.toUrlOrNull(imageUrl),
        rating = rating,
        ratingCount = ratingCount,
        lat = lat,
        lng = lng,
        isOpen = isOpen,
        active = active,
        paymentsEnabled = paymentsEnabled,
        discountPercent = discountPercent,
        upiId = upiId,
        address = address,
        deliveryRadiusKm = deliveryRadiusKm,
        shopType = shopType,
        joinedAtEpochMs = joinedAt,
        features = ShopFeaturesDto(
            voiceSearch = features.voiceSearch,
            barcodeSearch = features.barcodeSearch,
            reportGeneration = features.reportGeneration,
            onlineOrders = features.onlineOrders,
            inventoryAlerts = features.inventoryAlerts,
        ),
        ownerName = ownerName,
        ownerPhone = ownerPhone,
        ownerEmail = ownerEmail,
        city = city,
        state = state,
        zip = zip,
        country = country,
        registrationNumber = registrationNumber,
        taxId = taxId,
        operatingHours = operatingHours.map { ShopHoursDayDto(it.day, it.start, it.end, it.closed) },
    )

    private fun AdminShopDto.toLegacyShopDto() = ShopDto(
        id = id,
        name = name,
        imageUrl = imageUrl,
        rating = rating,
        ratingCount = ratingCount,
        distanceKm = 0.0,
        isOpen = isOpen,
        lat = lat,
        lng = lng,
        offerCount = 0,
        discountPercent = discountPercent,
        upiId = upiId,
        deliveryRadiusKm = deliveryRadiusKm,
        shopType = shopType,
        active = active,
        paymentsEnabled = paymentsEnabled,
        address = address,
    )

    private fun CustomerDoc.toAdminUserDto() = AdminUserDto(
        id = id,
        name = name.ifBlank { phone },
        phone = phone,
        address = address,
        imageUrl = assets.toUrlOrNull(imageUrl),
        active = active,
        joinedAtEpochMs = joinedAt,
    )

    private fun PartnerDoc.toAdminPartnerDto() = AdminPartnerDto(
        id = id,
        name = name,
        phone = phone,
        email = email,
        address = address,
        idImageUrl = assets.toUrl(idPhoto.ifBlank { platePhoto }),
        vehicleImageUrl = assets.toUrl(vehiclePhoto),
        profileImageUrl = assets.toUrl(profilePhoto),
        vehicleNumber = vehicleReg,
        vehicleName = vehicleName,
        vehicleBrand = vehicleBrand,
        vehicleColor = vehicleColor,
        vehicleType = vehicleType,
        active = active,
        verified = verified,
        joinedAtEpochMs = joinedAt,
    )

    private fun OrderDto.toAdminTxn(): AdminTxnDto {
        val offers = quote?.discount ?: 0.0
        val charges = (quote?.platformFee ?: 0.0) + (quote?.deliveryCharge ?: 0.0)
        val total = quote?.payable ?: items.sumOf { it.quantity * it.unitPrice }
        val paid = if (!paymentId.isNullOrBlank() && status !in setOf("CANCELLED", "REJECTED")) total else 0.0
        val summary = items.joinToString(", ") { "${it.quantity.toInt()}x ${it.productName}" }
        return AdminTxnDto(
            orderId = id,
            transactionNo = paymentId ?: razorpayTxnFallback(),
            createdAtEpochMs = createdAtEpochMs,
            status = status,
            itemsSummary = summary.ifBlank { "—" },
            customerName = customerName.orEmpty(),
            customerPhone = customerPhone.orEmpty(),
            customerAddress = deliveryAddress.orEmpty(),
            customerId = customerId,
            shopId = shopId,
            shopName = shopName.orEmpty(),
            offers = offers,
            charges = charges,
            total = total,
            paid = paid,
            paymentMethod = paymentMethod,
            refundId = refundId,
            partnerId = partnerId,
            partnerName = partnerName,
            items = items,
            deliveryDurationMin = deliveryDurationMin,
            partnerVehicleReg = partnerVehicleReg,
        )
    }

    private fun OrderDto.razorpayTxnFallback(): String = "TXN-${id.takeLast(8).uppercase()}"

    @Suppress("UNUSED_PARAMETER")
    private suspend fun broadcastOffer(offer: DeliveryOfferDoc, shop: ShopDoc, dropAddress: String) {
        val partners = partnerCol.find(and(eq("verified", true), eq("online", true)))
            .projection(partnerLiteProjection)
            .toList()
        val nearby = partners.filter { partner ->
            partner.hasGpsFix() && toOfferDto(offer, partner, shop).shopDistanceKm <= platform().partnerRingKm
        }
        var payout = offer.payout
        nearby.forEach { partner ->
            val dto = toOfferDto(offer, partner, shop).copy(dropAddress = dropAddress.ifBlank { offer.dropAddress })
            if (payout <= 0) payout = dto.payoutInr
            sockets[partner.id]?.let { session ->
                runCatching { session.send(Frame.Text(json.encodeToString(dto))) }
            }
        }
        push.deliveryOffer(
            partnerIds = nearby.map { it.id },
            offerId = offer.id,
            orderId = offer.orderId,
            shopName = shop.name,
            payoutInr = payout,
        )
    }

    private fun dummyPartner() = PartnerDoc(
        id = "none",
        phone = "",
        name = "",
        email = "",
        address = "",
        vehicleReg = "",
        platePhoto = "",
        vehiclePhoto = "",
        lat = DEFAULT_MAP_LAT,
        lng = DEFAULT_MAP_LNG,
        verified = true,
    )

    private fun toOfferDto(
        offer: DeliveryOfferDoc,
        partner: PartnerDoc,
        shop: ShopDoc = shopById(offer.shopId),
        order: OrderDoc? = orderCol.find(eq("_id", offer.orderId)).firstOrNull(),
    ): DeliveryOfferDto {
        val plat = partner.lat ?: DEFAULT_MAP_LAT
        val plng = partner.lng ?: DEFAULT_MAP_LNG
        // Always partner→shop; never reuse stored shop→customer as serving-area distance.
        val shopDist = haversine(plat, plng, shop.lat, shop.lng)
        val dropDist = when {
            offer.dropDistanceKm > 0 -> offer.dropDistanceKm
            order?.customerLat != null && order.customerLng != null ->
                haversine(shop.lat, shop.lng, order.customerLat, order.customerLng)
            else -> shopDist
        }
        val totalKm = shopDist + dropDist
        val payout = if (offer.payout > 0) offer.payout else platform().partnerPayoutPerKmInr * totalKm
        val etaMin = ((shopDist / 20.0) * 60).toInt().coerceAtLeast(3)
        return DeliveryOfferDto(
            id = offer.id,
            orderId = offer.orderId,
            shopId = offer.shopId,
            shopName = shop.name,
            shopImageUrl = assets.toUrlOrNull(shop.imageUrl),
            shopAddress = shop.address,
            shopDistanceKm = shopDist,
            dropAddress = offer.dropAddress,
            dropDistanceKm = dropDist,
            totalDistanceKm = totalKm,
            payoutInr = payout,
            expiresAtEpochMs = offer.expiresAt,
            status = offer.status,
            acceptedByPartnerId = offer.acceptedBy,
            shopLat = shop.lat,
            shopLng = shop.lng,
            shopRating = shop.rating,
            customerName = order?.customerName,
            estimatedMinutes = etaMin,
        )
    }

    private fun ProductDoc.toDto() = ProductDto(
        id, shopId, name, nameHi, category, unit, barcode, sellingPrice, costPrice, stockQty, lowStockThreshold, voiceAlias,
        imageUrls = assets.toUrls(imageUrls.filter { assets.isValidRef(it) }.take(4)),
        variants = variants.filter { it.active }.map { it.toDto() },
        brandName = brandName,
        masterProductId = masterProductId,
        active = active,
    )

    private fun ProductDto.toDoc() = ProductDoc(
        id, shopId, name, nameHi, category, unit, barcode, sellingPrice, costPrice, stockQty, lowStockThreshold, voiceAlias,
        imageUrls = assets.toKeys(imageUrls.filter { assets.isValidRef(it) }).distinct().take(4),
        variants = variants.toVariantDocs(),
        brandName = brandName.trim(),
        masterProductId = masterProductId?.takeIf { it.isNotBlank() },
        active = active,
    )

    private fun ProductVariantDoc.toDto() = ProductVariantDto(
        name = name,
        sku = sku,
        price = price,
        id = id,
        sellMode = sellMode,
        attributes = attributes,
        mrp = mrp,
        baseUnit = baseUnit,
        minQty = minQty,
        maxQty = maxQty,
        stepQty = stepQty,
        quickPicks = quickPicks,
        bundleCount = bundleCount,
        imageUrls = assets.toUrls(imageUrls.filter { assets.isValidRef(it) }),
        stockQty = stockQty,
        barcode = barcode,
        active = active,
    )

    /** Assigns stable ids to new variants and validates loose/bundle settings. */
    private fun List<ProductVariantDto>.toVariantDocs(): List<ProductVariantDoc> = mapNotNull { v ->
        val label = v.name.trim()
        if (label.isBlank()) return@mapNotNull null
        val mode = v.sellMode.uppercase().takeIf { it in VARIANT_SELL_MODES } ?: VARIANT_PACK
        val loose = mode == VARIANT_LOOSE
        ProductVariantDoc(
            name = label,
            sku = v.sku.trim(),
            price = v.price.coerceAtLeast(0.0),
            id = v.id.trim().ifBlank { "v_${UUID.randomUUID().toString().take(8)}" },
            sellMode = mode,
            attributes = v.attributes.mapKeys { it.key.trim() }.mapValues { it.value.trim() }
                .filter { it.key.isNotBlank() && it.value.isNotBlank() },
            mrp = v.mrp.coerceAtLeast(0.0),
            baseUnit = if (loose) v.baseUnit.ifBlank { "KG" } else v.baseUnit,
            minQty = if (loose) v.minQty.takeIf { it > 0 } ?: DEFAULT_LOOSE_STEP else 0.0,
            maxQty = if (loose) v.maxQty.coerceAtLeast(0.0) else 0.0,
            stepQty = if (loose) v.stepQty.takeIf { it > 0 } ?: DEFAULT_LOOSE_STEP else 0.0,
            quickPicks = if (loose) v.quickPicks.filter { it > 0 }.distinct().sorted() else emptyList(),
            bundleCount = if (mode == VARIANT_BUNDLE) v.bundleCount.coerceAtLeast(2) else 0,
            imageUrls = assets.toKeys(v.imageUrls.filter { assets.isValidRef(it) }).distinct().take(4),
            stockQty = v.stockQty.coerceAtLeast(0.0),
            barcode = v.barcode?.trim()?.ifBlank { null },
            active = v.active,
        )
    }

    /** Keys for storage; call sites resolve to URLs when building DTOs. */
    private fun normalizeProductImageUrls(
        imageUrls: List<String>,
        imageUrl: String?,
        thumbnailUrl: String?,
    ): List<String> {
        val merged = (imageUrls + listOfNotNull(imageUrl, thumbnailUrl))
            .filter { assets.isValidRef(it) }
            .map { assets.toKey(it) }
            .distinct()
        return merged.take(4)
    }

    private fun refundAmountPaise(row: OrderDoc): Long {
        val quote = row.quote
        val items = if (quote != null) {
            (quote.itemsSubtotal - quote.discount).coerceAtLeast(0.0)
        } else {
            row.items.sumOf { it.quantity * it.unitPrice }
        }
        val delivery = quote?.deliveryCharge ?: 0.0
        return ((items + delivery) * 100.0).toLong().coerceAtLeast(0L)
    }

    private fun mapOrders(rows: List<OrderDoc>): List<OrderDto> {
        if (rows.isEmpty()) return emptyList()
        val shops = shopCol.find(`in`("_id", rows.map { it.shopId }.distinct())).toList().associateBy { it.id }
        val partnerIds = rows.mapNotNull { it.partnerId }.distinct()
        val partners = if (partnerIds.isEmpty()) {
            emptyMap()
        } else {
            partnerCol.find(`in`("_id", partnerIds)).projection(partnerLiteProjection).toList().associateBy { it.id }
        }
        return rows.map { row -> row.toDto(shops[row.shopId], row.partnerId?.let(partners::get)) }
    }

    private fun partnerLite(id: String): PartnerDoc? =
        partnerCol.find(eq("_id", id)).projection(partnerLiteProjection).firstOrNull()

    private fun OrderDoc.toDto(
        shop: ShopDoc? = null,
        partner: PartnerDoc? = null,
    ): OrderDto {
        val shopRow = shop ?: shopCol.find(eq("_id", shopId)).firstOrNull()
        val partnerRow = partner ?: partnerId?.let { partnerLite(it) }
        return OrderDto(
            id = id,
            shopId = shopId,
            shopName = shopRow?.name,
            createdAtEpochMs = createdAt,
            status = status,
            customerName = customerName,
            customerId = customerId,
            channel = channel,
            deliveryAddress = deliveryAddress,
            dropoffInstructions = dropoffInstructions,
            deliveryOtp = deliveryOtp,
            pickupPhotoUrls = assets.toUrls(pickupPhotos),
            partnerId = partnerId,
            partnerName = partnerRow?.name ?: ownPartnerName,
            partnerPhone = partnerRow?.phone ?: ownPartnerPhone,
            partnerVehicleReg = partnerRow?.vehicleReg ?: ownPartnerVehicle,
            paymentId = paymentId,
            paymentMethod = paymentMethod,
            customerPhone = customerPhone,
            shopAddress = shopRow?.address?.takeIf { it.isNotBlank() } ?: shopRow?.name,
            shopLat = shopRow?.lat,
            shopLng = shopRow?.lng,
            customerLat = customerLat,
            customerLng = customerLng,
            deliveryMode = deliveryMode,
            totalDistanceKm = totalDistanceKm,
            deliveryDurationMin = deliveryDurationMin,
            partnerPayoutInr = partnerPayoutInr,
            partnerProgress = partnerProgress,
            items = items,
            quote = quote,
            ratingStars = ratingStars,
            ratingComment = ratingComment,
            cancelReason = cancelReason,
            refundId = refundId,
            packedProductIds = packedProductIds,
            discount = discount,
        )
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earth = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * earth * asin(min(1.0, sqrt(a)))
    }

    private fun startOfToday(): Long {
        val now = java.time.LocalDate.now()
        return now.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private data class DropPoint(val lat: Double, val lng: Double, val formatted: String)

    private fun resolveDrop(customer: CustomerDto, request: PlaceOrderRequest): DropPoint {
        val chosen = request.addressId?.let { id -> customer.addresses.firstOrNull { it.id == id } }
            ?: customer.addresses.firstOrNull { it.isDefault }
            ?: customer.addresses.firstOrNull()
        val lat = chosen?.lat ?: request.userLat ?: customer.lat ?: DEFAULT_MAP_LAT
        val lng = chosen?.lng ?: request.userLng ?: customer.lng ?: DEFAULT_MAP_LNG
        val formatted = listOfNotNull(
            chosen?.line ?: customer.address.takeIf { it.isNotBlank() },
            (chosen?.locality ?: customer.locality).takeIf { it.isNotBlank() },
        ).joinToString(", ").ifBlank { customer.address }
        return DropPoint(lat, lng, formatted)
    }

    private fun upsertDefaultAddress(
        existing: List<CustomerAddressDoc>,
        line: String,
        locality: String,
        lat: Double?,
        lng: Double?,
    ): List<CustomerAddressDoc> {
        if (line.isBlank() || lat == null || lng == null) return existing
        val currentDefault = existing.firstOrNull { it.isDefault }
        val updated = if (currentDefault != null) {
            existing.map {
                if (it.id == currentDefault.id) it.copy(line = line, locality = locality, lat = lat, lng = lng)
                else it
            }
        } else {
            existing.map { it.copy(isDefault = false) } + CustomerAddressDoc(
                id = security.randomId("addr"),
                line = line,
                locality = locality,
                lat = lat,
                lng = lng,
                isDefault = true,
            )
        }
        return updated
    }

    private fun CustomerDoc.toCustomerDto() = CustomerDto(
        id = id,
        phone = phone,
        name = name,
        address = address,
        locality = locality,
        lat = lat,
        lng = lng,
        addresses = addresses.map {
            SavedAddressDto(it.id, it.line, it.locality, it.lat, it.lng, it.isDefault)
        },
    )
}
