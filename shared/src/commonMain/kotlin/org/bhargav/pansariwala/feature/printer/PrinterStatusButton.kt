package org.bhargav.pansariwala.feature.printer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrintDisabled
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.printer.PrinterManager
import org.bhargav.pansariwala.printer.model.PrinterConnectionState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.printer_icon_cd

/** Toolbar printer icon with the live connection status underneath. */
@Composable
fun PrinterStatusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    printerManager: PrinterManager = koinInject(),
) {
    val state by printerManager.connectionState.collectAsStateWithLifecycle()
    val connected = state is PrinterConnectionState.Connected
    val tint = when (state) {
        is PrinterConnectionState.Connected -> MaterialTheme.colorScheme.primary
        is PrinterConnectionState.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (connected || state is PrinterConnectionState.Connecting) Icons.Filled.Print else Icons.Filled.PrintDisabled,
                contentDescription = stringResource(Res.string.printer_icon_cd),
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            if (state is PrinterConnectionState.Connecting) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
        }
        Text(
            text = state.statusLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
