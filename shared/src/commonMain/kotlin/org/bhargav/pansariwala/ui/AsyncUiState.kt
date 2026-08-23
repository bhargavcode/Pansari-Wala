package org.bhargav.pansariwala.ui

import org.bhargav.pansariwala.i18n.UiText

enum class ErrorBannerAction {
    Close,
    Okay,
    Retry,
}

data class ErrorBannerState(
    val message: UiText,
    val actions: List<ErrorBannerAction> = listOf(ErrorBannerAction.Close),
) {
    companion object {
        fun retryable(message: UiText) = ErrorBannerState(
            message = message,
            actions = listOf(ErrorBannerAction.Retry, ErrorBannerAction.Close),
        )

        fun ack(message: UiText) = ErrorBannerState(
            message = message,
            actions = listOf(ErrorBannerAction.Okay),
        )
    }
}

fun UiText?.toErrorBanner(retryable: Boolean = true): ErrorBannerState? = this?.let {
    if (retryable) ErrorBannerState.retryable(it) else ErrorBannerState.ack(it)
}

fun String?.toErrorBanner(retryable: Boolean = true): ErrorBannerState? =
    this?.takeIf { it.isNotBlank() }?.let { UiText.Plain(it).toErrorBanner(retryable) }

sealed interface AsyncUiState<out T> {
    data object Idle : AsyncUiState<Nothing>
    data object Loading : AsyncUiState<Nothing>
    data class Error(
        val message: UiText,
        val actions: List<ErrorBannerAction> = listOf(
            ErrorBannerAction.Retry,
            ErrorBannerAction.Close,
        ),
    ) : AsyncUiState<Nothing>

    data class Success<T>(
        val data: T,
        val isRefreshing: Boolean = false,
        val bannerError: UiText? = null,
    ) : AsyncUiState<T>
}

fun AsyncUiState<*>.errorBannerOrNull(): ErrorBannerState? = when (this) {
    is AsyncUiState.Error -> ErrorBannerState(message, actions)
    is AsyncUiState.Success -> bannerError.toErrorBanner(retryable = true)
    AsyncUiState.Idle, AsyncUiState.Loading -> null
}

fun AsyncUiState<*>.isBlockingLoad(): Boolean =
    this is AsyncUiState.Idle || this is AsyncUiState.Loading

fun AsyncUiState<*>.isRefreshing(): Boolean =
    this is AsyncUiState.Success && isRefreshing

fun <T> AsyncUiState<T>.beginLoad(): AsyncUiState<T> = when (this) {
    is AsyncUiState.Success -> copy(isRefreshing = true, bannerError = null)
    else -> AsyncUiState.Loading
}
