package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.formatClockTime
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.ShopReview
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.media.isHttpImageUrl
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.catalog_no_ratings
import pansariwala.shared.generated.resources.catalog_rating_filter_all
import pansariwala.shared.generated.resources.catalog_shop_rating_count
import pansariwala.shared.generated.resources.shop_closed
import pansariwala.shared.generated.resources.shop_distance
import pansariwala.shared.generated.resources.shop_open_now
import pansariwala.shared.generated.resources.shop_outside_delivery
import pansariwala.shared.generated.resources.shop_pickup_after_cutoff_message
import pansariwala.shared.generated.resources.shop_pickup_only_message
import pansariwala.shared.generated.resources.shop_rating_cd
import pansariwala.shared.generated.resources.shop_ratings_sheet_title
import kotlin.math.roundToInt

/** Shown on shop cards and the shop page once the shop is past its delivery cutoff. */
@Composable
fun PickupOnlyStrip(modifier: Modifier = Modifier, deliveryUntil: String = "") {
    val cutoff = formatClockTime(deliveryUntil)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WARNING_CONTAINER,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, WARNING_BORDER),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = WARNING_CONTENT,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (cutoff != null) stringResource(Res.string.shop_pickup_after_cutoff_message, cutoff)
                else stringResource(Res.string.shop_pickup_only_message),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = WARNING_CONTENT,
            )
        }
    }
}

/**
 * Shop image (or gradient placeholder) with name, address, distance and a tappable rating chip at the
 * bottom. Shared by the shop page header and the shop list; [footer] renders below the image.
 */
@Composable
fun ShopHeroHeader(
    shop: MarketplaceShop,
    onRatingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    heroHeight: Dp = HERO_HEIGHT,
    onClick: (() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
    ) {
        Box(Modifier.fillMaxWidth().height(heroHeight)) {
            if (isHttpImageUrl(shop.imageUrl.orEmpty())) {
                NetworkImage(url = shop.imageUrl, contentDescription = shop.name, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary, colors.secondary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = colors.onPrimary.copy(alpha = 0.35f),
                        modifier = Modifier.size(96.dp).padding(bottom = 48.dp),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))),
            )
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        shop.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (shop.isOpen) colors.primaryContainer else colors.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            stringResource(if (shop.isOpen) Res.string.shop_open_now else Res.string.shop_closed),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                if (shop.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            shop.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShopRatingChip(shop.rating, shop.ratingCount, onRatingsClick)
                    Text(
                        stringResource(Res.string.shop_distance, shop.distanceKm.let { (it * 10).roundToInt() / 10.0 }.toString()),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    if (shop.isOpen && !shop.deliversToYou) {
                        Text(
                            stringResource(Res.string.shop_outside_delivery),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.errorContainer,
                        )
                    }
                }
            }
        }
        footer?.let { content ->
            Column(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ShopRatingChip(rating: Double, ratingCount: Int, onClick: () -> Unit) {
    val label = ((rating * 10).roundToInt() / 10.0).toString()
    val description = stringResource(Res.string.shop_rating_cd, label, ratingCount)
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = STAR_COLOR, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                "$label ($ratingCount)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopRatingsSheet(
    shop: MarketplaceShop?,
    ratings: List<ShopReview>,
    allRatings: List<ShopReview>,
    loaded: Boolean,
    selectedStars: Set<Int>,
    onToggleStars: (Int) -> Unit,
    onClearFilter: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = SHEET_MAX_HEIGHT)) {
            Text(
                stringResource(Res.string.shop_ratings_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            shop?.let {
                Text(
                    stringResource(
                        Res.string.catalog_shop_rating_count,
                        ((it.rating * 10).roundToInt() / 10.0).toString(),
                        it.ratingCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
            val counts = allRatings.groupingBy { it.stars }.eachCount()
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedStars.isEmpty(),
                    onClick = onClearFilter,
                    label = { Text(stringResource(Res.string.catalog_rating_filter_all)) },
                )
                (5 downTo 1).forEach { stars ->
                    FilterChip(
                        selected = stars in selectedStars,
                        onClick = { onToggleStars(stars) },
                        label = { Text("$stars★ (${counts[stars] ?: 0})") },
                    )
                }
            }
            when {
                !loaded -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp))
                ratings.isEmpty() -> Text(
                    stringResource(Res.string.catalog_no_ratings),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(ratings, key = { it.id }) { review -> ShopReviewCard(review = review) }
                }
            }
        }
    }
}

private val HERO_HEIGHT = 210.dp
private val SHEET_MAX_HEIGHT = 640.dp
private val STAR_COLOR = Color(0xFFFFB300)
private val WARNING_CONTAINER = Color(0xFFFFF3E0)
private val WARNING_BORDER = Color(0xFFFFB74D)
private val WARNING_CONTENT = Color(0xFFB34700)
