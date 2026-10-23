package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.i18n.AppLanguage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_close
import pansariwala.shared.generated.resources.master_save
import pansariwala.shared.generated.resources.master_translate_english
import pansariwala.shared.generated.resources.master_translate_hint
import pansariwala.shared.generated.resources.master_translate_language
import pansariwala.shared.generated.resources.master_translate_saved
import pansariwala.shared.generated.resources.master_translate_title

/** One translatable field: its [label], the English [english] source and lang code → saved translation. */
internal class TranslatableField(
    val label: StringResource,
    val english: String,
    val translations: Map<String, String>,
)

/**
 * Side-by-side translation editor: read-only English fields on the left, the picked language on the right.
 * Picking a language fills in its saved translations; [onSave] gets one value per field (blank = English).
 */
@Composable
internal fun TranslationDialog(
    title: String,
    fields: List<TranslatableField>,
    onSave: suspend (language: String, values: List<String>) -> Unit,
    onStatus: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val languages = remember { AppLanguage.entries.filter { it != AppLanguage.ENGLISH } }
    var language by remember { mutableStateOf(languages.first()) }
    var menuOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    /** Values saved during this dialog session, so switching languages back shows them. */
    val saved = remember { mutableStateMapOf<String, List<String>>() }
    val values = remember(language) {
        mutableStateListOf<String>().apply {
            addAll(saved[language.code] ?: fields.map { it.translations[language.code].orEmpty() })
        }
    }

    WideDialog(onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.master_translate_title, title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.master_close)) }
                }
            }
            ResponsiveColumns(
                modifier = Modifier.padding(16.dp),
                left = {
                    MasterSectionCard(title = stringResource(Res.string.master_translate_english)) {
                        fields.forEach { field ->
                            OutlinedTextField(
                                value = field.english,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(field.label)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
                right = {
                    MasterSectionCard {
                        Box {
                            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(Res.string.master_translate_language, language.displayLabel),
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                languages.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.displayLabel) },
                                        onClick = {
                                            language = lang
                                            menuOpen = false
                                        },
                                    )
                                }
                            }
                        }
                        fields.forEachIndexed { index, field ->
                            OutlinedTextField(
                                value = values[index],
                                onValueChange = { values[index] = it },
                                label = { Text(stringResource(field.label)) },
                                placeholder = { Text(field.english) },
                                enabled = field.english.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            stringResource(Res.string.master_translate_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Button(
                                enabled = !saving,
                                onClick = {
                                    saving = true
                                    val lang = language
                                    val snapshot = values.map { it.trim() }
                                    scope.launch {
                                        runCatching { onSave(lang.code, snapshot) }
                                            .onSuccess {
                                                saved[lang.code] = snapshot
                                                onStatus(getString(Res.string.master_translate_saved, lang.displayLabel))
                                            }
                                            .onFailure { onStatus(it.message.orEmpty()) }
                                        saving = false
                                    }
                                },
                            ) { Text(stringResource(Res.string.master_save)) }
                        }
                    }
                },
            )
        }
    }
}
