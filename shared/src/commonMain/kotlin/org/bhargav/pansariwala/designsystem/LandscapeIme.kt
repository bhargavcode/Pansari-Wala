package org.bhargav.pansariwala.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_done
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField

/** Touch platforms where the on-screen keyboard covers the bottom of a landscape screen. */
internal expect val hasSoftKeyboard: Boolean

/** True when text fields should edit in the right-side panel (touch device in landscape). */
val LocalImeSidePanel = compositionLocalOf { false }

private const val PANEL_WIDTH_FRACTION = 0.46f

/**
 * Drop-in for Material3 `OutlinedTextField`. Normally identical; when [LocalImeSidePanel] is on, tapping it
 * opens an editor panel that slides in from the right (keyboard opens there) and slides back out on Done.
 */
@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    if (!LocalImeSidePanel.current || readOnly || !enabled) {
        M3OutlinedTextField(
            value, onValueChange, modifier, enabled, readOnly, textStyle, label, placeholder, leadingIcon,
            trailingIcon, prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions,
            keyboardActions, singleLine, maxLines, minLines, interactionSource, shape, colors,
        )
        return
    }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focusManager = LocalFocusManager.current
    var editing by remember { mutableStateOf(false) }
    LaunchedEffect(source) {
        source.interactions.collect {
            if (it is FocusInteraction.Focus && !editing) {
                focusManager.clearFocus()
                editing = true
            }
        }
    }
    M3OutlinedTextField(
        value, onValueChange, modifier, enabled, true, textStyle, label, placeholder, leadingIcon,
        trailingIcon, prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions,
        keyboardActions, singleLine, maxLines, minLines, source, shape, colors,
    )
    if (editing) {
        ImeSidePanel(onClosed = { editing = false }) { close ->
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            val closeOn: KeyboardActionScope.((KeyboardActionScope.() -> Unit)?) -> Unit = { action ->
                action?.invoke(this)
                close()
            }
            M3OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                prefix = prefix,
                suffix = suffix,
                supportingText = supportingText,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = if (singleLine && keyboardOptions.imeAction == ImeAction.Default) {
                    keyboardOptions.copy(imeAction = ImeAction.Done)
                } else {
                    keyboardOptions
                },
                keyboardActions = KeyboardActions(
                    onDone = { closeOn(keyboardActions.onDone) },
                    onGo = { closeOn(keyboardActions.onGo) },
                    onSearch = { closeOn(keyboardActions.onSearch) },
                    onSend = { closeOn(keyboardActions.onSend) },
                    onNext = { closeOn(keyboardActions.onNext) },
                    onPrevious = keyboardActions.onPrevious,
                ),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else maxLines.coerceAtMost(PANEL_MAX_LINES),
                minLines = minLines,
                shape = shape,
                colors = colors,
            )
        }
    }
}

private const val PANEL_MAX_LINES = 6

/** Right-anchored editor surface; slides in from the right and back out before [onClosed]. */
@Composable
private fun ImeSidePanel(onClosed: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val close = { visible.targetState = false }
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) onClosed()
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = null, indication = null, onClick = close),
        ) {
            AnimatedVisibility(
                visibleState = visible,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().fillMaxWidth(PANEL_WIDTH_FRACTION),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = null, indication = null, onClick = {}),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                ) {
                    Column(
                        Modifier.safeDrawingPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        content(close)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Button(onClick = close) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Text(
                                    stringResource(Res.string.action_done),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}