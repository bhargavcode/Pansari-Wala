package org.bhargav.pansariwala.api

import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import org.bhargav.pansariwala.i18n.UiText
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_network
import pansariwala.shared.generated.resources.error_request_timeout
import pansariwala.shared.generated.resources.error_session_expired

fun Throwable.rethrowIfStructuredCancellation() {
    if (this is CancellationException && this !is HttpRequestTimeoutException) throw this
}

fun Throwable.toApiUiText(): UiText {
    val message = this.message.orEmpty()
    return when {
        this is HttpRequestTimeoutException ||
            message.contains("timeout", ignoreCase = true) ->
            UiText.res(Res.string.error_request_timeout)
        message.contains("401") || message.contains("Unauthorized", ignoreCase = true) ->
            UiText.res(Res.string.error_session_expired)
        else -> UiText.res(Res.string.error_network)
    }
}
