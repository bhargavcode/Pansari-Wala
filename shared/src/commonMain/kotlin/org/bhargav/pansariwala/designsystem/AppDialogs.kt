package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_close

/** Dialogs and sheets never close on outside tap, back press or swipe; only via an explicit action. */
fun appDialogProperties(usePlatformDefaultWidth: Boolean = true) = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
    usePlatformDefaultWidth = usePlatformDefaultWidth,
)

@Composable
fun DialogCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClose, modifier = modifier) {
        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_close))
    }
}

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    usePlatformDefaultWidth: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { title?.invoke() }
                DialogCloseButton(onDismissRequest)
            }
        },
        text = text,
        properties = appDialogProperties(usePlatformDefaultWidth),
    )
}

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    usePlatformDefaultWidth: Boolean = true,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = appDialogProperties(usePlatformDefaultWidth),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp)) {
                DialogCloseButton(
                    onClose = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() } },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false, shouldDismissOnClickOutside = false),
        content = content,
    )
}
