package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.i18n.AppLanguage
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.translate_field_brand
import pansariwala.shared.generated.resources.translate_field_name
import pansariwala.shared.generated.resources.translate_hint

/** The shop's chosen language. English name and brand translate into these fields as the user types. */
@Composable
internal fun ProductTranslationsSection(
    state: AddEditInventoryUiState,
    onNameChange: (String, String) -> Unit,
    onBrandChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = AppLanguage.entries.filter { it.code in state.translationLanguages }
    if (shown.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.translate_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        shown.forEach { language ->
            if (language != AppLanguage.HINDI) {
                OutlinedTextField(
                    value = state.nameTranslations[language.code].orEmpty(),
                    onValueChange = { onNameChange(language.code, it) },
                    label = { Text(stringResource(Res.string.translate_field_name, language.displayLabel)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (language.code in state.translating) {
                        { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) }
                    } else {
                        null
                    },
                )
            }
            if (state.brandName.isNotBlank() || !state.brandTranslations[language.code].isNullOrBlank()) {
                OutlinedTextField(
                    value = state.brandTranslations[language.code].orEmpty(),
                    onValueChange = { onBrandChange(language.code, it) },
                    label = { Text(stringResource(Res.string.translate_field_brand, language.displayLabel)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
