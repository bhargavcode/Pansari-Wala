package org.bhargav.pansariwala.platform

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

@Composable
actual fun ApplySystemBarsFromTheme() {
    val activity = AndroidActivityHolder.activity ?: return
    val window = activity.window ?: return
    val primary = MaterialTheme.colorScheme.primary
    val useDarkIcons = primary.luminance() > 0.5f

    SideEffect {
        val color = primary.toArgb()
        window.statusBarColor = color
        window.navigationBarColor = color
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}
