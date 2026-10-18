package org.bhargav.pansariwala.printing.external

import android.print.PrintAttributes
import android.print.PrinterCapabilitiesInfo
import android.print.PrinterId
import android.print.PrinterInfo
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.printer.model.PaperWidth
import org.bhargav.pansariwala.printing.NothingToPrintException
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.external_print_printer_name
import pansariwala.shared.generated.resources.external_print_resolution
import pansariwala.shared.generated.resources.printer_no_device
import pansariwala.shared.generated.resources.printer_paper_58
import pansariwala.shared.generated.resources.printer_paper_80

/**
 * Lists the shop's thermal printer in the system print dialog, so any app's "Print" (receipts,
 * invoices, PDFs, web pages) can be sent to it. The framework hands over a PDF which is rasterised.
 */
class PansariPrintService : PrintService(), KoinComponent {
    private val gateway: ExternalPrintGateway by inject()
    private val scope = MainScope()

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession = Discovery()

    override fun onPrintJobQueued(printJob: PrintJob) {
        if (!printJob.isQueued) return
        printJob.start()
        val data = printJob.document.data
        val copies = printJob.info.copies
        scope.launch {
            val result = data?.let { gateway.printPdf(it, copies) } ?: Result.failure(NothingToPrintException())
            if (!printJob.isStarted) return@launch
            result.fold(
                onSuccess = { printJob.complete() },
                onFailure = { printJob.fail(gateway.errorMessage(it)) },
            )
        }
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        if (printJob.isQueued || printJob.isStarted) printJob.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun printerInfo(id: PrinterId): PrinterInfo {
        val settings = gateway.settings
        val device = settings.lastDevice
        val capabilities = PrinterCapabilitiesInfo.Builder(id)
            .addMediaSize(rollSize(PaperWidth.MM_58, getString(Res.string.printer_paper_58)), settings.paperWidth == PaperWidth.MM_58)
            .addMediaSize(rollSize(PaperWidth.MM_80, getString(Res.string.printer_paper_80)), settings.paperWidth == PaperWidth.MM_80)
            .addMediaSize(PrintAttributes.MediaSize.ISO_A4, false)
            .addResolution(
                PrintAttributes.Resolution(RESOLUTION_ID, getString(Res.string.external_print_resolution), PRINTER_DPI, PRINTER_DPI),
                true,
            )
            .setColorModes(PrintAttributes.COLOR_MODE_MONOCHROME, PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        return PrinterInfo.Builder(
            id,
            getString(Res.string.external_print_printer_name),
            if (device != null) PrinterInfo.STATUS_IDLE else PrinterInfo.STATUS_UNAVAILABLE,
        )
            .setDescription(device?.displayName ?: getString(Res.string.printer_no_device))
            .setCapabilities(capabilities)
            .build()
    }

    /** Page width equals the printable width so apps lay out content at the printer's real size. */
    private fun rollSize(paper: PaperWidth, label: String) = PrintAttributes.MediaSize(
        "$ROLL_ID_PREFIX${paper.millimeters}",
        label,
        paper.dots * MILS_PER_INCH / PRINTER_DPI,
        ROLL_PAGE_HEIGHT_MILS,
    )

    private inner class Discovery : PrinterDiscoverySession() {
        override fun onStartPrinterDiscovery(priorityList: MutableList<PrinterId>) = publish()
        override fun onStopPrinterDiscovery() = Unit
        override fun onValidatePrinters(printerIds: MutableList<PrinterId>) = publish()
        override fun onStartPrinterStateTracking(printerId: PrinterId) = publish()
        override fun onStopPrinterStateTracking(printerId: PrinterId) = Unit
        override fun onDestroy() = Unit

        private fun publish() {
            scope.launch {
                val info = printerInfo(generatePrinterId(LOCAL_PRINTER_ID))
                if (!isDestroyed) addPrinters(listOf(info))
            }
        }
    }

    private companion object {
        const val LOCAL_PRINTER_ID = "pansari_thermal"
        const val ROLL_ID_PREFIX = "PANSARI_ROLL_"
        const val RESOLUTION_ID = "thermal_203"
        const val PRINTER_DPI = 203
        const val MILS_PER_INCH = 1000
        /** A4 length; content is trimmed after rendering so short receipts don't waste paper. */
        const val ROLL_PAGE_HEIGHT_MILS = 11_693
    }
}
