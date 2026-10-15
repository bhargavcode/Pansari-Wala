package org.bhargav.pansariwala.printer

import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.bluetooth.BlePrinterTransport
import org.bhargav.pansariwala.printer.transport.network.NetworkPrinterTransport

object IosPrinterTransports {
    fun create(): List<PrinterTransport> = listOf(
        BlePrinterTransport(),
        NetworkPrinterTransport(),
    )
}
