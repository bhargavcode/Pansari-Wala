package org.bhargav.pansariwala.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity

/** Camera scanning needs the browser `BarcodeDetector`; keyboard-wedge (USB / Bluetooth HID) scanners work everywhere. */
actual val barcodeScannerSupported: Boolean = true

@Composable
actual fun BarcodeScannerPreview(
    settings: ScannerSettings,
    active: Boolean,
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) {
    val currentOnBarcode by rememberUpdatedState(onBarcode)
    val currentOnDenied by rememberUpdatedState(onPermissionDenied)
    val density = LocalDensity.current.density
    val holder = remember { ScannerHolder() }
    DisposableEffect(active, settings.lens, settings.retailFormatsOnly, settings.repeatDelayMs) {
        if (!active) return@DisposableEffect onDispose { }
        val handle = scannerStart(
            front = settings.lens == ScannerLens.FRONT,
            retail = settings.retailFormatsOnly,
            repeatMs = settings.repeatDelayMs.toInt(),
            onCode = { code -> currentOnBarcode(code) },
            onDenied = { currentOnDenied() },
        )
        holder.handle = handle
        holder.rect?.let { scannerPlace(handle, it[0], it[1], it[2], it[3]) }
        onDispose {
            scannerStop(handle)
            holder.handle = null
        }
    }
    Box(
        modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInWindow()
            val rect = doubleArrayOf(
                (position.x / density).toDouble(),
                (position.y / density).toDouble(),
                (coordinates.size.width / density).toDouble(),
                (coordinates.size.height / density).toDouble(),
            )
            holder.rect = rect
            holder.handle?.let { scannerPlace(it, rect[0], rect[1], rect[2], rect[3]) }
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

private class ScannerHolder {
    var handle: JsAny? = null
    var rect: DoubleArray? = null
}

actual fun scanFeedback(beep: Boolean, vibrate: Boolean) = playScanFeedback(beep, vibrate)

private fun playScanFeedback(beep: Boolean, vibrate: Boolean): Unit = js(
    """(() => {
  try {
    const C = window.AudioContext || window.webkitAudioContext;
    if (beep && C) {
      const ctx = window.__pwAudio || (window.__pwAudio = new C());
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.frequency.value = 1800;
      gain.gain.value = 0.1;
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.08);
    }
    if (vibrate && navigator.vibrate) navigator.vibrate(40);
  } catch (e) {}
})()"""
)

private fun scannerPlace(handle: JsAny, left: Double, top: Double, width: Double, height: Double): Unit =
    js("(() => { handle.pos = [left, top, width, height]; handle.place(); })()")

private fun scannerStop(handle: JsAny): Unit = js("handle.stop()")

/** Starts keyboard-wedge capture and, where supported, a camera feed drawn over the preview bounds. */
private fun scannerStart(
    front: Boolean,
    retail: Boolean,
    repeatMs: Int,
    onCode: (String) -> Unit,
    onDenied: () -> Unit,
): JsAny = js(
    """(() => {
  const s = { stopped: false, video: null, stream: null, timer: 0, buf: '', lastKey: 0, lastCode: '', lastAt: 0, pos: null };
  const deliver = (code) => {
    const now = Date.now();
    if (!code || (code === s.lastCode && now - s.lastAt < repeatMs)) return;
    s.lastCode = code;
    s.lastAt = now;
    onCode(code);
  };
  s.key = (e) => {
    if (e.ctrlKey || e.metaKey || e.altKey) return;
    const now = Date.now();
    if (e.key === 'Enter') {
      if (s.buf.length >= 4 && now - s.lastKey < 100) deliver(s.buf);
      s.buf = '';
      return;
    }
    if (!e.key || e.key.length !== 1) return;
    if (now - s.lastKey > 50) s.buf = '';
    s.buf += e.key;
    s.lastKey = now;
  };
  window.addEventListener('keydown', s.key, true);
  s.place = () => {
    const v = s.video, p = s.pos;
    if (!v || !p) return;
    v.style.left = p[0] + 'px';
    v.style.top = p[1] + 'px';
    v.style.width = p[2] + 'px';
    v.style.height = p[3] + 'px';
    v.style.borderRadius = Math.abs(p[2] - p[3]) < 2 ? '50%' : '12px';
  };
  s.stop = () => {
    s.stopped = true;
    clearTimeout(s.timer);
    window.removeEventListener('keydown', s.key, true);
    if (s.stream) s.stream.getTracks().forEach(t => t.stop());
    if (s.video) s.video.remove();
  };
  const md = navigator.mediaDevices;
  if ('BarcodeDetector' in window && md && md.getUserMedia) {
    md.getUserMedia({ video: { facingMode: front ? 'user' : 'environment' }, audio: false }).then(stream => {
      if (s.stopped) { stream.getTracks().forEach(t => t.stop()); return; }
      s.stream = stream;
      const v = document.createElement('video');
      v.muted = true;
      v.playsInline = true;
      v.autoplay = true;
      v.srcObject = stream;
      v.style.cssText = 'position:fixed;object-fit:cover;pointer-events:none;z-index:10;' + (front ? 'transform:scaleX(-1);' : '');
      document.body.appendChild(v);
      s.video = v;
      s.place();
      v.play().catch(() => {});
      let detector;
      try {
        detector = retail ? new BarcodeDetector({ formats: ['ean_13', 'ean_8', 'upc_a', 'upc_e', 'code_128'] }) : new BarcodeDetector();
      } catch (e) {
        detector = new BarcodeDetector();
      }
      const tick = () => {
        if (s.stopped) return;
        if (v.readyState < 2) { s.timer = setTimeout(tick, 200); return; }
        detector.detect(v)
          .then(codes => { if (!s.stopped && codes.length) deliver(codes[0].rawValue); })
          .catch(() => {})
          .then(() => { if (!s.stopped) s.timer = setTimeout(tick, 200); });
      };
      tick();
    }).catch(err => {
      if (!s.stopped && err && (err.name === 'NotAllowedError' || err.name === 'SecurityError')) onDenied();
    });
  }
  return s;
})()"""
)
