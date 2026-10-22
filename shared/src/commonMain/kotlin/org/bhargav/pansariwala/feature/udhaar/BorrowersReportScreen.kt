package org.bhargav.pansariwala.feature.udhaar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.api.BorrowerDto
import org.bhargav.pansariwala.api.BorrowerReportDto
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.feature.user.SpendBarChart
import org.bhargav.pansariwala.feature.user.SpendDonutChart
import org.bhargav.pansariwala.feature.user.SpendSlice
import org.bhargav.pansariwala.printing.formatPrintDate
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.pos_more_borrowers_title
import pansariwala.shared.generated.resources.udhaar_borrower_count
import pansariwala.shared.generated.resources.udhaar_borrower_meta
import pansariwala.shared.generated.resources.udhaar_daily_title
import pansariwala.shared.generated.resources.udhaar_due
import pansariwala.shared.generated.resources.udhaar_empty
import pansariwala.shared.generated.resources.udhaar_given_in_range
import pansariwala.shared.generated.resources.udhaar_list_title
import pansariwala.shared.generated.resources.udhaar_load_more
import pansariwala.shared.generated.resources.udhaar_others
import pansariwala.shared.generated.resources.udhaar_range_30d
import pansariwala.shared.generated.resources.udhaar_range_7d
import pansariwala.shared.generated.resources.udhaar_range_90d
import pansariwala.shared.generated.resources.udhaar_range_today
import pansariwala.shared.generated.resources.udhaar_range_year
import pansariwala.shared.generated.resources.udhaar_repaid_in_range
import pansariwala.shared.generated.resources.udhaar_search
import pansariwala.shared.generated.resources.udhaar_share_title
import pansariwala.shared.generated.resources.udhaar_sort_credit
import pansariwala.shared.generated.resources.udhaar_sort_name
import pansariwala.shared.generated.resources.udhaar_sort_outstanding
import pansariwala.shared.generated.resources.udhaar_sort_recent
import pansariwala.shared.generated.resources.udhaar_total_due
import kotlin.time.Instant

private const val OTHERS_KEY = "others"
private const val MIN_SLICE = 0.005

val UdhaarRange.labelRes: StringResource
    get() = when (this) {
        UdhaarRange.TODAY -> Res.string.udhaar_range_today
        UdhaarRange.WEEK -> Res.string.udhaar_range_7d
        UdhaarRange.MONTH -> Res.string.udhaar_range_30d
        UdhaarRange.QUARTER -> Res.string.udhaar_range_90d
        UdhaarRange.YEAR -> Res.string.udhaar_range_year
    }

private val sortOptions = listOf(
    AppConstants.Udhaar.SORT_OUTSTANDING to Res.string.udhaar_sort_outstanding,
    AppConstants.Udhaar.SORT_RECENT to Res.string.udhaar_sort_recent,
    AppConstants.Udhaar.SORT_CREDIT to Res.string.udhaar_sort_credit,
    AppConstants.Udhaar.SORT_NAME to Res.string.udhaar_sort_name,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BorrowersReportScreen(
    onBack: () -> Unit,
    onOpenBorrower: (String) -> Unit,
    viewModel: BorrowersViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        title = stringResource(Res.string.pos_more_borrowers_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && state.report == null,
    ) {
        PullToRefreshBox(
            isRefreshing = state.loading && state.report != null,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        UdhaarRange.entries.forEach { range ->
                            FilterChip(
                                selected = state.range == range,
                                onClick = { viewModel.setRange(range) },
                                label = { Text(stringResource(range.labelRes)) },
                            )
                        }
                    }
                }
                state.report?.let { report ->
                    item { UdhaarSummaryCard(report, onOpenBorrower) }
                }
                item {
                    Text(
                        stringResource(Res.string.udhaar_list_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        placeholder = { Text(stringResource(Res.string.udhaar_search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sortOptions.forEach { (key, label) ->
                            FilterChip(
                                selected = state.sort == key,
                                onClick = { viewModel.setSort(key) },
                                label = { Text(stringResource(label)) },
                            )
                        }
                    }
                }
                if (state.borrowers.isEmpty() && !state.loading) {
                    item {
                        Text(
                            stringResource(Res.string.udhaar_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(state.borrowers, key = { it.phone }) { borrower ->
                    BorrowerRow(borrower, onClick = { onOpenBorrower(borrower.phone) })
                }
                if (state.hasMore) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (state.loadingMore) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            } else {
                                OutlinedButton(onClick = viewModel::loadMore) { Text(stringResource(Res.string.udhaar_load_more)) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun UdhaarSummaryCard(report: BorrowerReportDto, onOpenBorrower: (String) -> Unit) {
    val othersLabel = stringResource(Res.string.udhaar_others)
    val slices = remember(report, othersLabel) {
        val top = report.shares.map { SpendSlice(it.phone, it.name.ifBlank { it.phone }, it.amount) }
        val rest = report.totalOutstanding - top.sumOf { it.amount }
        if (rest > MIN_SLICE) top + SpendSlice(OTHERS_KEY, othersLabel, rest) else top
    }
    val labels = remember(report.days) {
        val tz = TimeZone.currentSystemDefault()
        report.days.map { Instant.fromEpochMilliseconds(it.dayStartMs).toLocalDateTime(tz).date.day.toString() }
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFFC62828), Color(0xFFEF6C00))))
                .padding(16.dp),
        ) {
            Text(stringResource(Res.string.udhaar_total_due), style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(
                report.totalOutstanding.asMoney(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                listOf(
                    stringResource(Res.string.udhaar_borrower_count, report.borrowerCount),
                    stringResource(Res.string.udhaar_given_in_range, report.creditInRange.asMoney()),
                    stringResource(Res.string.udhaar_repaid_in_range, report.repaidInRange.asMoney()),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.udhaar_share_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SpendDonutChart(
                slices = slices,
                onSliceClick = { slice -> if (slice.key != OTHERS_KEY) onOpenBorrower(slice.key) },
            )
            Text(stringResource(Res.string.udhaar_daily_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SpendBarChart(amounts = report.days.map { it.credit }, labels = labels, onBarClick = {})
        }
    }
}

@Composable
private fun BorrowerRow(borrower: BorrowerDto, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BorrowerAvatar(borrower.name.ifBlank { borrower.phone }, borrower.photoUrl, size = 44)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    borrower.name.ifBlank { borrower.phone },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(borrower.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(
                        Res.string.udhaar_borrower_meta,
                        borrower.orderCount,
                        borrower.lastActivityAt.takeIf { it > 0 }?.let(::formatPrintDate).orEmpty(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(Res.string.udhaar_due, borrower.outstanding.asMoney()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (borrower.outstanding > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BorrowerAvatar(name: String, photoUrl: String?, size: Int) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            NetworkImage(url = photoUrl, contentDescription = name, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
