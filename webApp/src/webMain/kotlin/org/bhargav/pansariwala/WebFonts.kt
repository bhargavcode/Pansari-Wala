package org.bhargav.pansariwala

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import org.bhargav.pansariwala.web.resources.Res
import org.bhargav.pansariwala.web.resources.noto_sans
import org.bhargav.pansariwala.web.resources.noto_sans_bengali
import org.bhargav.pansariwala.web.resources.noto_sans_devanagari
import org.bhargav.pansariwala.web.resources.noto_sans_gurmukhi
import org.bhargav.pansariwala.web.resources.noto_sans_kannada
import org.bhargav.pansariwala.web.resources.noto_sans_math
import org.bhargav.pansariwala.web.resources.noto_sans_symbols2
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

private const val FONT_LOAD_TIMEOUT_MS = 8_000L

/**
 * Compose web draws on a canvas with no browser font fallback, so Indic scripts and symbols
 * (₹ — × … ★ ✓ →) render as blanks unless their fonts are preloaded before any text is laid out.
 * `noto_sans`, `noto_sans_symbols2` and `noto_sans_math` are subset to punctuation/currency/symbol blocks.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun WithScriptFallbackFonts(content: @Composable () -> Unit) {
    val fonts = listOf(
        preloadFont(Res.font.noto_sans),
        preloadFont(Res.font.noto_sans_symbols2),
        preloadFont(Res.font.noto_sans_math),
        preloadFont(Res.font.noto_sans_devanagari),
        preloadFont(Res.font.noto_sans_bengali),
        preloadFont(Res.font.noto_sans_kannada),
        preloadFont(Res.font.noto_sans_gurmukhi),
    ).map { it.value }
    val resolver = LocalFontFamilyResolver.current
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(fonts) {
        if (fonts.all { it != null }) {
            resolver.preload(FontFamily(fonts.filterNotNull()))
            ready = true
        }
    }
    LaunchedEffect(Unit) {
        delay(FONT_LOAD_TIMEOUT_MS)
        ready = true
    }
    if (ready) content()
}
