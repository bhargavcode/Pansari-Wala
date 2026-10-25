package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField

/** Touch platforms that show an on-screen keyboard. */
internal expect val hasSoftKeyboard: Boolean

/**
 * Drop-in for Material3 `OutlinedTextField`. Uses the in-app floating keyboard when one is hosted for this
 * window, otherwise the platform keyboard.
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
    val floatingKeyboard = currentFloatingKeyboard()
    if (floatingKeyboard != null && !readOnly && enabled) {
        FloatingKeyboardTextField(
            floatingKeyboard, value, onValueChange, modifier, textStyle, label, placeholder, leadingIcon,
            trailingIcon, prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions,
            keyboardActions, singleLine, maxLines, minLines, interactionSource, shape, colors,
        )
        return
    }
    M3OutlinedTextField(
        value, onValueChange, modifier, enabled, readOnly, textStyle, label, placeholder, leadingIcon,
        trailingIcon, prefix, suffix, supportingText, isError, visualTransformation, keyboardOptions,
        keyboardActions, singleLine, maxLines, minLines, interactionSource, shape, colors,
    )
}
