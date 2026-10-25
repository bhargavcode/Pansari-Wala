package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.platform.InterceptPlatformTextInput
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.keyboard_backspace
import pansariwala.shared.generated.resources.keyboard_dock
import pansariwala.shared.generated.resources.keyboard_enter
import pansariwala.shared.generated.resources.keyboard_hide
import pansariwala.shared.generated.resources.keyboard_letters
import pansariwala.shared.generated.resources.keyboard_move
import pansariwala.shared.generated.resources.keyboard_shift
import pansariwala.shared.generated.resources.keyboard_space
import pansariwala.shared.generated.resources.keyboard_symbols
import pansariwala.shared.generated.resources.keyboard_undock
import pansariwala.shared.generated.resources.keyboard_zoom_in
import pansariwala.shared.generated.resources.keyboard_zoom_out
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField

internal enum class KeyPadMode { TEXT, NUMBER, DECIMAL, PHONE }

internal class KeyboardTarget(
    val mode: KeyPadMode,
    val imeAction: ImeAction,
    val text: () -> String,
    val onText: (String) -> Unit,
    val onAction: () -> Unit,
)

/** Routes key presses from the in-app keyboard to the focused field. */
@Stable
class FloatingKeyboardController internal constructor() {
    internal var target: KeyboardTarget? by mutableStateOf(null)
        private set

    /** Window the panel is drawn in; fields in other windows (dialogs, sheets) can't see it. */
    internal var hostWindow: WindowInfo? = null

    internal fun attach(field: KeyboardTarget) {
        target = field
    }

    internal fun detach(field: KeyboardTarget) {
        if (target === field) target = null
    }
}

/** Non-null when text fields should use the in-app floating keyboard instead of the system one. */
val LocalFloatingKeyboard = compositionLocalOf<FloatingKeyboardController?> { null }

/** True on devices where the floating keyboard can be offered (POS phones); drives the settings toggle. */
val LocalFloatingKeyboardEligible = compositionLocalOf { false }

/** True inside dialogs/sheets: they sit above the floating keyboard panel, so fields use the system keyboard. */
val LocalInOverlayWindow = compositionLocalOf { false }

/** The floating keyboard for a field at this point in the UI, or null when it must use the system keyboard. */
@Composable
internal fun currentFloatingKeyboard(): FloatingKeyboardController? {
    val controller = LocalFloatingKeyboard.current ?: return null
    if (LocalInOverlayWindow.current) return null
    return controller.takeIf { it.hostWindow == null || it.hostWindow === LocalWindowInfo.current }
}

private val KEY_HEIGHT = 44.dp
private val KEY_GAP = 5.dp
private val FLOATING_WIDTH = 340.dp
private val NUMPAD_WIDTH = 260.dp
private val FLOATING_MARGIN = 12.dp
private const val KEY_FONT_SP = 19
private const val BRING_INTO_VIEW_DELAY_MS = 120L
private const val REPEAT_DELAY_MS = 400L
private const val REPEAT_INTERVAL_MS = 55L

private val LETTER_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
private val SYMBOL_ROWS = listOf("1234567890", "@#₹&-+()/", "%*\"':;!?")

/**
 * Hosts the floating keyboard over [content]. Docked: full-width at the bottom and [content] shrinks above it.
 * Floating: drag the top bar to move, pinch with two fingers to zoom. Layout state persists in [preferences].
 */
@Composable
fun FloatingKeyboardHost(
    enabled: Boolean,
    preferences: AppPreferences,
    content: @Composable () -> Unit,
) {
    val controller = remember { FloatingKeyboardController() }
    controller.hostWindow = LocalWindowInfo.current
    val active = if (enabled) controller else null
    val savedDocked by preferences.floatingKeyboardDocked.collectAsStateWithLifecycle(initialValue = true)
    val savedScale by preferences.floatingKeyboardScale.collectAsStateWithLifecycle(initialValue = 1f)
    // One state object for the host's lifetime: gesture handlers outlive recompositions, and a state
    // re-created per saved value made a drag persist the stale pre-zoom scale back.
    var docked by remember { mutableStateOf(savedDocked) }
    var scale by remember { mutableFloatStateOf(savedScale) }
    LaunchedEffect(savedDocked) { docked = savedDocked }
    LaunchedEffect(savedScale) {
        scale = savedScale.coerceIn(AppConstants.KEYBOARD_SCALE_MIN, AppConstants.KEYBOARD_SCALE_MAX)
    }
    var container by remember { mutableStateOf(IntSize.Zero) }
    var panel by remember { mutableStateOf(IntSize.Zero) }
    var offset by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val marginPx = with(density) { FLOATING_MARGIN.toPx() }

    fun persist() {
        scope.launch { preferences.setFloatingKeyboardLayout(docked, scale) }
    }
    fun clamp(o: Offset) = Offset(
        o.x.coerceIn(0f, max(0, container.width - panel.width).toFloat()),
        o.y.coerceIn(0f, max(0, container.height - panel.height).toFloat()),
    )
    fun current() = clamp(
        offset ?: Offset(
            (container.width - panel.width) / 2f,
            container.height - panel.height - marginPx,
        ),
    )
    fun zoomBy(factor: Float) {
        scale = (scale * factor).coerceIn(AppConstants.KEYBOARD_SCALE_MIN, AppConstants.KEYBOARD_SCALE_MAX)
    }

    if (!enabled) LaunchedEffect(Unit) { focusManager.clearFocus() }
    val target = active?.target
    val dockedInset = if (target != null && docked) with(density) { panel.height.toDp() } else 0.dp
    CompositionLocalProvider(LocalFloatingKeyboard provides active) {
        Box(Modifier.fillMaxSize().onSizeChanged { container = it }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = dockedInset)
                    .consumeWindowInsets(PaddingValues(bottom = dockedInset)),
            ) { content() }
            if (target != null) {
                NavigationBackHandler(
                    state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                    isBackEnabled = true,
                    onBackCompleted = { focusManager.clearFocus() },
                )
                val placement = if (docked) {
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                } else {
                    val base = if (target.mode == KeyPadMode.TEXT) FLOATING_WIDTH else NUMPAD_WIDTH
                    Modifier
                        .offset { current().let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
                        .width(base * scale)
                }
                KeyboardPanel(
                    target = target,
                    docked = docked,
                    scale = scale,
                    modifier = placement.onSizeChanged { panel = it },
                    onMove = { offset = clamp(current() + it) },
                    onZoom = { factor, pan ->
                        zoomBy(factor)
                        if (!docked) offset = clamp(current() + pan)
                    },
                    onGestureEnd = ::persist,
                    onZoomStep = { up ->
                        scale = (scale + if (up) AppConstants.KEYBOARD_SCALE_STEP else -AppConstants.KEYBOARD_SCALE_STEP)
                            .coerceIn(AppConstants.KEYBOARD_SCALE_MIN, AppConstants.KEYBOARD_SCALE_MAX)
                        persist()
                    },
                    onToggleDock = {
                        docked = !docked
                        offset = null
                        persist()
                    },
                    onHide = { focusManager.clearFocus() },
                )
            }
        }
    }
}

/** Read-only field that takes input from the floating keyboard; the system keyboard never opens. */
@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
internal fun FloatingKeyboardTextField(
    controller: FloatingKeyboardController,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    label: @Composable (() -> Unit)?,
    placeholder: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    prefix: @Composable (() -> Unit)?,
    suffix: @Composable (() -> Unit)?,
    supportingText: @Composable (() -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    colors: TextFieldColors,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val latestValue by rememberUpdatedState(value)
    val latestOnChange by rememberUpdatedState(onValueChange)
    val latestActions by rememberUpdatedState(keyboardActions)
    val imeAction = when {
        keyboardOptions.imeAction != ImeAction.Default -> keyboardOptions.imeAction
        singleLine -> ImeAction.Done
        else -> ImeAction.None
    }.let { if (singleLine && it == ImeAction.None) ImeAction.Done else it }
    val mode = when (keyboardOptions.keyboardType) {
        KeyboardType.Number, KeyboardType.NumberPassword -> KeyPadMode.NUMBER
        KeyboardType.Decimal -> KeyPadMode.DECIMAL
        KeyboardType.Phone -> KeyPadMode.PHONE
        else -> KeyPadMode.TEXT
    }
    val target = remember(controller, mode, imeAction) {
        KeyboardTarget(
            mode = mode,
            imeAction = imeAction,
            text = { latestValue },
            onText = { latestOnChange(it) },
            onAction = { runImeAction(imeAction, latestActions, focusManager) },
        )
    }
    LaunchedEffect(focused, target) {
        if (focused) {
            controller.attach(target)
            delay(BRING_INTO_VIEW_DELAY_MS)
            bringIntoView.bringIntoView()
        } else {
            controller.detach(target)
        }
    }
    DisposableEffect(target) { onDispose { controller.detach(target) } }
    // Editable (so the caret blinks and hardware keys work) but the platform IME session never starts.
    InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
        M3OutlinedTextField(
            value, onValueChange, modifier.bringIntoViewRequester(bringIntoView), true, false, textStyle, label,
            placeholder, leadingIcon, trailingIcon, prefix, suffix, supportingText, isError, visualTransformation,
            keyboardOptions, keyboardActions, singleLine, maxLines, minLines, source, shape, colors,
        )
    }
}

private fun runImeAction(action: ImeAction, actions: KeyboardActions, focusManager: FocusManager) {
    val scope = object : KeyboardActionScope {
        override fun defaultKeyboardAction(imeAction: ImeAction) {
            when (imeAction) {
                ImeAction.Next -> focusManager.moveFocus(FocusDirection.Next)
                ImeAction.Previous -> focusManager.moveFocus(FocusDirection.Previous)
                else -> focusManager.clearFocus()
            }
        }
    }
    val handler = when (action) {
        ImeAction.Done -> actions.onDone
        ImeAction.Go -> actions.onGo
        ImeAction.Search -> actions.onSearch
        ImeAction.Send -> actions.onSend
        ImeAction.Next -> actions.onNext
        ImeAction.Previous -> actions.onPrevious
        else -> null
    }
    if (handler != null) scope.handler() else scope.defaultKeyboardAction(action)
    if (action == ImeAction.Done || action == ImeAction.Go || action == ImeAction.Search || action == ImeAction.Send) {
        focusManager.clearFocus()
    }
}

private enum class Shift { OFF, ONCE, LOCK }

@Composable
private fun KeyboardPanel(
    target: KeyboardTarget,
    docked: Boolean,
    scale: Float,
    modifier: Modifier,
    onMove: (Offset) -> Unit,
    onZoom: (factor: Float, pan: Offset) -> Unit,
    onGestureEnd: () -> Unit,
    onZoomStep: (up: Boolean) -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
) {
    val shape = if (docked) RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp) else RoundedCornerShape(20.dp)
    val latestZoom by rememberUpdatedState(onZoom)
    val latestGestureEnd by rememberUpdatedState(onGestureEnd)
    Surface(
        modifier = modifier.pinchToZoom({ factor, pan -> latestZoom(factor, pan) }, { latestGestureEnd() }),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 16.dp,
    ) {
        Column(
            Modifier
                .then(if (docked) Modifier.navigationBarsPadding() else Modifier)
                .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(KEY_GAP),
        ) {
            ControlBar(docked, onMove, onGestureEnd, onZoomStep, onToggleDock, onHide)
            val keyHeight = KEY_HEIGHT * scale
            if (target.mode == KeyPadMode.TEXT) {
                TextKeys(target, keyHeight, scale)
            } else {
                NumberKeys(target, keyHeight, scale)
            }
        }
    }
}

@Composable
private fun ControlBar(
    docked: Boolean,
    onMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onZoomStep: (up: Boolean) -> Unit,
    onToggleDock: () -> Unit,
    onHide: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val latestMove by rememberUpdatedState(onMove)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .then(
                if (docked) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(onDragEnd = { latestDragEnd() }) { change, drag ->
                            change.consume()
                            latestMove(drag)
                        }
                    }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarButton(Icons.Default.ZoomOut, stringResource(Res.string.keyboard_zoom_out), tint) { onZoomStep(false) }
        BarButton(Icons.Default.ZoomIn, stringResource(Res.string.keyboard_zoom_in), tint) { onZoomStep(true) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (!docked) Icon(Icons.Default.DragHandle, stringResource(Res.string.keyboard_move), tint = tint)
        }
        if (docked) {
            BarButton(Icons.AutoMirrored.Filled.OpenInNew, stringResource(Res.string.keyboard_undock), tint, onToggleDock)
        } else {
            BarButton(Icons.Default.VerticalAlignBottom, stringResource(Res.string.keyboard_dock), tint, onToggleDock)
        }
        BarButton(Icons.Default.KeyboardHide, stringResource(Res.string.keyboard_hide), tint, onHide)
    }
}

@Composable
private fun BarButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun TextKeys(target: KeyboardTarget, keyHeight: Dp, scale: Float) {
    var symbols by remember(target) { mutableStateOf(false) }
    var shift by remember(target) { mutableStateOf(Shift.OFF) }
    val type: (String) -> Unit = { s ->
        target.onText(target.text() + s)
        if (shift == Shift.ONCE) shift = Shift.OFF
    }
    val rows = if (symbols) SYMBOL_ROWS else LETTER_ROWS
    val upper = !symbols && shift != Shift.OFF
    KeyRow {
        rows[0].forEach { c -> CharKey(if (upper) c.uppercaseChar() else c, keyHeight, scale, type) }
    }
    KeyRow {
        if (!symbols) Spacer(Modifier.weight(0.5f))
        rows[1].forEach { c -> CharKey(if (upper) c.uppercaseChar() else c, keyHeight, scale, type) }
        if (!symbols) Spacer(Modifier.weight(0.5f))
    }
    KeyRow {
        if (!symbols) {
            val shiftIcon = if (shift == Shift.LOCK) Icons.Default.KeyboardCapslock else Icons.Default.ArrowUpward
            Key(
                weight = 1.5f,
                height = keyHeight,
                style = if (shift == Shift.OFF) KeyStyle.FUNCTION else KeyStyle.ACTIVE,
                onPress = {
                    shift = when (shift) {
                        Shift.OFF -> Shift.ONCE
                        Shift.ONCE -> Shift.LOCK
                        Shift.LOCK -> Shift.OFF
                    }
                },
            ) { Icon(shiftIcon, stringResource(Res.string.keyboard_shift)) }
        }
        rows[2].forEach { c -> CharKey(if (upper) c.uppercaseChar() else c, keyHeight, scale, type) }
        BackspaceKey(target, keyHeight)
    }
    KeyRow {
        Key(weight = 1.5f, height = keyHeight, style = KeyStyle.FUNCTION, onPress = { symbols = !symbols }) {
            Text(
                stringResource(if (symbols) Res.string.keyboard_letters else Res.string.keyboard_symbols),
                fontSize = (14 * scale).sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        CharKey(',', keyHeight, scale, type)
        val spaceLabel = stringResource(Res.string.keyboard_space)
        Key(weight = 5f, height = keyHeight, onPress = { type(" ") }) {
            Text(spaceLabel, fontSize = (13 * scale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CharKey('.', keyHeight, scale, type)
        ActionKey(target, keyHeight, weight = 1.5f)
    }
}

@Composable
private fun NumberKeys(target: KeyboardTarget, keyHeight: Dp, scale: Float) {
    val type: (String) -> Unit = { s -> target.onText(target.text() + s) }
    listOf("123", "456", "789").forEach { row ->
        KeyRow { row.forEach { c -> CharKey(c, keyHeight * 1.1f, scale, type) } }
    }
    KeyRow {
        when (target.mode) {
            KeyPadMode.DECIMAL -> CharKey('.', keyHeight * 1.1f, scale, type)
            KeyPadMode.PHONE -> CharKey('+', keyHeight * 1.1f, scale, type)
            else -> Spacer(Modifier.weight(1f))
        }
        CharKey('0', keyHeight * 1.1f, scale, type)
        BackspaceKey(target, keyHeight * 1.1f, weight = 1f)
    }
    KeyRow { ActionKey(target, keyHeight, weight = 1f) }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(KEY_GAP), content = content)
}

@Composable
private fun RowScope.CharKey(c: Char, height: Dp, scale: Float, type: (String) -> Unit) {
    Key(weight = 1f, height = height, onPress = { type(c.toString()) }) {
        Text(c.toString(), fontSize = (KEY_FONT_SP * scale).sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RowScope.BackspaceKey(target: KeyboardTarget, height: Dp, weight: Float = 1.5f) {
    Key(
        weight = weight,
        height = height,
        style = KeyStyle.FUNCTION,
        repeat = true,
        onPress = {
            val text = target.text()
            if (text.isNotEmpty()) {
                val drop = if (text.length >= 2 && text.last().isLowSurrogate()) 2 else 1
                target.onText(text.dropLast(drop))
            }
        },
    ) { Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(Res.string.keyboard_backspace)) }
}

@Composable
private fun RowScope.ActionKey(target: KeyboardTarget, height: Dp, weight: Float) {
    val icon = when (target.imeAction) {
        ImeAction.Search -> Icons.Default.Search
        ImeAction.Next -> Icons.AutoMirrored.Filled.ArrowForward
        ImeAction.Previous -> Icons.AutoMirrored.Filled.ArrowBack
        ImeAction.Go, ImeAction.Send -> Icons.AutoMirrored.Filled.Send
        ImeAction.None -> Icons.AutoMirrored.Filled.KeyboardReturn
        else -> Icons.Default.Check
    }
    Key(
        weight = weight,
        height = height,
        style = KeyStyle.ACTION,
        onPress = {
            if (target.imeAction == ImeAction.None) target.onText(target.text() + "\n") else target.onAction()
        },
    ) { Icon(icon, stringResource(Res.string.keyboard_enter)) }
}

private enum class KeyStyle { CHAR, FUNCTION, ACTIVE, ACTION }

@Composable
private fun RowScope.Key(
    weight: Float,
    height: Dp,
    style: KeyStyle = KeyStyle.CHAR,
    repeat: Boolean = false,
    onPress: () -> Unit,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val currentOnPress by rememberUpdatedState(onPress)
    var pressed by remember { mutableStateOf(false) }
    val (bg, fg) = when (style) {
        KeyStyle.CHAR -> scheme.surface to scheme.onSurface
        KeyStyle.FUNCTION -> scheme.secondaryContainer to scheme.onSecondaryContainer
        KeyStyle.ACTIVE -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        KeyStyle.ACTION -> scheme.primary to scheme.onPrimary
    }
    Box(
        Modifier
            .weight(weight)
            .height(height)
            .background(if (pressed) scheme.primaryContainer else bg, RoundedCornerShape(8.dp))
            .pointerInput(repeat) {
                awaitEachGesture {
                    awaitFirstDown().consume()
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (repeat) {
                        currentOnPress()
                        var ended = withTimeoutOrNull(REPEAT_DELAY_MS) { waitForUpOrCancellation(); true }
                        while (ended == null) {
                            currentOnPress()
                            ended = withTimeoutOrNull(REPEAT_INTERVAL_MS) { waitForUpOrCancellation(); true }
                        }
                    } else {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            currentOnPress()
                        }
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) { content() }
    }
}

/** Two-finger pinch zooms (and pans when floating); consumes the gesture so keys underneath don't fire. */
private fun Modifier.pinchToZoom(onZoom: (Float, Offset) -> Unit, onEnd: () -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var transformed = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.count { it.pressed } >= 2) {
                    transformed = true
                    onZoom(event.calculateZoom(), event.calculatePan())
                    event.changes.forEach { it.consume() }
                }
            } while (event.changes.any { it.pressed })
            if (transformed) onEnd()
        }
    }
