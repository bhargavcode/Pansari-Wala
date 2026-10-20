package org.bhargav.pansariwala.feature.support

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.api.IssueReportDto
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.media.rememberImageSourceLauncher
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.issue_action_report
import pansariwala.shared.generated.resources.issue_attach
import pansariwala.shared.generated.resources.issue_attachments_count
import pansariwala.shared.generated.resources.issue_change_feature
import pansariwala.shared.generated.resources.issue_description_hint
import pansariwala.shared.generated.resources.issue_description_label
import pansariwala.shared.generated.resources.issue_empty_closed
import pansariwala.shared.generated.resources.issue_empty_open
import pansariwala.shared.generated.resources.issue_fixed_in
import pansariwala.shared.generated.resources.issue_fixed_update_hint
import pansariwala.shared.generated.resources.issue_my_issues
import pansariwala.shared.generated.resources.issue_pick_feature
import pansariwala.shared.generated.resources.issue_pick_type
import pansariwala.shared.generated.resources.issue_remove_attachment
import pansariwala.shared.generated.resources.issue_reported_on
import pansariwala.shared.generated.resources.issue_resolution_note
import pansariwala.shared.generated.resources.issue_sheet_title
import pansariwala.shared.generated.resources.issue_status_open
import pansariwala.shared.generated.resources.issue_status_resolved
import pansariwala.shared.generated.resources.issue_submit
import pansariwala.shared.generated.resources.issue_tab_closed
import pansariwala.shared.generated.resources.issue_tab_open

/** Home-bar icon on every app; opens the report / my-issues bottom sheet. */
@Composable
fun ReportIssueButton(modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = modifier) {
        Icon(Icons.Outlined.ReportProblem, contentDescription = stringResource(Res.string.issue_action_report))
    }
    if (open) IssueReportSheet(onDismiss = { open = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueReportSheet(
    onDismiss: () -> Unit,
    viewModel: IssueReportViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppBottomSheet(onDismissRequest = {
        viewModel.reset()
        onDismiss()
    }) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        if (state.mode == IssueSheetMode.REPORT) Res.string.issue_sheet_title else Res.string.issue_my_issues,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (state.mode == IssueSheetMode.REPORT) {
                    TextButton(onClick = { viewModel.showMode(IssueSheetMode.LIST) }) {
                        Text(stringResource(Res.string.issue_my_issues))
                    }
                } else {
                    TextButton(onClick = { viewModel.showMode(IssueSheetMode.REPORT) }) {
                        Text(stringResource(Res.string.issue_action_report))
                    }
                }
            }
            state.message?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            state.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            when (state.mode) {
                IssueSheetMode.REPORT -> ReportForm(state, viewModel)
                IssueSheetMode.LIST -> IssueList(state, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportForm(state: IssueReportUiState, viewModel: IssueReportViewModel) {
    val feature = state.feature
    if (feature == null) {
        Text(stringResource(Res.string.issue_pick_feature), style = MaterialTheme.typography.titleSmall)
        LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.features, key = { it.name }) { f ->
                OutlinedCard(
                    onClick = { viewModel.selectFeature(f) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(f.label),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        return
    }
    val launchPicker = rememberImageSourceLauncher(ImageUploadFeature.ISSUE_ATTACHMENT, viewModel::attach)
    Column(
        Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::clearFeature) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.issue_change_feature))
            }
            Text(
                stringResource(feature.label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(stringResource(Res.string.issue_pick_type), style = MaterialTheme.typography.titleSmall)
        Column {
            feature.types.forEach { type ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.issueType == type,
                            onClick = { viewModel.selectType(type) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = state.issueType == type, onClick = null)
                    Text(stringResource(type.label), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::setDescription,
            label = { Text(stringResource(Res.string.issue_description_label)) },
            placeholder = { Text(stringResource(Res.string.issue_description_hint)) },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = launchPicker, enabled = state.canAttach) {
                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                Text(stringResource(Res.string.issue_attach), modifier = Modifier.padding(start = 6.dp))
            }
            if (state.uploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
                stringResource(Res.string.issue_attachments_count, state.attachments.size, AppConstants.Support.MAX_ATTACHMENTS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (state.attachments.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.attachments.forEachIndexed { index, attachment ->
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))) {
                        NetworkImage(
                            url = attachment.url,
                            localPreviewBytes = attachment.previewBytes,
                            modifier = Modifier.size(72.dp),
                        )
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(Res.string.issue_remove_attachment),
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { viewModel.removeAttachment(index) },
                        )
                    }
                }
            }
        }
        Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (state.submitting) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(stringResource(Res.string.issue_submit))
            }
        }
    }
}

@Composable
private fun IssueList(state: IssueReportUiState, viewModel: IssueReportViewModel) {
    TabRow(selectedTabIndex = state.tab.ordinal) {
        IssueTab.entries.forEach { tab ->
            Tab(
                selected = state.tab == tab,
                onClick = { viewModel.selectTab(tab) },
                text = {
                    Text(stringResource(if (tab == IssueTab.OPEN) Res.string.issue_tab_open else Res.string.issue_tab_closed))
                },
            )
        }
    }
    when {
        state.loadingIssues -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.issues.isEmpty() -> Text(
            stringResource(if (state.tab == IssueTab.OPEN) Res.string.issue_empty_open else Res.string.issue_empty_closed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
        )
        else -> LazyColumn(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.issues, key = { it.id }) { IssueRow(it) }
        }
    }
}

@Composable
private fun IssueRow(issue: IssueReportDto) {
    val feature = IssueFeature.entries.firstOrNull { it.name == issue.feature }
    val type = IssueType.entries.firstOrNull { it.name == issue.issueType }
    val resolved = issue.status == AppConstants.Support.STATUS_RESOLVED
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    feature?.let { stringResource(it.label) } ?: issue.featureLabel.ifBlank { issue.feature },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = resolved,
                    onClick = {},
                    label = { Text(stringResource(if (resolved) Res.string.issue_status_resolved else Res.string.issue_status_open)) },
                )
            }
            Text(
                type?.let { stringResource(it.label) } ?: issue.issueLabel.ifBlank { issue.issueType },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (issue.description.isNotBlank()) {
                Text(issue.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(Res.string.issue_reported_on, formatPrintDateTime(issue.createdAt), issue.appVersion),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (resolved) {
                if (issue.fixedInVersion.isNotBlank()) {
                    Text(
                        stringResource(Res.string.issue_fixed_in, issue.fixedInVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(Res.string.issue_fixed_update_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (issue.resolutionNote.isNotBlank()) {
                    Text(
                        stringResource(Res.string.issue_resolution_note, issue.resolutionNote),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
