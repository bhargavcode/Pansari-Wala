package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_change_password
import pansariwala.shared.generated.resources.master_confirm_password
import pansariwala.shared.generated.resources.master_current_password
import pansariwala.shared.generated.resources.master_customer_charges
import pansariwala.shared.generated.resources.master_default_delivery_radius
import pansariwala.shared.generated.resources.master_delivery_per_km
import pansariwala.shared.generated.resources.master_delivery_surcharge
import pansariwala.shared.generated.resources.master_free_delivery_above
import pansariwala.shared.generated.resources.master_invalid_number
import pansariwala.shared.generated.resources.master_last_updated
import pansariwala.shared.generated.resources.master_max_delivery_charge
import pansariwala.shared.generated.resources.master_min_delivery_charge
import pansariwala.shared.generated.resources.master_min_order_value
import pansariwala.shared.generated.resources.master_new_password
import pansariwala.shared.generated.resources.master_offer_ttl_min
import pansariwala.shared.generated.resources.master_online_ordering
import pansariwala.shared.generated.resources.master_online_ordering_hint
import pansariwala.shared.generated.resources.master_partner_payout_per_km
import pansariwala.shared.generated.resources.master_partner_registration
import pansariwala.shared.generated.resources.master_partner_ring_km
import pansariwala.shared.generated.resources.master_partner_settings
import pansariwala.shared.generated.resources.master_password_changed
import pansariwala.shared.generated.resources.master_password_mismatch
import pansariwala.shared.generated.resources.master_paused_message
import pansariwala.shared.generated.resources.master_platform_fee
import pansariwala.shared.generated.resources.master_quote_customer_pays
import pansariwala.shared.generated.resources.master_quote_delivery
import pansariwala.shared.generated.resources.master_quote_partner_earns
import pansariwala.shared.generated.resources.master_quote_platform_fee
import pansariwala.shared.generated.resources.master_quote_preview
import pansariwala.shared.generated.resources.master_save
import pansariwala.shared.generated.resources.master_save_changes
import pansariwala.shared.generated.resources.master_settings_saved
import pansariwala.shared.generated.resources.master_shop_defaults

private const val PREVIEW_ORDER_INR = 500.0
private const val PREVIEW_DISTANCE_KM = 3.0

private fun Double.plain(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

/** Text-backed form so partially typed numbers ("1.") survive recomposition. */
private class PlatformForm(s: PlatformSettingsDto) {
    var platformFee by mutableStateOf(s.platformFeeInr.plain())
    var perKm by mutableStateOf(s.deliveryBasePerKmInr.plain())
    var surcharge by mutableStateOf(s.deliverySurchargePercent.plain())
    var minDelivery by mutableStateOf(s.minDeliveryChargeInr.plain())
    var maxDelivery by mutableStateOf(s.maxDeliveryChargeInr.plain())
    var freeAbove by mutableStateOf(s.freeDeliveryAboveInr.plain())
    var minOrder by mutableStateOf(s.minOrderValueInr.plain())
    var payoutPerKm by mutableStateOf(s.partnerPayoutPerKmInr.plain())
    var ringKm by mutableStateOf(s.partnerRingKm.plain())
    var offerTtl by mutableStateOf(s.offerTtlMinutes.toString())
    var defaultRadius by mutableStateOf(s.defaultShopDeliveryRadiusKm.plain())
}

/** Mirrors the server's delivery-charge rule so admins see the effect before saving. */
private fun previewDelivery(s: PlatformSettingsDto, subtotal: Double, km: Double): Double {
    if (s.freeDeliveryAboveInr > 0 && subtotal >= s.freeDeliveryAboveInr) return 0.0
    val base = s.deliveryBasePerKmInr * km
    val charge = (base + base * s.deliverySurchargePercent / 100.0).coerceAtLeast(s.minDeliveryChargeInr)
    return if (s.maxDeliveryChargeInr > 0) charge.coerceAtMost(s.maxDeliveryChargeInr) else charge
}

@Composable
internal fun PlatformConfigSections(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf<PlatformSettingsDto?>(null) }
    var form by remember { mutableStateOf<PlatformForm?>(null) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(token) {
        runCatching { api.platformSettings(token) }
            .onSuccess { saved = it; form = PlatformForm(it) }
            .onFailure { onStatus(it.message.orEmpty()) }
    }
    val base = saved ?: return
    val f = form ?: return

    val labels = PlatformLabels.current()
    fun parsed(): Result<PlatformSettingsDto> = runCatching {
        fun num(text: String, label: String) = text.trim().toDoubleOrNull() ?: throw IllegalArgumentException(label)
        base.copy(
            platformFeeInr = num(f.platformFee, labels.platformFee),
            deliveryBasePerKmInr = num(f.perKm, labels.perKm),
            deliverySurchargePercent = num(f.surcharge, labels.surcharge),
            minDeliveryChargeInr = num(f.minDelivery, labels.minDelivery),
            maxDeliveryChargeInr = num(f.maxDelivery, labels.maxDelivery),
            freeDeliveryAboveInr = num(f.freeAbove, labels.freeAbove),
            minOrderValueInr = num(f.minOrder, labels.minOrder),
            partnerPayoutPerKmInr = num(f.payoutPerKm, labels.payoutPerKm),
            partnerRingKm = num(f.ringKm, labels.ringKm),
            offerTtlMinutes = f.offerTtl.trim().toIntOrNull() ?: throw IllegalArgumentException(labels.offerTtl),
            defaultShopDeliveryRadiusKm = num(f.defaultRadius, labels.defaultRadius),
        )
    }
    val preview = parsed().getOrNull() ?: base

    ResponsiveColumns(
        left = {
            MasterSectionCard(title = stringResource(Res.string.master_customer_charges)) {
                NumberField(f.platformFee, labels.platformFee) { f.platformFee = it }
                NumberField(f.perKm, labels.perKm) { f.perKm = it }
                NumberField(f.surcharge, labels.surcharge) { f.surcharge = it }
                NumberField(f.minDelivery, labels.minDelivery) { f.minDelivery = it }
                NumberField(f.maxDelivery, labels.maxDelivery) { f.maxDelivery = it }
                NumberField(f.freeAbove, labels.freeAbove) { f.freeAbove = it }
                NumberField(f.minOrder, labels.minOrder) { f.minOrder = it }
            }
        },
        right = {
            MasterSectionCard(title = stringResource(Res.string.master_partner_settings)) {
                NumberField(f.payoutPerKm, labels.payoutPerKm) { f.payoutPerKm = it }
                NumberField(f.ringKm, labels.ringKm) { f.ringKm = it }
                NumberField(f.offerTtl, labels.offerTtl) { f.offerTtl = it }
            }
            MasterSectionCard(title = stringResource(Res.string.master_shop_defaults)) {
                NumberField(f.defaultRadius, labels.defaultRadius) { f.defaultRadius = it }
            }
            QuotePreviewCard(preview)
        },
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            enabled = !saving,
            onClick = {
                scope.launch {
                    val body = parsed().getOrElse {
                        onStatus(getString(Res.string.master_invalid_number, it.message.orEmpty()))
                        return@launch
                    }
                    saving = true
                    runCatching { api.savePlatformSettings(token, body) }
                        .onSuccess { saved = it; form = PlatformForm(it); onStatus(getString(Res.string.master_settings_saved)) }
                        .onFailure { onStatus(it.message.orEmpty()) }
                    saving = false
                }
            },
        ) { Text(stringResource(Res.string.master_save_changes)) }
        if (base.updatedAt > 0) {
            Text(
                "${stringResource(Res.string.master_last_updated)}: ${formatEpochDate(base.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private class PlatformLabels(
    val platformFee: String,
    val perKm: String,
    val surcharge: String,
    val minDelivery: String,
    val maxDelivery: String,
    val freeAbove: String,
    val minOrder: String,
    val payoutPerKm: String,
    val ringKm: String,
    val offerTtl: String,
    val defaultRadius: String,
) {
    companion object {
        @Composable
        fun current() = PlatformLabels(
            platformFee = stringResource(Res.string.master_platform_fee),
            perKm = stringResource(Res.string.master_delivery_per_km),
            surcharge = stringResource(Res.string.master_delivery_surcharge),
            minDelivery = stringResource(Res.string.master_min_delivery_charge),
            maxDelivery = stringResource(Res.string.master_max_delivery_charge),
            freeAbove = stringResource(Res.string.master_free_delivery_above),
            minOrder = stringResource(Res.string.master_min_order_value),
            payoutPerKm = stringResource(Res.string.master_partner_payout_per_km),
            ringKm = stringResource(Res.string.master_partner_ring_km),
            offerTtl = stringResource(Res.string.master_offer_ttl_min),
            defaultRadius = stringResource(Res.string.master_default_delivery_radius),
        )
    }
}

@Composable
private fun QuotePreviewCard(s: PlatformSettingsDto) {
    val delivery = previewDelivery(s, PREVIEW_ORDER_INR, PREVIEW_DISTANCE_KM)
    // Partner travels shop → customer and back, matching the server's offer payout.
    val partner = s.partnerPayoutPerKmInr * PREVIEW_DISTANCE_KM * 2
    MasterSectionCard(
        title = stringResource(Res.string.master_quote_preview, PREVIEW_ORDER_INR.plain(), PREVIEW_DISTANCE_KM.plain()),
    ) {
        LabelValue(stringResource(Res.string.master_quote_platform_fee), formatInr(s.platformFeeInr))
        LabelValue(stringResource(Res.string.master_quote_delivery), formatInr(delivery))
        LabelValue(stringResource(Res.string.master_quote_customer_pays), formatInr(PREVIEW_ORDER_INR + s.platformFeeInr + delivery))
        LabelValue(stringResource(Res.string.master_quote_partner_earns), formatInr(partner))
    }
}

@Composable
private fun NumberField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> if (next.all { it.isDigit() || it == '.' }) onChange(next) },
        label = { Text(label) },
        singleLine = true,
        isError = value.isNotEmpty() && value.toDoubleOrNull() == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Platform-wide on/off switches; saved immediately on toggle. */
@Composable
internal fun PlatformControlsSection(token: String, onStatus: (String) -> Unit, title: String) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<PlatformSettingsDto?>(null) }
    var pausedMessage by remember { mutableStateOf("") }
    LaunchedEffect(token) {
        runCatching { api.platformSettings(token) }
            .onSuccess { settings = it; pausedMessage = it.orderingPausedMessage }
            .onFailure { onStatus(it.message.orEmpty()) }
    }
    val s = settings ?: return
    fun save(next: PlatformSettingsDto) {
        scope.launch {
            runCatching { api.savePlatformSettings(token, next) }
                .onSuccess { settings = it; onStatus(getString(Res.string.master_settings_saved)) }
                .onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    MasterSectionCard(title = title) {
        FeatureToggleRow(stringResource(Res.string.master_online_ordering), s.onlineOrderingEnabled) {
            save(s.copy(onlineOrderingEnabled = !s.onlineOrderingEnabled, orderingPausedMessage = pausedMessage))
        }
        Text(
            stringResource(Res.string.master_online_ordering_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = pausedMessage,
            onValueChange = { pausedMessage = it.take(200) },
            label = { Text(stringResource(Res.string.master_paused_message)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (pausedMessage != s.orderingPausedMessage) {
            Button(onClick = { save(s.copy(orderingPausedMessage = pausedMessage)) }) {
                Text(stringResource(Res.string.master_save))
            }
        }
        HorizontalDivider()
        FeatureToggleRow(stringResource(Res.string.master_partner_registration), s.partnerRegistrationEnabled) {
            save(s.copy(partnerRegistrationEnabled = !s.partnerRegistrationEnabled, orderingPausedMessage = pausedMessage))
        }
    }
}

@Composable
internal fun AdminAccountSection(token: String, onStatus: (String) -> Unit, title: String) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    MasterSectionCard(title = title) {
        PasswordField(current, stringResource(Res.string.master_current_password)) { current = it }
        PasswordField(next, stringResource(Res.string.master_new_password)) { next = it }
        PasswordField(confirm, stringResource(Res.string.master_confirm_password)) { confirm = it }
        Button(
            enabled = !saving && current.isNotBlank() && next.isNotBlank(),
            onClick = {
                scope.launch {
                    if (next != confirm) {
                        onStatus(getString(Res.string.master_password_mismatch))
                        return@launch
                    }
                    saving = true
                    runCatching { api.changePassword(token, current, next) }
                        .onSuccess {
                            current = ""; next = ""; confirm = ""
                            onStatus(getString(Res.string.master_password_changed))
                        }
                        .onFailure { onStatus(it.message.orEmpty()) }
                    saving = false
                }
            },
        ) { Text(stringResource(Res.string.master_change_password)) }
    }
}

@Composable
private fun PasswordField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}