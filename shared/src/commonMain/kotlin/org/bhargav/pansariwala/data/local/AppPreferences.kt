package org.bhargav.pansariwala.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.api.ShopFeaturesDto
import org.bhargav.pansariwala.scanner.ScannerSettings
import org.bhargav.pansariwala.domain.auth.ShopAccess
import org.bhargav.pansariwala.domain.auth.ShopPermission
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.PartnerProfile
import org.bhargav.pansariwala.domain.model.SupplierBook
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.platform.stopPartnerLocationTracking
import org.bhargav.pansariwala.settings.AppUserSettings
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.util.AppConstants
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AppPreferences(
    private val store: SessionStore,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        const val accessToken = "access_token"
        const val refreshToken = "refresh_token"
        const val userId = "user_id"
        const val shopId = "shop_id"
        const val userDisplayName = "user_display_name"
        const val language = "pref_language"
        const val themeMode = "pref_theme_mode"
        const val customTheme = "pref_custom_theme"
        const val notifyLowStock = "pref_notify_low_stock"
        const val notifyOrderEvents = "pref_notify_order_events"
        const val searchRadiusKm = "pref_search_radius_km"
        const val role = "pref_auth_role"
        const val notifyOffers = "pref_notify_offers"
        const val notifyDelivery = "pref_notify_delivery"
        const val cachedPartnerProfile = AppConstants.Prefs.CACHED_PARTNER_PROFILE
        const val cachedCustomerProfile = AppConstants.Prefs.CACHED_CUSTOMER_PROFILE
        /** FCM token last registered with the server; survives logout so it can be unregistered. */
        const val registeredPushToken = AppConstants.Prefs.FCM_TOKEN
        const val registeredPushUserId = AppConstants.Prefs.FCM_TOKEN_USER_ID
        /** Stable per-install id; survives logout so the server can replace this device's old FCM token. */
        const val deviceId = AppConstants.Prefs.DEVICE_ID
        /** "false" between OTP verify and finishing profile setup; absent for legacy sessions. */
        const val profileComplete = AppConstants.Prefs.PROFILE_COMPLETE
        /** Legacy duty flag — cleared on logout; online now lives in cached partner profile. */
        const val legacyPartnerOnlineDuty = "pref_partner_online_duty"
    }

    val accessToken: Flow<String?> = store.observeString(Keys.accessToken)

    val userSettings: Flow<AppUserSettings> = combine(
        store.observeString(Keys.language),
        store.observeString(Keys.themeMode),
        store.observeString(Keys.customTheme),
        store.observeString(Keys.notifyLowStock),
        store.observeString(Keys.notifyOrderEvents),
    ) { language, themeMode, customTheme, lowStock, orderEvents ->
        AppUserSettings(
            language = AppLanguage.fromCode(language ?: AppLanguage.ENGLISH.code),
            themeMode = ThemeMode.fromName(themeMode),
            customTheme = CustomTheme.fromName(customTheme),
            notifyLowStock = lowStock.toBooleanPref(default = true),
            notifyOrderEvents = orderEvents.toBooleanPref(default = true),
        )
    }

    suspend fun getAccessToken(): String? = store.getString(Keys.accessToken)

    suspend fun getUserId(): String? = store.getString(Keys.userId)

    suspend fun getShopId(): String? = store.getString(Keys.shopId)

    suspend fun getRole(): String? = store.getString(Keys.role)

    suspend fun getSearchRadiusKm(): Double =
        store.getString(Keys.searchRadiusKm)?.toDoubleOrNull()
            ?: org.bhargav.pansariwala.util.AppConstants.DEFAULT_SEARCH_RADIUS_KM

    fun observeSearchRadiusKm(): Flow<Double> = store.observeString(Keys.searchRadiusKm).map { raw ->
        raw?.toDoubleOrNull() ?: AppConstants.DEFAULT_SEARCH_RADIUS_KM
    }

    suspend fun saveToken(token: org.bhargav.pansariwala.api.TokenResponse) {
        saveSession(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            userId = token.userId,
            shopId = token.shopId,
            displayName = token.displayName,
            role = token.role,
        )
        setProfileComplete(token.profileComplete)
        token.shopRole?.let { setShopAccess(it, token.permissions) }
    }

    /** Missing role (legacy session / offline login) means full shop-admin access. */
    val shopAccess: Flow<ShopAccess> = combine(
        store.observeString(AppConstants.Prefs.SHOP_STAFF_ROLE),
        store.observeString(AppConstants.Prefs.SHOP_PERMISSIONS),
    ) { role, perms -> toShopAccess(role, perms) }

    suspend fun getShopAccess(): ShopAccess = toShopAccess(
        store.getString(AppConstants.Prefs.SHOP_STAFF_ROLE),
        store.getString(AppConstants.Prefs.SHOP_PERMISSIONS),
    )

    suspend fun setShopAccess(role: String, permissions: List<String>) {
        store.putStrings(
            mapOf(
                AppConstants.Prefs.SHOP_STAFF_ROLE to role,
                AppConstants.Prefs.SHOP_PERMISSIONS to permissions.joinToString(PERMISSION_SEPARATOR),
            ),
        )
    }

    /** Server-side shop feature switches; defaults (all on) until the profile has been fetched once. */
    val shopFeatures: Flow<ShopFeaturesDto> = store.observeString(AppConstants.Prefs.SHOP_FEATURES).map { raw ->
        raw?.let { runCatching { json.decodeFromString<ShopFeaturesDto>(it) }.getOrNull() } ?: ShopFeaturesDto()
    }

    suspend fun setShopFeatures(features: ShopFeaturesDto) {
        store.putStrings(mapOf(AppConstants.Prefs.SHOP_FEATURES to json.encodeToString(features)))
    }

    val scannerSettings: Flow<ScannerSettings> = store.observeString(AppConstants.Prefs.SCANNER_SETTINGS).map { raw ->
        raw?.let { runCatching { json.decodeFromString<ScannerSettings>(it) }.getOrNull() } ?: ScannerSettings()
    }

    suspend fun setScannerSettings(settings: ScannerSettings) {
        store.putStrings(mapOf(AppConstants.Prefs.SCANNER_SETTINGS to json.encodeToString(settings)))
    }

    /** On: confident voice matches go straight to the cart. Off: every match opens the product sheet. */
    val voiceAutoAdd: Flow<Boolean> = store.observeString(AppConstants.Prefs.VOICE_AUTO_ADD)
        .map { it.toBooleanPref(default = true) }

    suspend fun setVoiceAutoAdd(enabled: Boolean) {
        store.putStrings(mapOf(AppConstants.Prefs.VOICE_AUTO_ADD to enabled.toString()))
    }

    suspend fun getSupplierBook(shopId: String): SupplierBook =
        store.getString(AppConstants.Prefs.SUPPLIER_BOOK_PREFIX + shopId)?.let { raw ->
            runCatching { json.decodeFromString(SupplierBook.serializer(), raw) }.getOrNull()
        } ?: SupplierBook()

    suspend fun setSupplierBook(shopId: String, book: SupplierBook) {
        store.putStrings(
            mapOf(AppConstants.Prefs.SUPPLIER_BOOK_PREFIX + shopId to json.encodeToString(SupplierBook.serializer(), book)),
        )
    }

    private fun toShopAccess(role: String?, perms: String?): ShopAccess =
        if (role.isNullOrBlank()) {
            ShopAccess.FULL
        } else {
            ShopAccess(role, ShopPermission.parse(perms.orEmpty().split(PERMISSION_SEPARATOR)))
        }

    suspend fun isProfileComplete(): Boolean =
        store.getString(Keys.profileComplete).toBooleanPref(default = true)

    suspend fun setProfileComplete(complete: Boolean) {
        store.putStrings(mapOf(Keys.profileComplete to complete.toString()))
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getOrCreateDeviceId(): String =
        store.getString(Keys.deviceId)?.takeIf { it.isNotBlank() }
            ?: Uuid.random().toString().also { store.putStrings(mapOf(Keys.deviceId to it)) }

    suspend fun getDisplayName(): String? = store.getString(Keys.userDisplayName)

    suspend fun getRegisteredPushToken(): String? = store.getString(Keys.registeredPushToken)

    suspend fun getRegisteredPushUserId(): String? = store.getString(Keys.registeredPushUserId)

    suspend fun setRegisteredPushToken(token: String?, userId: String? = null) {
        if (token == null) store.remove(setOf(Keys.registeredPushToken, Keys.registeredPushUserId))
        else store.putStrings(mapOf(Keys.registeredPushToken to token, Keys.registeredPushUserId to userId))
    }

    suspend fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    suspend fun getUserSettings(): AppUserSettings = AppUserSettings(
        language = AppLanguage.fromCode(store.getString(Keys.language) ?: AppLanguage.ENGLISH.code),
        themeMode = ThemeMode.fromName(store.getString(Keys.themeMode)),
        customTheme = CustomTheme.fromName(store.getString(Keys.customTheme)),
        notifyLowStock = store.getString(Keys.notifyLowStock).toBooleanPref(default = true),
        notifyOrderEvents = store.getString(Keys.notifyOrderEvents).toBooleanPref(default = true),
        searchRadiusKm = store.getString(Keys.searchRadiusKm)?.toDoubleOrNull()
            ?: AppConstants.DEFAULT_SEARCH_RADIUS_KM,
        notifyOffers = store.getString(Keys.notifyOffers).toBooleanPref(default = true),
        notifyDelivery = store.getString(Keys.notifyDelivery).toBooleanPref(default = true),
    )

    suspend fun setLanguage(language: AppLanguage) {
        store.putStrings(mapOf(Keys.language to language.code))
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.putStrings(mapOf(Keys.themeMode to mode.name))
    }

    suspend fun setCustomTheme(theme: CustomTheme) {
        store.putStrings(mapOf(Keys.customTheme to theme.name))
    }

    suspend fun setNotifyLowStock(enabled: Boolean) {
        store.putStrings(mapOf(Keys.notifyLowStock to enabled.toString()))
    }

    suspend fun setNotifyOrderEvents(enabled: Boolean) {
        store.putStrings(mapOf(Keys.notifyOrderEvents to enabled.toString()))
    }

    suspend fun setSearchRadiusKm(km: Double) {
        store.putStrings(mapOf(Keys.searchRadiusKm to km.toString()))
    }

    suspend fun setNotifyOffers(enabled: Boolean) {
        store.putStrings(mapOf(Keys.notifyOffers to enabled.toString()))
    }

    suspend fun setNotifyDelivery(enabled: Boolean) {
        store.putStrings(mapOf(Keys.notifyDelivery to enabled.toString()))
    }

    suspend fun isNotificationPromptShown(): Boolean =
        store.getString(AppConstants.Prefs.NOTIFICATION_PROMPT_SHOWN).toBooleanPref(default = false)

    suspend fun setNotificationPromptShown() {
        store.putStrings(mapOf(AppConstants.Prefs.NOTIFICATION_PROMPT_SHOWN to true.toString()))
    }

    suspend fun getCachedPartnerProfile(): PartnerProfile? =
        store.getString(Keys.cachedPartnerProfile)?.let { raw ->
            runCatching { json.decodeFromString(PartnerProfile.serializer(), raw) }.getOrNull()
        }

    suspend fun setCachedPartnerProfile(profile: PartnerProfile) {
        store.putStrings(
            mapOf(Keys.cachedPartnerProfile to json.encodeToString(PartnerProfile.serializer(), profile)),
        )
    }

    suspend fun getCachedCustomerProfile(): CustomerProfile? =
        store.getString(Keys.cachedCustomerProfile)?.let { raw ->
            runCatching { json.decodeFromString(CustomerProfile.serializer(), raw) }.getOrNull()
        }

    suspend fun setCachedCustomerProfile(profile: CustomerProfile) {
        store.putStrings(
            mapOf(Keys.cachedCustomerProfile to json.encodeToString(CustomerProfile.serializer(), profile)),
        )
    }

    suspend fun getSavedCart(): SavedCart? =
        store.getString(AppConstants.Prefs.SAVED_CART)?.let { raw ->
            runCatching { json.decodeFromString(SavedCart.serializer(), raw) }.getOrNull()
        }

    /** Null or an empty cart removes the saved copy. */
    suspend fun setSavedCart(cart: SavedCart?) {
        if (cart == null || cart.lines.isEmpty()) {
            store.remove(setOf(AppConstants.Prefs.SAVED_CART))
        } else {
            store.putStrings(mapOf(AppConstants.Prefs.SAVED_CART to json.encodeToString(SavedCart.serializer(), cart)))
        }
    }

    /** Online duty is derived from the cached partner profile (no separate pref). */
    suspend fun getPartnerOnlineDuty(): Boolean = getCachedPartnerProfile()?.online == true

    suspend fun setPartnerOnlineDuty(online: Boolean) {
        val cached = getCachedPartnerProfile() ?: return
        if (cached.online == online) return
        setCachedPartnerProfile(cached.copy(online = online))
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        shopId: String?,
        displayName: String?,
        role: String = AppConstants.Roles.SHOP,
    ) {
        store.putStrings(
            mapOf(
                Keys.accessToken to accessToken,
                Keys.refreshToken to refreshToken,
                Keys.userId to userId,
                Keys.shopId to shopId,
                Keys.userDisplayName to displayName,
                Keys.role to role,
            ),
        )
    }

    suspend fun getShopUpiId(): String = store.getString(AppConstants.Prefs.SHOP_UPI_ID).orEmpty()

    suspend fun getShopName(): String = store.getString(AppConstants.Prefs.SHOP_NAME).orEmpty()

    suspend fun setShopPaymentInfo(shopName: String, upiId: String) {
        store.putStrings(
            mapOf(
                AppConstants.Prefs.SHOP_NAME to shopName,
                AppConstants.Prefs.SHOP_UPI_ID to upiId,
            ),
        )
    }

    suspend fun clearSession() {
        stopPartnerLocationTracking()
        store.remove(
            setOf(
                AppConstants.Prefs.SHOP_UPI_ID,
                AppConstants.Prefs.SHOP_NAME,
                AppConstants.Prefs.SHOP_STAFF_ROLE,
                AppConstants.Prefs.SHOP_PERMISSIONS,
                AppConstants.Prefs.SHOP_FEATURES,
                Keys.accessToken,
                Keys.refreshToken,
                Keys.userId,
                Keys.shopId,
                Keys.userDisplayName,
                Keys.role,
                Keys.cachedPartnerProfile,
                Keys.cachedCustomerProfile,
                Keys.legacyPartnerOnlineDuty,
                Keys.profileComplete,
                AppConstants.Prefs.SAVED_CART,
            ),
        )
    }
}

/** Customer cart persisted across app restarts. */
@Serializable
data class SavedCart(
    val shopId: String,
    val shopName: String? = null,
    val lines: List<SavedCartLine> = emptyList(),
)

@Serializable
data class SavedCartLine(
    val product: Product,
    val variantId: String? = null,
    val quantity: Double,
)

private const val PERMISSION_SEPARATOR = ","

private fun String?.toBooleanPref(default: Boolean): Boolean =
    when {
        this == null -> default
        equals("true", ignoreCase = true) -> true
        equals("false", ignoreCase = true) -> false
        else -> default
    }
