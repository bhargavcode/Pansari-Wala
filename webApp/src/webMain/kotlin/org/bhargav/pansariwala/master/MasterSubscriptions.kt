package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.BillingModel
import org.bhargav.pansariwala.api.CommissionSummaryDto
import org.bhargav.pansariwala.api.ShopSubscriptionDto
import org.bhargav.pansariwala.api.SubscriptionPlanDto
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.MILLIS_PER_DAY
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_billing_apply
import pansariwala.shared.generated.resources.master_billing_commission
import pansariwala.shared.generated.resources.master_billing_defaults
import pansariwala.shared.generated.resources.master_billing_expires
import pansariwala.shared.generated.resources.master_billing_free_until
import pansariwala.shared.generated.resources.master_billing_history
import pansariwala.shared.generated.resources.master_billing_model
import pansariwala.shared.generated.resources.master_billing_note
import pansariwala.shared.generated.resources.master_billing_plan
import pansariwala.shared.generated.resources.master_billing_saved
import pansariwala.shared.generated.resources.master_default_commission
import pansariwala.shared.generated.resources.master_feature_inventory
import pansariwala.shared.generated.resources.master_feature_online
import pansariwala.shared.generated.resources.master_feature_reports
import pansariwala.shared.generated.resources.master_feature_scanner
import pansariwala.shared.generated.resources.master_feature_voice
import pansariwala.shared.generated.resources.master_nav_subscriptions
import pansariwala.shared.generated.resources.master_plan_active
import pansariwala.shared.generated.resources.master_plan_add
import pansariwala.shared.generated.resources.master_plan_delete
import pansariwala.shared.generated.resources.master_plan_description
import pansariwala.shared.generated.resources.master_plan_features
import pansariwala.shared.generated.resources.master_plan_monthly
import pansariwala.shared.generated.resources.master_plan_name
import pansariwala.shared.generated.resources.master_plan_sort
import pansariwala.shared.generated.resources.master_plan_yearly
import pansariwala.shared.generated.resources.master_plans_title
import pansariwala.shared.generated.resources.master_save
import pansariwala.shared.generated.resources.master_shop_billing
import pansariwala.shared.generated.resources.master_warning_days
import pansariwala.shared.generated.resources.sub_active
import pansariwala.shared.generated.resources.sub_commission_summary
import pansariwala.shared.generated.resources.sub_expired
import pansariwala.shared.generated.resources.sub_expires_on
import pansariwala.shared.generated.resources.sub_model_commission
import pansariwala.shared.generated.resources.sub_model_free
import pansariwala.shared.generated.resources.sub_model_monthly
import pansariwala.shared.generated.resources.sub_model_none
import pansariwala.shared.generated.resources.sub_model_yearly
import pansariwala.shared.generated.resources.sub_plan_name

private val PLAN_FEATURES: List<Pair<String, StringResource>> = listOf(
    AppConstants.ShopFeature.BARCODE_SCANNER to Res.string.master_feature_scanner,
    AppConstants.ShopFeature.VOICE_SEARCH to Res.string.master_feature_voice,
    AppConstants.ShopFeature.REPORT_GENERATION to Res.string.master_feature_reports,
    AppConstants.ShopFeature.ONLINE_ORDERS to Res.string.master_feature_online,
    AppConstants.ShopFeature.INVENTORY_ALERTS to Res.string.master_feature_inventory,
)

private fun modelLabel(model: String): StringResource = when (model) {
    BillingModel.MONTHLY -> Res.string.sub_model_monthly
    BillingModel.YEARLY -> Res.string.sub_model_yearly
    BillingModel.COMMISSION -> Res.string.sub_model_commission
    BillingModel.FREE -> Res.string.sub_model_free
    else -> Res.string.sub_model_none
}

private fun Double.plain(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

/** `yyyy-MM-dd` (UTC, matching [formatEpochDate]) → epoch ms at the end of that day. */
private fun parseEpochDateEnd(text: String): Long? {
    val parts = text.trim().split('-').mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return null
    val (y0, m, d) = parts
    if (m !in 1..12 || d !in 1..31) return null
    val y = if (m <= 2) y0 - 1 else y0
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    val days = era.toLong() * 146_097 + doe - 719_468
    return days * MILLIS_PER_DAY + MILLIS_PER_DAY - 1
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubscriptionsScreen(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<SubscriptionPlanDto>>(emptyList()) }
    var commission by remember { mutableStateOf("") }
    var warningDays by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            runCatching {
                plans = api.subscriptionPlans(token)
                val settings = api.billingSettings(token)
                commission = settings.defaultCommissionPercent.plain()
                warningDays = settings.expiryWarningDays.toString()
            }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(title = stringResource(Res.string.master_nav_subscriptions), subtitle = stringResource(Res.string.master_plans_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasterSectionCard(title = stringResource(Res.string.master_billing_defaults)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(commission, { commission = it }, label = { Text(stringResource(Res.string.master_default_commission)) }, singleLine = true)
                    OutlinedTextField(warningDays, { warningDays = it }, label = { Text(stringResource(Res.string.master_warning_days)) }, singleLine = true)
                }
                Button(onClick = {
                    scope.launch {
                        runCatching {
                            api.saveBillingSettings(
                                token,
                                BillingSettingsDto(
                                    defaultCommissionPercent = commission.trim().toDouble(),
                                    expiryWarningDays = warningDays.trim().toInt(),
                                ),
                            )
                        }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_save)) }
            }
            plans.forEach { plan ->
                PlanEditor(
                    plan = plan,
                    onSave = { updated ->
                        scope.launch {
                            runCatching { api.saveSubscriptionPlan(token, updated) }
                                .onSuccess { refresh() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    },
                    onDelete = {
                        scope.launch {
                            runCatching { api.deleteSubscriptionPlan(token, plan.id) }
                                .onSuccess { refresh() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    },
                )
            }
            if (adding) {
                PlanEditor(
                    plan = SubscriptionPlanDto(name = "", features = listOf(AppConstants.ShopFeature.BARCODE_SCANNER), sortOrder = plans.size + 1),
                    onSave = { created ->
                        scope.launch {
                            runCatching { api.saveSubscriptionPlan(token, created) }
                                .onSuccess { adding = false; refresh() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    },
                    onDelete = { adding = false },
                )
            } else {
                OutlinedButton(onClick = { adding = true }) { Text(stringResource(Res.string.master_plan_add)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanEditor(plan: SubscriptionPlanDto, onSave: (SubscriptionPlanDto) -> Unit, onDelete: () -> Unit) {
    var name by remember(plan) { mutableStateOf(plan.name) }
    var description by remember(plan) { mutableStateOf(plan.description) }
    var monthly by remember(plan) { mutableStateOf(plan.monthlyPriceInr.plain()) }
    var yearly by remember(plan) { mutableStateOf(plan.yearlyPriceInr.plain()) }
    var sort by remember(plan) { mutableStateOf(plan.sortOrder.toString()) }
    var active by remember(plan) { mutableStateOf(plan.active) }
    var features by remember(plan) { mutableStateOf(plan.features.toSet()) }
    MasterSectionCard(title = plan.name.ifBlank { stringResource(Res.string.master_plan_add) }) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.master_plan_name)) }, singleLine = true)
            OutlinedTextField(monthly, { monthly = it }, label = { Text(stringResource(Res.string.master_plan_monthly)) }, singleLine = true)
            OutlinedTextField(yearly, { yearly = it }, label = { Text(stringResource(Res.string.master_plan_yearly)) }, singleLine = true)
            OutlinedTextField(sort, { sort = it }, label = { Text(stringResource(Res.string.master_plan_sort)) }, singleLine = true)
        }
        OutlinedTextField(
            description,
            { description = it },
            label = { Text(stringResource(Res.string.master_plan_description)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(Res.string.master_plan_features), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PLAN_FEATURES.forEach { (key, label) ->
                FilterChip(
                    selected = key in features,
                    onClick = { features = if (key in features) features - key else features + key },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        FeatureToggleRow(stringResource(Res.string.master_plan_active), active) { active = !active }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onSave(
                    plan.copy(
                        name = name,
                        description = description,
                        monthlyPriceInr = monthly.trim().toDoubleOrNull() ?: plan.monthlyPriceInr,
                        yearlyPriceInr = yearly.trim().toDoubleOrNull() ?: plan.yearlyPriceInr,
                        sortOrder = sort.trim().toIntOrNull() ?: plan.sortOrder,
                        active = active,
                        features = PLAN_FEATURES.map { it.first }.filter { it in features },
                    ),
                )
            }) { Text(stringResource(Res.string.master_save)) }
            OutlinedButton(onClick = onDelete) { Text(stringResource(Res.string.master_plan_delete)) }
        }
    }
}

/** Shop detail card: assign monthly / yearly / commission / free billing and review the shop's history. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopBillingSection(token: String, shopId: String, onStatus: (String) -> Unit, onChanged: () -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var sub by remember { mutableStateOf<ShopSubscriptionDto?>(null) }
    var allPlans by remember { mutableStateOf<List<SubscriptionPlanDto>>(emptyList()) }
    var commissionSummary by remember { mutableStateOf<CommissionSummaryDto?>(null) }
    val savedLabel = stringResource(Res.string.master_billing_saved)

    var model by remember { mutableStateOf(BillingModel.NONE) }
    var planId by remember { mutableStateOf<String?>(null) }
    var commission by remember { mutableStateOf("") }
    var expires by remember { mutableStateOf("") }
    var freeUntil by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    fun bind(data: ShopSubscriptionDto) {
        sub = data
        val b = data.billing
        model = b.model
        planId = b.planId
        commission = (b.commissionPercent.takeIf { it > 0.0 } ?: data.defaultCommissionPercent).plain()
        expires = b.expiresAtEpochMs?.let(::formatEpochDate).orEmpty()
        freeUntil = b.freeUntilEpochMs?.let(::formatEpochDate).orEmpty()
        note = b.note
    }

    LaunchedEffect(shopId) {
        runCatching {
            bind(api.shopSubscription(token, shopId))
            allPlans = api.subscriptionPlans(token)
            if (sub?.billing?.model == BillingModel.COMMISSION) commissionSummary = api.shopCommission(token, shopId)
        }.onFailure { onStatus(it.message.orEmpty()) }
    }
    val data = sub ?: return
    val billing = data.billing

    MasterSectionCard(title = stringResource(Res.string.master_shop_billing)) {
        Text(stringResource(modelLabel(billing.model)), fontWeight = FontWeight.SemiBold)
        if (billing.planName.isNotBlank()) Text(stringResource(Res.string.sub_plan_name, billing.planName))
        if (billing.model != BillingModel.NONE) {
            Text(
                stringResource(if (billing.active) Res.string.sub_active else Res.string.sub_expired),
                color = if (billing.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        (billing.expiresAtEpochMs ?: billing.freeUntilEpochMs)?.let {
            Text(stringResource(Res.string.sub_expires_on, formatEpochDate(it)))
        }
        commissionSummary?.let {
            Text(
                stringResource(Res.string.sub_commission_summary, formatInr(it.totalSaleInr), formatInr(it.commissionInr), formatInr(it.payoutInr)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(Res.string.master_billing_model), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BillingModel.ALL.forEach { option ->
                FilterChip(selected = model == option, onClick = { model = option }, label = { Text(stringResource(modelLabel(option))) })
            }
        }
        if (model == BillingModel.MONTHLY || model == BillingModel.YEARLY) {
            Text(stringResource(Res.string.master_billing_plan), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allPlans.forEach { plan ->
                    FilterChip(selected = planId == plan.id, onClick = { planId = plan.id }, label = { Text(plan.name) })
                }
            }
            OutlinedTextField(expires, { expires = it }, label = { Text(stringResource(Res.string.master_billing_expires)) }, singleLine = true)
        }
        if (model == BillingModel.COMMISSION) {
            OutlinedTextField(commission, { commission = it }, label = { Text(stringResource(Res.string.master_billing_commission)) }, singleLine = true)
        }
        if (model == BillingModel.FREE) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                allPlans.forEach { plan ->
                    FilterChip(selected = planId == plan.id, onClick = { planId = if (planId == plan.id) null else plan.id }, label = { Text(plan.name) })
                }
            }
            OutlinedTextField(freeUntil, { freeUntil = it }, label = { Text(stringResource(Res.string.master_billing_free_until)) }, singleLine = true)
        }
        OutlinedTextField(note, { note = it }, label = { Text(stringResource(Res.string.master_billing_note)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                runCatching {
                    api.updateShopBilling(
                        token,
                        shopId,
                        AdminBillingUpdate(
                            model = model,
                            planId = planId.takeIf { model in setOf(BillingModel.MONTHLY, BillingModel.YEARLY, BillingModel.FREE) },
                            commissionPercent = commission.trim().toDoubleOrNull(),
                            expiresAtEpochMs = parseEpochDateEnd(expires),
                            freeUntilEpochMs = parseEpochDateEnd(freeUntil),
                            note = note,
                        ),
                    )
                }.onSuccess {
                    bind(it)
                    onStatus(savedLabel)
                    onChanged()
                }.onFailure { onStatus(it.message.orEmpty()) }
            }
        }) { Text(stringResource(Res.string.master_billing_apply)) }

        if (data.transactions.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(Res.string.master_billing_history), style = MaterialTheme.typography.labelLarge)
            data.transactions.take(MAX_HISTORY_ROWS).forEach { txn ->
                Text(
                    listOfNotNull(
                        formatEpochDate(txn.createdAtEpochMs),
                        txn.type,
                        txn.planName.takeIf { it.isNotBlank() },
                        txn.period.takeIf { it.isNotBlank() },
                        txn.amountInr.takeIf { it > 0.0 }?.let { "₹" + formatInr(it) },
                        txn.paymentId,
                        txn.note.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val MAX_HISTORY_ROWS = 20
