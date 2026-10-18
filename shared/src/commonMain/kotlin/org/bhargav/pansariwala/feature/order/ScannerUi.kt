package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import org.bhargav.pansariwala.api.ShopBillingDto
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.api.SubscriptionPlanDto
import org.bhargav.pansariwala.scanner.BarcodeScannerPreview
import org.bhargav.pansariwala.scanner.ScannerLens
import org.bhargav.pansariwala.scanner.ScannerSettings
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.scanner_avatar_cd
import pansariwala.shared.generated.resources.scanner_lens_back
import pansariwala.shared.generated.resources.scanner_lens_front
import pansariwala.shared.generated.resources.scanner_repeat_seconds
import pansariwala.shared.generated.resources.scanner_settings_beep
import pansariwala.shared.generated.resources.scanner_settings_camera_on
import pansariwala.shared.generated.resources.scanner_settings_lens
import pansariwala.shared.generated.resources.scanner_settings_repeat
import pansariwala.shared.generated.resources.scanner_settings_retail_only
import pansariwala.shared.generated.resources.scanner_settings_retail_only_hint
import pansariwala.shared.generated.resources.scanner_settings_title
import pansariwala.shared.generated.resources.scanner_settings_torch
import pansariwala.shared.generated.resources.scanner_settings_vibrate
import pansariwala.shared.generated.resources.scanner_switch_camera
import pansariwala.shared.generated.resources.scanner_upsell_benefit_accuracy
import pansariwala.shared.generated.resources.scanner_upsell_benefit_queue
import pansariwala.shared.generated.resources.scanner_upsell_benefit_speed
import pansariwala.shared.generated.resources.scanner_upsell_benefit_transition
import pansariwala.shared.generated.resources.scanner_upsell_contact
import pansariwala.shared.generated.resources.scanner_upsell_cta
import pansariwala.shared.generated.resources.scanner_upsell_plan_price
import pansariwala.shared.generated.resources.scanner_upsell_pricing
import pansariwala.shared.generated.resources.scanner_upsell_subtitle
import pansariwala.shared.generated.resources.scanner_upsell_title
import pansariwala.shared.generated.resources.sub_expired_warning
import pansariwala.shared.generated.resources.sub_pay_now
import pansariwala.shared.generated.resources.sub_renew_warning

private val AVATAR_SIZE = 40.dp

/** Toolbar actions: round live preview (opens settings) + switch camera, or a locked icon (opens upsell). */
@Composable
internal fun ScannerToolbarActions(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    val description = stringResource(Res.string.scanner_avatar_cd)
    if (!state.barcodeScannerEnabled) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = viewModel::onScannerAvatarClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.QrCodeScanner, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).size(14.dp),
            )
        }
        return
    }
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // Camera runs only while visible and nothing covers the counter view.
    val cameraActive = !state.scannerPaused &&
        lifecycleState.isAtLeast(Lifecycle.State.RESUMED) &&
        !state.showScannerSettings &&
        !state.showPaymentPicker &&
        !state.showCartSheet &&
        state.variantPicker == null
    IconButton(onClick = viewModel::switchCamera, enabled = !state.scannerPaused) {
        Icon(Icons.Filled.Cameraswitch, contentDescription = stringResource(Res.string.scanner_switch_camera))
    }
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = viewModel::onScannerAvatarClick),
        contentAlignment = Alignment.Center,
    ) {
        if (state.scannerPaused) {
            Icon(Icons.Filled.CameraAlt, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            BarcodeScannerPreview(
                settings = state.scannerSettings,
                active = cameraActive,
                onBarcode = viewModel::onBarcodeScanned,
                onPermissionDenied = viewModel::onScannerPermissionDenied,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun ScanMessageStrip(state: OrderEditorUiState, modifier: Modifier = Modifier) {
    val message = state.scanMessage ?: return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            message.asString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerSettingsSheet(
    initial: ScannerSettings,
    cameraOn: Boolean,
    onSave: (ScannerSettings, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    var on by remember(cameraOn) { mutableStateOf(cameraOn) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(Res.string.scanner_settings_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SettingSwitch(stringResource(Res.string.scanner_settings_camera_on), on) { on = it }
            Text(stringResource(Res.string.scanner_settings_lens), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.lens == ScannerLens.BACK,
                    onClick = { draft = draft.copy(lens = ScannerLens.BACK) },
                    label = { Text(stringResource(Res.string.scanner_lens_back)) },
                )
                FilterChip(
                    selected = draft.lens == ScannerLens.FRONT,
                    onClick = { draft = draft.copy(lens = ScannerLens.FRONT) },
                    label = { Text(stringResource(Res.string.scanner_lens_front)) },
                )
            }
            SettingSwitch(stringResource(Res.string.scanner_settings_torch), draft.torch) { draft = draft.copy(torch = it) }
            SettingSwitch(stringResource(Res.string.scanner_settings_beep), draft.beepOnScan) { draft = draft.copy(beepOnScan = it) }
            SettingSwitch(stringResource(Res.string.scanner_settings_vibrate), draft.vibrateOnScan) { draft = draft.copy(vibrateOnScan = it) }
            SettingSwitch(
                stringResource(Res.string.scanner_settings_retail_only),
                draft.retailFormatsOnly,
                hint = stringResource(Res.string.scanner_settings_retail_only_hint),
            ) { draft = draft.copy(retailFormatsOnly = it) }
            Text(stringResource(Res.string.scanner_settings_repeat), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScannerSettings.REPEAT_DELAY_OPTIONS_MS.forEach { ms ->
                    FilterChip(
                        selected = draft.repeatDelayMs == ms,
                        onClick = { draft = draft.copy(repeatDelayMs = ms) },
                        label = { Text(stringResource(Res.string.scanner_repeat_seconds, (ms / 100 / 10.0).toString())) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.action_cancel)) }
                Button(onClick = { onSave(draft, on) }, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.action_save)) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, hint: String? = null, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerUpsellSheet(
    plans: List<SubscriptionPlanDto>,
    canPurchase: Boolean,
    onSubscribe: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Column {
                    Text(stringResource(Res.string.scanner_upsell_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(Res.string.scanner_upsell_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            listOf(
                Res.string.scanner_upsell_benefit_speed,
                Res.string.scanner_upsell_benefit_accuracy,
                Res.string.scanner_upsell_benefit_transition,
                Res.string.scanner_upsell_benefit_queue,
            ).forEach { benefit ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PackedGreen, modifier = Modifier.size(20.dp))
                    Text(stringResource(benefit), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (plans.isNotEmpty()) {
                HorizontalDivider()
                Text(stringResource(Res.string.scanner_upsell_pricing), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                plans.forEach { plan ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(plan.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(Res.string.scanner_upsell_plan_price, plan.monthlyPriceInr.asMoney(), plan.yearlyPriceInr.asMoney()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (canPurchase) {
                Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.scanner_upsell_cta)) }
            } else {
                Text(stringResource(Res.string.scanner_upsell_contact), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Pay-to-continue message shown inside the expiry warning window and after a paid/free period lapses. */
@Composable
fun SubscriptionRenewBanner(billing: ShopBillingDto?, onPayNow: () -> Unit, modifier: Modifier = Modifier) {
    if (billing == null || !billing.showRenewWarning) return
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (billing.active) {
                    stringResource(Res.string.sub_renew_warning, billing.daysLeft ?: 0)
                } else {
                    stringResource(Res.string.sub_expired_warning)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onPayNow, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(Res.string.sub_pay_now)) }
        }
    }
}
