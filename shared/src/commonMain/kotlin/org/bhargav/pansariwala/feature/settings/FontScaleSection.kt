package org.bhargav.pansariwala.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.settings.FontScale
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.font_size_default
import pansariwala.shared.generated.resources.font_size_extra_large
import pansariwala.shared.generated.resources.font_size_large
import pansariwala.shared.generated.resources.font_size_small
import pansariwala.shared.generated.resources.settings_font_size
import pansariwala.shared.generated.resources.settings_font_size_hint
import pansariwala.shared.generated.resources.settings_font_size_preview

/** App-wide text size; applied through the theme typography so every screen follows it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FontScaleSection(selected: FontScale, onSelect: (FontScale) -> Unit) {
    SectionCard(title = stringResource(Res.string.settings_font_size)) {
        Text(
            stringResource(Res.string.settings_font_size_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            FontScale.entries.forEach { scale ->
                FilterChip(
                    selected = scale == selected,
                    onClick = { onSelect(scale) },
                    label = { Text(stringResource(scale.labelRes())) },
                )
            }
        }
        Text(stringResource(Res.string.settings_font_size_preview), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun FontScale.labelRes() = when (this) {
    FontScale.SMALL -> Res.string.font_size_small
    FontScale.DEFAULT -> Res.string.font_size_default
    FontScale.LARGE -> Res.string.font_size_large
    FontScale.EXTRA_LARGE -> Res.string.font_size_extra_large
}
