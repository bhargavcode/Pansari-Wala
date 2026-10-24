package org.bhargav.pansariwala.navigation

/** One-shot start destinations set before [org.bhargav.pansariwala.App] composes (web deep links, reminder taps). */
object LaunchIntent {
    /** Open shop registration on top of login instead of stopping at login. */
    var openShopRegistration: Boolean = false

    fun consumeShopRegistration(): Boolean = openShopRegistration.also { openShopRegistration = false }
}
