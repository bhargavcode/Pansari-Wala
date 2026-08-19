package org.bhargav.pansariwala.i18n

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Localizable text for ViewModels / UI state. Resolve with [asString] in Composables.
 */
sealed interface UiText {
    data class Plain(val value: String) : UiText
    data class Res(val id: StringResource, val args: List<Any> = emptyList()) : UiText

    companion object {
        fun res(id: StringResource, vararg args: Any): UiText = Res(id, args.toList())
    }
}

@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Plain -> value
    is UiText.Res -> if (args.isEmpty()) {
        stringResource(id)
    } else {
        stringResource(id, *args.toTypedArray())
    }
}
