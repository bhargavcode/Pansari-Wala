package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.i18n.AppLanguage
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_next
import pansariwala.shared.generated.resources.action_previous
import pansariwala.shared.generated.resources.master_messages_column
import pansariwala.shared.generated.resources.master_messages_hint
import pansariwala.shared.generated.resources.master_messages_title
import pansariwala.shared.generated.resources.master_page_info
import pansariwala.shared.generated.resources.master_product_id
import pansariwala.shared.generated.resources.master_search
import pansariwala.shared.generated.resources.master_translations_english
import pansariwala.shared.generated.resources.master_translations_hint
import pansariwala.shared.generated.resources.master_translations_language
import pansariwala.shared.generated.resources.master_translations_missing_only
import pansariwala.shared.generated.resources.master_translations_save
import pansariwala.shared.generated.resources.master_translations_saved
import pansariwala.shared.generated.resources.master_translations_shop_title
import pansariwala.shared.generated.resources.master_translations_title

private const val TRANSLATIONS_PAGE_SIZE = 50
private const val TRANSLATIONS_SEARCH_DEBOUNCE_MS = 350L

private data class TranslationRow(val id: String, val name: String, val translations: Map<String, String>)

private data class TranslationPage(val rows: List<TranslationRow> = emptyList(), val totalCount: Int = 0, val hasMore: Boolean = false)

/**
 * Spreadsheet-style editor: English name column, then one column for the selected language.
 * [shopId] null edits master catalog names; otherwise that shop's POS products.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProductTranslationsScreen(token: String, shopId: String?, shopName: String?, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    val languages = remember { AppLanguage.entries.filter { it != AppLanguage.ENGLISH } }
    var languageCode by rememberSaveable { mutableStateOf(languages.first().code) }
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var missingOnly by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf(TranslationPage()) }
    /** product id → edited name for [languageCode]; cleared on language/page change and after save. */
    val drafts = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(token, shopId, query, page, reload) {
        if (query.isNotBlank()) delay(TRANSLATIONS_SEARCH_DEBOUNCE_MS)
        runCatching {
            data = if (shopId == null) {
                val p = api.productPage(token, query.trim(), null, page, TRANSLATIONS_PAGE_SIZE)
                TranslationPage(p.items.map { TranslationRow(it.id, it.name, it.translations) }, p.totalCount, p.hasMore)
            } else {
                val p = api.shopProducts(token, shopId, query, null, STATUS_ALL, false, page, TRANSLATIONS_PAGE_SIZE)
                TranslationPage(p.items.map { TranslationRow(it.id, it.name, it.translations) }, p.totalCount, p.hasMore)
            }
        }.onFailure { onStatus(it.message.orEmpty()) }
    }
    LaunchedEffect(languageCode, page) { drafts.clear() }

    val language = AppLanguage.fromCode(languageCode)
    val changed = drafts.filter { (id, value) ->
        data.rows.firstOrNull { it.id == id }?.translations?.get(languageCode).orEmpty() != value.trim()
    }
    val rows = if (missingOnly) data.rows.filter { it.translations[languageCode].isNullOrBlank() || it.id in drafts } else data.rows
    val pageCount = ((data.totalCount + TRANSLATIONS_PAGE_SIZE - 1) / TRANSLATIONS_PAGE_SIZE).coerceAtLeast(1)

    fun save() {
        if (changed.isEmpty()) return
        saving = true
        scope.launch {
            val body = ProductTranslationsBody(languageCode, changed.map { (id, name) -> ProductTranslationItemBody(id, name.trim()) })
            runCatching {
                if (shopId == null) api.saveMasterTranslations(token, body) else api.saveShopTranslations(token, shopId, body)
            }.onSuccess {
                onStatus(getString(Res.string.master_translations_saved, it.updated))
                drafts.clear()
                reload++
            }.onFailure { onStatus(it.message.orEmpty()) }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(
            if (shopId == null) stringResource(Res.string.master_translations_title)
            else stringResource(Res.string.master_translations_shop_title, shopName ?: shopId),
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.master_translations_language), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = lang.code == languageCode,
                        onClick = { languageCode = lang.code },
                        label = { Text(lang.displayLabel) },
                    )
                }
                FilterChip(
                    selected = missingOnly,
                    onClick = { missingOnly = !missingOnly },
                    label = { Text(stringResource(Res.string.master_translations_missing_only)) },
                )
            }
            Text(
                stringResource(Res.string.master_translations_hint, language.displayLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    query,
                    {
                        query = it
                        page = 0
                    },
                    label = { Text(stringResource(Res.string.master_search)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = ::save, enabled = changed.isNotEmpty() && !saving) {
                    Text(stringResource(Res.string.master_translations_save, changed.size))
                }
            }
            MasterSectionCard {
                ResponsiveTable(columns = 3) {
                    TableHeader(
                        stringResource(Res.string.master_product_id),
                        stringResource(Res.string.master_translations_english),
                        language.displayLabel,
                    )
                    rows.forEach { row ->
                        TableRow {
                            CellText(row.id)
                            CellText(row.name)
                            Cell {
                                OutlinedTextField(
                                    value = drafts[row.id] ?: row.translations[languageCode].orEmpty(),
                                    onValueChange = { drafts[row.id] = it },
                                    placeholder = { Text(row.name) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { page-- }, enabled = page > 0) {
                    Text(stringResource(Res.string.action_previous))
                }
                Text(stringResource(Res.string.master_page_info, page + 1, pageCount, data.totalCount))
                OutlinedButton(onClick = { page++ }, enabled = data.hasMore) {
                    Text(stringResource(Res.string.action_next))
                }
            }
        }
    }
}

/** Error and notification texts the server sends to the apps, one language column at a time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServerMessagesScreen(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    val languages = remember { AppLanguage.entries.filter { it != AppLanguage.ENGLISH } }
    var languageCode by rememberSaveable { mutableStateOf(languages.first().code) }
    var query by rememberSaveable { mutableStateOf("") }
    var missingOnly by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<ServerMessageDto>>(emptyList()) }
    val drafts = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(token, reload) {
        runCatching { messages = api.serverMessages(token) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    LaunchedEffect(languageCode) { drafts.clear() }

    val language = AppLanguage.fromCode(languageCode)
    val changed = drafts.filter { (id, value) ->
        messages.firstOrNull { it.id == id }?.translations?.get(languageCode).orEmpty() != value.trim()
    }
    val q = query.trim()
    val rows = messages.filter { m ->
        (q.isEmpty() || m.id.contains(q, ignoreCase = true) || m.translations.values.any { it.contains(q, ignoreCase = true) }) &&
            (!missingOnly || m.translations[languageCode].isNullOrBlank() || m.id in drafts)
    }

    fun save() {
        if (changed.isEmpty()) return
        saving = true
        scope.launch {
            val body = ProductTranslationsBody(languageCode, changed.map { (id, text) -> ProductTranslationItemBody(id, text.trim()) })
            runCatching { api.saveServerMessageTranslations(token, body) }
                .onSuccess {
                    onStatus(getString(Res.string.master_translations_saved, it.updated))
                    drafts.clear()
                    reload++
                }
                .onFailure { onStatus(it.message.orEmpty()) }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(stringResource(Res.string.master_messages_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.master_translations_language), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = lang.code == languageCode,
                        onClick = { languageCode = lang.code },
                        label = { Text(lang.displayLabel) },
                    )
                }
                FilterChip(
                    selected = missingOnly,
                    onClick = { missingOnly = !missingOnly },
                    label = { Text(stringResource(Res.string.master_translations_missing_only)) },
                )
            }
            Text(
                stringResource(Res.string.master_messages_hint, language.displayLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    query,
                    { query = it },
                    label = { Text(stringResource(Res.string.master_search)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = ::save, enabled = changed.isNotEmpty() && !saving) {
                    Text(stringResource(Res.string.master_translations_save, changed.size))
                }
            }
            MasterSectionCard {
                ResponsiveTable(columns = 2) {
                    TableHeader(stringResource(Res.string.master_messages_column), language.displayLabel)
                    rows.forEach { row ->
                        TableRow {
                            CellText(row.id)
                            Cell {
                                OutlinedTextField(
                                    value = drafts[row.id] ?: row.translations[languageCode].orEmpty(),
                                    onValueChange = { drafts[row.id] = it },
                                    placeholder = { Text(row.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
