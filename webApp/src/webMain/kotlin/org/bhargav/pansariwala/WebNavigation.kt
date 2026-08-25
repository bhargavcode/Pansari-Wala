@file:Suppress("unused")

package org.bhargav.pansariwala

import kotlinx.browser.window

fun jsPathname(): String = window.location.pathname

fun jsNavigate(path: String) {
    window.history.pushState(null, "", path)
    window.location.reload()
}

fun navigateToUserApp() = jsNavigate("/app")
fun navigateToMaster() = jsNavigate("/master")
fun navigateToLanding() = jsNavigate("/")

fun webStorageGet(key: String): String? = window.localStorage.getItem(key)
fun webStorageSet(key: String, value: String) = window.localStorage.setItem(key, value)
fun webStorageRemove(key: String) = window.localStorage.removeItem(key)
