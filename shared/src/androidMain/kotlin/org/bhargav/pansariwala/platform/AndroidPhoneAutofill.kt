package org.bhargav.pansariwala.platform

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val KEY_PHONE_HINT = "pansari_phone_hint"
private const val KEY_SMS_CONSENT = "pansari_sms_consent"

class AndroidPhoneAutofill(private val context: Context) : PhoneAutofill {

    override suspend fun requestPhoneNumber(): String? {
        val client = Identity.getSignInClient(context)
        val pending = client.getPhoneNumberHintIntent(GetPhoneNumberHintIntentRequest.builder().build()).awaitOrNull()
            ?: return null
        val result = ActivityResultBridge.launch(
            KEY_PHONE_HINT,
            ActivityResultContracts.StartIntentSenderForResult(),
            IntentSenderRequest.Builder(pending.intentSender).build(),
        ) ?: return null
        if (result.resultCode != Activity.RESULT_OK) return null
        val raw = runCatching { client.getPhoneNumberFromIntent(result.data) }.getOrNull() ?: return null
        return normalizeLocalPhone(raw)
    }

    override suspend fun awaitOtp(length: Int): String? {
        SmsRetriever.getClient(context).startSmsUserConsent(null).awaitOrNull() ?: return null
        val consent = awaitConsentIntent() ?: return null
        val result = ActivityResultBridge.launch(
            KEY_SMS_CONSENT,
            ActivityResultContracts.StartActivityForResult(),
            consent,
        ) ?: return null
        if (result.resultCode != Activity.RESULT_OK) return null
        val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return null
        return extractOtp(message, length)
    }

    /** One SMS (or the 5-minute timeout) per [SmsRetriever.startSmsUserConsent] call. */
    private suspend fun awaitConsentIntent(): Intent? = suspendCancellableCoroutine { cont ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                runCatching { context.unregisterReceiver(this) }
                val status = IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_STATUS, Status::class.java)
                val consent = if (status?.statusCode == CommonStatusCodes.SUCCESS) {
                    IntentCompat.getParcelableExtra(intent, SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                } else {
                    null
                }
                if (cont.isActive) cont.resume(consent)
            }
        }
        val filter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        val registered = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, filter, SmsRetriever.SEND_PERMISSION, null, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, filter, SmsRetriever.SEND_PERMISSION, null)
            }
        }.isSuccess
        if (!registered) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
    }
}

private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resume(null) }
    addOnCanceledListener { if (cont.isActive) cont.resume(null) }
}
