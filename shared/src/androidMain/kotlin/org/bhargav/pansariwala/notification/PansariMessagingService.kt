package org.bhargav.pansariwala.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Server sends data-only messages on Android so this runs in foreground, background and after the
 * process was killed: open screens update from the payload and alerts are built locally (localized).
 */
class PansariMessagingService : FirebaseMessagingService(), KoinComponent {
    private val handler: PushMessageHandler by inject()

    override fun onNewToken(token: String) {
        PushSignals.token.value = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data.isEmpty()) return
        runBlocking { handler.handle(data) }
    }
}
