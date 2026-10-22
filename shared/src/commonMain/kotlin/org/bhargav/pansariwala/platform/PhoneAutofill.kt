package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.bhargav.pansariwala.util.AppConstants
import org.koin.compose.koinInject

/** SIM phone-number picker and one-tap OTP SMS reading (Android Play services; no-op elsewhere). */
interface PhoneAutofill {
    /** Shows the system number picker; returns a 10-digit local number or null if dismissed. */
    suspend fun requestPhoneNumber(): String?

    /** Waits for the next OTP SMS (user approves once) and returns its [length]-digit code, or null. */
    suspend fun awaitOtp(length: Int): String?
}

class NoPhoneAutofill : PhoneAutofill {
    override suspend fun requestPhoneNumber(): String? = null
    override suspend fun awaitOtp(length: Int): String? = null
}

/** Offers the SIM number picker once when the phone field is still empty. */
@Composable
fun PhoneNumberHintEffect(currentPhone: String, onPhone: (String) -> Unit) {
    val autofill = koinInject<PhoneAutofill>()
    val latest by rememberUpdatedState(onPhone)
    var asked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (asked || currentPhone.isNotBlank()) return@LaunchedEffect
        asked = true
        runCatching { autofill.requestPhoneNumber() }.getOrNull()?.let(latest)
    }
}

/** Fills (and lets the caller submit) the code from the incoming OTP SMS while [key] is active. */
@Composable
fun OtpAutofillEffect(key: Any?, length: Int = AppConstants.OTP_LENGTH, onCode: (String) -> Unit) {
    val autofill = koinInject<PhoneAutofill>()
    val latest by rememberUpdatedState(onCode)
    LaunchedEffect(key) {
        runCatching { autofill.awaitOtp(length) }.getOrNull()?.let(latest)
    }
}

/** Keeps the last 10 digits of an Indian number ("+91 98765-43210" → "9876543210"). */
fun normalizeLocalPhone(raw: String): String? =
    raw.filter(Char::isDigit).takeLast(AppConstants.PHONE_LOCAL_DIGITS).takeIf { it.length == AppConstants.PHONE_LOCAL_DIGITS }

/** First run of exactly [length] digits in an SMS body. */
fun extractOtp(message: String, length: Int): String? =
    Regex("(?<!\\d)\\d{$length}(?!\\d)").find(message)?.value
