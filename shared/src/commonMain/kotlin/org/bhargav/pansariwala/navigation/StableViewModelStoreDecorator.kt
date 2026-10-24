package org.bhargav.pansariwala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.defaultViewModelCreationExtras
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreProvider
import androidx.lifecycle.viewmodel.navigation3.ViewModelStoreNavEntryDecorator

/**
 * Use instead of `rememberViewModelStoreNavEntryDecorator()`: the library re-reads the activity's default
 * creation extras on every recomposition, and `ComponentActivity` returns a fresh copy each time (its intent
 * extras `Bundle` only equals itself). When launched with intent extras, any nav-graph recomposition (e.g.
 * rotation) then swaps the store provider and the old one clears every screen's ViewModels.
 */
@Composable
fun <T : Any> rememberStableViewModelStoreNavEntryDecorator(): ViewModelStoreNavEntryDecorator<T> {
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
