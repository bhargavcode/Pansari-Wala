package org.bhargav.pansariwala.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.settings.AppUserSettings
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode

class SettingsViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {
    val uiState: StateFlow<AppUserSettings> = preferences.userSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUserSettings(),
    )

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setCustomTheme(theme: CustomTheme) {
        viewModelScope.launch { preferences.setCustomTheme(theme) }
    }

    fun setNotifyLowStock(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotifyLowStock(enabled) }
    }

    fun setNotifyOrderEvents(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotifyOrderEvents(enabled) }
    }
}
