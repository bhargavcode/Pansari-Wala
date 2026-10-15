package org.bhargav.pansariwala.api

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import org.bhargav.pansariwala.i18n.UiText
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_network
import pansariwala.shared.generated.resources.error_request_timeout
import pansariwala.shared.generated.resources.error_session_expired

/**
 * Non-2xx API response. [serverMessage] is the server's `{"error": "..."}` text; the exception
 * message keeps `HTTP <status>` so existing status checks on `message` keep working.
 */
class ApiException(val status: Int, val serverMessage: String) :
    Exception("HTTP $status: $serverMessage")

fun Throwable.rethrowIfStructuredCancellation() {
    if (this is CancellationException && this !is HttpRequestTimeoutException) throw this
}

/** Server validation text for 4xx (except 401), else null. */
fun Throwable.userFacingServerMessage(): String? =
    (this as? ApiException)
        ?.takeIf { it.status in 400..499 && it.status != 401 }
        ?.serverMessage
        ?.takeIf { it.isNotBlank() }

fun Throwable.toApiUiText(): UiText {
    val message = this.message.orEmpty()
    userFacingServerMessage()?.let { return UiText.Plain(it) }
    return when {
        this is HttpRequestTimeoutException ||
            message.contains("timeout", ignoreCase = true) ->
            UiText.res(Res.string.error_request_timeout)
        (this as? ApiException)?.status == 401 ||
            message.contains("401") || message.contains("Unauthorized", ignoreCase = true) ->
            UiText.res(Res.string.error_session_expired)
        else -> UiText.res(Res.string.error_network)
    }
}
