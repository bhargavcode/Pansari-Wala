package org.bhargav.pansariwala.feature.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.nav_rail_collapse
import pansariwala.shared.generated.resources.nav_rail_expand
import pansariwala.shared.generated.resources.create_order

private val RAIL_EXPANDED = 208.dp
private val RAIL_COLLAPSED = 84.dp

/** One rail destination or shortcut. */
internal class RailItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
    val badge: String? = null,
    val onClick: () -> Unit,
)

/**
 * Landscape replacement for the POS bottom bar: tile buttons (icon + text) on the left.
 * [expanded] = false collapses to icon-only tiles.
 */
@Composable
internal fun PosNavRail(
    expanded: Boolean,
    onToggle: () -> Unit,
    destinations: List<RailItem>,
    shortcuts: List<RailItem>,
    onNewOrder: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(if (expanded) RAIL_EXPANDED else RAIL_COLLAPSED, label = "railWidth")
    Surface(
        modifier = modifier.width(width).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val toggleLabel = stringResource(if (expanded) Res.string.nav_rail_collapse else Res.string.nav_rail_expand)
            IconButton(onClick = onToggle, modifier = Modifier.padding(start = 2.dp)) {
                Icon(if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu, contentDescription = toggleLabel)
            }
            onNewOrder?.let {
                NewOrderTile(expanded, it)
                Spacer(Modifier.height(6.dp))
            }
            destinations.forEach { RailTile(it, expanded) }
            if (shortcuts.isNotEmpty()) {
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
                shortcuts.forEach { RailTile(it, expanded) }
            }
        }
    }
}

@Composable
private fun NewOrderTile(expanded: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
            .clickableTile(onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = stringResource(Res.string.create_order),
            tint = scheme.onPrimary,
            modifier = Modifier.padding(start = if (expanded) 16.dp else 0.dp).size(26.dp),
        )
        if (expanded) {
            Text(
                stringResource(Res.string.create_order),
                color = scheme.onPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
            )
        }
    }
}

@Composable
private fun RailTile(item: RailItem, expanded: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (item.selected) scheme.secondaryContainer else Color.Transparent
    val fg = if (item.selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant
    if (expanded) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .clickableTile(item.onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(item.icon, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
            Text(
                item.label,
                color = fg,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (item.selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            item.badge?.let { Badge(it) }
        }
    } else {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .clickableTile(item.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = item.label, tint = fg, modifier = Modifier.size(24.dp))
            item.badge?.let {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 10.dp)
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.error),
                )
            }
        }
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onError,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.error)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

private fun Modifier.clickableTile(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
