package org.bhargav.pansariwala.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication

private const val LOOKUP_URL = "https://itunes.apple.com/lookup?country=in&bundleId="

private var storeUrl: String? = null

actual fun requestInAppReview() {
    @Suppress("DEPRECATION")
    SKStoreReviewController.requestReview()
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun isStoreUpdateAvailable(): Boolean = withContext(Dispatchers.IO) {
    val bundle = NSBundle.mainBundle
    val bundleId = bundle.bundleIdentifier ?: return@withContext false
    val installed = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: return@withContext false
    val url = NSURL.URLWithString(LOOKUP_URL + bundleId) ?: return@withContext false
    val body = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null) ?: return@withContext false
    runCatching {
        val result = Json.parseToJsonElement(body).jsonObject["results"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: return@runCatching false
        val latest = result["version"]?.jsonPrimitive?.contentOrNull ?: return@runCatching false
        storeUrl = result["trackViewUrl"]?.jsonPrimitive?.contentOrNull
        isNewer(latest, installed)
    }.getOrDefault(false)
}

actual fun startStoreUpdate() {
    val url = storeUrl?.let(NSURL::URLWithString) ?: return
    UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
}

private fun isNewer(latest: String, installed: String): Boolean {
    val a = latest.split('.').map { it.toIntOrNull() ?: 0 }
    val b = installed.split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}
