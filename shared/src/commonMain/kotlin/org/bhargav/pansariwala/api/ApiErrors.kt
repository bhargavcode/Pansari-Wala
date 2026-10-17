package org.bhargav.pansariwala.api

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import org.bhargav.pansariwala.i18n.UiText
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_network
import pansariwala.shared.generated.resources.error_request_timeout
import pansariwala.shared.generated.resources.error_server
import pansariwala.shared.generated.resources.error_server_unavailable
import pansariwala.shared.generated.resources.error_session_expired

/**
 * Non-2xx API response. [serverMessage] is the server's `{"error": "..."}` text; the exception
 * message keeps `HTTP <status>` so existing status checks on `message` keep working.
 */
class ApiException(
    val status: Int,
    val serverMessage: String,
    val code: String? = null,
    val retryAfterSec: Int? = null,
) : Exception("HTTP $status: $serverMessage")

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
    val status = (this as? ApiException)?.status
    return when {
        this is HttpRequestTimeoutException ||
            message.contains("timeout", ignoreCase = true) ->
            UiText.res(Res.string.error_request_timeout)
        status == 401 ||
            message.contains("401") || message.contains("Unauthorized", ignoreCase = true) ->
            UiText.res(Res.string.error_session_expired)
        status in GATEWAY_STATUSES -> UiText.res(Res.string.error_server_unavailable)
        status != null && status >= 500 -> UiText.res(Res.string.error_server)
        // Response arrived but could not be decoded (e.g. HTML error page or schema mismatch).
        this is SerializationException || this is ContentConvertException ||
            this is NoTransformationFoundException -> UiText.res(Res.string.error_server)
        else -> UiText.res(Res.string.error_network)
    }
}

private val GATEWAY_STATUSES = setOf(502, 503, 504)
