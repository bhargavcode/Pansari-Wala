package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.feature.settings.SettingsViewModel
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_confirm
import pansariwala.shared.generated.resources.action_manage_notifications
import pansariwala.shared.generated.resources.notify_delivery
import pansariwala.shared.generated.resources.notify_offers
import pansariwala.shared.generated.resources.notify_order_status
import pansariwala.shared.generated.resources.notify_system
import pansariwala.shared.generated.resources.settings_current_theme
import pansariwala.shared.generated.resources.settings_high_contrast
import pansariwala.shared.generated.resources.settings_language
import pansariwala.shared.generated.resources.settings_language_card
import pansariwala.shared.generated.resources.settings_notification_card
import pansariwala.shared.generated.resources.settings_theme_card
import pansariwala.shared.generated.resources.theme_dark
import pansariwala.shared.generated.resources.theme_default
import pansariwala.shared.generated.resources.theme_light
import pansariwala.shared.generated.resources.theme_rose
import pansariwala.shared.generated.resources.theme_saffron
import pansariwala.shared.generated.resources.theme_system
import pansariwala.shared.generated.resources.theme_teal

@Composable
fun UserLanguageScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PansariTopBar(title = stringResource(Res.string.settings_language_card), onBack = onBack)
        SectionCard(title = stringResource(Res.string.settings_language)) {
            AppLanguage.entries.forEach { language ->
                LanguageRadioRow(
                    label = language.displayLabel,
                    selected = state.language == language,
                    onClick = { viewModel.setLanguage(language) },
                )
            }
        }
        UserPrimaryButton(text = stringResource(Res.string.action_confirm), onClick = onBack)
    }
}

@Composable
fun UserThemeScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val themeLabel = stringResource(
        when (state.customTheme) {
            CustomTheme.DEFAULT -> Res.string.theme_default
            CustomTheme.SAFFRON -> Res.string.theme_saffron
            CustomTheme.TEAL -> Res.string.theme_teal
            CustomTheme.ROSE -> Res.string.theme_rose
        },
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PansariTopBar(title = stringResource(Res.string.settings_theme_card), onBack = onBack)
        Text(
            stringResource(Res.string.settings_current_theme, themeLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionCard(title = stringResource(Res.string.settings_theme_card)) {
            ThemeMode.entries.forEach { mode ->
                LanguageRadioRow(
                    label = stringResource(mode.labelRes()),
                    selected = state.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            CustomTheme.entries.forEach { theme ->
                LanguageRadioRow(
                    label = stringResource(theme.labelRes()),
                    selected = state.customTheme == theme,
                    onClick = { viewModel.setCustomTheme(theme) },
                )
            }
        }
        UserPrimaryButton(text = stringResource(Res.string.action_confirm), onClick = onBack)
    }
}

@Composable
fun UserNotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: UserSettingsViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle(
        org.bhargav.pansariwala.settings.AppUserSettings(),
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PansariTopBar(
            title = stringResource(Res.string.settings_notification_card),
            onBack = onBack,
        )
        SectionCard(title = stringResource(Res.string.settings_notification_card)) {
            NotificationToggle(
                title = stringResource(Res.string.notify_offers),
                checked = settings.notifyOffers,
                onCheckedChange = viewModel::setNotifyOffers,
            )
            HorizontalDivider()
            NotificationToggle(
                title = stringResource(Res.string.notify_order_status),
                checked = settings.notifyOrderEvents,
                onCheckedChange = viewModel::setNotifyOrderEvents,
            )
            HorizontalDivider()
            NotificationToggle(
                title = stringResource(Res.string.notify_delivery),
                checked = settings.notifyDelivery,
                onCheckedChange = viewModel::setNotifyDelivery,
            )
        }
        UserPrimaryButton(
            text = stringResource(Res.string.action_manage_notifications),
            onClick = onBack,
        )
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LanguageRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun ThemeMode.labelRes() = when (this) {
    ThemeMode.SYSTEM -> Res.string.theme_system
    ThemeMode.LIGHT -> Res.string.theme_light
    ThemeMode.DARK -> Res.string.theme_dark
}

private fun CustomTheme.labelRes() = when (this) {
    CustomTheme.DEFAULT -> Res.string.theme_default
    CustomTheme.SAFFRON -> Res.string.theme_saffron
    CustomTheme.TEAL -> Res.string.theme_teal
    CustomTheme.ROSE -> Res.string.theme_rose
}
