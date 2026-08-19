package org.bhargav.pansariwala.settings

import org.bhargav.pansariwala.i18n.AppLanguage

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

enum class CustomTheme {
    DEFAULT,
    SAFFRON,
    TEAL,
    ROSE,
    ;

    companion object {
        fun fromName(value: String?): CustomTheme =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
    }
}

data class AppUserSettings(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customTheme: CustomTheme = CustomTheme.DEFAULT,
    val notifyLowStock: Boolean = true,
    val notifyOrderEvents: Boolean = true,
    val searchRadiusKm: Double = org.bhargav.pansariwala.util.AppConstants.DEFAULT_SEARCH_RADIUS_KM,
    val notifyOffers: Boolean = true,
    val notifyDelivery: Boolean = true,
)
