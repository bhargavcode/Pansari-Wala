package org.bhargav.pansariwala.feature.catalog

import org.bhargav.pansariwala.designsystem.AppAlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.api.ProductImportReportDto
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.sheet_apply
import pansariwala.shared.generated.resources.sheet_checking
import pansariwala.shared.generated.resources.sheet_choose_file
import pansariwala.shared.generated.resources.sheet_counts
import pansariwala.shared.generated.resources.sheet_done
import pansariwala.shared.generated.resources.sheet_done_title
import pansariwala.shared.generated.resources.sheet_download_sample
import pansariwala.shared.generated.resources.sheet_error_row
import pansariwala.shared.generated.resources.sheet_errors_title
import pansariwala.shared.generated.resources.sheet_export_current
import pansariwala.shared.generated.resources.sheet_import_hint
import pansariwala.shared.generated.resources.sheet_import_title
import pansariwala.shared.generated.resources.sheet_preview_title

/** [report] with dryRun = preview of [fileName]; without = finished import. */
data class SheetImportUiState(
    val busy: Boolean = false,
    val fileName: String? = null,
    val report: ProductImportReportDto? = null,
    val error: UiText? = null,
)

private const val MAX_ERRORS_SHOWN = 50

/** Shared by shop inventory (POS) and the Master admin: sample, export, pick → preview → import. */
@Composable
fun ProductSheetImportDialog(
    state: SheetImportUiState,
    onDownloadSample: () -> Unit,
    onExport: () -> Unit,
    onPickFile: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val report = state.report
    val title = when {
        report == null -> stringResource(Res.string.sheet_import_title)
        report.dryRun -> stringResource(Res.string.sheet_preview_title, state.fileName.orEmpty())
        else -> stringResource(Res.string.sheet_done_title)
    }
    AppAlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    state.fileName?.let { Text(stringResource(Res.string.sheet_checking, it)) }
                }
                state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
                if (report == null) {
                    Text(stringResource(Res.string.sheet_import_hint), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onDownloadSample, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.sheet_download_sample))
                    }
                    OutlinedButton(onClick = onExport, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.sheet_export_current))
                    }
                } else {
                    Text(
                        stringResource(
                            Res.string.sheet_counts,
                            report.productsCreated,
                            report.productsUpdated,
                            report.variantsCreated,
                            report.variantsUpdated,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (report.errors.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            stringResource(Res.string.sheet_errors_title, report.errors.size),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                        report.errors.take(MAX_ERRORS_SHOWN).forEach { err ->
                            Text(stringResource(Res.string.sheet_error_row, err.row, err.message), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                report == null -> TextButton(onClick = onPickFile, enabled = !state.busy) {
                    Text(stringResource(Res.string.sheet_choose_file))
                }
                report.dryRun -> TextButton(
                    onClick = onApply,
                    enabled = !state.busy && (report.productsCreated + report.productsUpdated) > 0,
                ) { Text(stringResource(Res.string.sheet_apply)) }
                else -> TextButton(onClick = onDismiss) { Text(stringResource(Res.string.sheet_done)) }
            }
        },
        dismissButton = {
            if (report?.dryRun != false) {
                TextButton(onClick = onDismiss, enabled = !state.busy) { Text(stringResource(Res.string.action_cancel)) }
            }
        },
    )
}
