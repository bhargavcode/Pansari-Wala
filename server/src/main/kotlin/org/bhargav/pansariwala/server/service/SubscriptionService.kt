package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.gte
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.Projections
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.roundToLong
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.db.BillingSettingsDoc
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.db.OrderDoc
import org.bhargav.pansariwala.server.db.ShopBillingDoc
import org.bhargav.pansariwala.server.db.ShopDoc
import org.bhargav.pansariwala.server.db.ShopFeaturesDoc
import org.bhargav.pansariwala.server.db.SubscriptionPlanDoc
import org.bhargav.pansariwala.server.db.SubscriptionTxnDoc
import org.bhargav.pansariwala.server.dto.AdminBillingUpdate
import org.bhargav.pansariwala.server.dto.BillingModel
import org.bhargav.pansariwala.server.dto.BillingSettingsDto
import org.bhargav.pansariwala.server.dto.CommissionDayDto
import org.bhargav.pansariwala.server.dto.CommissionDayOrdersDto
import org.bhargav.pansariwala.server.dto.CommissionOrderDto
import org.bhargav.pansariwala.server.dto.CommissionOrderItemDto
import org.bhargav.pansariwala.server.dto.CommissionSummaryDto
import org.bhargav.pansariwala.server.dto.ShopBillingDto
import org.bhargav.pansariwala.server.dto.ShopSubscriptionDto
import org.bhargav.pansariwala.server.dto.SubscriptionCheckoutDto
import org.bhargav.pansariwala.server.dto.SubscriptionCheckoutRequest
import org.bhargav.pansariwala.server.dto.SubscriptionConfirmRequest
import org.bhargav.pansariwala.server.dto.SubscriptionPlanDto
import org.bhargav.pansariwala.server.dto.SubscriptionTxnDto
import org.bhargav.pansariwala.server.dto.SubscriptionTxnType
import org.bhargav.pansariwala.server.security.Security

private val BILLING_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")
private const val DAY_MS = 24L * 60 * 60 * 1000
private const val MONTH_DAYS = 30L
private const val YEAR_DAYS = 365L
private const val MAX_LEDGER_DAYS = 366L
private const val QUERY_MAX_MS = 15_000L
private const val SETTINGS_ID = "global"
private val SALE_STATUSES = listOf("DELIVERED", "COMPLETED")

/** Features that a lapsed subscription switches off; the rest stay under master's plain toggle. */
private val PREMIUM_FEATURES = setOf("barcodeScanner", "voiceSearch", "reportGeneration", "inventoryAlerts", "onlineOrders")

class SubscriptionService(
    private val config: ServerConfig,
    private val security: Security,
    mongo: MongoApp,
) {
    private val shopCol = mongo.db.getCollection<ShopDoc>("shops")
    private val planCol = mongo.db.getCollection<SubscriptionPlanDoc>("subscription_plans")
    private val billingCol = mongo.db.getCollection<ShopBillingDoc>("shop_billing")
    private val txnCol = mongo.db.getCollection<SubscriptionTxnDoc>("subscription_txns")
    private val settingsCol = mongo.db.getCollection<BillingSettingsDoc>("billing_settings")
    private val orderCol = mongo.db.getCollection<OrderDoc>("orders")

    // ---- Settings ----

    fun settings(): BillingSettingsDto = settingsDoc().let {
        BillingSettingsDto(it.defaultCommissionPercent, it.expiryWarningDays)
    }

    fun updateSettings(body: BillingSettingsDto): BillingSettingsDto {
        require(body.defaultCommissionPercent in 0.0..50.0) { "Commission must be 0-50%" }
        require(body.expiryWarningDays in 0..60) { "Warning days must be 0-60" }
        settingsCol.replaceOne(
            eq("_id", SETTINGS_ID),
            BillingSettingsDoc(SETTINGS_ID, body.defaultCommissionPercent, body.expiryWarningDays, now()),
            ReplaceOptions().upsert(true),
        )
        return settings()
    }

    private fun settingsDoc(): BillingSettingsDoc =
        settingsCol.find(eq("_id", SETTINGS_ID)).firstOrNull() ?: BillingSettingsDoc()

    // ---- Plans ----

    fun plans(includeInactive: Boolean): List<SubscriptionPlanDto> =
        planCol.find().toList()
            .filter { includeInactive || it.active }
            .sortedWith(compareBy({ it.sortOrder }, { it.monthlyPriceInr }))
            .map { it.toDto() }

    fun savePlan(id: String?, body: SubscriptionPlanDto): SubscriptionPlanDto {
        require(body.name.isNotBlank()) { "Plan name is required" }
        require(body.monthlyPriceInr >= 0.0 && body.yearlyPriceInr >= 0.0) { "Price cannot be negative" }
        require(body.features.isNotEmpty()) { "Pick at least one feature" }
        val planId = id?.takeIf { it.isNotBlank() } ?: "plan_${UUID.randomUUID().toString().take(8)}"
        val doc = SubscriptionPlanDoc(
            id = planId,
            name = body.name.trim(),
            description = body.description.trim(),
            features = body.features.filter { it in PREMIUM_FEATURES }.distinct(),
            monthlyPriceInr = body.monthlyPriceInr,
            yearlyPriceInr = body.yearlyPriceInr,
            active = body.active,
            sortOrder = body.sortOrder,
            updatedAt = now(),
        )
        planCol.replaceOne(eq("_id", planId), doc, ReplaceOptions().upsert(true))
        return doc.toDto()
    }

    fun deletePlan(id: String) {
        require(billingCol.countDocuments(eq("planId", id)) == 0L) { "Plan is in use by shops; deactivate it instead" }
        planCol.deleteOne(eq("_id", id))
    }

    private fun plan(id: String): SubscriptionPlanDoc =
        planCol.find(eq("_id", id)).firstOrNull() ?: error("Plan not found")

    // ---- Shop billing ----

    fun shopSubscription(shopId: String): ShopSubscriptionDto {
        val settings = settingsDoc()
        return ShopSubscriptionDto(
            billing = billingDto(shopId, settings),
            plans = plans(includeInactive = false),
            transactions = txnCol.find(eq("shopId", shopId))
                .sort(Sorts.descending("createdAt"))
                .limit(200)
                .toList()
                .map { it.toDto() },
            defaultCommissionPercent = settings.defaultCommissionPercent,
            expiryWarningDays = settings.expiryWarningDays,
        )
    }

    fun adminUpdateBilling(shopId: String, adminId: String, body: AdminBillingUpdate): ShopSubscriptionDto {
        require(body.model in BillingModel.ALL) { "Unknown billing model" }
        require(shopCol.countDocuments(eq("_id", shopId)) > 0L) { "Shop not found" }
        val current = billingDoc(shopId)
        val now = now()
        val plan = body.planId?.takeIf { it.isNotBlank() }?.let(::plan)
        if (body.model == BillingModel.MONTHLY || body.model == BillingModel.YEARLY) {
            requireNotNull(plan) { "Pick a plan for monthly/yearly billing" }
        }
        val commission = body.commissionPercent ?: current.commissionPercent.takeIf { it > 0.0 }
            ?: settingsDoc().defaultCommissionPercent
        require(commission in 0.0..50.0) { "Commission must be 0-50%" }
        val next = current.copy(
            model = body.model,
            planId = plan?.id,
            commissionPercent = commission,
            startedAt = if (body.model != current.model) now else current.startedAt ?: now,
            expiresAt = when (body.model) {
                BillingModel.MONTHLY, BillingModel.YEARLY -> body.expiresAtEpochMs ?: current.expiresAt
                else -> null
            },
            freeUntil = if (body.model == BillingModel.FREE) body.freeUntilEpochMs else null,
            note = body.note.trim(),
            updatedAt = now,
        )
        billingCol.replaceOne(eq("_id", shopId), next, ReplaceOptions().upsert(true))
        if (plan != null) enableFeatures(shopId, plan.features)
        txnCol.insertOne(
            SubscriptionTxnDoc(
                id = UUID.randomUUID().toString(),
                shopId = shopId,
                type = if (body.model == BillingModel.FREE) SubscriptionTxnType.FREE_GRANT else SubscriptionTxnType.MODEL_CHANGE,
                planId = plan?.id,
                planName = plan?.name.orEmpty(),
                period = body.model,
                periodStart = next.startedAt,
                periodEnd = next.expiresAt ?: next.freeUntil,
                note = buildString {
                    append(body.model)
                    if (body.model == BillingModel.COMMISSION) append(" @ ").append(commission).append('%')
                    if (next.note.isNotBlank()) append(" · ").append(next.note)
                },
                by = adminId,
                createdAt = now,
            ),
        )
        return shopSubscription(shopId)
    }

    suspend fun checkout(shopId: String, body: SubscriptionCheckoutRequest): SubscriptionCheckoutDto {
        val plan = plan(body.planId)
        require(plan.active) { "Plan is not available" }
        val amountPaise = planPricePaise(plan, body.period)
        require(amountPaise > 0) { "Plan price is not set" }
        val orderId = security.createRazorpayOrder(
            amountPaise,
            "sub_${shopId.take(8)}_${System.currentTimeMillis()}",
            notes = mapOf("shop_id" to shopId, "plan_id" to plan.id, "period" to body.period),
        )
        return SubscriptionCheckoutDto(
            razorpayOrderId = orderId,
            amountPaise = amountPaise,
            keyId = config.razorpayKeyId.ifBlank { "rzp_test_dev" },
            planId = plan.id,
            period = body.period,
        )
    }

    fun confirm(shopId: String, userId: String, body: SubscriptionConfirmRequest): ShopSubscriptionDto {
        require(security.verifyRazorpaySignature(body.razorpayOrderId, body.razorpayPaymentId, body.razorpaySignature)) {
            "Payment signature mismatch"
        }
        if (txnCol.countDocuments(eq("paymentId", body.razorpayPaymentId)) > 0L) return shopSubscription(shopId)
        val plan = plan(body.planId)
        val amountPaise = planPricePaise(plan, body.period)
        val current = billingDoc(shopId)
        val now = now()
        val renewing = current.model == body.period && current.planId == plan.id && (current.expiresAt ?: 0L) > now
        val start = if (renewing) current.expiresAt!! else now
        val end = start + (if (body.period == BillingModel.YEARLY) YEAR_DAYS else MONTH_DAYS) * DAY_MS
        billingCol.replaceOne(
            eq("_id", shopId),
            current.copy(
                model = body.period,
                planId = plan.id,
                startedAt = if (renewing) current.startedAt ?: now else now,
                expiresAt = end,
                freeUntil = null,
                updatedAt = now,
            ),
            ReplaceOptions().upsert(true),
        )
        enableFeatures(shopId, plan.features)
        txnCol.insertOne(
            SubscriptionTxnDoc(
                id = UUID.randomUUID().toString(),
                shopId = shopId,
                type = if (renewing) SubscriptionTxnType.RENEWAL else SubscriptionTxnType.PURCHASE,
                planId = plan.id,
                planName = plan.name,
                period = body.period,
                amountInr = amountPaise / 100.0,
                paymentId = body.razorpayPaymentId,
                razorpayOrderId = body.razorpayOrderId,
                periodStart = start,
                periodEnd = end,
                by = userId,
                createdAt = now,
            ),
        )
        return shopSubscription(shopId)
    }

    /**
     * Applies billing entitlement on top of master's raw switches. Only the camera scanner is gated:
     * the other features predate subscriptions and stay under master's plain toggle.
     */
    fun effectiveFeatures(shopId: String, raw: ShopFeaturesDoc): ShopFeaturesDoc {
        if (!raw.barcodeScanner) return raw
        val billing = billingDoc(shopId)
        val entitled = when {
            !isActive(billing) -> false
            billing.model == BillingModel.MONTHLY || billing.model == BillingModel.YEARLY ->
                billing.planId?.let { id -> planCol.find(eq("_id", id)).firstOrNull()?.features?.contains("barcodeScanner") } ?: true
            else -> true
        }
        return if (entitled) raw else raw.copy(barcodeScanner = false)
    }

    private fun enableFeatures(shopId: String, keys: List<String>) {
        val updates = keys.filter { it in PREMIUM_FEATURES }.map { set("features.$it", true) }
        if (updates.isNotEmpty()) shopCol.updateOne(eq("_id", shopId), combine(updates))
    }

    private fun billingDoc(shopId: String): ShopBillingDoc =
        billingCol.find(eq("_id", shopId)).firstOrNull() ?: ShopBillingDoc(shopId)

    private fun isActive(doc: ShopBillingDoc, now: Long = now()): Boolean = when (doc.model) {
        BillingModel.MONTHLY, BillingModel.YEARLY -> (doc.expiresAt ?: 0L) > now
        BillingModel.FREE -> doc.freeUntil == null || doc.freeUntil > now
        else -> true
    }

    private fun billingDto(shopId: String, settings: BillingSettingsDoc): ShopBillingDto {
        val doc = billingDoc(shopId)
        val plan = doc.planId?.let { planCol.find(eq("_id", it)).firstOrNull() }
        val now = now()
        val endsAt = when (doc.model) {
            BillingModel.MONTHLY, BillingModel.YEARLY -> doc.expiresAt
            BillingModel.FREE -> doc.freeUntil
            else -> null
        }
        val active = isActive(doc, now)
        val daysLeft = endsAt?.let { ceil((it - now).toDouble() / DAY_MS).toInt().coerceAtLeast(0) }
        return ShopBillingDto(
            shopId = shopId,
            model = doc.model,
            planId = doc.planId,
            planName = plan?.name.orEmpty(),
            planFeatures = plan?.features.orEmpty(),
            commissionPercent = doc.commissionPercent,
            startedAtEpochMs = doc.startedAt,
            expiresAtEpochMs = doc.expiresAt,
            freeUntilEpochMs = doc.freeUntil,
            note = doc.note,
            active = active,
            daysLeft = daysLeft,
            showRenewWarning = endsAt != null && (!active || (daysLeft ?: Int.MAX_VALUE) <= settings.expiryWarningDays),
            updatedAtEpochMs = doc.updatedAt,
        )
    }

    // ---- Commission ledger ----

    fun commissionSummary(shopId: String, fromMs: Long?, toMs: Long?): CommissionSummaryDto {
        val billing = billingDoc(shopId)
        val percent = billing.commissionPercent
        val (from, to) = ledgerRange(billing, fromMs, toMs)
        val days = commissionOrders(shopId, from, to)
            .groupBy { dayKey(it.createdAt) }
            .map { (day, rows) -> dayRow(day, rows, percent) }
            .sortedByDescending { it.dayStartEpochMs }
        return CommissionSummaryDto(
            commissionPercent = percent,
            totalSaleInr = days.sumOf { it.totalSaleInr }.money(),
            commissionInr = days.sumOf { it.commissionInr }.money(),
            payoutInr = days.sumOf { it.payoutInr }.money(),
            days = days,
        )
    }

    fun commissionDay(shopId: String, day: String): CommissionDayOrdersDto {
        val date = runCatching { LocalDate.parse(day) }.getOrElse { error("Day must be yyyy-MM-dd") }
        val start = date.atStartOfDay(BILLING_ZONE).toInstant().toEpochMilli()
        val billing = billingDoc(shopId)
        val percent = billing.commissionPercent
        val rows = commissionOrders(shopId, maxOf(start, billing.startedAt ?: start), start + DAY_MS)
        return CommissionDayOrdersDto(
            day = dayRow(day, rows, percent),
            orders = rows.sortedByDescending { it.createdAt }.map { it.toCommissionOrder(percent) },
        )
    }

    private fun ledgerRange(billing: ShopBillingDoc, fromMs: Long?, toMs: Long?): Pair<Long, Long> {
        val to = toMs ?: (now() + DAY_MS)
        val earliest = to - MAX_LEDGER_DAYS * DAY_MS
        val from = maxOf(fromMs ?: earliest, earliest, billing.startedAt ?: earliest)
        return from to to
    }

    private fun commissionOrders(shopId: String, from: Long, to: Long): List<OrderDoc> =
        orderCol.find(
            and(eq("shopId", shopId), `in`("status", SALE_STATUSES), gte("createdAt", from), lt("createdAt", to)),
        )
            .projection(
                Projections.include(
                    "shopId", "status", "channel", "createdAt", "items", "quote", "discount",
                    "paymentMethod", "customerName",
                ),
            )
            .maxTime(QUERY_MAX_MS, TimeUnit.MILLISECONDS)
            .toList()

    private fun dayRow(day: String, rows: List<OrderDoc>, percent: Double): CommissionDayDto {
        val sale = rows.sumOf { it.saleInr() }.money()
        val commission = (sale * percent / 100.0).money()
        return CommissionDayDto(
            day = day,
            dayStartEpochMs = LocalDate.parse(day).atStartOfDay(BILLING_ZONE).toInstant().toEpochMilli(),
            orderCount = rows.size,
            totalSaleInr = sale,
            commissionPercent = percent,
            commissionInr = commission,
            payoutInr = (sale - commission).money(),
        )
    }

    private fun OrderDoc.itemsSubtotal(): Double = quote?.itemsSubtotal ?: items.sumOf { it.quantity * it.unitPrice }
    private fun OrderDoc.discountInr(): Double = quote?.discount ?: discount
    private fun OrderDoc.saleInr(): Double = (itemsSubtotal() - discountInr()).coerceAtLeast(0.0)

    private fun OrderDoc.toCommissionOrder(percent: Double): CommissionOrderDto {
        val sale = saleInr().money()
        val commission = (sale * percent / 100.0).money()
        return CommissionOrderDto(
            orderId = id,
            createdAtEpochMs = createdAt,
            channel = channel,
            status = status,
            paymentMethod = paymentMethod,
            customerName = customerName,
            itemsSubtotalInr = itemsSubtotal().money(),
            discountInr = discountInr().money(),
            saleInr = sale,
            commissionInr = commission,
            payoutInr = (sale - commission).money(),
            items = items.map {
                CommissionOrderItemDto(it.productName, it.variantLabel, it.quantity, it.unit, it.unitPrice)
            },
        )
    }

    private fun dayKey(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(BILLING_ZONE).toLocalDate().toString()

    private fun planPricePaise(plan: SubscriptionPlanDoc, period: String): Long = when (period) {
        BillingModel.MONTHLY -> (plan.monthlyPriceInr * 100).roundToLong()
        BillingModel.YEARLY -> (plan.yearlyPriceInr * 100).roundToLong()
        else -> error("Period must be MONTHLY or YEARLY")
    }

    private fun SubscriptionPlanDoc.toDto() = SubscriptionPlanDto(
        id, name, description, features, monthlyPriceInr, yearlyPriceInr, active, sortOrder,
    )

    private fun SubscriptionTxnDoc.toDto() = SubscriptionTxnDto(
        id = id,
        shopId = shopId,
        type = type,
        planId = planId,
        planName = planName,
        period = period,
        amountInr = amountInr,
        paymentId = paymentId,
        razorpayOrderId = razorpayOrderId,
        status = status,
        periodStartEpochMs = periodStart,
        periodEndEpochMs = periodEnd,
        note = note,
        by = by,
        createdAtEpochMs = createdAt,
    )

    private fun Double.money(): Double = (this * 100).roundToLong() / 100.0
    private fun now(): Long = System.currentTimeMillis()
}