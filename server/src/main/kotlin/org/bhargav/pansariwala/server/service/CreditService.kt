package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.lte
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import org.bhargav.pansariwala.server.db.BorrowerDoc
import org.bhargav.pansariwala.server.db.CreditTxnDoc
import org.bhargav.pansariwala.server.db.CreditTxnItemDoc
import org.bhargav.pansariwala.server.db.CustomerDoc
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.db.ShopDoc
import org.bhargav.pansariwala.server.db.ShopUserDoc
import org.bhargav.pansariwala.server.dto.BorrowerDetailDto
import org.bhargav.pansariwala.server.dto.BorrowerDto
import org.bhargav.pansariwala.server.dto.BorrowerReportDto
import org.bhargav.pansariwala.server.dto.BorrowerUpdateRequest
import org.bhargav.pansariwala.server.dto.CreditDayDto
import org.bhargav.pansariwala.server.dto.CreditShareDto
import org.bhargav.pansariwala.server.dto.CreditTxnDto
import org.bhargav.pansariwala.server.dto.CreditTxnItemDto
import org.bhargav.pansariwala.server.dto.MyCreditShopDetailDto
import org.bhargav.pansariwala.server.dto.MyCreditShopDto
import org.bhargav.pansariwala.server.dto.MyCreditSummaryDto
import org.bhargav.pansariwala.server.dto.MyRepayResponse
import org.bhargav.pansariwala.server.dto.OrderDto
import org.bhargav.pansariwala.server.dto.RepaymentRequest
import org.bhargav.pansariwala.server.payment.CashfreeGateway
import org.bhargav.pansariwala.server.payment.PaymentMethod
import org.bhargav.pansariwala.server.payment.PaymentMode
import org.bhargav.pansariwala.server.storage.AssetRefs
import java.util.UUID
import kotlin.math.floor
import kotlin.math.round

private const val PAYMENT_UDHAAR = "UDHAAR"
private const val TYPE_CREDIT = "CREDIT"
private const val TYPE_REPAYMENT = "REPAYMENT"
private const val STATUS_SUCCESS = "SUCCESS"
private const val STATUS_PENDING = "PENDING"
private const val STATUS_FAILED = "FAILED"
private const val SOURCE_POS = "POS"
private const val SOURCE_USER_APP = "USER_APP"
private const val ORDER_CANCELLED = "CANCELLED"
private const val PHONE_DIGITS = 10
/** POS placeholder name for bills without a customer (shared `WALK_IN_CUSTOMER_KEY`). */
private const val WALK_IN_NAME = "Walk-in"
private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val DEFAULT_RANGE_DAYS = 30
private const val MAX_RANGE_DAYS = 366
private const val MAX_PAGE_SIZE = 100
private const val DEFAULT_TXN_PAGE_SIZE = 20
private const val SHARE_SLICES = 6
private const val MONEY_EPSILON = 0.005
private val REPAYMENT_MODES = setOf("CASH", "UPI", "CARD")
/** Gateway order ids of app repayments start with this (payment intents use `PTX`). */
const val REPAY_GATEWAY_PREFIX = "RPY"
private const val REPAY_FAIL_CANCELLED = "Payment cancelled or failed in the UPI app"
private const val REPAY_FAIL_EXPIRED = "Payment not received in time"

/** Platform checkout setup that app repayments follow; supplied by `AppStore`. */
class RepayGateway(
    val cashfree: CashfreeGateway,
    val paymentMode: () -> String,
    val windowMs: () -> Long,
    val defaultShopUpi: String,
)

/**
 * Udhaar ledger: POS bills paid with UDHAAR become CREDIT rows, repayments (shop counter or the
 * customer's app) become REPAYMENT rows. Balance = successful credit − successful repayments.
 */
class CreditService(
    mongo: MongoApp,
    private val assets: AssetRefs,
    private val gateway: RepayGateway,
) {
    private val borrowerCol = mongo.db.getCollection<BorrowerDoc>("borrowers")
    private val txnCol = mongo.db.getCollection<CreditTxnDoc>("credit_txns")
    private val shopCol = mongo.db.getCollection<ShopDoc>("shops")
    private val shopUserCol = mongo.db.getCollection<ShopUserDoc>("shop_users")
    private val customerCol = mongo.db.getCollection<CustomerDoc>("customers")

    init {
        borrowerCol.createIndex(Indexes.ascending("shopId"))
        borrowerCol.createIndex(Indexes.ascending("phone"))
        txnCol.createIndex(Indexes.compoundIndex(Indexes.ascending("shopId"), Indexes.descending("createdAt")))
        txnCol.createIndex(Indexes.compoundIndex(Indexes.ascending("phone"), Indexes.ascending("shopId")))
    }

    /** Called after /sync/push stored POS bills; keeps the CREDIT row of every udhaar bill in step. */
    fun syncPosOrders(shopId: String, userId: String, orders: List<OrderDto>) {
        if (orders.isEmpty()) return
        val staff by lazy { staffName(userId) }
        orders.filter { it.shopId == shopId }.forEach { order ->
            val txnId = "credit:${order.id}"
            val phone = normalizePhone(order.customerPhone)
            val isCredit = order.paymentMethod == PAYMENT_UDHAAR && order.status != ORDER_CANCELLED && phone != null
            if (!isCredit) {
                txnCol.deleteOne(eq("_id", txnId))
                return@forEach
            }
            val now = System.currentTimeMillis()
            val existing = txnCol.find(eq("_id", txnId)).firstOrNull()
            val subtotal = order.items.sumOf { it.quantity * it.unitPrice }
            val amount = roundMoney((subtotal - order.discount).coerceAtLeast(0.0))
            txnCol.replaceOne(
                eq("_id", txnId),
                CreditTxnDoc(
                    id = txnId,
                    shopId = shopId,
                    phone = phone!!,
                    type = TYPE_CREDIT,
                    amount = amount,
                    status = STATUS_SUCCESS,
                    paymentMode = PAYMENT_UDHAAR,
                    processedBy = existing?.processedBy?.ifBlank { null } ?: staff,
                    source = SOURCE_POS,
                    orderId = order.id,
                    items = order.items.map {
                        CreditTxnItemDoc(it.productName, it.variantLabel, it.quantity, it.unit, roundMoney(it.quantity * it.unitPrice))
                    },
                    createdAt = existing?.createdAt ?: order.createdAtEpochMs,
                    updatedAt = now,
                ),
                ReplaceOptions().upsert(true),
            )
            ensureBorrower(shopId, phone, order.customerName)
        }
    }

    fun report(
        shopId: String,
        from: Long?,
        to: Long?,
        tzOffsetMin: Int,
        query: String,
        sort: String,
        page: Int,
        size: Int,
    ): BorrowerReportDto {
        val now = System.currentTimeMillis()
        val rangeTo = to ?: now
        val rangeFrom = (from ?: (rangeTo - DEFAULT_RANGE_DAYS * DAY_MS)).coerceAtLeast(rangeTo - MAX_RANGE_DAYS * DAY_MS)
        val txns = txnCol.find(eq("shopId", shopId)).toList()
        val borrowers = borrowerCol.find(eq("shopId", shopId)).toList().associateBy { it.phone }
        val txnPhones = txns.mapTo(HashSet()) { it.phone }
        val summaries = summarize(txns, borrowers) +
            borrowers.values.filter { it.phone !in txnPhones }.map { it.toDto(emptyList()) }

        val inRange = txns.filter { it.status == STATUS_SUCCESS && it.createdAt in rangeFrom until rangeTo }
        val offsetMs = tzOffsetMin * 60_000L
        val dayOf = { t: Long -> floor((t + offsetMs).toDouble() / DAY_MS).toLong() * DAY_MS - offsetMs }
        val byDay = inRange.groupBy { dayOf(it.createdAt) }
        val days = generateSequence(dayOf(rangeFrom)) { it + DAY_MS }
            .takeWhile { it < rangeTo }
            .take(MAX_RANGE_DAYS)
            .map { day ->
                val rows = byDay[day].orEmpty()
                CreditDayDto(
                    dayStartMs = day,
                    credit = roundMoney(rows.filter { it.type == TYPE_CREDIT }.sumOf { it.amount }),
                    repaid = roundMoney(rows.filter { it.type == TYPE_REPAYMENT }.sumOf { it.amount }),
                )
            }
            .toList()

        val q = query.trim().lowercase()
        val filtered = summaries.filter {
            q.isEmpty() || it.phone.contains(q) || it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
        val sorted = when (sort.uppercase()) {
            "NAME" -> filtered.sortedBy { it.name.ifBlank { it.phone }.lowercase() }
            "RECENT" -> filtered.sortedByDescending { it.lastActivityAt }
            "CREDIT" -> filtered.sortedByDescending { it.totalCredit }
            else -> filtered.sortedWith(compareByDescending<BorrowerDto> { it.outstanding }.thenByDescending { it.lastActivityAt })
        }
        val pageSize = size.coerceIn(1, MAX_PAGE_SIZE)
        val start = (page.coerceAtLeast(0) * pageSize).coerceAtMost(sorted.size)
        val pageItems = sorted.subList(start, (start + pageSize).coerceAtMost(sorted.size))

        val owing = summaries.filter { it.outstanding > MONEY_EPSILON }.sortedByDescending { it.outstanding }
        val shares = owing.take(SHARE_SLICES).map { CreditShareDto(it.phone, it.name, it.outstanding) }

        return BorrowerReportDto(
            totalOutstanding = roundMoney(summaries.sumOf { it.outstanding }),
            creditInRange = roundMoney(inRange.filter { it.type == TYPE_CREDIT }.sumOf { it.amount }),
            repaidInRange = roundMoney(inRange.filter { it.type == TYPE_REPAYMENT }.sumOf { it.amount }),
            borrowerCount = owing.size,
            shares = shares,
            days = days,
            borrowers = pageItems,
            totalCount = sorted.size,
            hasMore = start + pageSize < sorted.size,
        )
    }

    /** [type] = CREDIT / REPAYMENT narrows the rows; [page] (0-based) turns on paging. */
    fun borrowerDetail(
        shopId: String,
        rawPhone: String,
        type: String? = null,
        page: Int? = null,
        size: Int? = null,
    ): BorrowerDetailDto {
        val phone = requirePhone(rawPhone)
        val txns = txnCol.find(and(eq("shopId", shopId), eq("phone", phone))).sort(Sorts.descending("createdAt")).toList()
        val doc = borrowerCol.find(eq("_id", borrowerId(shopId, phone))).firstOrNull()
        val summary = summarize(txns, doc?.let { mapOf(phone to it) }.orEmpty()).firstOrNull()
            ?: doc?.toDto(emptyList()) ?: error("Borrower not found")
        val shopName = shopName(shopId)
        val rows = when (type?.uppercase()) {
            TYPE_CREDIT -> txns.filter { it.type == TYPE_CREDIT }
            TYPE_REPAYMENT -> txns.filter { it.type == TYPE_REPAYMENT }
            else -> txns
        }
        if (page == null) return BorrowerDetailDto(summary, rows.map { it.toDto(shopName) }, rows.size, hasMore = false)
        val pageSize = (size ?: DEFAULT_TXN_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val start = (page.coerceAtLeast(0) * pageSize).coerceAtMost(rows.size)
        val end = (start + pageSize).coerceAtMost(rows.size)
        return BorrowerDetailDto(
            borrower = summary,
            transactions = rows.subList(start, end).map { it.toDto(shopName) },
            totalCount = rows.size,
            hasMore = end < rows.size,
        )
    }

    fun updateBorrower(shopId: String, rawPhone: String, body: BorrowerUpdateRequest): BorrowerDto {
        val phone = requirePhone(rawPhone)
        val now = System.currentTimeMillis()
        val existing = borrowerCol.find(eq("_id", borrowerId(shopId, phone))).firstOrNull()
        val photoKey = when {
            body.photoUrl == null -> existing?.photoKey
            body.photoUrl.isBlank() -> null
            else -> assets.toKey(body.photoUrl)
        }
        val doc = BorrowerDoc(
            id = borrowerId(shopId, phone),
            shopId = shopId,
            phone = phone,
            name = body.name.trim().take(MAX_TEXT),
            photoKey = photoKey,
            address = body.address.trim().take(MAX_TEXT),
            notes = body.notes.trim().take(MAX_NOTES),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        borrowerCol.replaceOne(eq("_id", doc.id), doc, ReplaceOptions().upsert(true))
        return borrowerDetail(shopId, phone).borrower
    }

    fun repayAtShop(shopId: String, userId: String, rawPhone: String, body: RepaymentRequest): CreditTxnDto {
        val phone = requirePhone(rawPhone)
        val mode = body.paymentMode.uppercase()
        require(mode in REPAYMENT_MODES) { "Unsupported payment mode" }
        val amount = roundMoney(body.amount)
        require(amount > 0.0) { "Amount must be positive" }
        val outstanding = outstanding(shopId, phone)
        require(amount <= outstanding + MONEY_EPSILON) { "Amount is more than the due ₹$outstanding" }
        val now = System.currentTimeMillis()
        val doc = CreditTxnDoc(
            id = "repay:${UUID.randomUUID()}",
            shopId = shopId,
            phone = phone,
            type = TYPE_REPAYMENT,
            amount = amount,
            status = STATUS_SUCCESS,
            paymentMode = mode,
            receivedBy = body.receivedBy.trim().ifBlank { staffName(userId) }.take(MAX_TEXT),
            processedBy = staffName(userId),
            source = SOURCE_POS,
            note = body.note.trim().take(MAX_NOTES),
            createdAt = now,
            updatedAt = now,
        )
        txnCol.insertOne(doc)
        return doc.toDto(shopName(shopId))
    }

    fun mySummary(customerId: String): MyCreditSummaryDto {
        val phone = customerPhone(customerId) ?: return MyCreditSummaryDto()
        val txns = txnCol.find(eq("phone", phone)).toList()
        val shops = shopCol.find(`in`("_id", txns.map { it.shopId }.distinct())).toList().associateBy { it.id }
        val rows = txns.groupBy { it.shopId }.mapNotNull { (shopId, rows) ->
            val shop = shops[shopId] ?: return@mapNotNull null
            shop.toMyCreditShop(rows)
        }.sortedWith(compareByDescending<MyCreditShopDto> { it.outstanding }.thenByDescending { it.lastActivityAt })
        return MyCreditSummaryDto(roundMoney(rows.sumOf { it.outstanding }), rows)
    }

    fun myShopDetail(customerId: String, shopId: String): MyCreditShopDetailDto {
        val phone = customerPhone(customerId) ?: error("Add a mobile number to your profile")
        val shop = shopCol.find(eq("_id", shopId)).firstOrNull() ?: error("Shop not found")
        val txns = txnCol.find(and(eq("shopId", shopId), eq("phone", phone))).sort(Sorts.descending("createdAt")).toList()
        return MyCreditShopDetailDto(shop.toMyCreditShop(txns), txns.map { it.toDto(shop.name) })
    }

    /**
     * Starts a repayment on the platform's active gateway: Cashfree (confirmed by the gateway) when it is on,
     * otherwise the shop's own UPI ID. Every attempt is kept as a REPAYMENT row with its gateway and status.
     */
    suspend fun myRepay(customerId: String, shopId: String, rawAmount: Double): MyRepayResponse {
        val phone = customerPhone(customerId) ?: error("Add a mobile number to your profile")
        val shop = shopCol.find(eq("_id", shopId)).firstOrNull() ?: error("Shop not found")
        val amount = roundMoney(rawAmount)
        require(amount > 0.0) { "Amount must be positive" }
        val outstanding = outstanding(shopId, phone)
        require(outstanding > MONEY_EPSILON) { "Nothing is due at this shop" }
        require(amount <= outstanding + MONEY_EPSILON) { "Amount is more than the due ₹$outstanding" }
        val now = System.currentTimeMillis()
        val id = REPAY_GATEWAY_PREFIX + UUID.randomUUID().toString().replace("-", "").uppercase()
        val expiresAt = now + gateway.windowMs()
        val customer = customerCol.find(eq("_id", customerId)).firstOrNull()
        val gatewayOrderId = "${id}_1"
        val cashfreeUri = if (gateway.paymentMode() == PaymentMode.CASHFREE && gateway.cashfree.enabled) {
            runCatching {
                gateway.cashfree.createUpiIntent(
                    gatewayOrderId = gatewayOrderId,
                    amountInr = amount,
                    customerId = customerId,
                    customerName = customer?.name.orEmpty(),
                    customerPhone = phone,
                    note = "Pansari udhaar ${shop.name} $id",
                    expiresAtMs = expiresAt,
                )
            }.onFailure { println("Cashfree repayment intent failed for $gatewayOrderId: ${it.message}") }.getOrNull()
        } else {
            null
        }
        val directUpi = shop.upiId.trim().takeIf { it.contains('@') && it != gateway.defaultShopUpi }
        val payUri = cashfreeUri ?: directUpi?.let { upiPayUri(it, shop.name, amount, id) }
            ?: error("${shop.name} has not set up UPI payments yet.")
        val doc = CreditTxnDoc(
            id = id,
            shopId = shopId,
            phone = phone,
            type = TYPE_REPAYMENT,
            amount = amount,
            status = STATUS_PENDING,
            paymentMode = "UPI",
            receivedBy = shop.name,
            source = SOURCE_USER_APP,
            createdAt = now,
            updatedAt = now,
            gateway = if (cashfreeUri != null) PaymentMethod.CASHFREE else PaymentMethod.UPI_DIRECT,
            gatewayOrderId = gatewayOrderId.takeIf { cashfreeUri != null },
            expiresAt = expiresAt,
        )
        txnCol.insertOne(doc)
        return MyRepayResponse(doc.toDto(shop.name), directUpi.orEmpty(), shop.name, payUri, doc.gateway)
    }

    /** Cashfree webhook for a repayment order (signature already checked); status is re-read from Cashfree. */
    suspend fun onGatewayWebhook(gatewayOrderId: String) {
        val txn = txnCol.find(eq("gatewayOrderId", gatewayOrderId)).firstOrNull() ?: return
        syncCashfree(txn)
    }

    /** Sweeper: closes app repayments whose window passed (after a last gateway check). */
    suspend fun expireRepayments() {
        val now = System.currentTimeMillis()
        txnCol.find(
            and(eq("status", STATUS_PENDING), eq("source", SOURCE_USER_APP), ne("expiresAt", 0L), lte("expiresAt", now)),
        ).toList().forEach { txn ->
            runCatching {
                val fresh = syncCashfree(txn)
                if (fresh.status == STATUS_PENDING) markRepay(fresh, STATUS_FAILED, failure = REPAY_FAIL_EXPIRED)
            }.onFailure { println("Repayment sweep failed for ${txn.id}: ${it.message}") }
        }
    }

    /** Paid on Cashfree → SUCCESS (even after it was marked failed: the money did arrive). */
    private suspend fun syncCashfree(txn: CreditTxnDoc): CreditTxnDoc {
        val orderId = txn.gatewayOrderId ?: return txn
        if (txn.status == STATUS_SUCCESS) return txn
        if (gateway.cashfree.orderStatus(orderId) != CashfreeGateway.ORDER_PAID) return txn
        return markRepay(txn, STATUS_SUCCESS, ref = "cf_$orderId")
    }

    private fun markRepay(txn: CreditTxnDoc, status: String, ref: String? = null, failure: String = ""): CreditTxnDoc {
        val now = System.currentTimeMillis()
        txnCol.updateOne(
            and(eq("_id", txn.id), ne("status", STATUS_SUCCESS)),
            combine(
                set("status", status),
                set("updatedAt", now),
                set("paymentRef", ref ?: txn.paymentRef),
                set("failureReason", if (status == STATUS_SUCCESS) "" else failure),
            ),
        )
        return txnCol.find(eq("_id", txn.id)).firstOrNull() ?: txn
    }

    private fun upiPayUri(payee: String, payeeName: String, amountInr: Double, ref: String): String {
        fun enc(v: String) = java.net.URLEncoder.encode(v, Charsets.UTF_8).replace("+", "%20")
        val amount = String.format(java.util.Locale.US, "%.2f", amountInr)
        return "upi://pay?pa=${enc(payee)}&pn=${enc(payeeName.take(40))}&am=$amount&cu=INR&tr=${enc(ref)}&tn=${enc("Pansari udhaar $ref")}"
    }

    /** App's report after the UPI app returns; Cashfree repayments trust only the gateway for success. */
    suspend fun myRepayStatus(customerId: String, shopId: String, txnId: String, success: Boolean): CreditTxnDto {
        val phone = customerPhone(customerId) ?: error("Add a mobile number to your profile")
        val txn = txnCol.find(and(eq("_id", txnId), eq("shopId", shopId), eq("phone", phone))).firstOrNull()
            ?: error("Payment not found")
        require(txn.type == TYPE_REPAYMENT && txn.source == SOURCE_USER_APP) { "Not an app repayment" }
        if (txn.status != STATUS_PENDING) return txn.toDto(shopName(shopId))
        val updated = when {
            txn.gatewayOrderId != null -> syncCashfree(txn).let { fresh ->
                if (fresh.status == STATUS_PENDING && !success) markRepay(fresh, STATUS_FAILED, failure = REPAY_FAIL_CANCELLED) else fresh
            }
            success -> markRepay(txn, STATUS_SUCCESS)
            else -> markRepay(txn, STATUS_FAILED, failure = REPAY_FAIL_CANCELLED)
        }
        return updated.toDto(shopName(shopId))
    }

    private fun outstanding(shopId: String, phone: String): Double {
        val txns = txnCol.find(and(eq("shopId", shopId), eq("phone", phone), eq("status", STATUS_SUCCESS))).toList()
        return roundMoney(txns.signedSum())
    }

    private fun summarize(txns: List<CreditTxnDoc>, borrowers: Map<String, BorrowerDoc>): List<BorrowerDto> =
        txns.groupBy { it.phone }.map { (phone, rows) ->
            borrowers[phone]?.toDto(rows) ?: BorrowerDoc(
                id = "",
                shopId = rows.first().shopId,
                phone = phone,
                createdAt = rows.minOf { it.createdAt },
                updatedAt = rows.maxOf { it.updatedAt },
            ).toDto(rows)
        }

    private fun BorrowerDoc.toDto(rows: List<CreditTxnDoc>): BorrowerDto {
        val ok = rows.filter { it.status == STATUS_SUCCESS }
        val credit = ok.filter { it.type == TYPE_CREDIT }
        val repaid = ok.filter { it.type == TYPE_REPAYMENT }
        return BorrowerDto(
            phone = phone,
            name = name,
            photoUrl = assets.toUrlOrNull(photoKey),
            address = address,
            notes = notes,
            outstanding = roundMoney(ok.signedSum()),
            totalCredit = roundMoney(credit.sumOf { it.amount }),
            totalRepaid = roundMoney(repaid.sumOf { it.amount }),
            orderCount = credit.size,
            lastActivityAt = rows.maxOfOrNull { it.createdAt } ?: updatedAt,
            createdAt = createdAt,
        )
    }

    private fun ShopDoc.toMyCreditShop(rows: List<CreditTxnDoc>): MyCreditShopDto {
        val ok = rows.filter { it.status == STATUS_SUCCESS }
        return MyCreditShopDto(
            shopId = id,
            shopName = name,
            shopImageUrl = assets.toUrlOrNull(imageUrl),
            shopAddress = address,
            outstanding = roundMoney(ok.signedSum()),
            totalCredit = roundMoney(ok.filter { it.type == TYPE_CREDIT }.sumOf { it.amount }),
            totalRepaid = roundMoney(ok.filter { it.type == TYPE_REPAYMENT }.sumOf { it.amount }),
            lastActivityAt = rows.maxOfOrNull { it.createdAt } ?: 0L,
        )
    }

    private fun CreditTxnDoc.toDto(shopName: String) = CreditTxnDto(
        id = id,
        shopId = shopId,
        shopName = shopName,
        phone = phone,
        type = type,
        amount = amount,
        status = status,
        paymentMode = paymentMode,
        receivedBy = receivedBy,
        processedBy = processedBy,
        source = source,
        orderId = orderId,
        items = items.map { CreditTxnItemDto(it.name, it.variantLabel, it.quantity, it.unit, it.lineTotal) },
        note = note,
        createdAt = createdAt,
        gateway = gateway,
        paymentRef = paymentRef,
        failureReason = failureReason,
    )

    private fun List<CreditTxnDoc>.signedSum(): Double =
        sumOf { if (it.type == TYPE_CREDIT) it.amount else -it.amount }

    private fun ensureBorrower(shopId: String, phone: String, customerName: String?) {
        val id = borrowerId(shopId, phone)
        val existing = borrowerCol.find(eq("_id", id)).firstOrNull()
        val name = customerName?.trim().orEmpty().takeUnless { it.isBlank() || it.equals(WALK_IN_NAME, ignoreCase = true) }.orEmpty()
        val now = System.currentTimeMillis()
        when {
            existing == null -> borrowerCol.insertOne(
                BorrowerDoc(id = id, shopId = shopId, phone = phone, name = name, createdAt = now, updatedAt = now),
            )
            existing.name.isBlank() && name.isNotBlank() ->
                borrowerCol.replaceOne(eq("_id", id), existing.copy(name = name, updatedAt = now))
        }
    }

    private fun staffName(userId: String): String =
        shopUserCol.find(eq("_id", userId)).firstOrNull()?.let { it.displayName.ifBlank { it.username } }.orEmpty()

    private fun shopName(shopId: String): String = shopCol.find(eq("_id", shopId)).firstOrNull()?.name.orEmpty()

    private fun customerPhone(customerId: String): String? =
        normalizePhone(customerCol.find(eq("_id", customerId)).firstOrNull()?.phone)

    private fun borrowerId(shopId: String, phone: String) = "$shopId:$phone"

    private fun requirePhone(raw: String): String = normalizePhone(raw) ?: error("Invalid mobile number")

    private fun normalizePhone(raw: String?): String? =
        raw?.filter(Char::isDigit)?.takeLast(PHONE_DIGITS)?.takeIf { it.length == PHONE_DIGITS }

    private fun roundMoney(value: Double): Double = round(value * 100) / 100

    private companion object {
        const val MAX_TEXT = 120
        const val MAX_NOTES = 500
    }
}
