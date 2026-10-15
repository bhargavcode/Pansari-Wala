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

fun webSessionGet(key: String): String? = window.sessionStorage.getItem(key)
fun webSessionSet(key: String, value: String) = window.sessionStorage.setItem(key, value)
fun webSessionRemove(key: String) = window.sessionStorage.removeItem(key)

fun isLocalWebHost(): Boolean = window.location.hostname.let { it == "localhost" || it == "127.0.0.1" }
