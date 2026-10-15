package org.bhargav.pansariwala.printer.transport.network

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.discard
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.printer.DEFAULT_RAW_PRINT_PORT
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printer.networkPrinterDevice
import org.bhargav.pansariwala.printer.transport.PrinterConnection
import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.TransportAvailability

/**
 * Raw TCP printing (JetDirect / port 9100), used by almost every Wi-Fi and Ethernet receipt printer.
 * Discovery sweeps the local /24 subnet with a bounded number of short connect attempts.
 */
class NetworkPrinterTransport(
    private val ports: List<Int> = listOf(DEFAULT_RAW_PRINT_PORT),
    private val sweepConcurrency: Int = 32,
    private val probeConnectTimeoutMs: Long = 450,
    private val io: CoroutineDispatcher = printerIoDispatcher,
) : PrinterTransport {

    override val type: PrinterConnectionType = PrinterConnectionType.NETWORK

    private val selector by lazy { SelectorManager(io) }

    override fun availability(): TransportAvailability =
        if (localIpv4Addresses().isEmpty()) TransportAvailability.DISABLED else TransportAvailability.AVAILABLE

    override fun scan(): Flow<PrinterDevice> = channelFlow {
        val gate = Semaphore(sweepConcurrency)
        val hosts = localIpv4Addresses().flatMap { own ->
            val prefix = own.substringBeforeLast('.')
            (1..254).map { "$prefix.$it" }.filter { it != own }
        }.distinct()
        hosts.forEach { host ->
            ports.forEach { port ->
                launch {
                    gate.withPermit {
                        if (isReachable(host, port)) send(networkPrinterDevice(host, port))
                    }
                }
            }
        }
    }.flowOn(io)

    private suspend fun isReachable(host: String, port: Int): Boolean {
        val socket = withTimeoutOrNull(probeConnectTimeoutMs) {
            runCatching { aSocket(selector).tcp().connect(host, port) }.getOrNull()
        } ?: return false
        socket.close()
        return true
    }

    override suspend fun connect(device: PrinterDevice): PrinterConnection = withContext(io) {
        val socket = try {
            aSocket(selector).tcp().connect(device.address, device.port.takeIf { it > 0 } ?: DEFAULT_RAW_PRINT_PORT) {
                noDelay = true
                keepAlive = true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw PrinterException(PrinterError.CONNECTION_FAILED, e.message, e)
        }
        TcpPrinterConnection(device, socket, io)
    }
}

private class TcpPrinterConnection(
    override val device: PrinterDevice,
    private val socket: Socket,
    io: CoroutineDispatcher,
) : PrinterConnection {
    private val scope = CoroutineScope(SupervisorJob() + io)
    private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = false)
    private val input: ByteReadChannel = socket.openReadChannel()
    private val open = MutableStateFlow(true)
    override val isOpen: StateFlow<Boolean> = open.asStateFlow()

    init {
        // Drains status replies and notices a FIN / RST immediately, without polling.
        scope.launch {
            try {
                while (input.awaitContent()) {
                    input.discard(DRAIN_CHUNK)
                }
            } catch (_: Throwable) {
            }
            open.value = false
        }
    }

    override suspend fun write(data: ByteArray) {
        if (!open.value) throw PrinterException(PrinterError.NOT_CONNECTED)
        try {
            output.writeFully(data)
            output.flush()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            open.value = false
            throw PrinterException(PrinterError.WRITE_FAILED, e.message, e)
        }
    }

    /** A write to a powered-off printer eventually fails, which the silent-drop case needs. */
    override suspend fun probe(statusRequest: ByteArray?): Boolean {
        if (!open.value) return false
        if (statusRequest != null) {
            runCatching { write(statusRequest) }.onFailure { return false }
        }
        return open.value && !output.isClosedForWrite
    }

    override suspend fun close() {
        open.value = false
        runCatching { output.flushAndClose() }
        runCatching { socket.close() }
        scope.cancel()
    }

    private companion object {
        const val DRAIN_CHUNK = 256L
    }
}
