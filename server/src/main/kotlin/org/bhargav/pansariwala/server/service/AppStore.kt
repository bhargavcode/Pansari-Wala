package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.db.AdminUserDoc
import org.bhargav.pansariwala.server.db.CategoryDoc
import org.bhargav.pansariwala.server.db.CustomerDoc
import org.bhargav.pansariwala.server.db.DeliveryOfferDoc
import org.bhargav.pansariwala.server.db.MasterProductDoc
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.db.OfferDoc
import org.bhargav.pansariwala.server.db.OrderDoc
import org.bhargav.pansariwala.server.db.OtpDoc
import org.bhargav.pansariwala.server.db.PartnerDoc
import org.bhargav.pansariwala.server.db.ProductDoc
import org.bhargav.pansariwala.server.db.ShopDoc
import org.bhargav.pansariwala.server.db.ShopUserDoc
import org.bhargav.pansariwala.server.db.TxnDoc
import org.bhargav.pansariwala.server.dto.CreateRazorpayRequest
import org.bhargav.pansariwala.server.dto.CustomerDto
import org.bhargav.pansariwala.server.dto.DeliveryOfferDto
import org.bhargav.pansariwala.server.dto.MasterCategoryDto
import org.bhargav.pansariwala.server.dto.MasterProductDto
import org.bhargav.pansariwala.server.dto.OfferDto
import org.bhargav.pansariwala.server.dto.OrderDto
import org.bhargav.pansariwala.server.dto.OrderItemDto
import org.bhargav.pansariwala.server.dto.PartnerDashboardDto
import org.bhargav.pansariwala.server.dto.PartnerRegisterRequest
import org.bhargav.pansariwala.server.dto.PlaceOrderRequest
import org.bhargav.pansariwala.server.dto.ProductDto
import org.bhargav.pansariwala.server.dto.QuoteDto
import org.bhargav.pansariwala.server.dto.QuoteRequest
import org.bhargav.pansariwala.server.dto.RazorpayOrderDto
import org.bhargav.pansariwala.server.dto.ShopDto
import org.bhargav.pansariwala.server.dto.SyncPullResponse
import org.bhargav.pansariwala.server.dto.SyncPushRequest
import org.bhargav.pansariwala.server.dto.TokenResponse
import org.bhargav.pansariwala.server.dto.TxnDto
import org.bhargav.pansariwala.server.security.Security
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val PARTNER_RING_KM = 8.0
private const val OFFER_TTL_MS = 15 * 60_000L
private const val DEFAULT_MAP_LAT = 28.6139
private const val DEFAULT_MAP_LNG = 77.2090

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
    private val productCol = mongo.db.getCollection<ProductDoc>("products")
    private val offerCol = mongo.db.getCollection<OfferDoc>("offers")
    private val orderCol = mongo.db.getCollection<OrderDoc>("orders")
    private val txnCol = mongo.db.getCollection<TxnDoc>("transactions")
    private val deliveryOfferCol = mongo.db.getCollection<DeliveryOfferDoc>("delivery_offers")
    private val otpCol = mongo.db.getCollection<OtpDoc>("otp_challenges")
    private val adminUserCol = mongo.db.getCollection<AdminUserDoc>("admin_users")

    fun registerSocket(partnerId: String, session: WebSocketSession) {
        sockets[partnerId] = session
    }

    fun unregisterSocket(partnerId: String, session: WebSocketSession) {
        sockets.remove(partnerId, session)
    }

    fun shopLogin(username: String, password: String): TokenResponse {
        val row = shopUserCol.find(eq("username", username)).firstOrNull() ?: error("Invalid credentials")
        if (row.passwordHash != security.hashPassword(password)) error("Invalid credentials")
        val token = security.issueJwt(row.id, "SHOP", row.shopId, row.displayName)
        return TokenResponse(token, null, "SHOP", row.id, row.shopId, row.displayName)
    }

    fun adminLogin(username: String, password: String): TokenResponse {
        val row = adminUserCol.find(eq("username", username)).firstOrNull() ?: error("Invalid credentials")
        if (row.passwordHash != security.hashPassword(password)) error("Invalid credentials")
        val token = security.issueJwt(row.id, "ADMIN", displayName = "Admin")
        return TokenResponse(token, null, "ADMIN", row.id, null, "Admin")
    }

    fun loginFirebase(idToken: String): TokenResponse {
        val phone = security.verifyFirebaseOrDev(idToken)
        val partner = partnerCol.find().toList().firstOrNull { Security.normalizePhone(it.phone) == phone }
        if (partner != null) {
            partnerCol.updateOne(eq("_id", partner.id), set("verified", true))
            val token = security.issueJwt(partner.id, "PARTNER", displayName = partner.name)
            return TokenResponse(token, null, "PARTNER", partner.id, null, partner.name, true)
        }
        return upsertCustomerToken(phone)
    }

    fun requestOtp(phone: String): String {
        val normalized = Security.normalizePhone(phone)
        val sessionId = security.randomId("otp")
        val code = security.randomOtp()
        otpCol.deleteMany(eq("phone", normalized))
        otpCol.insertOne(
            OtpDoc(sessionId, normalized, security.sha256(code), "phone", System.currentTimeMillis() + 5 * 60_000),
        )
        return sessionId
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
        val partner = partnerCol.find().toList().firstOrNull { Security.normalizePhone(it.phone) == normalized }
        if (partner != null) {
            partnerCol.updateOne(eq("_id", partner.id), set("verified", true))
            val token = security.issueJwt(partner.id, "PARTNER", displayName = partner.name)
            return TokenResponse(token, null, "PARTNER", partner.id, null, partner.name, true)
        }
        return upsertCustomerToken(normalized)
    }

    private fun upsertCustomerToken(phone: String): TokenResponse {
        val existing = customerCol.find(eq("phone", phone)).firstOrNull()
        val id = existing?.id ?: security.randomId("cust")
        if (existing == null) {
            customerCol.insertOne(CustomerDoc(id, phone, "", ""))
        }
        val name = existing?.name.orEmpty()
        val complete = name.isNotBlank() && existing?.address?.isNotBlank() == true
        val token = security.issueJwt(id, "CUSTOMER", displayName = name.ifBlank { phone })
        return TokenResponse(token, null, "CUSTOMER", id, null, name.ifBlank { phone }, complete)
    }

    fun me(userId: String): CustomerDto {
        val row = customerCol.find(eq("_id", userId)).firstOrNull() ?: error("Customer not found")
        return CustomerDto(row.id, row.phone, row.name, row.address, row.lat, row.lng)
    }

    fun updateProfile(userId: String, name: String, address: String, lat: Double?, lng: Double?): CustomerDto {
        val current = customerCol.find(eq("_id", userId)).firstOrNull() ?: error("Customer not found")
        customerCol.replaceOne(eq("_id", userId), current.copy(name = name, address = address, lat = lat, lng = lng))
        return me(userId)
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
            )
        }.filter { it.distanceKm <= radiusKm }
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
        val customer = me(userId)
        require(customer.name.isNotBlank() && customer.address.isNotBlank()) { "Complete your profile" }
        val lat = customer.lat ?: 28.6139
        val lng = customer.lng ?: 77.2090
        val shop = shopById(request.shopId)
        val shopUpi = shop.upiId.ifBlank { config.defaultShopUpi }
        val q = quote(userId, QuoteRequest(request.shopId, request.items, lat, lng))
        val catalog = catalog(request.shopId).associateBy { it.id }
        val items = request.items.map { line ->
            val product = catalog[line.productId] ?: error("Unknown product")
            require(product.stockQty >= line.quantity) { "${product.name} is out of stock" }
            OrderItemDto(product.id, product.name, product.unit, line.quantity, product.sellingPrice)
        }
        val id = security.randomId("ord")
        val otp = security.deliveryOtp()
        val now = System.currentTimeMillis()
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
                deliveryAddress = customer.address,
                deliveryOtp = otp,
                paymentId = request.razorpayPaymentId ?: "pay_dev",
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
        orderCol.find(eq("customerId", userId)).toList().sortedByDescending { it.createdAt }.map { it.toDto() }

    fun transactions(userId: String): List<TxnDto> =
        txnCol.find(eq("customerId", userId)).toList().sortedByDescending { it.createdAt }
            .map { TxnDto(it.id, it.orderId, it.amount, it.title, it.createdAt) }

    fun rateOrder(userId: String, orderId: String, stars: Int, comment: String?): OrderDto {
        require(stars in 1..5) { "Stars must be 1-5" }
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.customerId == userId) { "Forbidden" }
        require(row.status in listOf("DELIVERED", "COMPLETED")) { "Rate after delivery" }
        orderCol.replaceOne(eq("_id", orderId), row.copy(ratingStars = stars, ratingComment = comment))
        return getOrder(orderId)
    }

    fun shopOrders(shopId: String): List<OrderDto> =
        orderCol.find(and(eq("shopId", shopId), eq("channel", "ONLINE"))).toList()
            .sortedByDescending { it.createdAt }
            .map { it.toDto() }

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
        val offer = DeliveryOfferDoc(
            id = offerId,
            orderId = orderId,
            shopId = shopId,
            status = "RINGING",
            payout = 0.0,
            shopDistanceKm = 0.0,
            dropDistanceKm = 0.0,
            expiresAt = expires,
            dropAddress = order.deliveryAddress.orEmpty(),
        )
        orderCol.updateOne(eq("_id", orderId), set("status", "LOOKING_FOR_PARTNER"))
        deliveryOfferCol.insertOne(offer)
        broadcastOffer(offer, shop, order.deliveryAddress.orEmpty())
        return toOfferDto(offer, dummyPartner())
    }

    fun incomingForPartner(partnerId: String): DeliveryOfferDto? {
        val partner = partnerCol.find(eq("_id", partnerId)).firstOrNull() ?: return null
        if (!partner.verified) return null
        val now = System.currentTimeMillis()
        return deliveryOfferCol.find(eq("status", "RINGING")).toList()
            .filter { it.expiresAt >= now }
            .filter { partnerId !in it.rejectedBy }
            .map { offer -> toOfferDto(offer, partner) }
            .let { offers ->
                val nearby = offers.filter { it.shopDistanceKm <= PARTNER_RING_KM }
                (nearby.ifEmpty { offers }).minByOrNull { it.shopDistanceKm }
            }
    }

    fun offerById(offerId: String, partnerId: String): DeliveryOfferDto {
        val now = System.currentTimeMillis()
        val offer = deliveryOfferCol.find(eq("_id", offerId)).firstOrNull() ?: error("Offer not found")
        val partner = partnerCol.find(eq("_id", partnerId)).firstOrNull() ?: error("Partner not found")
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
            val now = System.currentTimeMillis()
            if (offer.status != "RINGING" || offer.expiresAt < now) {
                error("ALREADY_TAKEN")
            }
            deliveryOfferCol.updateOne(
                eq("_id", offerId),
                combine(set("status", "ACCEPTED"), set("acceptedBy", partnerId)),
            )
            orderCol.updateOne(
                eq("_id", offer.orderId),
                combine(set("status", "PARTNER_ACCEPTED"), set("partnerId", partnerId)),
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

    fun registerPartner(request: PartnerRegisterRequest): String {
        val reg = Security.normalizeReg(request.vehicleReg)
        require(Security.vehicleRegRegex.matches(reg)) { "Invalid vehicle registration" }
        require(request.platePhotoBase64.length > 64 && request.vehiclePhotoBase64.length > 64) { "Vehicle photos required" }
        require(request.name.isNotBlank() && request.email.contains("@") && request.address.isNotBlank()) { "Incomplete profile" }
        val phone = Security.normalizePhone(request.phone)
        val id = security.randomId("ptr")
        partnerCol.insertOne(
            PartnerDoc(
                id = id,
                phone = phone,
                name = request.name,
                email = request.email,
                address = request.address,
                vehicleReg = reg,
                platePhoto = request.platePhotoBase64.take(400_000),
                vehiclePhoto = request.vehiclePhotoBase64.take(400_000),
                lat = request.lat,
                lng = request.lng,
                verified = false,
            ),
        )
        return requestOtp(phone)
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
        return orderCol.find(and(eq("partnerId", partnerId), `in`("status", statuses))).toList()
            .filter { row ->
                !delivered || ((from == null || row.createdAt >= from) && (to == null || row.createdAt <= to))
            }
            .map { it.toDto() }
    }

    suspend fun cancelPickup(partnerId: String, orderId: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        orderCol.replaceOne(eq("_id", orderId), row.copy(status = "LOOKING_FOR_PARTNER", partnerId = null))
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
            combine(set("status", "ON_THE_WAY"), set("pickupPhotos", listOf("pickup_1", "pickup_2"))),
        )
        return getOrder(orderId)
    }

    fun deliver(partnerId: String, orderId: String, otp: String): OrderDto {
        val row = orderCol.find(eq("_id", orderId)).firstOrNull() ?: error("Order not found")
        require(row.partnerId == partnerId) { "Forbidden" }
        require(row.deliveryOtp == otp) { "Invalid delivery OTP" }
        orderCol.updateOne(eq("_id", orderId), set("status", "DELIVERED"))
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
        masterProductCol.find().toList().map { MasterProductDto(it.id, it.name, it.nameHi, it.categoryId, it.unit, it.barcode) }

    fun patchShop(shopId: String, active: Boolean?, payments: Boolean?) {
        val updates = buildList {
            if (active != null) add(set("active", active))
            if (payments != null) add(set("paymentsEnabled", payments))
        }
        if (updates.isNotEmpty()) {
            shopCol.updateOne(eq("_id", shopId), combine(updates))
        }
    }

    fun listShopsAdmin(): List<ShopDto> = nearbyShops(28.6139, 77.2090, 500.0, "")

    private fun shopById(shopId: String): ShopDoc =
        shopCol.find(eq("_id", shopId)).firstOrNull() ?: error("Shop not found")

    @Suppress("UNUSED_PARAMETER")
    private suspend fun broadcastOffer(offer: DeliveryOfferDoc, shop: ShopDoc, dropAddress: String) {
        val partners = partnerCol.find(eq("verified", true)).toList()
        val nearby = partners.filter { partner ->
            toOfferDto(offer, partner).shopDistanceKm <= PARTNER_RING_KM
        }
        (nearby.ifEmpty { partners }).forEach { partner ->
            val dto = toOfferDto(offer, partner).copy(dropAddress = dropAddress.ifBlank { offer.dropAddress })
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

    private fun toOfferDto(offer: DeliveryOfferDoc, partner: PartnerDoc): DeliveryOfferDto {
        val shop = shopById(offer.shopId)
        val plat = partner.lat ?: DEFAULT_MAP_LAT
        val plng = partner.lng ?: DEFAULT_MAP_LNG
        val shopDist = haversine(plat, plng, shop.lat, shop.lng)
        return DeliveryOfferDto(
            id = offer.id,
            orderId = offer.orderId,
            shopId = offer.shopId,
            shopName = shop.name,
            shopImageUrl = shop.imageUrl,
            shopDistanceKm = shopDist,
            dropAddress = offer.dropAddress,
            dropDistanceKm = shopDist,
            payoutInr = 8.0 * shopDist,
            expiresAtEpochMs = offer.expiresAt,
            status = offer.status,
            acceptedByPartnerId = offer.acceptedBy,
            shopLat = shop.lat,
            shopLng = shop.lng,
            shopRating = shop.rating,
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

    private fun OrderDoc.toDto(): OrderDto {
        val shopName = shopCol.find(eq("_id", shopId)).firstOrNull()?.name
        val partner = partnerId?.let { partnerCol.find(eq("_id", it)).firstOrNull() }
        return OrderDto(
            id = id,
            shopId = shopId,
            shopName = shopName,
            createdAtEpochMs = createdAt,
            status = status,
            customerName = customerName,
            customerId = customerId,
            channel = channel,
            deliveryAddress = deliveryAddress,
            deliveryOtp = deliveryOtp,
            pickupPhotoUrls = pickupPhotos,
            partnerId = partnerId,
            partnerName = partner?.name,
            partnerPhone = partner?.phone,
            partnerVehicleReg = partner?.vehicleReg,
            paymentId = paymentId,
            items = items,
            quote = quote,
            ratingStars = ratingStars,
            ratingComment = ratingComment,
            cancelReason = cancelReason,
        )
    }

    private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earth = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * earth * asin(min(1.0, sqrt(a)))
    }
}
