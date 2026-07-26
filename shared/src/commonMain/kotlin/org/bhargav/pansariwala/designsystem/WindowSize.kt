package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun AdaptivePane(
    modifier: Modifier = Modifier,
    content: @Composable (WindowWidthClass) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthClass.Compact
            maxWidth < 840.dp -> WindowWidthClass.Medium
            else -> WindowWidthClass.Expanded
        }
        content(widthClass)
    }
}
