package org.bhargav.pansariwala.feature.user

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.bhargav.pansariwala.i18n.LocalAppLocale
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.cart_limit_reached

/** Snackbar shown whenever the cart capped an item at its shop-set per-order limit. */
@Composable
fun CartLimitSnackbar(modifier: Modifier = Modifier, cart: CartStore = koinInject()) {
    val host = remember { SnackbarHostState() }
    val language = LocalAppLocale.current
    LaunchedEffect(cart, language) {
        cart.limitReached.collect { product ->
            host.currentSnackbarData?.dismiss()
            val limit = product.maxPerOrder.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
            host.showSnackbar(getString(Res.string.cart_limit_reached, product.displayName(language), limit))
        }
    }
    SnackbarHost(host, modifier)
}
