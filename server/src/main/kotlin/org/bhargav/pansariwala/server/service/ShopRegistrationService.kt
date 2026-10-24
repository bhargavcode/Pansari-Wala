package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.lt
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.dto.ShopHoursDayDto
import org.bhargav.pansariwala.server.dto.ShopRegistrationDraftDto
import org.bhargav.pansariwala.server.dto.ShopRegistrationResultDto
import org.bhargav.pansariwala.server.dto.ShopRegistrationSessionDto
import org.bhargav.pansariwala.server.dto.ShopRegistrationSubmit
import org.bhargav.pansariwala.server.security.Security
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Onboarding failure with a stable [code] the apps map to localized text. */
class ShopRegistrationException(val code: String, message: String) : RuntimeException(message)

@Serializable
data class ShopRegistrationDoc(
    @SerialName("_id") val phone: String,
    val draft: ShopRegistrationDraftDto,
    val createdAt: Long,
    val updatedAt: Long,
    val reminderCount: Int = 0,
    val lastReminderAt: Long = 0L,
    val completedAt: Long? = null,
    val shopId: String? = null,
)

/**
 * Self-service shop onboarding: phone OTP → short-lived registrant token → draft saved per step →
 * submit creates the shop + owner login. Unfinished drafts get push (app) or SMS (web) reminders.
 */
class ShopRegistrationService(
    mongo: MongoApp,
    private val security: Security,
    private val store: AppStore,
    private val push: PushService,
    private val sms: SmsGateway,
) {
    private val col = mongo.db.getCollection<ShopRegistrationDoc>("shop_registrations")

    fun verify(phone: String, otp: String, sessionId: String?): ShopRegistrationSessionDto {
        val normalized = store.verifyPhoneOtp(phone, otp, sessionId)
        if (store.shopOwnerExists(normalized)) throw ShopRegistrationException(CODE_ALREADY_REGISTERED, ALREADY_REGISTERED)
        val now = System.currentTimeMillis()
        val existing = col.find(eq("_id", normalized)).firstOrNull()?.takeIf { it.completedAt == null }
        val doc = existing ?: ShopRegistrationDoc(
            phone = normalized,
            draft = ShopRegistrationDraftDto(phone = normalized, loginUsername = normalized),
            createdAt = now,
            updatedAt = now,
        ).also { col.replaceOne(eq("_id", normalized), it, ReplaceOptions().upsert(true)) }
        val token = security.issueJwt(normalized, ROLE, displayName = doc.draft.ownerName, ttlSeconds = TOKEN_TTL_SECONDS)
        return ShopRegistrationSessionDto(token, TOKEN_TTL_SECONDS, doc.draft.copy(phone = normalized))
    }

    fun draft(phone: String): ShopRegistrationDraftDto =
        activeDoc(phone)?.draft ?: ShopRegistrationDraftDto(phone = phone, loginUsername = phone)

    fun saveDraft(phone: String, body: ShopRegistrationDraftDto): ShopRegistrationDraftDto {
        val now = System.currentTimeMillis()
        val current = activeDoc(phone)
        val draft = sanitize(phone, body, current?.draft)
        val doc = (current ?: ShopRegistrationDoc(phone, draft, now, now)).copy(
            draft = draft,
            updatedAt = now,
            reminderCount = 0,
        )
        col.replaceOne(eq("_id", phone), doc, ReplaceOptions().upsert(true))
        return draft
    }

    fun submit(phone: String, body: ShopRegistrationSubmit): ShopRegistrationResultDto {
        if (store.shopOwnerExists(phone)) throw ShopRegistrationException(CODE_ALREADY_REGISTERED, ALREADY_REGISTERED)
        val draft = sanitize(phone, body.draft, activeDoc(phone)?.draft)
        require(draft.ownerName.isNotBlank()) { "Owner name is required" }
        if (!draft.locationVerified) throw ShopRegistrationException(CODE_LOCATION_UNVERIFIED, LOCATION_NOT_VERIFIED)
        val password = body.password.trim()
        if (password.length < MIN_PASSWORD || password.none { it.isLetter() } || password.none { it.isDigit() }) {
            throw ShopRegistrationException(CODE_WEAK_PASSWORD, "Password must be at least $MIN_PASSWORD characters with letters and numbers")
        }
        val shop = store.createShopAdmin(
            name = draft.shopName,
            shopType = draft.shopTypes.firstOrNull().orEmpty(),
            shopTypes = draft.shopTypes,
            address = draft.address,
            lat = draft.lat ?: 0.0,
            lng = draft.lng ?: 0.0,
            active = true,
            ownerName = draft.ownerName,
            ownerPhone = phone,
            ownerEmail = draft.ownerEmail,
            city = draft.city,
            state = draft.state,
            zip = draft.zip,
            country = draft.country,
            registrationNumber = draft.registrationNumber,
            taxId = draft.taxId,
            operatingHours = draft.operatingHours,
            loginUsername = draft.loginUsername.ifBlank { phone },
            loginPassword = password,
        )
        val now = System.currentTimeMillis()
        col.updateOne(
            eq("_id", phone),
            combine(set("completedAt", now), set("updatedAt", now), set("shopId", shop.id), unset("draft.pushToken")),
        )
        return ShopRegistrationResultDto(shop.id, shop.name, draft.loginUsername.ifBlank { phone })
    }

    /** Sends due reminders for abandoned drafts and drops stale ones; returns how many were sent. */
    suspend fun sendReminders(): Int {
        val now = System.currentTimeMillis()
        col.deleteMany(lt("updatedAt", now - DRAFT_RETENTION_MS))
        var sent = 0
        col.find(and(eq("completedAt", null), lt("reminderCount", REMINDER_DELAYS_MS.size))).toList().forEach { doc ->
            val due = doc.updatedAt + REMINDER_DELAYS_MS[doc.reminderCount]
            if (now < due) return@forEach
            val token = doc.draft.pushToken
            if (doc.draft.platform != PLATFORM_WEB && !token.isNullOrBlank() && push.enabled) {
                push.registrationReminder(token, doc.phone)
            } else {
                sms.send(doc.phone, reminderSms(doc.draft))
            }
            col.updateOne(eq("_id", doc.phone), combine(set("reminderCount", doc.reminderCount + 1), set("lastReminderAt", now)))
            sent++
        }
        return sent
    }

    private fun activeDoc(phone: String) = col.find(eq("_id", phone)).firstOrNull()?.takeIf { it.completedAt == null }

    private fun sanitize(phone: String, body: ShopRegistrationDraftDto, previous: ShopRegistrationDraftDto?): ShopRegistrationDraftDto {
        val lat = body.lat?.takeIf { it in -90.0..90.0 }
        val lng = body.lng?.takeIf { it in -180.0..180.0 }
        val deviceLat = body.deviceLat?.takeIf { it in -90.0..90.0 }
        val deviceLng = body.deviceLng?.takeIf { it in -180.0..180.0 }
        val verified = lat != null && lng != null && deviceLat != null && deviceLng != null &&
            (lat != 0.0 || lng != 0.0) &&
            distanceMeters(lat, lng, deviceLat, deviceLng) <= LOCATION_TOLERANCE_M
        return ShopRegistrationDraftDto(
            phone = phone,
            step = body.step.coerceIn(0, MAX_STEP).coerceAtLeast(previous?.step ?: 0),
            ownerName = body.ownerName.trim().take(80),
            ownerEmail = body.ownerEmail.trim().take(120),
            shopName = body.shopName.trim().take(80),
            shopTypes = body.shopTypes.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(MAX_SHOP_TYPES),
            address = body.address.trim().take(300),
            city = body.city.trim().take(80),
            state = body.state.trim().take(80),
            zip = body.zip.filter { it.isDigit() }.take(6),
            country = body.country.trim().ifBlank { "India" }.take(60),
            lat = lat,
            lng = lng,
            deviceLat = deviceLat,
            deviceLng = deviceLng,
            locationVerified = verified,
            registrationNumber = body.registrationNumber.trim().take(60),
            taxId = body.taxId.trim().uppercase().take(20),
            operatingHours = body.operatingHours.take(7).map {
                ShopHoursDayDto(it.day.take(12), it.start.take(5), it.end.take(5), it.closed)
            },
            loginUsername = body.loginUsername.trim().lowercase().take(40),
            platform = body.platform.trim().lowercase().take(16),
            pushToken = body.pushToken?.takeIf { it.isNotBlank() && it.length <= MAX_PUSH_TOKEN } ?: previous?.pushToken,
        )
    }

    private fun reminderSms(draft: ShopRegistrationDraftDto): String {
        val shop = draft.shopName.ifBlank { "your shop" }
        return "Pansari Wala: registration of $shop is almost done. Continue at https://pansariwala.shop/pos/register"
    }

    private fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
    }

    companion object {
        const val ROLE = "SHOP_REGISTRANT"
        const val CODE_ALREADY_REGISTERED = "REG_ALREADY_REGISTERED"
        const val CODE_LOCATION_UNVERIFIED = "REG_LOCATION_UNVERIFIED"
        const val CODE_WEAK_PASSWORD = "REG_WEAK_PASSWORD"
        const val ALREADY_REGISTERED = "This mobile number already has a registered shop. Please log in."
        const val LOCATION_NOT_VERIFIED =
            "Shop location could not be verified. Capture your current location at the shop and keep the pin near it."
        private const val PLATFORM_WEB = "web"
        private const val TOKEN_TTL_SECONDS = 24L * 60 * 60
        private const val MIN_PASSWORD = 8
        private const val MAX_STEP = 10
        private const val MAX_SHOP_TYPES = 10
        private const val MAX_PUSH_TOKEN = 4_096
        /** Max distance between the device GPS fix and the shop pin. */
        private const val LOCATION_TOLERANCE_M = 1_000.0
        private const val EARTH_RADIUS_M = 6_371_000.0
        private const val HOUR_MS = 3_600_000L
        /** Delay after the last edit before reminder #1, #2, #3. */
        private val REMINDER_DELAYS_MS = listOf(HOUR_MS, 24 * HOUR_MS, 72 * HOUR_MS)
        private const val DRAFT_RETENTION_MS = 30 * 24 * HOUR_MS
    }
}
