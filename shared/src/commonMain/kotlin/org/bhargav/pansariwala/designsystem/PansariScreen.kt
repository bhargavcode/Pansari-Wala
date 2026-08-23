package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.ErrorBannerAction
import org.bhargav.pansariwala.ui.ErrorBannerState
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_okay
import pansariwala.shared.generated.resources.action_retry

fun handleErrorBannerAction(
    action: ErrorBannerAction,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (action) {
        ErrorBannerAction.Retry -> onRetry()
        ErrorBannerAction.Close, ErrorBannerAction.Okay -> onDismiss()
    }
}

@Composable
fun PansariErrorBanner(
    state: ErrorBannerState,
    onAction: (ErrorBannerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.message.asString(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
            state.actions.forEach { action ->
                when (action) {
                    ErrorBannerAction.Close -> IconButton(
                        onClick = { onAction(action) },
                    ) {
                        Text(
                            text = "\u2715",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    ErrorBannerAction.Okay -> TextButton(onClick = { onAction(action) }) {
                        Text(stringResource(Res.string.action_okay))
                    }
                    ErrorBannerAction.Retry -> TextButton(onClick = { onAction(action) }) {
                        Text(stringResource(Res.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
fun PansariScreen(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    topBar: @Composable (() -> Unit)? = null,
    error: ErrorBannerState? = null,
    onErrorAction: (ErrorBannerAction) -> Unit = {},
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(Modifier.fillMaxSize()) {
            error?.let { banner ->
                PansariErrorBanner(state = banner, onAction = onErrorAction)
            }
            when {
                topBar != null -> topBar()
                title != null -> PansariTopBar(title = title, onBack = onBack, actions = actions)
            }
            if (isRefreshing && !isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                content()
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
