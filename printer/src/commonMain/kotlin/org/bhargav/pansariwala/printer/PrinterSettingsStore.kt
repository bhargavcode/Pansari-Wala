package org.bhargav.pansariwala.printer

import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.printer.model.PrinterSettings

/** Persistence hook so the host app decides where printer settings live. */
interface PrinterSettingsStore {
    suspend fun load(): PrinterSettings?
    suspend fun save(settings: PrinterSettings)
}

class InMemoryPrinterSettingsStore : PrinterSettingsStore {
    private var value: PrinterSettings? = null
    override suspend fun load(): PrinterSettings? = value
    override suspend fun save(settings: PrinterSettings) {
        value = settings
    }
}

/** JSON codec for stores that persist plain strings (DataStore, NSUserDefaults, localStorage). */
object PrinterSettingsCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(settings: PrinterSettings): String = json.encodeToString(PrinterSettings.serializer(), settings)

    fun decode(raw: String?): PrinterSettings? =
        raw?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(PrinterSettings.serializer(), it) }.getOrNull() }
}
