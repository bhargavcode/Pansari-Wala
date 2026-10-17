package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.api.CustomerRatingDto
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.customer_ratings_by_partner
import pansariwala.shared.generated.resources.customer_ratings_empty
import pansariwala.shared.generated.resources.customer_ratings_end
import pansariwala.shared.generated.resources.customer_ratings_filter_all
import pansariwala.shared.generated.resources.customer_ratings_filter_stars
import pansariwala.shared.generated.resources.customer_ratings_summary
import pansariwala.shared.generated.resources.customer_ratings_title

@Composable
fun CustomerRatingsScreen(
    customerId: String,
    customerName: String,
    onBack: () -> Unit,
    viewModel: CustomerRatingsViewModel = koinViewModel(),
) {
    LaunchedEffect(customerId) { viewModel.start(customerId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - AppConstants.LIST_PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, state.hasMore, state.items.size) {
        if (nearEnd && state.hasMore) viewModel.loadNextPage()
    }

    PansariScreen(
        title = customerName.ifBlank { stringResource(Res.string.customer_ratings_title) },
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::retry, onDismiss = viewModel::dismissError) },
        isLoading = state.loadingInitial && state.items.isEmpty(),
    ) {
        Column(Modifier.fillMaxSize()) {
            if (state.ratingCount > 0) {
                Text(
                    stringResource(Res.string.customer_ratings_summary, formatRating(state.average), state.ratingCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.selectedStars.isEmpty(),
                    onClick = viewModel::clearFilter,
                    label = { Text(stringResource(Res.string.customer_ratings_filter_all)) },
                )
                (5 downTo 1).forEach { stars ->
                    FilterChip(
                        selected = stars in state.selectedStars,
                        onClick = { viewModel.toggleStars(stars) },
                        label = {
                            Text(stringResource(Res.string.customer_ratings_filter_stars, stars, state.starCounts[stars] ?: 0))
                        },
                    )
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.orderId }, contentType = { "rating" }) { rating ->
                    CustomerRatingRow(rating)
                }
                when {
                    state.loadingMore -> item(key = "loading_more", contentType = "footer") {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    state.items.isEmpty() && !state.loadingInitial && state.error == null ->
                        item(key = "empty", contentType = "footer") {
                            Text(
                                stringResource(Res.string.customer_ratings_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    !state.hasMore && state.items.isNotEmpty() -> item(key = "end", contentType = "footer") {
                        Text(
                            stringResource(Res.string.customer_ratings_end),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerRatingRow(rating: CustomerRatingDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "★".repeat(rating.stars) + "☆".repeat(5 - rating.stars),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    formatPrintDateTime(rating.createdAtEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            rating.comment?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                listOfNotNull(
                    rating.partnerName.takeIf { it.isNotBlank() }
                        ?.let { stringResource(Res.string.customer_ratings_by_partner, it) },
                    rating.orderId,
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
