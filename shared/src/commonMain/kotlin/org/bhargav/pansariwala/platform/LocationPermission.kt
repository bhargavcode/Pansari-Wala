package org.bhargav.pansariwala.platform

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_no
import pansariwala.shared.generated.resources.location_permission_denied_message
import pansariwala.shared.generated.resources.location_permission_denied_title
import pansariwala.shared.generated.resources.location_permission_open_settings
import pansariwala.shared.generated.resources.location_permission_retry
import pansariwala.shared.generated.resources.location_permission_web_hint

/**
 * Requests platform location permission when [trigger] is true.
 * [onResult] is invoked with true when location access may proceed.
 */
@Composable
expect fun RequestLocationPermission(
    trigger: Boolean,
    onConsumed: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
)

expect fun canOpenLocationSettings(): Boolean

expect fun openAppLocationSettings()

@Composable
fun LocationPermissionDeniedDialog(
    visible: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    message: String? = null,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title ?: stringResource(Res.string.location_permission_denied_title)) },
        text = {
            Text(
                buildString {
                    append(message ?: stringResource(Res.string.location_permission_denied_message))
                    if (!canOpenLocationSettings()) {
                        append("\n\n")
                        append(stringResource(Res.string.location_permission_web_hint))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(Res.string.location_permission_retry))
            }
        },
        dismissButton = {
            if (canOpenLocationSettings()) {
                TextButton(onClick = onOpenSettings) {
                    Text(stringResource(Res.string.location_permission_open_settings))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_no))
                }
            }
        },
    )
}
