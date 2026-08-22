package org.bhargav.pansariwala.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.platform.stopPartnerLocationTracking
import org.bhargav.pansariwala.settings.AppUserSettings
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.util.AppConstants

class AppPreferences(
    private val store: SessionStore,
) {
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
        const val partnerOnlineDuty = "pref_partner_online_duty"
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
    }

    suspend fun getDisplayName(): String? = store.getString(Keys.userDisplayName)

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

    suspend fun getPartnerOnlineDuty(): Boolean =
        store.getString(Keys.partnerOnlineDuty).toBooleanPref(default = false)

    suspend fun setPartnerOnlineDuty(online: Boolean) {
        store.putStrings(mapOf(Keys.partnerOnlineDuty to online.toString()))
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

    suspend fun clearSession() {
        stopPartnerLocationTracking()
        store.remove(
            setOf(
                Keys.accessToken,
                Keys.refreshToken,
                Keys.userId,
                Keys.shopId,
                Keys.userDisplayName,
                Keys.role,
                Keys.partnerOnlineDuty,
            ),
        )
    }
}

private fun String?.toBooleanPref(default: Boolean): Boolean =
    when {
        this == null -> default
        equals("true", ignoreCase = true) -> true
        equals("false", ignoreCase = true) -> false
        else -> default
    }
