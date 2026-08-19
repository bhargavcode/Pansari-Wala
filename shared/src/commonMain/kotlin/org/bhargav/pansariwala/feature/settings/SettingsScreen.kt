package org.bhargav.pansariwala.feature.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
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
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.settings_custom_theme
import pansariwala.shared.generated.resources.settings_language
import pansariwala.shared.generated.resources.settings_notifications
import pansariwala.shared.generated.resources.settings_notify_low_stock
import pansariwala.shared.generated.resources.settings_notify_low_stock_desc
import pansariwala.shared.generated.resources.settings_notify_orders
import pansariwala.shared.generated.resources.settings_notify_orders_desc
import pansariwala.shared.generated.resources.settings_theme
import pansariwala.shared.generated.resources.settings_theme_mode
import pansariwala.shared.generated.resources.settings_title
import pansariwala.shared.generated.resources.theme_dark
import pansariwala.shared.generated.resources.theme_default
import pansariwala.shared.generated.resources.theme_light
import pansariwala.shared.generated.resources.theme_rose
import pansariwala.shared.generated.resources.theme_saffron
import pansariwala.shared.generated.resources.theme_system
import pansariwala.shared.generated.resources.theme_teal

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PansariTopBar(
            title = stringResource(Res.string.settings_title),
            onBack = onBack,
        )

        SectionCard(title = stringResource(Res.string.settings_notifications)) {
            SettingsToggle(
                title = stringResource(Res.string.settings_notify_low_stock),
                subtitle = stringResource(Res.string.settings_notify_low_stock_desc),
                checked = state.notifyLowStock,
                onCheckedChange = viewModel::setNotifyLowStock,
            )
            HorizontalDivider()
            SettingsToggle(
                title = stringResource(Res.string.settings_notify_orders),
                subtitle = stringResource(Res.string.settings_notify_orders_desc),
                checked = state.notifyOrderEvents,
                onCheckedChange = viewModel::setNotifyOrderEvents,
            )
        }

        SectionCard(title = stringResource(Res.string.settings_language)) {
            AppLanguage.entries.forEach { language ->
                RadioRow(
                    label = language.displayLabel,
                    selected = state.language == language,
                    onClick = { viewModel.setLanguage(language) },
                )
            }
        }

        SectionCard(title = stringResource(Res.string.settings_theme)) {
            Text(
                text = stringResource(Res.string.settings_theme_mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ThemeMode.entries.forEach { mode ->
                RadioRow(
                    label = stringResource(mode.labelRes()),
                    selected = state.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
            Text(
                text = stringResource(Res.string.settings_custom_theme),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            CustomTheme.entries.forEach { theme ->
                RadioRow(
                    label = stringResource(theme.labelRes()),
                    selected = state.customTheme == theme,
                    onClick = { viewModel.setCustomTheme(theme) },
                )
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RadioRow(
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
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
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
