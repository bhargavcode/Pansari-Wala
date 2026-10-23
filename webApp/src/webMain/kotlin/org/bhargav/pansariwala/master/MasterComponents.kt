package org.bhargav.pansariwala.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_filter_custom
import pansariwala.shared.generated.resources.master_filter_monthly
import pansariwala.shared.generated.resources.master_filter_today
import pansariwala.shared.generated.resources.master_filter_weekly
import pansariwala.shared.generated.resources.master_filter_yearly
import pansariwala.shared.generated.resources.master_filter_yesterday
import pansariwala.shared.generated.resources.master_active
import pansariwala.shared.generated.resources.master_inactive
import pansariwala.shared.generated.resources.master_off
import pansariwala.shared.generated.resources.master_on

@Composable
fun MasterStatCard(
    title: String,
    value: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    MasterCard(
        modifier = modifier
            .widthIn(min = 180.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) { Text(actionLabel) }
            }
        }
    }
}

/** Flat bordered surface every master card is built on. */
@Composable
fun MasterCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppConstants.MasterWeb.CARD_RADIUS_DP.dp)
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content,
    )
}

@Composable
fun MasterSectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    MasterCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = {
            if (title != null || trailing != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    trailing?.invoke(this)
                }
            }
            content()
        })
    }
}

@Composable
fun StatusChip(active: Boolean) {
    val color = (if (active) MasterAction.ACTIVATE else MasterAction.DELETE).color()
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(
            text = stringResource(if (active) Res.string.master_active else Res.string.master_inactive),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun OnOffToggle(active: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(
            checked = active,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MasterAction.ACTIVATE.color(),
                checkedThumbColor = MaterialTheme.colorScheme.surface,
            ),
        )
        Text(
            stringResource(if (active) Res.string.master_on else Res.string.master_off),
            color = (if (active) MasterAction.ACTIVATE else MasterAction.NEUTRAL).color(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun DateFilterBar(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        AppConstants.DateFilter.TODAY to Res.string.master_filter_today,
        AppConstants.DateFilter.YESTERDAY to Res.string.master_filter_yesterday,
        AppConstants.DateFilter.WEEKLY to Res.string.master_filter_weekly,
        AppConstants.DateFilter.MONTHLY to Res.string.master_filter_monthly,
        AppConstants.DateFilter.YEARLY to Res.string.master_filter_yearly,
        AppConstants.DateFilter.CUSTOM to Res.string.master_filter_custom,
    )
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(id) },
                label = { Text(stringResource(label)) },
            )
        }
    }
}

private val TableMinColumnWidth = 130.dp

/**
 * Weighted table cells need a bounded width; a bare `horizontalScroll` gives infinite width and
 * collapses every weighted cell to zero. Fills the container, or scrolls when [columns] need more room.
 */
@Composable
fun ResponsiveTable(
    columns: Int,
    modifier: Modifier = Modifier,
    minWidth: Dp = TableMinColumnWidth * columns,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val tableWidth = if (maxWidth > minWidth) maxWidth else minWidth
        Column(
            Modifier.horizontalScroll(rememberScrollState()).width(tableWidth),
            content = content,
        )
    }
}

@Composable
fun RowScope.Cell(weight: Float = 1f, content: @Composable () -> Unit) {
    Box(Modifier.weight(weight), contentAlignment = Alignment.CenterStart) { content() }
}

@Composable
fun TableHeader(vararg labels: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        labels.forEach {
            Text(
                it.uppercase(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TableRow(
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val bg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        hovered && onClick != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else -> Color.Transparent
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(bg)
                .hoverable(interaction)
                .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
fun RowScope.CellText(text: String, weight: Float = 1f, emphasis: Boolean = false) {
    Text(
        text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Normal,
        color = if (emphasis) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun PageHeaderBar(title: String, trailing: @Composable (() -> Unit)? = null) {
    MasterHeaderSurface {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = AppConstants.MasterWeb.CONTENT_PADDING_DP.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            trailing?.invoke()
            MasterThemeMenu(tint = LocalContentColor.current)
        }
    }
}

/** Brand gradient band behind every page title. */
@Composable
fun MasterHeaderSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val start = if (dark) scheme.primaryContainer else scheme.primary
    val end = if (dark) scheme.tertiaryContainer else scheme.tertiary
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(start, end))),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (dark) scheme.onPrimaryContainer else scheme.onPrimary,
        ) { content() }
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
