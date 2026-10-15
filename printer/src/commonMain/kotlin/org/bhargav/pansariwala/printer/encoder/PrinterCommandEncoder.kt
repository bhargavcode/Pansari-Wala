package org.bhargav.pansariwala.printer.encoder

import org.bhargav.pansariwala.printer.document.PrintDocument
import org.bhargav.pansariwala.printer.model.PrinterSettings

/** Turns a [PrintDocument] into the byte stream a specific printer language understands. */
interface PrinterCommandEncoder {
    /** Full job: init, body, copies and trailing actions (feed / beep / drawer / cut). */
    fun encode(document: PrintDocument, settings: PrinterSettings): ByteArray

    /** Lightweight "are you there" request; null when the language has none. */
    fun statusRequest(): ByteArray? = null
}

/** Growable byte buffer without platform dependencies. */
internal class ByteBuffer(initial: Int = 1024) {
    private var bytes = ByteArray(initial)
    var size: Int = 0
        private set

    private fun ensure(extra: Int) {
        if (size + extra <= bytes.size) return
        var next = bytes.size * 2
        while (next < size + extra) next *= 2
        bytes = bytes.copyOf(next)
    }

    fun put(vararg values: Int): ByteBuffer {
        ensure(values.size)
        values.forEach { bytes[size++] = it.toByte() }
        return this
    }

    fun put(values: ByteArray): ByteBuffer {
        ensure(values.size)
        values.copyInto(bytes, size)
        size += values.size
        return this
    }

    fun toByteArray(): ByteArray = bytes.copyOf(size)
}
