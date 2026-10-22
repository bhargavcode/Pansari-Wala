@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.domain.model.ShopFilter
import org.bhargav.pansariwala.domain.model.ShopSortOption
import org.bhargav.pansariwala.domain.model.ShopType
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_apply
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.market_filter_title
import pansariwala.shared.generated.resources.market_radius_range
import pansariwala.shared.generated.resources.market_shop_types
import pansariwala.shared.generated.resources.market_sort_distance_asc
import pansariwala.shared.generated.resources.market_sort_distance_desc
import pansariwala.shared.generated.resources.market_sort_name_asc
import pansariwala.shared.generated.resources.market_sort_name_desc
import pansariwala.shared.generated.resources.market_sort_rating
import pansariwala.shared.generated.resources.settings_radius
import pansariwala.shared.generated.resources.settings_radius_value
import kotlin.math.roundToInt

/** Lower bound of the radius slider, configured by the platform admin. */
suspend fun PansariApi.minSearchRadiusKm(): Double =
    runCatching { publicConfig().minUserSearchRadiusKm }
        .onFailure { it.rethrowIfStructuredCancellation() }
        .getOrDefault(AppConstants.MIN_SEARCH_RADIUS_KM)
        .coerceIn(AppConstants.SEARCH_RADIUS_STEP_KM, AppConstants.MAX_SEARCH_RADIUS_KM - 1)

fun ShopSortOption.labelRes(): StringResource = when (this) {
    ShopSortOption.DISTANCE_ASC -> Res.string.market_sort_distance_asc
    ShopSortOption.DISTANCE_DESC -> Res.string.market_sort_distance_desc
    ShopSortOption.RATING_DESC -> Res.string.market_sort_rating
    ShopSortOption.NAME_ASC -> Res.string.market_sort_name_asc
    ShopSortOption.NAME_DESC -> Res.string.market_sort_name_desc
}

/** Same sheet on Home and More; whatever is applied becomes the app-wide shop filter. */
@Composable
fun ShopFilterSheet(
    initial: ShopFilter,
    minRadiusKm: Double,
    onApply: (ShopFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var radius by remember(initial, minRadiusKm) {
        mutableStateOf(initial.radiusKm.coerceIn(minRadiusKm, AppConstants.MAX_SEARCH_RADIUS_KM).toFloat())
    }
    var sort by remember(initial) { mutableStateOf(initial.sort) }
    var types by remember(initial) { mutableStateOf(initial.shopTypes) }
    val radiusSteps = remember(minRadiusKm) {
        ((AppConstants.MAX_SEARCH_RADIUS_KM - minRadiusKm) / AppConstants.SEARCH_RADIUS_STEP_KM).roundToInt() - 1
    }
    val sortedShopTypes = ShopType.entries.map { it to shopTypeLabel(it) }.sortedBy { it.second.lowercase() }
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.market_filter_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.settings_radius), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    stringResource(Res.string.settings_radius_value, radius.roundToInt().toString()),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                stringResource(
                    Res.string.market_radius_range,
                    minRadiusKm.roundToInt().toString(),
                    AppConstants.MAX_SEARCH_RADIUS_KM.roundToInt().toString(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = radius,
                onValueChange = { radius = it.roundToInt().toFloat() },
                valueRange = minRadiusKm.toFloat()..AppConstants.MAX_SEARCH_RADIUS_KM.toFloat(),
                steps = radiusSteps,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ShopSortOption.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth().clickable { sort = option },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = sort == option, onClick = { sort = option })
                    Text(stringResource(option.labelRes()))
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(Res.string.market_shop_types), fontWeight = FontWeight.SemiBold)
            sortedShopTypes.forEach { (type, label) ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        types = types.toMutableSet().apply { if (contains(type)) remove(type) else add(type) }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = type in types, onCheckedChange = null)
                    Text(label)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    radius = AppConstants.DEFAULT_SEARCH_RADIUS_KM.coerceIn(minRadiusKm, AppConstants.MAX_SEARCH_RADIUS_KM).toFloat()
                    sort = ShopSortOption.DISTANCE_ASC
                    types = emptySet()
                }) { Text(stringResource(Res.string.action_clear)) }
                UserPrimaryButton(
                    text = stringResource(Res.string.action_apply),
                    onClick = { onApply(ShopFilter(radius.toDouble(), types, sort)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
