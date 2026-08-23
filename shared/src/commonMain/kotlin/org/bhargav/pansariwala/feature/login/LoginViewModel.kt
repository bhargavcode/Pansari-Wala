package org.bhargav.pansariwala.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.domain.auth.LoginCredentials
import org.bhargav.pansariwala.domain.auth.LoginUseCase
import org.bhargav.pansariwala.i18n.UiText
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_forgot_password_later
import pansariwala.shared.generated.resources.error_invalid_credentials
import pansariwala.shared.generated.resources.error_login_failed
import pansariwala.shared.generated.resources.error_login_validation

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val isPasswordVisible: Boolean = false,
)

sealed interface LoginEvent {
    data object Success : LoginEvent
}

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val analytics: Analytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<LoginEvent?>(null)
    val events: StateFlow<LoginEvent?> = _events.asStateFlow()

    fun onIdentifierChange(value: String) {
        _uiState.update { it.copy(identifier = value, errorMessage = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onForgotPasswordClick() {
        analytics.log(
            AnalyticsEvent.PopOpened(
                popId = "forgot_password",
                screen = "login",
            ),
        )
        _uiState.update {
            it.copy(errorMessage = UiText.res(Res.string.error_forgot_password_later))
        }
    }

    fun onLoginClick() {
        analytics.log(
            AnalyticsEvent.ButtonClicked(
                buttonId = "login_submit",
                screen = "login",
            ),
        )
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = loginUseCase(
                LoginCredentials(
                    identifier = state.identifier,
                    password = state.password,
                ),
            )
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.value = LoginEvent.Success
                },
                onFailure = { error ->
                    analytics.log(
                        AnalyticsEvent.Error(
                            code = "login_ui_error",
                            message = error.message.orEmpty(),
                            payload = mapOf("screen" to "login"),
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapLoginError(error),
                        )
                    }
                },
            )
        }
    }

    fun consumeEvent() {
        _events.value = null
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun mapLoginError(error: Throwable): UiText = when (error.message) {
        "Invalid username or password." -> UiText.res(Res.string.error_invalid_credentials)
        "Enter a valid phone/email and password (min 4 chars)." ->
            UiText.res(Res.string.error_login_validation)
        else -> error.message?.let { UiText.Plain(it) } ?: UiText.res(Res.string.error_login_failed)
    }
}
