package org.bhargav.pansariwala.master

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_all
import pansariwala.shared.generated.resources.master_app
import pansariwala.shared.generated.resources.master_app_delivery
import pansariwala.shared.generated.resources.master_app_pos
import pansariwala.shared.generated.resources.master_app_user
import pansariwala.shared.generated.resources.master_cancel
import pansariwala.shared.generated.resources.master_contact
import pansariwala.shared.generated.resources.master_issue_attachments
import pansariwala.shared.generated.resources.master_issue_description
import pansariwala.shared.generated.resources.master_issue_fixed_in
import pansariwala.shared.generated.resources.master_issue_fixed_in_hint
import pansariwala.shared.generated.resources.master_issue_none
import pansariwala.shared.generated.resources.master_issue_note
import pansariwala.shared.generated.resources.master_issue_reopen
import pansariwala.shared.generated.resources.master_issue_reported_on
import pansariwala.shared.generated.resources.master_issue_reporter
import pansariwala.shared.generated.resources.master_issue_resolve
import pansariwala.shared.generated.resources.master_issue_resolve_title
import pansariwala.shared.generated.resources.master_issue_resolved_on
import pansariwala.shared.generated.resources.master_issues_open
import pansariwala.shared.generated.resources.master_issues_resolved
import pansariwala.shared.generated.resources.master_issues_title
import pansariwala.shared.generated.resources.master_last_seen
import pansariwala.shared.generated.resources.master_name
import pansariwala.shared.generated.resources.master_pay_card
import pansariwala.shared.generated.resources.master_pay_cash
import pansariwala.shared.generated.resources.master_pay_cashfree
import pansariwala.shared.generated.resources.master_pay_cod
import pansariwala.shared.generated.resources.master_pay_razorpay
import pansariwala.shared.generated.resources.master_pay_upi_direct
import pansariwala.shared.generated.resources.master_pay_upi_pos
import pansariwala.shared.generated.resources.master_payment_gateway
import pansariwala.shared.generated.resources.master_payment_usage
import pansariwala.shared.generated.resources.master_payment_users_orders
import pansariwala.shared.generated.resources.master_platform
import pansariwala.shared.generated.resources.master_total_users
import pansariwala.shared.generated.resources.master_usage_title
import pansariwala.shared.generated.resources.master_users_list
import pansariwala.shared.generated.resources.master_version
import pansariwala.shared.generated.resources.master_versions
import pansariwala.shared.generated.resources.master_version_users

private val APP_FILTERS: List<Pair<String?, StringResource>> = listOf(
    null to Res.string.master_all,
    "POS" to Res.string.master_app_pos,
    "USER" to Res.string.master_app_user,
    "DELIVERY" to Res.string.master_app_delivery,
)

private val PLATFORM_FILTERS: List<String> = listOf(
    AppConstants.Push.PLATFORM_ANDROID,
    AppConstants.Push.PLATFORM_IOS,
    AppConstants.Support.PLATFORM_WEB,
)

/** `OrderDoc.paymentMethod` values (online gateways first, then POS tender types). */
private val PAYMENT_METHODS: List<Pair<String, StringResource>> = listOf(
    "ONLINE" to Res.string.master_pay_razorpay,
    AppConstants.Payment.METHOD_CASHFREE to Res.string.master_pay_cashfree,
    AppConstants.Payment.METHOD_UPI_DIRECT to Res.string.master_pay_upi_direct,
    "COD" to Res.string.master_pay_cod,
    AppConstants.PosPayment.CASH to Res.string.master_pay_cash,
    AppConstants.PosPayment.UPI to Res.string.master_pay_upi_pos,
    AppConstants.PosPayment.CARD to Res.string.master_pay_card,
)

@Composable
private fun paymentLabel(method: String): String =
    PAYMENT_METHODS.firstOrNull { it.first == method }?.let { stringResource(it.second) } ?: method

@Composable
private fun appLabel(app: String): String =
    APP_FILTERS.firstOrNull { it.first == app }?.let { stringResource(it.second) } ?: app

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppUsageScreen(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    var app by rememberSaveable { mutableStateOf<String?>(null) }
    var platform by rememberSaveable { mutableStateOf<String?>(null) }
    var version by rememberSaveable { mutableStateOf<String?>(null) }
    var method by rememberSaveable { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf(AppUsageReportDto()) }
    LaunchedEffect(token, app, platform, version, method) {
        runCatching { report = api.appUsage(token, app, platform, version, method) }
            .onFailure { onStatus(it.message.orEmpty()) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(stringResource(Res.string.master_usage_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasterSectionCard {
                FilterLine(stringResource(Res.string.master_app)) {
                    APP_FILTERS.forEach { (value, label) ->
                        FilterChip(
                            selected = app == value,
                            onClick = { app = value; version = null },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                FilterLine(stringResource(Res.string.master_platform)) {
                    FilterChip(selected = platform == null, onClick = { platform = null }, label = { Text(stringResource(Res.string.master_all)) })
                    PLATFORM_FILTERS.forEach { p ->
                        FilterChip(selected = platform == p, onClick = { platform = p; version = null }, label = { Text(p) })
                    }
                }
                FilterLine(stringResource(Res.string.master_version)) {
                    FilterChip(selected = version == null, onClick = { version = null }, label = { Text(stringResource(Res.string.master_all)) })
                    report.versions.map { it.versionName }.distinct().forEach { v ->
                        FilterChip(selected = version == v, onClick = { version = v }, label = { Text(v.ifBlank { "—" }) })
                    }
                }
                FilterLine(stringResource(Res.string.master_payment_gateway)) {
                    FilterChip(selected = method == null, onClick = { method = null }, label = { Text(stringResource(Res.string.master_all)) })
                    PAYMENT_METHODS.forEach { (value, label) ->
                        FilterChip(selected = method == value, onClick = { method = value }, label = { Text(stringResource(label)) })
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_total_users), "${report.totalUsers}")
                report.paymentMethods.forEach { usage ->
                    MasterStatCard(
                        title = paymentLabel(usage.method),
                        value = stringResource(Res.string.master_payment_users_orders, usage.users, usage.orders),
                        onClick = { method = usage.method },
                    )
                }
            }
            MasterSectionCard(title = stringResource(Res.string.master_versions)) {
                ResponsiveTable(columns = 4) {
                    TableHeader(
                        stringResource(Res.string.master_app),
                        stringResource(Res.string.master_platform),
                        stringResource(Res.string.master_version),
                        stringResource(Res.string.master_version_users),
                    )
                    report.versions.forEach { v ->
                        TableRow(selected = version == v.versionName, onClick = { version = v.versionName }) {
                            CellText(appLabel(v.app))
                            CellText(v.platform)
                            CellText(if (v.versionCode > 0) "${v.versionName} (${v.versionCode})" else v.versionName)
                            CellText("${v.users}")
                        }
                    }
                }
            }
            if (report.paymentMethods.isNotEmpty()) {
                MasterSectionCard(title = stringResource(Res.string.master_payment_usage)) {
                    ResponsiveTable(columns = 3) {
                        TableHeader(
                            stringResource(Res.string.master_payment_gateway),
                            stringResource(Res.string.master_version_users),
                            stringResource(Res.string.master_payment_usage),
                        )
                        report.paymentMethods.forEach { usage ->
                            TableRow(selected = method == usage.method, onClick = { method = usage.method }) {
                                CellText(paymentLabel(usage.method))
                                CellText("${usage.users}")
                                CellText("${usage.orders}")
                            }
                        }
                    }
                }
            }
            MasterSectionCard(title = stringResource(Res.string.master_users_list)) {
                ResponsiveTable(columns = 7) {
                    TableHeader(
                        stringResource(Res.string.master_name),
                        stringResource(Res.string.master_contact),
                        stringResource(Res.string.master_app),
                        stringResource(Res.string.master_platform),
                        stringResource(Res.string.master_version),
                        stringResource(Res.string.master_payment_gateway),
                        stringResource(Res.string.master_last_seen),
                    )
                    report.users.forEach { u ->
                        TableRow {
                            CellText(u.name.ifBlank { u.principalId })
                            CellText(u.phone)
                            CellText(appLabel(u.app))
                            CellText(u.platform)
                            CellText(u.versionName)
                            CellText(u.paymentMethods.map { paymentLabel(it) }.joinToString(", "))
                            CellText(formatEpochDate(u.lastSeenAt))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterLine(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 12.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IssuesScreen(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var resolvedTab by rememberSaveable { mutableStateOf(false) }
    var app by rememberSaveable { mutableStateOf<String?>(null) }
    var issues by remember { mutableStateOf<List<AdminIssueDto>>(emptyList()) }
    var resolving by remember { mutableStateOf<AdminIssueDto?>(null) }
    val status = if (resolvedTab) AppConstants.Support.STATUS_RESOLVED else AppConstants.Support.STATUS_OPEN
    fun refresh() {
        scope.launch {
            runCatching { issues = api.issues(token, status, app) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token, resolvedTab, app) { refresh() }

    resolving?.let { issue ->
        ResolveIssueDialog(
            onDismiss = { resolving = null },
            onConfirm = { fixedIn, note ->
                scope.launch {
                    runCatching { api.resolveIssue(token, issue.id, IssueResolveBody(fixedIn, note)) }
                        .onSuccess { resolving = null; refresh() }
                        .onFailure { onStatus(it.message.orEmpty()) }
                }
            },
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(stringResource(Res.string.master_issues_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TabRow(selectedTabIndex = if (resolvedTab) 1 else 0) {
                Tab(selected = !resolvedTab, onClick = { resolvedTab = false }, text = { Text(stringResource(Res.string.master_issues_open)) })
                Tab(selected = resolvedTab, onClick = { resolvedTab = true }, text = { Text(stringResource(Res.string.master_issues_resolved)) })
            }
            FilterLine(stringResource(Res.string.master_app)) {
                APP_FILTERS.forEach { (value, label) ->
                    FilterChip(selected = app == value, onClick = { app = value }, label = { Text(stringResource(label)) })
                }
            }
            if (issues.isEmpty()) {
                Text(
                    stringResource(Res.string.master_issue_none),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }
            issues.forEach { issue ->
                IssueCard(
                    issue = issue,
                    onResolve = { resolving = issue },
                    onReopen = {
                        scope.launch {
                            runCatching { api.reopenIssue(token, issue.id) }
                                .onSuccess { refresh() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueCard(issue: AdminIssueDto, onResolve: () -> Unit, onReopen: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val resolved = issue.status == AppConstants.Support.STATUS_RESOLVED
    MasterSectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${appLabel(issue.app)} · ${issue.featureLabel.ifBlank { issue.feature }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(issue.issueLabel.ifBlank { issue.issueType }, style = MaterialTheme.typography.bodyLarge)
            }
            if (resolved) {
                OutlinedButton(onClick = onReopen) { Text(stringResource(Res.string.master_issue_reopen)) }
            } else {
                Button(onClick = onResolve) { Text(stringResource(Res.string.master_issue_resolve)) }
            }
        }
        if (issue.description.isNotBlank()) {
            LabelValue(stringResource(Res.string.master_issue_description), issue.description)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            LabelValue(
                stringResource(Res.string.master_issue_reporter),
                listOf(issue.reporterName, issue.reporterPhone, issue.reporterRole).filter { it.isNotBlank() }.joinToString(" · "),
            )
            LabelValue(stringResource(Res.string.master_version), "${issue.appVersion} (${issue.platform})")
            LabelValue(stringResource(Res.string.master_issue_reported_on), formatEpochDate(issue.createdAt))
            if (resolved) {
                LabelValue(
                    stringResource(Res.string.master_issue_resolved_on),
                    listOfNotNull(
                        issue.resolvedAt?.let(::formatEpochDate),
                        issue.fixedInVersion.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                )
            }
        }
        if (issue.resolutionNote.isNotBlank()) {
            LabelValue(stringResource(Res.string.master_issue_note), issue.resolutionNote)
        }
        if (issue.attachments.isNotEmpty()) {
            Text(
                stringResource(Res.string.master_issue_attachments),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                issue.attachments.forEach { url ->
                    NetworkImage(
                        url = url,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { uriHandler.openUri(url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolveIssueDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var fixedIn by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.master_issue_resolve_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fixedIn,
                    onValueChange = { fixedIn = it.take(32) },
                    label = { Text(stringResource(Res.string.master_issue_fixed_in)) },
                    placeholder = { Text(stringResource(Res.string.master_issue_fixed_in_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(Res.string.master_issue_note)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(fixedIn.trim(), note.trim()) }) {
                Text(stringResource(Res.string.master_issue_resolve))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.master_cancel)) } },
    )
}
