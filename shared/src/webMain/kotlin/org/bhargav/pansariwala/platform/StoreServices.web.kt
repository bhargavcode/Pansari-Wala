package org.bhargav.pansariwala.platform

actual fun requestInAppReview() = Unit

actual suspend fun isStoreUpdateAvailable(): Boolean = false

actual fun startStoreUpdate() = Unit
