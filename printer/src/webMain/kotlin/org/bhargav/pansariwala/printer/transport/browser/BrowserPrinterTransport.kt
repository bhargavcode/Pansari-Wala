package org.bhargav.pansariwala.printer.transport.browser

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.printer.model.PrinterConnectionType
import org.bhargav.pansariwala.printer.model.PrinterDevice
import org.bhargav.pansariwala.printer.model.PrinterError
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printer.transport.PrinterConnection
import org.bhargav.pansariwala.printer.transport.PrinterTransport
import org.bhargav.pansariwala.printer.transport.TransportAvailability
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Browser printers: USB via WebUSB (WebSerial for USB COM ports), Bluetooth Classic via WebSerial (SPP) and
 * BLE via Web Bluetooth. Browsers allow only one device chooser per user gesture, so [scan] lists already
 * granted devices first and opens a chooser only if no other transport holds it.
 */
class BrowserPrinterTransport(override val type: PrinterConnectionType) : PrinterTransport {
    private val kind = when (type) {
        PrinterConnectionType.USB -> KIND_USB
        PrinterConnectionType.BLUETOOTH -> KIND_SERIAL_BT
        PrinterConnectionType.BLE -> KIND_BLE
        PrinterConnectionType.NETWORK -> KIND_NONE
    }

    init {
        installPrinterBridge()
    }

    override fun availability(): TransportAvailability =
        if (bridgeSupported(kind)) TransportAvailability.AVAILABLE else TransportAvailability.UNSUPPORTED

    override fun scan(): Flow<PrinterDevice> = callbackFlow {
        bridgeScan(
            kind,
            onDevice = { id, name -> trySend(PrinterDevice(id, name.ifBlank { id }, type, id, isPaired = true)) },
            onDone = { close() },
            onError = { code -> close(PrinterException(code.toPrinterError())) },
        )
        awaitClose { }
    }

    override suspend fun connect(device: PrinterDevice): PrinterConnection {
        suspendCancellableCoroutine { cont ->
            bridgeConnect(
                device.id,
                onOk = { if (cont.isActive) cont.resume(Unit) },
                onError = { code -> if (cont.isActive) cont.resumeWithException(PrinterException(code.toPrinterError())) },
            )
        }
        return BrowserPrinterConnection(device)
    }
}

private class BrowserPrinterConnection(override val device: PrinterDevice) : PrinterConnection {
    private val open = MutableStateFlow(true)
    override val isOpen: StateFlow<Boolean> = open.asStateFlow()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun write(data: ByteArray) {
        if (!open.value) throw PrinterException(PrinterError.NOT_CONNECTED)
        suspendCancellableCoroutine { cont ->
            bridgeWrite(
                device.id,
                Base64.encode(data),
                onOk = { if (cont.isActive) cont.resume(Unit) },
                onError = { code ->
                    if (!bridgeIsOpen(device.id)) open.value = false
                    if (cont.isActive) cont.resumeWithException(PrinterException(code.toPrinterError()))
                },
            )
        }
    }

    override suspend fun probe(statusRequest: ByteArray?): Boolean =
        bridgeIsOpen(device.id).also { open.value = it }

    override suspend fun close() {
        open.value = false
        suspendCancellableCoroutine { cont -> bridgeClose(device.id) { if (cont.isActive) cont.resume(Unit) } }
    }
}

/** Transports the current browser can actually use; empty on browsers without WebUSB/WebSerial/Web Bluetooth. */
fun browserPrinterTransports(): List<PrinterTransport> =
    listOf(PrinterConnectionType.USB, PrinterConnectionType.BLUETOOTH, PrinterConnectionType.BLE)
        .map(::BrowserPrinterTransport)
        .filter { it.availability() == TransportAvailability.AVAILABLE }

private fun String.toPrinterError(): PrinterError =
    PrinterError.entries.firstOrNull { it.name == this } ?: PrinterError.CONNECTION_FAILED

private const val KIND_USB = "usb"
private const val KIND_SERIAL_BT = "serial-bt"
private const val KIND_BLE = "ble"
private const val KIND_NONE = "none"

private fun bridgeSupported(kind: String): Boolean = js("window.__pwPrinter.supported(kind)")

private fun bridgeScan(
    kind: String,
    onDevice: (String, String) -> Unit,
    onDone: () -> Unit,
    onError: (String) -> Unit,
): Unit = js("window.__pwPrinter.scan(kind, onDevice, onDone, onError)")

private fun bridgeConnect(id: String, onOk: () -> Unit, onError: (String) -> Unit): Unit =
    js("window.__pwPrinter.connect(id, onOk, onError)")

private fun bridgeWrite(id: String, base64: String, onOk: () -> Unit, onError: (String) -> Unit): Unit =
    js("window.__pwPrinter.write(id, base64, onOk, onError)")

private fun bridgeIsOpen(id: String): Boolean = js("window.__pwPrinter.isOpen(id)")

private fun bridgeClose(id: String, onDone: () -> Unit): Unit = js("window.__pwPrinter.close(id, onDone)")

/** Installs `window.__pwPrinter` once; all device handles stay on the JS side, keyed by a stable id. */
private fun installPrinterBridge(): Unit = js(
    """(() => {
  if (window.__pwPrinter) return;
  const nav = navigator;
  const SPP = '00001101-0000-1000-8000-00805f9b34fb';
  const BLE_SERVICES = [
    '000018f0-0000-1000-8000-00805f9b34fb',
    'e7810a71-73ae-499d-8c15-faa9aef0c3f2',
    '49535343-fe7d-4ae5-8fa9-9fafd205e455',
    '0000ff00-0000-1000-8000-00805f9b34fb',
    '0000fee7-0000-1000-8000-00805f9b34fb'
  ];
  const BLE_CHUNK = 180;
  const P = { devices: new Map(), links: new Map(), choosing: false };
  const usbEntry = (d) => {
    const id = 'usb:' + d.vendorId + ':' + d.productId + ':' + (d.serialNumber || '');
    return { id: id, name: d.productName || 'USB printer', mode: 'usb', handle: d };
  };
  const serialEntry = (p, i) => {
    const info = p.getInfo ? p.getInfo() : {};
    const bt = !!info.bluetoothServiceClassId;
    return { id: 'serial:' + (info.usbVendorId || '') + ':' + (info.usbProductId || '') + ':' + (bt ? 'bt' : 'usb') + ':' + i, name: bt ? 'Bluetooth printer' : 'Serial printer', mode: 'serial', bt: bt, handle: p };
  };
  const bleEntry = (d) => {
    const id = 'ble:' + d.id;
    return { id: id, name: d.name || 'BLE printer', mode: 'ble', handle: d };
  };
  P.supported = (kind) => kind === 'usb' ? !!(nav.usb || nav.serial) : kind === 'serial-bt' ? !!nav.serial : kind === 'ble' ? !!nav.bluetooth : false;
  const ports = () => nav.serial ? nav.serial.getPorts().then(list => list.map(serialEntry)) : Promise.resolve([]);
  const granted = (kind) => {
    if (kind === 'usb') {
      const usb = nav.usb ? nav.usb.getDevices().then(l => l.map(usbEntry)) : Promise.resolve([]);
      return Promise.all([usb, ports().then(l => l.filter(e => !e.bt))]).then(r => r[0].concat(r[1]));
    }
    if (kind === 'serial-bt') return ports().then(l => l.filter(e => e.bt));
    if (kind === 'ble') return nav.bluetooth && nav.bluetooth.getDevices ? nav.bluetooth.getDevices().then(l => l.map(bleEntry)) : Promise.resolve([]);
    return Promise.resolve([]);
  };
  const request = (kind) => {
    if (kind === 'usb' && nav.usb) return nav.usb.requestDevice({ filters: [] }).then(usbEntry);
    if (kind === 'usb') return nav.serial.requestPort({ filters: [] }).then(() => ports()).then(l => l[l.length - 1]);
    if (kind === 'serial-bt') return nav.serial.requestPort({ allowedBluetoothServiceClassIds: [SPP], filters: [{ bluetoothServiceClassId: SPP }] }).then(() => ports()).then(l => l[l.length - 1]);
    if (kind === 'ble') return nav.bluetooth.requestDevice({ acceptAllDevices: true, optionalServices: BLE_SERVICES }).then(bleEntry);
    return Promise.resolve(null);
  };
  P.scan = (kind, onDevice, onDone, onError) => {
    const emit = (e) => { if (!e) return; P.devices.set(e.id, e); onDevice(e.id, e.name); };
    granted(kind).then(list => {
      list.forEach(emit);
      if (P.choosing) { onDone(); return; }
      P.choosing = true;
      return request(kind)
        .then(e => { emit(e); onDone(); })
        .catch(err => {
          const name = err && err.name;
          if (name === 'NotFoundError' || name === 'AbortError') onDone();
          else onError(name === 'SecurityError' || name === 'NotAllowedError' ? 'PERMISSION_DENIED' : 'UNSUPPORTED');
        })
        .finally(() => { P.choosing = false; });
    }).catch(() => onError('UNSUPPORTED'));
  };
  const openUsb = (d) => Promise.resolve()
    .then(() => d.opened ? null : d.open())
    .then(() => d.configuration ? null : d.selectConfiguration(1))
    .then(() => {
      let target = null;
      const ifaces = d.configuration.interfaces;
      for (let i = 0; i < ifaces.length; i++) {
        const alts = ifaces[i].alternates;
        for (let j = 0; j < alts.length; j++) {
          const alt = alts[j];
          const ep = alt.endpoints.find(x => x.direction === 'out' && x.type === 'bulk');
          if (ep && (!target || alt.interfaceClass === 7)) target = { iface: ifaces[i].interfaceNumber, ep: ep.endpointNumber };
        }
      }
      if (!target) throw new Error('No bulk OUT endpoint');
      return d.claimInterface(target.iface).then(() => {
        const link = { write: (b) => d.transferOut(target.ep, b), close: () => d.close().catch(() => {}), alive: () => d.opened };
        return link;
      });
    });
  const openSerial = (p) => Promise.resolve(p.writable ? null : p.open({ baudRate: 9600 })).then(() => {
    const writer = p.writable.getWriter();
    return {
      write: (b) => writer.write(b),
      close: () => { try { writer.releaseLock(); } catch (e) {} return p.close().catch(() => {}); },
      alive: () => !!p.writable
    };
  });
  const openBle = (d) => d.gatt.connect()
    .then(server => server.getPrimaryServices())
    .then(services => {
      const pick = (i) => {
        if (i >= services.length) throw new Error('No writable characteristic');
        return services[i].getCharacteristics().then(chars => {
          const ch = chars.find(c => c.properties.writeWithoutResponse || c.properties.write);
          return ch || pick(i + 1);
        });
      };
      return pick(0);
    })
    .then(ch => {
      const writeOne = (part) => ch.properties.writeWithoutResponse && ch.writeValueWithoutResponse ? ch.writeValueWithoutResponse(part) : ch.writeValue(part);
      return {
        write: (b) => {
          let chain = Promise.resolve();
          for (let i = 0; i < b.length; i += BLE_CHUNK) {
            const part = b.slice(i, i + BLE_CHUNK);
            chain = chain.then(() => writeOne(part));
          }
          return chain;
        },
        close: () => { try { d.gatt.disconnect(); } catch (e) {} },
        alive: () => d.gatt.connected
      };
    });
  const open = (e) => e.mode === 'usb' ? openUsb(e.handle) : e.mode === 'serial' ? openSerial(e.handle) : openBle(e.handle);
  P.connect = (id, onOk, onError) => {
    const go = (e) => {
      if (!e) { onError('NOT_FOUND'); return; }
      open(e).then(link => { P.links.set(id, link); onOk(); }).catch(() => onError('CONNECTION_FAILED'));
    };
    const known = P.devices.get(id);
    if (known) { go(known); return; }
    Promise.all(['usb', 'serial-bt', 'ble'].filter(P.supported).map(granted))
      .then(all => {
        const e = [].concat.apply([], all).find(x => x.id === id);
        if (e) P.devices.set(id, e);
        go(e);
      })
      .catch(() => onError('NOT_FOUND'));
  };
  P.write = (id, base64, onOk, onError) => {
    const link = P.links.get(id);
    if (!link) { onError('NOT_CONNECTED'); return; }
    const raw = atob(base64);
    const bytes = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) bytes[i] = raw.charCodeAt(i);
    Promise.resolve().then(() => link.write(bytes)).then(() => onOk()).catch(() => onError('WRITE_FAILED'));
  };
  P.isOpen = (id) => { const link = P.links.get(id); return !!link && !!link.alive(); };
  P.close = (id, onDone) => {
    const link = P.links.get(id);
    P.links.delete(id);
    Promise.resolve(link ? link.close() : null).catch(() => {}).then(() => onDone());
  };
  window.__pwPrinter = P;
})()"""
)
