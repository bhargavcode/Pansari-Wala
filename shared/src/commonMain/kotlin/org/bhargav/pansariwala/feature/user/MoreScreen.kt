@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.bhargav.pansariwala.feature.user

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.ShopFilter
import org.bhargav.pansariwala.feature.delivery.PartnerProfileCircle
import org.bhargav.pansariwala.feature.reports.short
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.rememberImageSourceLauncher
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.settings.AppUserSettings
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_logout
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_resend_otp_in
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.more_address_add
import pansariwala.shared.generated.resources.more_address_change
import pansariwala.shared.generated.resources.more_address_empty
import pansariwala.shared.generated.resources.more_address_primary
import pansariwala.shared.generated.resources.more_address_title
import pansariwala.shared.generated.resources.more_app_settings_subtitle
import pansariwala.shared.generated.resources.more_app_settings_title
import pansariwala.shared.generated.resources.more_change_number
import pansariwala.shared.generated.resources.more_change_number_info_data
import pansariwala.shared.generated.resources.more_change_number_info_login
import pansariwala.shared.generated.resources.more_change_number_info_otp
import pansariwala.shared.generated.resources.more_change_number_title
import pansariwala.shared.generated.resources.more_current_number
import pansariwala.shared.generated.resources.more_delete_cancel
import pansariwala.shared.generated.resources.more_delete_confirm
import pansariwala.shared.generated.resources.more_delete_keep
import pansariwala.shared.generated.resources.more_delete_pending_body
import pansariwala.shared.generated.resources.more_delete_pending_title
import pansariwala.shared.generated.resources.more_delete_point_active
import pansariwala.shared.generated.resources.more_delete_point_device
import pansariwala.shared.generated.resources.more_delete_point_orders
import pansariwala.shared.generated.resources.more_delete_point_profile
import pansariwala.shared.generated.resources.more_delete_point_rejoin
import pansariwala.shared.generated.resources.more_delete_point_undo
import pansariwala.shared.generated.resources.more_delete_sheet_intro
import pansariwala.shared.generated.resources.more_delete_sheet_title
import pansariwala.shared.generated.resources.more_delete_subtitle
import pansariwala.shared.generated.resources.more_delete_title
import pansariwala.shared.generated.resources.more_edit_number
import pansariwala.shared.generated.resources.more_edit_profile
import pansariwala.shared.generated.resources.more_filter_all_categories
import pansariwala.shared.generated.resources.more_filter_categories
import pansariwala.shared.generated.resources.more_filter_sort
import pansariwala.shared.generated.resources.more_name_label
import pansariwala.shared.generated.resources.more_new_number_label
import pansariwala.shared.generated.resources.more_otp_label
import pansariwala.shared.generated.resources.more_preferences_title
import pansariwala.shared.generated.resources.more_spend_orders
import pansariwala.shared.generated.resources.more_spend_range
import pansariwala.shared.generated.resources.more_spend_title
import pansariwala.shared.generated.resources.more_spend_view_report
import pansariwala.shared.generated.resources.more_verify_change
import pansariwala.shared.generated.resources.more_welcome_action
import pansariwala.shared.generated.resources.more_welcome_body
import pansariwala.shared.generated.resources.more_welcome_loyal
import pansariwala.shared.generated.resources.more_welcome_title
import pansariwala.shared.generated.resources.settings_language_card
import pansariwala.shared.generated.resources.settings_notification_card
import pansariwala.shared.generated.resources.settings_radius
import pansariwala.shared.generated.resources.settings_radius_value
import pansariwala.shared.generated.resources.settings_theme_card
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MoreScreen(
    onAddAddress: () -> Unit,
    onOpenSpendReport: () -> Unit,
    onOpenBorrowed: () -> Unit,
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
    onTheme: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MoreViewModel = koinViewModel(),
    settingsViewModel: UserSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle(AppUserSettings())
    var showEditName by rememberSaveable { mutableStateOf(false) }
    var showFilter by rememberSaveable { mutableStateOf(false) }
    var showDeleteInfo by rememberSaveable { mutableStateOf(false) }
    val pickPhoto = rememberImageSourceLauncher(ImageUploadFeature.USER_PROFILE_PHOTO, viewModel::changePhoto)

    LaunchedEffect(Unit) { viewModel.refresh() }

    PansariScreen(
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && state.profile == null,
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileCard(
                profile = state.profile,
                uploading = state.photoUploading,
                previewBytes = state.photoPreview,
                onPhoto = pickPhoto,
                onEditName = { showEditName = true },
                onChangeNumber = viewModel::openPhoneChange,
            )
            AddressCard(
                profile = state.profile,
                busy = state.addressBusy,
                onSelect = viewModel::selectAddress,
                onAddNew = onAddAddress,
            )
            SpendCard(spend = state.spend, onClick = onOpenSpendReport)
            org.bhargav.pansariwala.feature.udhaar.BorrowedCard(onClick = onOpenBorrowed)
            AppSettingsCard(filter = state.filter, onClick = { showFilter = true })
            SectionCard(title = stringResource(Res.string.more_preferences_title)) {
                UserMenuRow(
                    title = stringResource(Res.string.settings_language_card),
                    onClick = onLanguage,
                    trailing = settings.language.displayLabel,
                )
                HorizontalDivider()
                UserMenuRow(title = stringResource(Res.string.settings_notification_card), onClick = onNotifications)
                HorizontalDivider()
                UserMenuRow(title = stringResource(Res.string.settings_theme_card), onClick = onTheme)
            }
            val scheduledAt = state.profile?.deletionScheduledAtEpochMs
            if (scheduledAt == null) {
                DeleteAccountCard(onClick = { showDeleteInfo = true })
            } else {
                PendingDeletionCard(scheduledAt, busy = state.deletionBusy, onKeep = viewModel::cancelDeletion)
            }
            TextButton(onClick = { settingsViewModel.logout(onLogout) }, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(Res.string.action_logout))
            }
        }
    }

    if (showEditName) {
        EditNameSheet(
            initial = state.profile?.name.orEmpty(),
            saving = state.nameSaving,
            onSave = { name -> viewModel.saveName(name) { showEditName = false } },
            onDismiss = { showEditName = false },
        )
    }
    state.phoneChange?.let { ui ->
        ChangePhoneSheet(
            ui = ui,
            currentPhone = state.profile?.phone.orEmpty(),
            onPhone = viewModel::setNewPhone,
            onOtp = viewModel::setPhoneOtp,
            onSend = viewModel::sendPhoneOtp,
            onVerify = viewModel::verifyPhoneOtp,
            onEditNumber = viewModel::backToPhoneEntry,
            onDismiss = viewModel::closePhoneChange,
        )
    }
    if (showFilter) {
        ShopFilterSheet(
            initial = state.filter,
            minRadiusKm = state.minRadiusKm,
            onApply = {
                viewModel.applyFilter(it)
                showFilter = false
            },
            onDismiss = { showFilter = false },
            recent = state.recentShopTypes,
        )
    }
    if (showDeleteInfo) {
        DeleteAccountSheet(
            busy = state.deletionBusy,
            onConfirm = { viewModel.requestDeletion { showDeleteInfo = false } },
            onDismiss = { showDeleteInfo = false },
        )
    }
    if (state.showWelcomeBack) {
        WelcomeBackSheet(onDismiss = viewModel::dismissWelcomeBack)
    }
}

// ---------- cards ----------

@Composable
private fun ProfileCard(
    profile: CustomerProfile?,
    uploading: Boolean,
    previewBytes: ByteArray?,
    onPhoto: () -> Unit,
    onEditName: () -> Unit,
    onChangeNumber: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PartnerProfileCircle(
                imageUrl = profile?.imageUrl.orEmpty(),
                onClick = onPhoto,
                showEditBadge = true,
                uploading = uploading,
                localPreviewBytes = previewBytes,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile?.name.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(onClick = onEditName) {
                        Icon(Icons.Default.Edit, stringResource(Res.string.more_edit_profile), tint = colors.onPrimary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, null, tint = colors.onPrimary.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        profile?.phone.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimary.copy(alpha = 0.9f),
                    )
                }
                OutlinedButton(
                    onClick = onChangeNumber,
                    modifier = Modifier.padding(top = 8.dp),
                    border = BorderStroke(1.dp, colors.onPrimary.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onPrimary),
                ) { Text(stringResource(Res.string.more_change_number)) }
            }
        }
    }
}

@Composable
private fun AddressCard(
    profile: CustomerProfile?,
    busy: Boolean,
    onSelect: (String) -> Unit,
    onAddNew: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val addresses = profile?.addresses.orEmpty()
    val primary = addresses.firstOrNull { it.isDefault }
    val line = primary?.let { listOf(it.line, it.locality).filter(String::isNotBlank).joinToString(", ") }
        ?: profile?.address?.takeIf(String::isNotBlank)
    SectionCard(title = stringResource(Res.string.more_address_title)) {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable(enabled = !busy) { expanded = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    if (line != null) {
                        Text(
                            stringResource(Res.string.more_address_primary),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        line ?: stringResource(Res.string.more_address_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(Res.string.more_address_change))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                addresses.forEach { address ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                listOf(address.line, address.locality).filter(String::isNotBlank).joinToString(", "),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(address.id)
                        },
                        trailingIcon = if (address.isDefault) {
                            { Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) }
                        } else {
                            null
                        },
                    )
                }
                if (addresses.isNotEmpty()) HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.more_address_add), color = MaterialTheme.colorScheme.primary) },
                    leadingIcon = { Icon(Icons.Default.AddLocationAlt, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        expanded = false
                        onAddNew()
                    },
                )
            }
        }
    }
}

@Composable
private fun SpendCard(spend: SpendSummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(colors.tertiary, colors.primary)))
                .padding(16.dp),
        ) {
            Text(
                "${stringResource(Res.string.more_spend_title)} · " +
                    stringResource(Res.string.more_spend_range, spend.range.start.short(), spend.range.endInclusive.short()),
                style = MaterialTheme.typography.labelLarge,
                color = colors.onPrimary.copy(alpha = 0.9f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    spend.total.asMoney(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onPrimary,
                )
                if (spend.loading) {
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.more_spend_orders, spend.orderCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(Res.string.more_spend_view_report),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimary,
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onPrimary)
            }
        }
    }
}

@Composable
private fun AppSettingsCard(filter: ShopFilter, onClick: () -> Unit) {
    val categories = filter.shopTypes.map { shopTypeLabel(it) }.sorted()
    SectionCard(
        title = stringResource(Res.string.more_app_settings_title),
        subtitle = stringResource(Res.string.more_app_settings_subtitle),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        action = { Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary) },
    ) {
        SettingLine(
            stringResource(Res.string.settings_radius),
            stringResource(Res.string.settings_radius_value, filter.radiusKm.roundToInt().toString()),
        )
        SettingLine(
            stringResource(Res.string.more_filter_categories),
            if (categories.isEmpty()) stringResource(Res.string.more_filter_all_categories) else categories.joinToString(", "),
        )
        SettingLine(stringResource(Res.string.more_filter_sort), stringResource(filter.sort.labelRes()))
    }
}

@Composable
private fun SettingLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.4f),
        )
    }
}

@Composable
private fun DeleteAccountCard(onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, error.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DeleteForever, null, tint = error)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(stringResource(Res.string.more_delete_title), color = error, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.more_delete_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = error)
        }
    }
}

@Composable
private fun PendingDeletionCard(scheduledAt: Long, busy: Boolean, onKeep: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, tint = colors.error)
                Text(
                    stringResource(Res.string.more_delete_pending_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                stringResource(Res.string.more_delete_pending_body, formatPrintDateTime(scheduledAt)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onKeep,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.more_delete_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------- sheets ----------

@Composable
private fun EditNameSheet(initial: String, saving: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.more_edit_profile), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.more_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            UserPrimaryButton(
                text = stringResource(Res.string.action_save),
                onClick = { onSave(name) },
                enabled = !saving && name.trim().length >= 2,
            )
        }
    }
}

@Composable
private fun ChangePhoneSheet(
    ui: PhoneChangeUi,
    currentPhone: String,
    onPhone: (String) -> Unit,
    onOtp: (String) -> Unit,
    onSend: () -> Unit,
    onVerify: () -> Unit,
    onEditNumber: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(Res.string.more_change_number_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(Res.string.more_current_number, currentPhone),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InfoPoint(stringResource(Res.string.more_change_number_info_otp))
            InfoPoint(stringResource(Res.string.more_change_number_info_data))
            InfoPoint(stringResource(Res.string.more_change_number_info_login))
            when (ui.step) {
                PhoneChangeStep.ENTER_PHONE -> {
                    OutlinedTextField(
                        value = ui.phone,
                        onValueChange = onPhone,
                        label = { Text(stringResource(Res.string.more_new_number_label)) },
                        singleLine = true,
                        enabled = !ui.loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SheetError(ui)
                    UserPrimaryButton(
                        text = stringResource(Res.string.action_send_otp),
                        onClick = onSend,
                        enabled = !ui.loading && ui.phone.length == AppConstants.PHONE_LOCAL_DIGITS,
                    )
                }
                PhoneChangeStep.ENTER_OTP -> {
                    ui.hint?.let {
                        Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = ui.otp,
                        onValueChange = onOtp,
                        label = { Text(stringResource(Res.string.more_otp_label)) },
                        singleLine = true,
                        enabled = !ui.loading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SheetError(ui)
                    UserPrimaryButton(
                        text = stringResource(Res.string.more_verify_change),
                        onClick = onVerify,
                        enabled = !ui.loading && ui.otp.length == AppConstants.OTP_LENGTH,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onEditNumber, enabled = !ui.loading) {
                            Text(stringResource(Res.string.more_edit_number))
                        }
                        TextButton(onClick = onSend, enabled = !ui.loading && ui.resendInSec == 0) {
                            Text(
                                if (ui.resendInSec > 0) {
                                    stringResource(Res.string.action_resend_otp_in, ui.resendInSec)
                                } else {
                                    stringResource(Res.string.action_resend_otp)
                                },
                            )
                        }
                    }
                }
            }
            if (ui.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun SheetError(ui: PhoneChangeUi) {
    ui.error?.let {
        Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun InfoPoint(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(color))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun DeleteAccountSheet(busy: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(64.dp).clip(CircleShape).background(colors.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.DeleteForever, null, tint = colors.error, modifier = Modifier.size(34.dp))
            }
            Text(
                stringResource(Res.string.more_delete_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(stringResource(Res.string.more_delete_sheet_intro), style = MaterialTheme.typography.bodyMedium)
            InfoPoint(stringResource(Res.string.more_delete_point_profile), colors.error)
            InfoPoint(stringResource(Res.string.more_delete_point_orders), colors.error)
            InfoPoint(stringResource(Res.string.more_delete_point_active), colors.error)
            InfoPoint(stringResource(Res.string.more_delete_point_device), colors.error)
            InfoPoint(stringResource(Res.string.more_delete_point_rejoin), colors.error)
            InfoPoint(stringResource(Res.string.more_delete_point_undo))
            Spacer(Modifier.size(4.dp))
            UserPrimaryButton(text = stringResource(Res.string.more_delete_keep), onClick = onDismiss)
            OutlinedButton(
                onClick = onConfirm,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, colors.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.error, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.more_delete_confirm), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun WelcomeBackSheet(onDismiss: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 120f)) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoyaltyCelebration(Modifier.graphicsLayer { scaleX = enter.value; scaleY = enter.value })
            Text(
                stringResource(Res.string.more_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.more_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.more_welcome_loyal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
            UserPrimaryButton(text = stringResource(Res.string.more_welcome_action), onClick = onDismiss)
        }
    }
}

private class Sparkle(val x: Float, val phase: Float, val radiusDp: Float, val colorIndex: Int)

/** Pulsing heart badge with a ripple ring and sparkles drifting up around it. */
@Composable
private fun LoyaltyCelebration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val palette = listOf(colors.primary, colors.tertiary, colors.secondary, colors.error)
    val sparkles = remember {
        val random = Random(SPARKLE_SEED)
        List(SPARKLE_COUNT) { Sparkle(random.nextFloat(), random.nextFloat(), 3f + random.nextFloat() * 4f, it % 4) }
    }
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    )
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearOutSlowInEasing)),
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
    )
    Box(modifier.size(180.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            drawCircle(
                colors.primary.copy(alpha = (1f - ring) * 0.45f),
                radius = maxRadius * (0.4f + 0.6f * ring),
                style = Stroke(width = 4.dp.toPx()),
            )
            sparkles.forEach { s ->
                val t = (drift + s.phase) % 1f
                val sway = sin((t * 2f * PI.toFloat()) + s.phase * 6f) * 12.dp.toPx()
                drawCircle(
                    palette[s.colorIndex].copy(alpha = 1f - t),
                    radius = s.radiusDp.dp.toPx() * (1f - t * 0.4f),
                    center = Offset(size.width * s.x + sway, size.height * (1f - t)),
                )
            }
        }
        Box(
            Modifier
                .size(96.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(48.dp))
        }
    }
}

private const val SPARKLE_COUNT = 18
private const val SPARKLE_SEED = 7
