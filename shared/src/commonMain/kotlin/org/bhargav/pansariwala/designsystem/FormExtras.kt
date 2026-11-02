package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SystemUpdate
import pansariwala.shared.generated.resources.app_update_later
import pansariwala.shared.generated.resources.app_update_message
import pansariwala.shared.generated.resources.app_update_now
import pansariwala.shared.generated.resources.app_update_title
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.scanner.BarcodeScannerPreview
import org.bhargav.pansariwala.scanner.ScannerSettings
import org.bhargav.pansariwala.scanner.barcodeScannerSupported
import org.bhargav.pansariwala.scanner.scanFeedback
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_apply
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.discard_entry_confirm
import pansariwala.shared.generated.resources.discard_entry_keep
import pansariwala.shared.generated.resources.discard_entry_message
import pansariwala.shared.generated.resources.discard_entry_title
import pansariwala.shared.generated.resources.finder_scan_cd
import pansariwala.shared.generated.resources.scanner_drag_to_close
import pansariwala.shared.generated.resources.scanner_resize_cd
import kotlin.math.hypot
import kotlin.math.roundToInt

/** "dd/MM/yyyy" in the device time zone; numeric so it needs no month-name translations. */
fun formatDayMillis(millis: Long): String {
    val d = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${d.dayOfMonth.toString().padStart(2, '0')}/${d.monthNumber.toString().padStart(2, '0')}/${d.year}"
}

/** Read-only field that opens a calendar; [millis] null = unset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    millis: Long?,
    onChange: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var picking by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = millis?.let(::formatDayMillis).orEmpty(),
            onValueChange = {},
            readOnly = true,
            isError = isError,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { picking = true })
    }
    if (!picking) return
    val state = rememberDatePickerState(initialSelectedDateMillis = millis)
    DatePickerDialog(
        onDismissRequest = { picking = false },
        properties = appDialogProperties(),
        confirmButton = {
            TextButton(onClick = {
                onChange(state.selectedDateMillis)
                picking = false
            }) { Text(stringResource(Res.string.action_apply)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onChange(null)
                picking = false
            }) { Text(stringResource(Res.string.action_clear)) }
        },
    ) { DatePicker(state = state) }
}

/**
 * Barcode input: a USB/Bluetooth reader types straight into the field (they act as keyboards and send Enter);
 * without one, the scan icon opens the floating camera.
 */
@Composable
fun BarcodeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    scannerSettings: ScannerSettings = ScannerSettings(),
) {
    var scanning by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        trailingIcon = if (barcodeScannerSupported) {
            {
                IconButton(onClick = { scanning = true }) {
                    Icon(Icons.Default.QrCodeScanner, stringResource(Res.string.finder_scan_cd), tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            null
        },
        modifier = modifier,
    )
    if (scanning) {
        FloatingScannerWindow(
            settings = scannerSettings,
            onBarcode = {
                onValueChange(it)
                scanning = false
            },
            onClose = { scanning = false },
        )
    }
}

private val SCANNER_MIN_WIDTH = 160.dp
private val SCANNER_START_WIDTH = 240.dp
private val CLOSE_TARGET = 64.dp
private const val SCANNER_ASPECT = 3f / 4f
private const val SCANNER_MAX_WIDTH_FRACTION = 0.92f

/**
 * Floating camera that stays above the screen without blocking it. Drag the top bar to move, the corner
 * handle to resize; dropping it on the red target in the middle closes it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FloatingScannerWindow(
    onBarcode: (String) -> Unit,
    onClose: () -> Unit,
    settings: ScannerSettings = ScannerSettings(),
) {
    val density = LocalDensity.current
    val screen = LocalWindowInfo.current.containerSize
    val minW = with(density) { SCANNER_MIN_WIDTH.roundToPx() }
    val maxW = (screen.width * SCANNER_MAX_WIDTH_FRACTION).roundToInt().coerceAtLeast(minW)
    var widthPx by remember { mutableStateOf(with(density) { SCANNER_START_WIDTH.roundToPx() }.coerceIn(minW, maxW)) }
    val heightPx = (widthPx * SCANNER_ASPECT).roundToInt()
    var offset by remember { mutableStateOf(IntOffset((screen.width - widthPx) / 2, screen.height / 6)) }
    var dragging by remember { mutableStateOf(false) }
    val targetPx = with(density) { CLOSE_TARGET.toPx() }
    val latestClose by rememberUpdatedState(onClose)

    fun clamp(o: IntOffset) = IntOffset(
        o.x.coerceIn(0, (screen.width - widthPx).coerceAtLeast(0)),
        o.y.coerceIn(0, (screen.height - heightPx).coerceAtLeast(0)),
    )
    fun overTarget(): Boolean {
        val cx = offset.x + widthPx / 2f
        val cy = offset.y + heightPx / 2f
        return hypot(cx - screen.width / 2f, cy - screen.height / 2f) < targetPx * 1.5f
    }

    if (dragging) {
        Popup(alignment = Alignment.Center) {
            val hot = overTarget()
            Box(
                Modifier
                    .size(if (hot) CLOSE_TARGET * 1.25f else CLOSE_TARGET)
                    .background(Color(0xFFE53935).copy(alpha = if (hot) 1f else 0.85f), CircleShape)
                    .border(3.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Close, stringResource(Res.string.scanner_drag_to_close), tint = Color.White) }
        }
    }
    val provider = remember(offset) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ) = offset
        }
    }
    Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false)) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(with(density) { widthPx.toDp() }, with(density) { (heightPx).toDp() + 32.dp }),
        ) {
            Box {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .size(with(density) { widthPx.toDp() }, with(density) { heightPx.toDp() })
                        .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)),
                ) {
                    BarcodeScannerPreview(
                        settings = settings,
                        active = true,
                        onBarcode = { code ->
                            scanFeedback(settings.beepOnScan, settings.vibrateOnScan)
                            onBarcode(code)
                        },
                        onPermissionDenied = onClose,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .size(with(density) { widthPx.toDp() }, 32.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { dragging = true },
                                onDragEnd = {
                                    val close = overTarget()
                                    dragging = false
                                    if (close) latestClose()
                                },
                                onDragCancel = { dragging = false },
                            ) { change, drag ->
                                change.consume()
                                offset = clamp(offset + IntOffset(drag.x.roundToInt(), drag.y.roundToInt()))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.DragHandle, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)) {
                        Icon(Icons.Default.Close, stringResource(Res.string.scanner_drag_to_close), tint = Color(0xFFE53935))
                    }
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                widthPx = (widthPx + drag.x.roundToInt()).coerceIn(minW, maxW)
                                offset = clamp(offset)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.OpenInFull, stringResource(Res.string.scanner_resize_cd), tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/** "New version available" prompt shown at most every few hours when the store has a newer build. */
@Composable
fun AppUpdateDialog(onUpdate: () -> Unit, onLater: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onLater,
        icon = {
            Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) }
        },
        title = { Text(stringResource(Res.string.app_update_title)) },
        text = { Text(stringResource(Res.string.app_update_message)) },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(Res.string.app_update_now)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(Res.string.app_update_later)) } },
    )
}

/** "Discard this entry?" confirmation shown when leaving a form with unsaved input. */
@Composable
fun DiscardChangesDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(Res.string.discard_entry_title)) },
        text = { Text(stringResource(Res.string.discard_entry_message)) },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(stringResource(Res.string.discard_entry_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text(stringResource(Res.string.discard_entry_keep)) } },
    )
}
