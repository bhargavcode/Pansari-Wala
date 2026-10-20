package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.setOnInsert
import org.bhargav.pansariwala.server.db.AppInstallDoc
import org.bhargav.pansariwala.server.db.CustomerDoc
import org.bhargav.pansariwala.server.db.IssueReportDoc
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.db.PartnerDoc
import org.bhargav.pansariwala.server.db.ShopDoc
import org.bhargav.pansariwala.server.db.ShopUserDoc
import org.bhargav.pansariwala.server.dto.AppInstallRequest
import org.bhargav.pansariwala.server.dto.AppUsageReportDto
import org.bhargav.pansariwala.server.dto.AppUserRowDto
import org.bhargav.pansariwala.server.dto.AppVersionCountDto
import org.bhargav.pansariwala.server.dto.IssueCountsDto
import org.bhargav.pansariwala.server.dto.IssueReportCreate
import org.bhargav.pansariwala.server.dto.IssueReportDto
import org.bhargav.pansariwala.server.dto.IssueResolveRequest
import org.bhargav.pansariwala.server.dto.PaymentMethodUsageDto
import org.bhargav.pansariwala.server.storage.AssetRefs
import org.bson.Document
import java.util.UUID

private val APPS = setOf("POS", "USER", "DELIVERY")
private const val ISSUE_OPEN = "OPEN"
private const val ISSUE_RESOLVED = "RESOLVED"
private const val MAX_ATTACHMENTS = 4
private const val MAX_DESCRIPTION = 2_000
private const val MAX_LABEL = 120
private const val MAX_USER_ROWS = 500
private const val MAX_ISSUES = 500

/** Who is on which app build, which payment methods they use, and the in-app issue reports. */
class SupportService(
    mongo: MongoApp,
    private val assets: AssetRefs,
    private val push: PushService,
) {
    private val installCol = mongo.db.getCollection<AppInstallDoc>("app_installs")
    private val issueCol = mongo.db.getCollection<IssueReportDoc>("issue_reports")
    private val customerCol = mongo.db.getCollection<CustomerDoc>("customers")
    private val partnerCol = mongo.db.getCollection<PartnerDoc>("partners")
    private val shopUserCol = mongo.db.getCollection<ShopUserDoc>("shop_users")
    private val shopCol = mongo.db.getCollection<ShopDoc>("shops")
    private val orderCol = mongo.db.getCollection<Document>("orders")

    fun recordInstall(principalId: String, role: String, shopId: String?, body: AppInstallRequest) {
        val app = body.app.uppercase()
        require(app in APPS) { "Unknown app: ${body.app}" }
        val now = System.currentTimeMillis()
        installCol.updateOne(
            eq("_id", "$app:$principalId"),
            combine(
                set("principalId", principalId),
                set("role", role),
                set("shopId", shopId),
                set("app", app),
                set("platform", body.platform.take(16)),
                set("versionName", body.versionName.trim().take(32)),
                set("versionCode", body.versionCode),
                set("deviceId", body.deviceId?.take(64)),
                set("lastSeenAt", now),
                setOnInsert("firstSeenAt", now),
            ),
            UpdateOptions().upsert(true),
        )
    }

    fun usageReport(app: String?, platform: String?, version: String?, paymentMethod: String?): AppUsageReportDto {
        val all = installCol.find().toList()
        val scoped = all.filter { d ->
            (app.isNullOrBlank() || d.app == app) && (platform.isNullOrBlank() || d.platform == platform)
        }
        val versions = scoped
            .groupBy { Triple(it.app, it.platform, it.versionName) }
            .map { (key, rows) ->
                AppVersionCountDto(key.first, key.second, key.third, rows.maxOf { it.versionCode }, rows.size)
            }
            .sortedWith(compareBy<AppVersionCountDto> { it.app }.thenByDescending { it.versionCode }.thenBy { it.platform })
        val inVersion = scoped.filter { version.isNullOrBlank() || it.versionName == version }
        val methodsByInstall = paymentMethods(inVersion)
        val filtered = if (paymentMethod.isNullOrBlank()) inVersion
        else inVersion.filter { paymentMethod in methodsByInstall[it.id].orEmpty().keys }
        val usage = filtered
            .flatMap { d -> methodsByInstall[d.id].orEmpty().map { (method, orders) -> Triple(method, d.id, orders) } }
            .groupBy { it.first }
            .map { (method, rows) -> PaymentMethodUsageDto(method, rows.map { it.second }.distinct().size, rows.sumOf { it.third }) }
            .sortedByDescending { it.users }
        val rows = filtered.sortedByDescending { it.lastSeenAt }.take(MAX_USER_ROWS)
        val people = people(rows.map { it.principalId to it.role })
        return AppUsageReportDto(
            totalUsers = filtered.size,
            versions = versions,
            paymentMethods = usage,
            users = rows.map { d ->
                val who = people[d.principalId]
                AppUserRowDto(
                    principalId = d.principalId,
                    role = d.role,
                    app = d.app,
                    platform = d.platform,
                    versionName = d.versionName,
                    versionCode = d.versionCode,
                    name = who?.first.orEmpty(),
                    phone = who?.second.orEmpty(),
                    lastSeenAt = d.lastSeenAt,
                    paymentMethods = methodsByInstall[d.id].orEmpty().keys.sorted(),
                )
            },
        )
    }

    /** install id → (payment method → order count): customers by their orders, shops by their shop's orders. */
    private fun paymentMethods(installs: List<AppInstallDoc>): Map<String, Map<String, Int>> {
        val customers = installs.filter { it.role == "CUSTOMER" }
        val shops = installs.filter { it.role == "SHOP" && !it.shopId.isNullOrBlank() }
        val byCustomer = countByMethod("customerId", customers.map { it.principalId }.distinct())
        val byShop = countByMethod("shopId", shops.mapNotNull { it.shopId }.distinct())
        return customers.associate { it.id to byCustomer[it.principalId].orEmpty() } +
            shops.associate { it.id to byShop[it.shopId].orEmpty() }
    }

    private fun countByMethod(field: String, ids: List<String>): Map<String, Map<String, Int>> {
        if (ids.isEmpty()) return emptyMap()
        return orderCol.aggregate(
            listOf(
                Aggregates.match(and(`in`(field, ids), ne("status", "PAYMENT_PENDING"))),
                Aggregates.group(
                    Document("owner", "\$$field").append("method", "\$paymentMethod"),
                    Accumulators.sum("n", 1),
                ),
            ),
            Document::class.java,
        ).toList()
            .mapNotNull { row ->
                val key = row.get("_id", Document::class.java) ?: return@mapNotNull null
                val owner = key.getString("owner") ?: return@mapNotNull null
                Triple(owner, key.getString("method").orEmpty().ifBlank { "UNKNOWN" }, row.getInteger("n") ?: 0)
            }
            .groupBy { it.first }
            .mapValues { (_, rows) -> rows.associate { it.second to it.third } }
    }

    /** principalId → (name, phone) across customers, partners and shop staff. */
    private fun people(principals: List<Pair<String, String>>): Map<String, Pair<String, String>> {
        val ids = principals.groupBy({ it.second }, { it.first })
        val out = HashMap<String, Pair<String, String>>()
        ids["CUSTOMER"]?.let { list ->
            customerCol.find(`in`("_id", list)).forEach { out[it.id] = it.name.ifBlank { it.phone } to it.phone }
        }
        ids["PARTNER"]?.let { list ->
            partnerCol.find(`in`("_id", list)).forEach { out[it.id] = it.name to it.phone }
        }
        ids["SHOP"]?.let { list ->
            val users = shopUserCol.find(`in`("_id", list)).toList()
            val shops = shopCol.find(`in`("_id", users.map { it.shopId }.distinct())).toList().associateBy { it.id }
            users.forEach { u ->
                val shopName = shops[u.shopId]?.name.orEmpty()
                out[u.id] = (if (shopName.isBlank()) u.displayName else "${u.displayName} · $shopName") to
                    shops[u.shopId]?.ownerPhone.orEmpty()
            }
        }
        return out
    }

    fun createIssue(principalId: String, role: String, shopId: String?, body: IssueReportCreate): IssueReportDto {
        val app = body.app.uppercase()
        require(app in APPS) { "Unknown app: ${body.app}" }
        require(body.feature.isNotBlank() && body.issueType.isNotBlank()) { "Pick a feature and an issue" }
        require(body.attachments.size <= MAX_ATTACHMENTS) { "At most $MAX_ATTACHMENTS attachments" }
        require(body.attachments.all(assets::isValidRef)) { "Invalid attachment" }
        val who = people(listOf(principalId to role))[principalId]
        val doc = IssueReportDoc(
            id = "iss_" + UUID.randomUUID().toString().replace("-", "").take(16),
            app = app,
            reporterId = principalId,
            reporterRole = role,
            reporterName = who?.first.orEmpty(),
            reporterPhone = who?.second.orEmpty(),
            shopId = shopId,
            feature = body.feature.take(MAX_LABEL),
            featureLabel = body.featureLabel.take(MAX_LABEL),
            issueType = body.issueType.take(MAX_LABEL),
            issueLabel = body.issueLabel.take(MAX_LABEL),
            description = body.description.trim().take(MAX_DESCRIPTION),
            attachments = assets.toKeys(body.attachments),
            platform = body.platform.take(16),
            appVersion = body.appVersion.take(32),
            createdAt = System.currentTimeMillis(),
        )
        issueCol.insertOne(doc)
        return doc.toDto()
    }

    fun myIssues(principalId: String, status: String?): List<IssueReportDto> {
        val filter = if (status.isNullOrBlank()) eq("reporterId", principalId)
        else and(eq("reporterId", principalId), eq("status", status.uppercase()))
        return issueCol.find(filter).sort(Sorts.descending("createdAt")).limit(MAX_ISSUES).toList().map { it.toDto() }
    }

    fun adminIssues(status: String?, app: String?): List<IssueReportDto> {
        val filters = listOfNotNull(
            status?.takeIf { it.isNotBlank() }?.let { eq("status", it.uppercase()) },
            app?.takeIf { it.isNotBlank() }?.let { eq("app", it.uppercase()) },
        )
        val query = if (filters.isEmpty()) issueCol.find() else issueCol.find(and(filters))
        return query.sort(Sorts.descending("createdAt")).limit(MAX_ISSUES).toList().map { it.toDto() }
    }

    fun issueCounts(): IssueCountsDto = IssueCountsDto(
        open = issueCol.countDocuments(eq("status", ISSUE_OPEN)).toInt(),
        resolved = issueCol.countDocuments(eq("status", ISSUE_RESOLVED)).toInt(),
    )

    fun resolveIssue(issueId: String, body: IssueResolveRequest): IssueReportDto {
        val doc = issueCol.find(eq("_id", issueId)).firstOrNull() ?: error("Issue not found")
        val fixedIn = body.fixedInVersion.trim().take(32)
        val updated = doc.copy(
            status = ISSUE_RESOLVED,
            resolvedAt = System.currentTimeMillis(),
            fixedInVersion = fixedIn,
            resolutionNote = body.note.trim().take(MAX_DESCRIPTION),
        )
        issueCol.replaceOne(eq("_id", issueId), updated)
        push.issueResolved(doc.reporterId, doc.id, doc.featureLabel.ifBlank { doc.feature }, fixedIn)
        return updated.toDto()
    }

    fun reopenIssue(issueId: String): IssueReportDto {
        val doc = issueCol.find(eq("_id", issueId)).firstOrNull() ?: error("Issue not found")
        val updated = doc.copy(status = ISSUE_OPEN, resolvedAt = null)
        issueCol.replaceOne(eq("_id", issueId), updated)
        return updated.toDto()
    }

    private fun IssueReportDoc.toDto() = IssueReportDto(
        id = id,
        app = app,
        reporterId = reporterId,
        reporterRole = reporterRole,
        reporterName = reporterName,
        reporterPhone = reporterPhone,
        shopId = shopId,
        feature = feature,
        featureLabel = featureLabel,
        issueType = issueType,
        issueLabel = issueLabel,
        description = description,
        attachments = assets.toUrls(attachments),
        platform = platform,
        appVersion = appVersion,
        status = status,
        createdAt = createdAt,
        resolvedAt = resolvedAt,
        fixedInVersion = fixedInVersion,
        resolutionNote = resolutionNote,
    )
}
