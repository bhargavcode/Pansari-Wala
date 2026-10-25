package org.bhargav.pansariwala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator

private class RemountRetainer(val provider: ViewModelStoreProvider) {
    val values = mutableMapOf<String, Any>()
}

private val LocalRemountRetainer = staticCompositionLocalOf<RemountRetainer?> { null }

/**
 * Keeps nav back stacks and screen ViewModels alive while [content] is remounted (the app locale switch
 * re-keys the whole tree). Without it a language change reset navigation to Splash and cleared screen
 * ViewModels mid-load, leaving home stuck on its spinner.
 */
@Composable
fun RetainAcrossRemount(content: @Composable () -> Unit) {
    val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner in composition" }
    val extras = remember(owner) { owner.defaultViewModelCreationExtras }
    val factory = remember(owner) { owner.defaultViewModelProviderFactory }
    val provider = rememberViewModelStoreProvider(parent = owner, defaultCreationExtras = extras, defaultFactory = factory)
    val retainer = remember(provider) { RemountRetainer(provider) }
    CompositionLocalProvider(LocalRemountRetainer provides retainer, content = content)
}

/** Returns the instance stored under [id] by an earlier mount, else [fresh] (which is then stored). */
@Composable
fun <T : Any> retainedAcrossRemount(id: String, fresh: T): T {
    val retainer = LocalRemountRetainer.current ?: return fresh
    @Suppress("UNCHECKED_CAST")
    return remember(retainer) { retainer.values.getOrPut(id) { fresh } as T }
}

/**
 * Use instead of `rememberViewModelStoreNavEntryDecorator()`: the library re-reads the activity's default
 * creation extras on every recomposition, and `ComponentActivity` returns a fresh copy each time (its intent
 * extras `Bundle` only equals itself). When launched with intent extras, any nav-graph recomposition (e.g.
 * rotation) then swaps the store provider and the old one clears every screen's ViewModels.
 */
@Composable
fun <T : Any> rememberStableViewModelStoreNavEntryDecorator(): ViewModelStoreNavEntryDecorator<T> {
    LocalRemountRetainer.current?.let { retainer ->
        return remember(retainer) { ViewModelStoreNavEntryDecorator(retainer.provider) }
    }
    val owner = checkNotNull(LocalViewModelStoreOwner.current) { "No ViewModelStoreOwner in composition" }
    val extras = remember(owner) { owner.defaultViewModelCreationExtras }
    val factory = remember(owner) { owner.defaultViewModelProviderFactory }
    val provider = rememberViewModelStoreProvider(
        parent = owner,
        defaultCreationExtras = extras,
        defaultFactory = factory,
    )
    return remember(provider) { ViewModelStoreNavEntryDecorator(provider) }
}
