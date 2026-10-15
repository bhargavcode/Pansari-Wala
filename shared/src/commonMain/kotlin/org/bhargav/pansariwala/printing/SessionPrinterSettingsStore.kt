package org.bhargav.pansariwala.printing

import org.bhargav.pansariwala.data.local.SessionStore
import org.bhargav.pansariwala.printer.PrinterSettingsCodec
import org.bhargav.pansariwala.printer.PrinterSettingsStore
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.util.AppConstants

class SessionPrinterSettingsStore(
    private val store: SessionStore,
) : PrinterSettingsStore {
    override suspend fun load(): PrinterSettings? =
        PrinterSettingsCodec.decode(store.getString(AppConstants.PRINTER_SETTINGS_KEY))

    override suspend fun save(settings: PrinterSettings) {
        store.putStrings(mapOf(AppConstants.PRINTER_SETTINGS_KEY to PrinterSettingsCodec.encode(settings)))
    }
}
