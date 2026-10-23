package org.bhargav.pansariwala.server.i18n

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.set
import com.mongodb.client.model.Updates.unset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.db.MongoApp
import org.bhargav.pansariwala.server.dto.ProductTranslationsRequest
import org.bhargav.pansariwala.server.dto.ProductTranslationsResult
import org.bhargav.pansariwala.server.dto.ServerMessageDto

private const val SEED_RESOURCE = "/server_messages.json"
private val PLACEHOLDER = Regex("""\{(\d+)}""")

/** One server message; [id] is the English text, with `{0}`, `{1}`… for values filled in at runtime. */
@Serializable
data class ServerMessageDoc(
    @SerialName("_id") val id: String,
    val translations: Map<String, String> = emptyMap(),
)

/**
 * Server-side user-facing messages (API errors, push alerts) and their translations. English templates live
 * in code; [SEED_RESOURCE] seeds the catalog and the master console edits translations afterwards.
 */
class MessageCatalog(mongo: MongoApp) {
    private val col = mongo.db.getCollection<ServerMessageDoc>("server_messages")

    private class Entry(val doc: ServerMessageDoc, val pattern: Regex?, val literalLength: Int)

    @Volatile private var exact: Map<String, ServerMessageDoc> = emptyMap()
    @Volatile private var templates: List<Entry> = emptyList()

    init {
        runCatching { seed() }.onFailure { println("Server messages seed failed: ${it.message}") }
        reload()
    }

    /** [message] (English, possibly with runtime values) in [lang]; unknown or untranslated → English. */
    fun localize(message: String, lang: String = RequestLanguage.current()): String {
        if (lang == RequestLanguage.ENGLISH || message.isBlank()) return message
        exact[message]?.let { return it.translations.localized(message, lang) }
        for (entry in templates) {
            val match = entry.pattern?.matchEntire(message) ?: continue
            val translated = entry.doc.translations[lang]?.takeIf { it.isNotBlank() } ?: return message
            return PLACEHOLDER.replace(translated) { m ->
                m.groupValues[1].toInt().let { i -> match.groupValues.getOrNull(i + 1) } ?: m.value
            }
        }
        return message
    }

    /** Fills [template]'s `{n}` placeholders with [args] after translating it to [lang]. */
    fun format(template: String, lang: String, vararg args: Any): String {
        val text = (exact[template] ?: templates.firstOrNull { it.doc.id == template }?.doc)
            ?.translations?.localized(template, lang) ?: template
        return PLACEHOLDER.replace(text) { m -> args.getOrNull(m.groupValues[1].toInt())?.toString() ?: m.value }
    }

    fun list(): List<ServerMessageDto> =
        col.find().toList().sortedBy { it.id.lowercase() }.map { ServerMessageDto(it.id, it.translations) }

    /** Saves one language column; a blank text clears that translation. */
    fun saveTranslations(body: ProductTranslationsRequest): ProductTranslationsResult {
        val lang = body.language.trim().lowercase()
        require(lang in RequestLanguage.TRANSLATED) { "Unsupported language: ${body.language}" }
        val updated = body.items.count { item ->
            val field = "translations.$lang"
            val value = item.name.trim()
            col.updateOne(eq("_id", item.id), if (value.isEmpty()) unset(field) else set(field, value)).matchedCount > 0L
        }
        reload()
        return ProductTranslationsResult(updated)
    }

    private fun reload() {
        val docs = runCatching { col.find().toList() }.getOrElse {
            println("Server messages load failed: ${it.message}")
            return
        }
        exact = docs.filterNot { PLACEHOLDER.containsMatchIn(it.id) }.associateBy { it.id }
        templates = docs.filter { PLACEHOLDER.containsMatchIn(it.id) }
            .map { Entry(it, templatePattern(it.id), PLACEHOLDER.replace(it.id, "").length) }
            .sortedByDescending { it.literalLength }
    }

    /** Adds new seed messages and fills only translations an admin has not set yet. */
    private fun seed() {
        val raw = javaClass.getResource(SEED_RESOURCE)?.readText() ?: return
        val seeds = Json.decodeFromString<List<Map<String, String>>>(raw)
        val existing = col.find().toList().associateBy { it.id }
        seeds.forEach { row ->
            val english = row[RequestLanguage.ENGLISH]?.trim().orEmpty()
            if (english.isEmpty()) return@forEach
            val seeded = row.filterKeys { it in RequestLanguage.TRANSLATED }.filterValues { it.isNotBlank() }
            val current = existing[english]
            val merged = seeded + current?.translations.orEmpty().filterValues { it.isNotBlank() }
            if (current == null || merged != current.translations) {
                col.replaceOne(eq("_id", english), ServerMessageDoc(english, merged), ReplaceOptions().upsert(true))
            }
        }
    }

    private fun templatePattern(template: String): Regex {
        val out = StringBuilder()
        var last = 0
        PLACEHOLDER.findAll(template).forEach { m ->
            out.append(Regex.escape(template.substring(last, m.range.first))).append("(.+?)")
            last = m.range.last + 1
        }
        out.append(Regex.escape(template.substring(last)))
        return Regex(out.toString(), RegexOption.DOT_MATCHES_ALL)
    }
}
