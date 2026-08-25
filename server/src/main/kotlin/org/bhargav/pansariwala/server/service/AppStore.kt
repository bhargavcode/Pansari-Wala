package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.`in`
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
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.db.AdminUserDoc
import org.bhargav.pansariwala.server.db.CategoryDoc
import org.bhargav.pansariwala.server.db.CustomerAddressDoc
import org.bhargav.pansariwala.server.db.CustomerDoc
import org.bhargav.pansariwala.server.db.DeliveryOfferDoc
import org.bhargav.pansariwala.server.db.MasterProductDoc
import org.bhargav.pansariwala.server.db.MasterProductVariantDoc
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.db.OfferDoc
import org.bhargav.pansariwala.server.db.OrderDoc
import org.bhargav.pansariwala.server.db.OtpDoc
import org.bhargav.pansariwala.server.db.PartnerDoc
import org.bhargav.pansariwala.server.db.ProductDoc
import org.bhargav.pansariwala.server.db.ShopDoc
import org.bhargav.pansariwala.server.db.ShopFeaturesDoc
import org.bhargav.pansariwala.server.db.ShopHoursDayDoc
import org.bhargav.pansariwala.server.db.ShopTypeDoc
import org.bhargav.pansariwala.server.db.ShopUserDoc
import org.bhargav.pansariwala.server.db.TxnDoc
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
import org.bhargav.pansariwala.server.dto.MasterProductVariantDto
import org.bhargav.pansariwala.server.dto.OfferDto
import org.bhargav.pansariwala.server.dto.OrderDto
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
import org.bhargav.pansariwala.server.dto.SyncPullResponse
import org.bhargav.pansariwala.server.dto.SyncPushRequest
import org.bhargav.pansariwala.server.dto.TokenResponse
import org.bhargav.pansariwala.server.dto.TxnDto
import org.bhargav.pansariwala.server.dto.UploadResultDto
import org.bhargav.pansariwala.server.security.Security
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

private const val QUERY_MAX_MS = 8_000L
private const val PARTNER_RING_KM = 8.0
/** How long an unpicked job stays listable for partners in range (not the 15s accept UI flash). */
private const val OFFER_TTL_MS = 15 * 60_000L
private const val DELIVERY_BASE_PER_KM = 8.0
private const val DEFAULT_MAP_LAT = 28.6139
private const val DEFAULT_MAP_LNG = 77.2090

private fun PartnerDoc.hasGpsFix(): Boolean {
    val latitude = lat ?: return false
    val longitude = lng ?: return false
    return latitude != 0.0 || longitude != 0.0
}

class AppStore(
    private val config: ServerConfig,
    private val security: Security,
    mongo: MongoApp,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val sockets = ConcurrentHashMap<String, WebSocketSession>()
    private val acceptLocks = ConcurrentHashMap<String, Any>()
    private val shopCol = mongo.db.getCollection<ShopDoc>("shops")
    private val shopUserCol = mongo.db.getCollection<ShopUserDoc>("shop_users")
    private val customerCol = mongo.db.getCollection<CustomerDoc>("customers")
    private val partnerCol = mongo.db.getCollection<PartnerDoc>("partners")
    private val categoryCol = mongo.db.getCollection<CategoryDoc>("master_categories")
    private val masterProductCol = mongo.db.getCollection<MasterProductDoc>("master_products")
    private val shopTypeCol = mongo.db.getCollection<ShopTypeDoc>("master_shop_types")
    private val assetStore = AssetStore(config)
    private val productCol = mongo.db.getCollection<ProductDoc>("products")
    private val offerCol = mongo.db.getCollection<OfferDoc>("offers")
    private val orderCol = mongo.db.getCollection<OrderDoc>("orders")
    private val txnCol = mongo.db.getCollection<TxnDoc>("transactions")
    private val deliveryOfferCol = mongo.db.getCollection<DeliveryOfferDoc>("delivery_offers")
    private val otpCol = mongo.db.getCollection<OtpDoc>("otp_challenges")
    private val adminUserCol = mongo.db.getCollection<AdminUserDoc>("admin_users")
    private val partnerLiteProjection = exclude(
        "platePhoto",
        "vehiclePhoto",
        "profilePhoto",
        "dlPhoto",
        "idPhoto",
    )
    /** Home/profile: keep avatar, drop other heavy base64 blobs. */
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
        val row = shopUserCol.find(usernameFilter(username)).firstOrNull() ?: error("Invalid credentials")
        verifyAndMigratePassword(row.passwordHash, password) { hash ->
            shopUserCol.updateOne(eq("_id", row.id), set("passwordHash", hash))
        }
        val token = security.issueJwt(row.id, "SHOP", row.shopId, row.displayName)
        return TokenResponse(token, null, "SHOP", row.id, row.shopId, row.displayName)
    }

    fun adminLogin(username: String, password: String): TokenResponse {
        val row = adminUserCol.find(usernameFilter(username)).firstOrNull() ?: error("Invalid credentials")
        verifyAndMigratePassword(row.passwordHash, password) { hash ->
            adminUserCol.updateOne(eq("_id", row.id), set("passwordHash", hash))
        }
        val token = security.issueJwt(row.id, "ADMIN", displayName = "Admin")
        return TokenResponse(token, null, "ADMIN", row.id, null, "Admin")
    }

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

    fun requestOtp(phone: String): OtpSessionResponse {
        val normalized = Security.normalizePhone(phone)
        require(normalized.length == 10) { "Invalid phone" }
        val sessionId = security.randomId("otp")
        // No SMS provider → fixed code so sideloaded / iOS Personal Team builds can sign in.
        // Real SMS (SMS_API_URL) → random code (or 123456 when AUTH_DEV_MODE=true).
        val useFixedOtp = config.devAuth || !config.smsConfigured
        val code = if (useFixedOtp) "123456" else (100000 + kotlin.random.Random.nextInt(900000)).toString()
        otpCol.deleteMany(eq("phone", normalized))
        otpCol.insertOne(
            OtpDoc(sessionId, normalized, security.sha256(code), "phone", System.currentTimeMillis() + 5 * 60_000),
        )
        if (config.smsConfigured) {
            deliverSmsOtp(normalized, code)
        } else {
            println("WARN: SMS_API_URL unset — OTP for $normalized is $code (set AUTH_DEV_MODE=true or configure SMS)")
        }
        return OtpSessionResponse(sessionId = sessionId, devOtp = code.takeIf { useFixedOtp })
    }

    private fun deliverSmsOtp(phone: String, code: String) {
        val url = config.smsApiUrl
        val body = """{"phone":"$phone","otp":"$code"}"""
        val connection = java.net.URI(url).toURL().openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            if (config.smsApiToken.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer ${config.smsApiToken}")
            }
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            require(status in 200..299) { "SMS provider returned HTTP $status" }
        } finally {
            connection.disconnect()
        }
    }

    fun verifyOtp(phone: String, otp: String, sessionId: String?): TokenResponse {
        val normalized = Security.normalizePhone(phone)
        val now = System.currentTimeMillis()
        val row = otpCol.find(eq("phone", normalized)).toList().firstOrNull {
            sessionId == null || it.sessionId == sessionId
        } ?: error("OTP not found")
        if (row.expiresAt < now) error("OTP expired")
        if (row.codeHash != security.sha256(otp)) error("Invalid OTP")
        otpCol.deleteOne(eq("_id", row.sessionId))
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

    fun nearbyShops(lat: Double, lng: Double, radiusKm: Double, query: String): List<ShopDto> {
        val offerCounts = offerCol.find().toList().groupBy { it.shopId }.mapValues { it.value.size }
        return shopCol.find(eq("active", true)).toList().map { row ->
            val dist = haversine(lat, lng, row.lat, row.lng)
            ShopDto(
                id = row.id,
                name = row.name,
                imageUrl = row.imageUrl,
                rating = row.rating,
                ratingCount = row.ratingCount,
                distanceKm = dist,
                isOpen = row.isOpen,
                lat = row.lat,
                lng = row.lng,
                offerCount = offerCounts[row.id] ?: 0,
                discountPercent = row.discountPercent,
                upiId = row.upiId.ifBlank { config.defaultShopUpi },
                deliveryRadiusKm = row.deliveryRadiusKm,
                shopType = row.shopType,
            )
        }.filter { it.distanceKm <= radiusKm && it.distanceKm <= it.deliveryRadiusKm }
            .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
            .sortedBy { it.distanceKm }
    }

    fun catalog(shopId: String): List<ProductDto> =
        productCol.find(eq("shopId", shopId)).toList().map { it.toDto() }

    fun offers(shopId: String): List<OfferDto> =
        offerCol.find(eq("shopId", shopId)).toList().map { OfferDto(it.id, it.title, it.description, it.discountPercent) }

    fun quote(userId: String, request: QuoteRequest): QuoteDto {
        me(userId)
        val shop = shopById(request.shopId)
        val catalog = catalog(request.shopId).associateBy { it.id }
        val subtotal = request.items.sumOf { line ->
            val product = catalog[line.productId] ?: error("Unknown product")
            product.sellingPrice * line.quantity
        }
        val dist = haversine(request.userLat, request.userLng, shop.lat, shop.lng)
        val discount = subtotal * (shop.discountPercent / 100.0)
        val base = 8.0 * dist
        val delivery = base + base * 0.30
        val fee = 10.0
        return QuoteDto(subtotal, discount, fee, delivery, (subtotal - discount + fee + delivery).coerceAtLeast(0.0), dist)
    }

    /** Non-payment checks (profile, cart, stock, delivery radius). Call before creating a Razorpay order. */
    fun validateCheckout(userId: String, request: PlaceOrderRequest): CustomerDto {
        require(request.items.isNotEmpty()) { "Cart is empty" }
        val customer = me(userId)
        require(customer.name.isNotBlank() && customer.address.isNotBlank()) { "Complete your profile" }
        val shop = shopById(request.shopId)
        val drop = resolveDrop(customer, request)
        val dist = haversine(drop.lat, drop.lng, shop.lat, shop.lng)
        require(dist <= shop.deliveryRadiusKm) { "Out of shop delivery range" }
        val catalog = catalog(request.shopId).associateBy { it.id }
        request.items.forEach { line ->
            val product = catalog[line.productId] ?: error("Unknown product")
            require(product.stockQty >= line.quantity) { "${product.name} is out of stock" }
        }
        return customer
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
        val catalog = catalog(request.shopId).associateBy { it.id }
        val items = request.items.map { line ->
            val product = catalog[line.productId]!!
            OrderItemDto(product.id, product.name, product.unit, line.quantity, product.sellingPrice)
        }
        val id = security.newOrderId()
        val otp = security.deliveryOtp()
        val now = System.currentTimeMillis()
        val paymentMethod = if (request.razorpayPaymentId.isNullOrBlank()) "COD" else "ONLINE"
        request.items.forEach { line ->
            val left = (catalog[line.productId]!!.stockQty - line.quantity).coerceAtLeast(0.0)
            productCol.updateOne(eq("_id", line.productId), set("stockQty", left))
        }
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
                )
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

    fun shopOrders(shopId: String): List<OrderDto> =
        mapOrders(
            orderCol.find(and(eq("shopId", shopId), eq("channel", "ONLINE")))
                .sort(Sorts.descending("createdAt"))
                .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
                .toList(),
        )

    suspend fun acceptShopOrder(shopId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        require(row.status == "RECEIVED" || row.status == "ACCEPTED") { "Cannot accept" }
        orderCol.updateOne(eq("_id", orderId), set("status", "ACCEPTED"))
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
        return getOrder(orderId)
    }

    fun setStatus(shopId: String, orderId: String, status: String): OrderDto {
        val allowed = setOf("ACCEPTED", "PACKING", "LOOKING_FOR_PARTNER", "COMPLETED", "REJECTED")
        require(status in allowed) { "Invalid status" }
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.shopId == shopId) { "Forbidden" }
        if (status == "PACKING") require(row.status == "ACCEPTED" || row.status == "PACKING") { "Accept first" }
        orderCol.updateOne(eq("_id", orderId), set("status", status))
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
        return getOrder(orderId)
    }

    suspend fun requestDelivery(shopId: String, orderId: String): DeliveryOfferDto {
        val order = getOrder(orderId)
        require(order.shopId == shopId) { "Forbidden" }
        require(order.status in setOf("PACKING", "LOOKING_FOR_PARTNER")) { "Pack order first" }
        val shop = shopById(shopId)
        val now = System.currentTimeMillis()
        val existing = deliveryOfferCol.find(
            and(eq("orderId", orderId), eq("status", "RINGING")),
        ).firstOrNull()
        if (existing != null && existing.expiresAt >= now) {
            orderCol.updateOne(eq("_id", orderId), set("status", "LOOKING_FOR_PARTNER"))
            broadcastOffer(existing, shop, order.deliveryAddress.orEmpty())
            return toOfferDto(existing, dummyPartner())
        }
        val expires = now + OFFER_TTL_MS
        val offerId = security.randomId("offr")
        val customerLat = order.customerLat ?: shop.lat
        val customerLng = order.customerLng ?: shop.lng
        // Stored drop = shop→customer. Partner→shop is computed per partner in toOfferDto.
        val dropKm = haversine(shop.lat, shop.lng, customerLat, customerLng)
        val totalKm = (dropKm * 2).coerceAtLeast(0.5)
        val payout = (DELIVERY_BASE_PER_KM * totalKm).let { kotlin.math.round(it * 100) / 100.0 }
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
        broadcastOffer(offer, shop, order.deliveryAddress.orEmpty())
        return toOfferDto(offer, dummyPartner())
    }

    fun incomingForPartner(partnerId: String): DeliveryOfferDto? =
        availableOffersForPartner(partnerId).minByOrNull { it.shopDistanceKm }

    fun availableOffersForPartner(partnerId: String): List<DeliveryOfferDto> {
        val partner = partnerCol.find(eq("_id", partnerId)).projection(partnerLiteProjection).firstOrNull() ?: return emptyList()
        if (!partner.verified || !partner.online || !partner.hasGpsFix()) return emptyList()
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
            .filter { it.shopDistanceKm <= PARTNER_RING_KM }
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
            require(dto.shopDistanceKm <= PARTNER_RING_KM) { "Outside serving area" }
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
        val reg = Security.normalizeReg(request.vehicleReg)
        require(Security.vehicleRegRegex.matches(reg)) { "Invalid vehicle registration" }
        require(request.vehiclePhotoBase64.length > 64) { "Vehicle photo required" }
        require(request.name.isNotBlank() && request.email.contains("@") && request.address.isNotBlank()) { "Incomplete profile" }
        val phone = Security.normalizePhone(request.phone)
        require(partnerCol.find(eq("phone", phone)).firstOrNull() == null) { "Phone already registered as partner" }
        customerCol.deleteMany(eq("phone", phone))
        val id = security.randomId("ptr")
        val now = System.currentTimeMillis()
        partnerCol.insertOne(
            PartnerDoc(
                id = id,
                phone = phone,
                name = request.name,
                email = request.email,
                address = request.address,
                vehicleReg = reg,
                platePhoto = request.platePhotoBase64.take(400_000).ifEmpty { "" },
                vehiclePhoto = request.vehiclePhotoBase64.take(400_000),
                profilePhoto = request.profilePhotoBase64.take(400_000),
                dlPhoto = request.dlPhotoBase64.take(400_000),
                idPhoto = request.idPhotoBase64.take(400_000),
                lat = request.lat,
                lng = request.lng,
                verified = false,
                joinedAt = now,
            ),
        )
        return requestOtp(phone)
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
        val todayOrderIds = orderCol.find(todayFilter)
            .projection(include("_id"))
            .map { it.id }
            .toList()
        val todayEarnings = sumPartnerPayout(partnerId, todayOrderIds)
        val allDeliveredIds = orderCol.find(deliveredFilter)
            .projection(include("_id"))
            .map { it.id }
            .toList()
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
            profilePhoto = partner.profilePhoto,
        )
    }

    private fun sumPartnerPayout(partnerId: String, orderIds: List<String>): Double {
        if (orderIds.isEmpty()) return 0.0
        return deliveryOfferCol.find(and(eq("acceptedBy", partnerId), `in`("orderId", orderIds)))
            .projection(include("payout"))
            .toList()
            .sumOf { it.payout }
    }

    fun setPartnerOnline(partnerId: String, online: Boolean) {
        partnerCol.updateOne(eq("_id", partnerId), set("online", online))
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
        }
        return getOrder(orderId)
    }

    fun verifyBags(partnerId: String, orderId: String, photoOne: String, photoTwo: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        require(row.status == "PARTNER_ACCEPTED") { "Invalid status" }
        val updates = if (photoOne.length > 64 && photoTwo.length > 64) {
            combine(set("partnerProgress", "CAPTURE"), set("pickupPhotos", listOf(photoOne, photoTwo)))
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
                .sort(Sorts.descending("createdAt"))
                .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
                .toList(),
        )
    }

    suspend fun cancelPickup(partnerId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        orderCol.replaceOne(eq("_id", orderId), row.copy(status = "LOOKING_FOR_PARTNER", partnerId = null, partnerProgress = ""))
        deliveryOfferCol.find(eq("orderId", orderId)).toList().forEach { offer ->
            deliveryOfferCol.replaceOne(eq("_id", offer.id), offer.copy(status = "CANCELLED", acceptedBy = null))
        }
        runCatching { requestDelivery(row.shopId, orderId) }
        return getOrder(orderId)
    }

    fun submitPickup(partnerId: String, orderId: String, photoOne: String, photoTwo: String): OrderDto {
        require(photoOne.length > 64 && photoTwo.length > 64) { "Two pickup photos required" }
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        orderCol.updateOne(
            eq("_id", orderId),
            combine(
                set("status", "ON_THE_WAY"),
                set("pickupPhotos", listOf(photoOne, photoTwo)),
                set("partnerProgress", "TO_CUSTOMER"),
            ),
        )
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
        return getOrder(orderId)
    }

    fun pullSync(shopId: String): SyncPullResponse = SyncPullResponse(
        products = catalog(shopId),
        onlineOrders = shopOrders(shopId),
        masterProducts = masterProducts(),
    )

    fun pushSync(shopId: String, request: SyncPushRequest) {
        request.products.filter { it.shopId == shopId }.forEach { product ->
            productCol.replaceOne(
                eq("_id", product.id),
                product.toDoc(),
                ReplaceOptions().upsert(true),
            )
        }
    }

    fun masterCategories(): List<MasterCategoryDto> =
        categoryCol.find().toList().map { MasterCategoryDto(it.id, it.name, it.parentId) }

    fun masterProducts(): List<MasterProductDto> =
        masterProductCol.find().toList().map { it.toDto() }

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
        val doc = MasterProductDoc(
            id = id,
            name = body.name.trim(),
            nameHi = body.nameHi.trim(),
            categoryId = body.categoryId,
            unit = body.unit.ifBlank { "KG" },
            barcode = body.barcode?.takeIf { it.isNotBlank() },
            imageUrl = body.imageUrl,
            thumbnailUrl = body.thumbnailUrl,
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
            variants = body.variants.map { MasterProductVariantDoc(it.name, it.sku, it.price) },
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

    fun uploadAsset(prefix: String, fileName: String, bytes: ByteArray, contentType: String): UploadResultDto =
        assetStore.save(prefix, fileName, bytes, contentType)

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
    ): AdminShopDto {
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
                imageUrl = imageUrl,
                rating = 0.0,
                ratingCount = 0,
                lat = lat,
                lng = lng,
                isOpen = true,
                active = active,
                paymentsEnabled = true,
                discountPercent = 0.0,
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
                ownerPhone = ownerPhone.trim(),
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
        return listAdminShops().first { it.id == id }
    }

    fun patchShop(
        shopId: String,
        active: Boolean?,
        payments: Boolean?,
        features: ShopFeaturesDto? = null,
        imageUrl: String? = null,
        name: String? = null,
        address: String? = null,
        shopType: String? = null,
    ) {
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
            if (imageUrl != null) add(set("imageUrl", imageUrl))
            if (name != null) add(set("name", name.trim()))
            if (address != null) add(set("address", address.trim()))
            if (shopType != null) add(set("shopType", shopType.trim()))
        }
        if (updates.isNotEmpty()) {
            shopCol.updateOne(eq("_id", shopId), combine(updates))
        }
    }

    fun listShopsAdmin(): List<ShopDto> = listAdminShops().map { it.toLegacyShopDto() }

    fun listAdminShops(): List<AdminShopDto> =
        shopCol.find().toList().map { it.toAdminDto() }.sortedBy { it.name }

    fun adminShopDetail(shopId: String): AdminShopDetailDto {
        val shop = shopById(shopId).toAdminDto()
        val txns = listAdminTransactions(shopId = shopId).transactions
        val uniqueCustomers = orderCol.find(eq("shopId", shopId))
            .toList()
            .mapNotNull { it.customerId }
            .distinct()
            .size
        return AdminShopDetailDto(
            shop = shop,
            transactions = txns,
            orderCount = txns.size,
            uniqueCustomers = uniqueCustomers,
        )
    }

    fun adminDashboard(fromEpochMs: Long? = null, toEpochMs: Long? = null): AdminDashboardDto {
        val txnSummary = listAdminTransactions(fromEpochMs, toEpochMs)
        val charts = buildAdminCharts(fromEpochMs, toEpochMs)
        return AdminDashboardDto(
            shopCount = shopCol.countDocuments().toInt(),
            productCount = masterProductCol.countDocuments().toInt(),
            transactionAmount = txnSummary.amount,
            transactionCount = txnSummary.count,
            userCount = customerCol.countDocuments().toInt(),
            partnerCount = partnerCol.countDocuments().toInt(),
            salesByWeekday = charts.first,
            txnTrendByMonth = charts.second,
        )
    }

    private fun buildAdminCharts(
        fromEpochMs: Long?,
        toEpochMs: Long?,
    ): Pair<List<AdminChartPointDto>, List<AdminChartPointDto>> {
        val filters = buildList {
            if (fromEpochMs != null) add(gte("createdAt", fromEpochMs))
            if (toEpochMs != null) add(lte("createdAt", toEpochMs))
        }
        val rows = if (filters.isEmpty()) orderCol.find().toList()
        else orderCol.find(and(filters)).toList()
        val weekdayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val byWeekday = DoubleArray(7)
        val byMonth = DoubleArray(12)
        rows.forEach { row ->
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = row.createdAt
            val dow = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
            val month = cal.get(java.util.Calendar.MONTH)
            val paid = row.quote?.payable ?: row.items.sumOf { it.quantity * it.unitPrice }
            byWeekday[dow] += paid
            byMonth[month] += paid
        }
        return weekdayLabels.mapIndexed { i, label ->
            AdminChartPointDto(label, byWeekday[i])
        } to monthLabels.mapIndexed { i, label ->
            AdminChartPointDto(label, byMonth[i])
        }
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
        val rows = if (filters.isEmpty()) {
            orderCol.find().sort(Sorts.descending("createdAt")).toList()
        } else {
            orderCol.find(and(filters)).sort(Sorts.descending("createdAt")).toList()
        }
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

    private fun shopById(shopId: String): ShopDoc =
        shopCol.find(eq("_id", shopId)).firstOrNull() ?: error("Shop not found")

    private fun MasterProductDoc.toDto() = MasterProductDto(
        id = id,
        name = name,
        nameHi = nameHi,
        categoryId = categoryId,
        unit = unit,
        barcode = barcode,
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl,
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
        variants = variants.map { MasterProductVariantDto(it.name, it.sku, it.price) },
    )

    private fun ShopDoc.toAdminDto() = AdminShopDto(
        id = id,
        name = name,
        imageUrl = imageUrl,
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
        imageUrl = imageUrl,
        active = active,
        joinedAtEpochMs = joinedAt,
    )

    private fun PartnerDoc.toAdminPartnerDto() = AdminPartnerDto(
        id = id,
        name = name,
        phone = phone,
        email = email,
        address = address,
        idImageUrl = idPhoto.ifBlank { platePhoto },
        vehicleImageUrl = vehiclePhoto,
        profileImageUrl = profilePhoto,
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
            partner.hasGpsFix() && toOfferDto(offer, partner, shop).shopDistanceKm <= PARTNER_RING_KM
        }
        nearby.forEach { partner ->
            val dto = toOfferDto(offer, partner, shop).copy(dropAddress = dropAddress.ifBlank { offer.dropAddress })
            sockets[partner.id]?.let { session ->
                runCatching { session.send(Frame.Text(json.encodeToString(dto))) }
            }
        }
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
        val payout = if (offer.payout > 0) offer.payout else DELIVERY_BASE_PER_KM * totalKm
        val etaMin = ((shopDist / 20.0) * 60).toInt().coerceAtLeast(3)
        return DeliveryOfferDto(
            id = offer.id,
            orderId = offer.orderId,
            shopId = offer.shopId,
            shopName = shop.name,
            shopImageUrl = shop.imageUrl,
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
    )

    private fun ProductDto.toDoc() = ProductDoc(
        id, shopId, name, nameHi, category, unit, barcode, sellingPrice, costPrice, stockQty, lowStockThreshold, voiceAlias,
    )

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
            pickupPhotoUrls = pickupPhotos,
            partnerId = partnerId,
            partnerName = partnerRow?.name,
            partnerPhone = partnerRow?.phone,
            partnerVehicleReg = partnerRow?.vehicleReg,
            paymentId = paymentId,
            paymentMethod = paymentMethod,
            customerPhone = customerPhone,
            shopAddress = shopRow?.address?.takeIf { it.isNotBlank() } ?: shopRow?.name,
            shopLat = shopRow?.lat,
            shopLng = shopRow?.lng,
            customerLat = customerLat,
            customerLng = customerLng,
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
