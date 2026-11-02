package org.bhargav.pansariwala.platform

/** Native store review sheet (Play In-App Review / StoreKit). The OS decides whether it actually shows. */
expect fun requestInAppReview()

/** True when the store has a newer build of this app. */
expect suspend fun isStoreUpdateAvailable(): Boolean

/** Play: immediate in-app update flow. iOS: opens the App Store page. */
expect fun startStoreUpdate()
