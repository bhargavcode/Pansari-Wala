package org.bhargav.pansariwala.feature.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.bhargav.pansariwala.designsystem.PansariScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.app_name
import pansariwala.shared.generated.resources.app_tagline
import pansariwala.shared.generated.resources.splash_step_checking_session
import pansariwala.shared.generated.resources.splash_step_loading_profile
import pansariwala.shared.generated.resources.splash_step_opening
import pansariwala.shared.generated.resources.splash_step_preparing_data

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCompleteProfile: () -> Unit = onNavigateToLogin,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val destination by viewModel.destination.collectAsState()
    val step by viewModel.step.collectAsState()
    val slow by viewModel.slow.collectAsState()

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Login -> onNavigateToLogin()
            SplashDestination.Home -> onNavigateToHome()
            SplashDestination.CompleteProfile -> onNavigateToCompleteProfile()
            null -> Unit
        }
    }

    PansariScreen {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(Res.string.app_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AnimatedVisibility(
            visible = slow,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                text = stringResource(
                    when (step) {
                        SplashStep.PREPARING_DATA -> Res.string.splash_step_preparing_data
                        SplashStep.CHECKING_SESSION -> Res.string.splash_step_checking_session
                        SplashStep.LOADING_PROFILE -> Res.string.splash_step_loading_profile
                        SplashStep.OPENING -> Res.string.splash_step_opening
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
        }
    }
}
