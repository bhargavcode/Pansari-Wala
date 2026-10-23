package org.bhargav.pansariwala.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.settings.CustomTheme
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.webStorageGet
import org.bhargav.pansariwala.webStorageSet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.settings_custom_theme
import pansariwala.shared.generated.resources.settings_theme
import pansariwala.shared.generated.resources.settings_theme_mode
import pansariwala.shared.generated.resources.theme_dark
import pansariwala.shared.generated.resources.theme_default
import pansariwala.shared.generated.resources.theme_light
import pansariwala.shared.generated.resources.theme_rose
import pansariwala.shared.generated.resources.theme_saffron
import pansariwala.shared.generated.resources.theme_system
import pansariwala.shared.generated.resources.theme_teal

/** Console appearance, persisted in local storage so it survives reloads and logouts. */
@Stable
class MasterThemeState(mode: ThemeMode, accent: CustomTheme) {
    var mode by mutableStateOf(mode)
        private set
    var accent by mutableStateOf(accent)
        private set

    fun updateMode(value: ThemeMode) {
        mode = value
        webStorageSet(AppConstants.MasterWeb.THEME_MODE_KEY, value.name)
    }

    fun updateAccent(value: CustomTheme) {
        accent = value
        webStorageSet(AppConstants.MasterWeb.CUSTOM_THEME_KEY, value.name)
    }
}

@Composable
fun rememberMasterThemeState(): MasterThemeState = remember {
    MasterThemeState(
        ThemeMode.fromName(webStorageGet(AppConstants.MasterWeb.THEME_MODE_KEY)),
        CustomTheme.fromName(webStorageGet(AppConstants.MasterWeb.CUSTOM_THEME_KEY)),
    )
}

val LocalMasterTheme = staticCompositionLocalOf<MasterThemeState?> { null }

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Default.LightMode
    ThemeMode.DARK -> Icons.Default.DarkMode
}

private fun ThemeMode.label(): StringResource = when (this) {
    ThemeMode.SYSTEM -> Res.string.theme_system
    ThemeMode.LIGHT -> Res.string.theme_light
    ThemeMode.DARK -> Res.string.theme_dark
}

private fun CustomTheme.label(): StringResource = when (this) {
    CustomTheme.DEFAULT -> Res.string.theme_default
    CustomTheme.SAFFRON -> Res.string.theme_saffron
    CustomTheme.TEAL -> Res.string.theme_teal
    CustomTheme.ROSE -> Res.string.theme_rose
}

private fun CustomTheme.swatch(): Color = when (this) {
    CustomTheme.DEFAULT -> Color(0xFF546524)
    CustomTheme.SAFFRON -> Color(0xFF8B5000)
    CustomTheme.TEAL -> Color(0xFF006A63)
    CustomTheme.ROSE -> Color(0xFFB01E45)
}

/** Palette button: light / dark / system plus accent colour. */
@Composable
fun MasterThemeMenu(tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val state = LocalMasterTheme.current ?: return
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Default.Palette, contentDescription = stringResource(Res.string.settings_theme), tint = tint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(14.dp)) {
            MenuHeader(stringResource(Res.string.settings_theme_mode))
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(mode.label())) },
                    leadingIcon = { Icon(mode.icon(), null) },
                    trailingIcon = { if (state.mode == mode) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { state.updateMode(mode) },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            MenuHeader(stringResource(Res.string.settings_custom_theme))
            CustomTheme.entries.forEach { accent ->
                DropdownMenuItem(
                    text = { Text(stringResource(accent.label())) },
                    leadingIcon = {
                        Box(
                            Modifier.size(20.dp).clip(CircleShape).background(accent.swatch())
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        )
                    },
                    trailingIcon = { if (state.accent == accent) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { state.updateAccent(accent) },
                )
            }
        }
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/** Semantic colour per action so the same action looks the same on every screen. */
enum class MasterAction { VIEW, EDIT, TRANSLATE, DELETE, ACTIVATE, DEACTIVATE, NEUTRAL }

@Composable
fun MasterAction.color(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        MasterAction.VIEW -> MaterialTheme.colorScheme.primary
        MasterAction.EDIT -> if (dark) Color(0xFF9ECAFF) else Color(0xFF1E63C6)
        MasterAction.TRANSLATE -> if (dark) Color(0xFFCDBDFF) else Color(0xFF6A4FB3)
        MasterAction.DELETE -> MaterialTheme.colorScheme.error
        MasterAction.ACTIVATE -> if (dark) Color(0xFF8BD89C) else Color(0xFF1B7F3B)
        MasterAction.DEACTIVATE -> if (dark) Color(0xFFFFB86B) else Color(0xFFA05A00)
        MasterAction.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/** Compact tonal pill used for row actions in tables and cards. */
@Composable
fun MasterActionChip(
    action: MasterAction,
    label: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val color = action.color()
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (enabled) 0.12f else 0.05f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, color = color, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** Icon-only variant for dense rows; [label] doubles as the accessibility description. */
@Composable
fun MasterActionIcon(action: MasterAction, icon: ImageVector, label: String, onClick: () -> Unit) {
    val color = action.color()
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
    }
}
