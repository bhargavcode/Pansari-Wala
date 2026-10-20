package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_apply
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.time_format_am
import pansariwala.shared.generated.resources.time_format_pm

/**
 * Read-only field that opens a clock picker. [value] is 24-hour "HH:mm" (blank = unset, shown as [emptyText]).
 * When [clearLabel] is set the picker offers a button that resets the value to blank.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emptyText: String = "",
    clearLabel: String? = null,
    defaultHour: Int = 9,
) {
    var picking by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = formatClockTime(value) ?: emptyText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable(enabled = enabled) { picking = true })
    }
    if (!picking) return
    val parts = parseClockTime(value)
    val pickerState = rememberTimePickerState(
        initialHour = parts?.first ?: defaultHour,
        initialMinute = parts?.second ?: 0,
        is24Hour = false,
    )
    AppAlertDialog(
        onDismissRequest = { picking = false },
        title = { Text(label) },
        text = { TimePicker(state = pickerState) },
        confirmButton = {
            TextButton(onClick = {
                onChange("${pickerState.hour.toString().padStart(2, '0')}:${pickerState.minute.toString().padStart(2, '0')}")
                picking = false
            }) { Text(stringResource(Res.string.action_apply)) }
        },
        dismissButton = {
            TextButton(onClick = {
                if (clearLabel != null) onChange("")
                picking = false
            }) { Text(clearLabel ?: stringResource(Res.string.action_cancel)) }
        },
    )
}

/** "17:00" → hour 17, minute 0; null when blank or malformed. */
fun parseClockTime(value: String): Pair<Int, Int>? {
    val parts = value.trim().split(":").mapNotNull { it.toIntOrNull() }
    if (parts.size != 2 || parts[0] !in 0..23 || parts[1] !in 0..59) return null
    return parts[0] to parts[1]
}

/** "17:00" → "5:00 PM"; null when blank or malformed. */
@Composable
fun formatClockTime(value: String): String? {
    val (hour, minute) = parseClockTime(value) ?: return null
    val clock = "${if (hour % 12 == 0) 12 else hour % 12}:${minute.toString().padStart(2, '0')}"
    return stringResource(if (hour < 12) Res.string.time_format_am else Res.string.time_format_pm, clock)
}
