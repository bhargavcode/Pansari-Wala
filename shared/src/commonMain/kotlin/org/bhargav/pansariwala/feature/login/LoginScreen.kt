package org.bhargav.pansariwala.feature.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.toErrorBanner
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_hide
import pansariwala.shared.generated.resources.action_show
import pansariwala.shared.generated.resources.app_name
import pansariwala.shared.generated.resources.app_tagline_login
import pansariwala.shared.generated.resources.login_demo_hint
import pansariwala.shared.generated.resources.login_forgot_password
import pansariwala.shared.generated.resources.login_password
import pansariwala.shared.generated.resources.login_sign_in
import pansariwala.shared.generated.resources.login_subtitle
import pansariwala.shared.generated.resources.login_username
import pansariwala.shared.generated.resources.login_welcome

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val event by viewModel.events.collectAsState()

    LaunchedEffect(event) {
        if (event is LoginEvent.Success) {
            viewModel.consumeEvent()
            onLoginSuccess()
        }
    }

    PansariScreen(
        error = uiState.errorMessage.toErrorBanner(retryable = true),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::onLoginClick, onDismiss = viewModel::dismissError)
        },
        isLoading = uiState.isLoading,
    ) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthClass = when {
            maxWidth < 600.dp -> WindowWidthClass.Compact
            maxWidth < 840.dp -> WindowWidthClass.Medium
            else -> WindowWidthClass.Expanded
        }
        when (widthClass) {
            WindowWidthClass.Compact, WindowWidthClass.Medium -> {
                LoginFormColumn(
                    uiState = uiState,
                    viewModel = viewModel,
                    paneModifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                )
            }
            WindowWidthClass.Expanded -> {
                Row(Modifier.fillMaxSize()) {
                    BrandPane(
                        paneModifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    LoginFormColumn(
                        uiState = uiState,
                        viewModel = viewModel,
                        paneModifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(48.dp),
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun BrandPane(paneModifier: Modifier) {
    Box(
        modifier = paneModifier.background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(Res.string.app_tagline_login),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun LoginFormColumn(
    uiState: LoginUiState,
    viewModel: LoginViewModel,
    paneModifier: Modifier,
) {
    Column(
        modifier = paneModifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(Res.string.login_welcome),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp),
            )

            OutlinedTextField(
                value = uiState.identifier,
                onValueChange = viewModel::onIdentifierChange,
                label = { Text(stringResource(Res.string.login_username)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            Box(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(Res.string.login_password)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (uiState.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = viewModel::onTogglePasswordVisibility) {
                        Text(
                            stringResource(
                                if (uiState.isPasswordVisible) Res.string.action_hide
                                else Res.string.action_show,
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.onLoginClick() },
                ),
            )

            TextButton(
                onClick = viewModel::onForgotPasswordClick,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.login_forgot_password))
            }

            Button(
                onClick = viewModel::onLoginClick,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(stringResource(Res.string.login_sign_in))
                }
            }

            Text(
                text = stringResource(Res.string.login_demo_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
