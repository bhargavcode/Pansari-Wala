package org.bhargav.pansariwala.printing.external

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.feature.printer.messageRes
import org.bhargav.pansariwala.printer.PrinterManager
import org.bhargav.pansariwala.printer.document.PrintDocument
import org.bhargav.pansariwala.printer.document.PrintElement
import org.bhargav.pansariwala.printer.image.ImageProcessor
import org.bhargav.pansariwala.printer.model.ArgbImage
import org.bhargav.pansariwala.printer.model.DitherMode
import org.bhargav.pansariwala.printer.model.PrinterException
import org.bhargav.pansariwala.printer.model.PrinterSettings
import org.bhargav.pansariwala.printing.NothingToPrintException
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.external_print_done
import pansariwala.shared.generated.resources.external_print_sending
import pansariwala.shared.generated.resources.external_print_unsupported
import pansariwala.shared.generated.resources.printer_error_connection_failed
import pansariwala.shared.generated.resources.printer_error_nothing_to_print
import java.io.File
import java.io.InputStream

/**
 * Prints content coming from other apps (system print dialog PDFs, shared text / PDF / images) on
 * the configured thermal printer. Everything is rasterised to the paper width, so any layout prints.
 */
class ExternalPrintGateway(
    private val context: Context,
    private val printer: PrinterManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: PrinterSettings get() = printer.settings.value

    /** Prints a PDF handed over by the Android print framework. */
    suspend fun printPdf(data: ParcelFileDescriptor, copies: Int): Result<Unit> {
        val document = runCatching {
            withContext(Dispatchers.IO) {
                withTempCopy(ParcelFileDescriptor.AutoCloseInputStream(data)) { assemble(pdfElements(it), copies) }
            }
        }.getOrElse { return Result.failure(it) } ?: return Result.failure(NothingToPrintException())
        return printer.print(document)
    }

    /**
     * Reads a share intent while the caller still holds the URI grant, then prints in the app scope
     * so the share screen can close right away.
     */
    suspend fun printShared(intent: Intent) {
        val document = runCatching {
            withContext(Dispatchers.IO) { assemble(sharedElements(intent), settings.copies) }
        }.getOrNull()
        if (document == null) {
            toast(getString(Res.string.external_print_unsupported))
            return
        }
        scope.launch {
            toast(getString(Res.string.external_print_sending))
            val result = printer.print(document)
            toast(result.fold(onSuccess = { getString(Res.string.external_print_done) }, onFailure = { errorMessage(it) }))
        }
    }

    suspend fun errorMessage(error: Throwable): String = getString(
        when (error) {
            is PrinterException -> error.error.messageRes
            is NothingToPrintException -> Res.string.printer_error_nothing_to_print
            else -> Res.string.printer_error_connection_failed
        },
    )

    private fun sharedElements(intent: Intent): List<PrintElement> {
        val uris = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            else -> emptyList()
        }
        if (uris.isNotEmpty()) return uris.flatMap { uriElements(it, intent.type) }
        val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        return if (text.isNullOrBlank()) emptyList() else textElements(text)
    }

    private fun uriElements(uri: Uri, fallbackType: String?): List<PrintElement> {
        val type = context.contentResolver.getType(uri) ?: fallbackType.orEmpty()
        return when {
            type == PDF_MIME || uri.path.orEmpty().endsWith(PDF_SUFFIX, ignoreCase = true) ->
                withTempCopy(open(uri)) { pdfElements(it) }
            type.startsWith(TEXT_MIME_PREFIX) ->
                textElements(open(uri).bufferedReader().use { it.readText() }.take(MAX_TEXT_CHARS))
            else -> imageElements(uri)
        }
    }

    private fun pdfElements(file: File): List<PrintElement> {
        val dots = settings.paperWidth.dots
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                (0 until renderer.pageCount).mapNotNull { index ->
                    renderer.openPage(index).use { page ->
                        val height = (page.height.toLong() * dots / page.width).toInt().coerceIn(1, MAX_PAGE_ROWS)
                        val bitmap = Bitmap.createBitmap(dots, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        bitmap.toImageElement(DitherMode.THRESHOLD).also { bitmap.recycle() }
                    }
                }
            }
        }
    }

    private fun imageElements(uri: Uri): List<PrintElement> {
        val dots = settings.paperWidth.dots
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return emptyList()
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= dots) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = open(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: return emptyList()
        return listOfNotNull(bitmap.toImageElement(settings.image.ditherMode)).also { bitmap.recycle() }
    }

    private fun textElements(text: String): List<PrintElement> =
        text.replace("\r", "").replace("\t", TAB_SPACES).trimEnd().lines().map { line ->
            if (line.isBlank()) PrintElement.Feed(1) else PrintElement.Text(line)
        }

    private fun Bitmap.toImageElement(mode: DitherMode): PrintElement.Image? {
        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)
        val image = ArgbImage(width, height, pixels).trimmedVertically() ?: return null
        val tuning = settings.image
        val mono = if (mode == DitherMode.THRESHOLD) {
            ImageProcessor.toMonochrome(image, settings.paperWidth.dots, mode, threshold = DOCUMENT_THRESHOLD)
        } else {
            ImageProcessor.toMonochrome(
                image = image,
                targetWidth = settings.paperWidth.dots,
                mode = mode,
                threshold = tuning.threshold,
                brightness = tuning.brightness,
                invert = tuning.invert,
                contrast = tuning.contrast,
                gammaPercent = tuning.gammaPercent,
            )
        }
        return PrintElement.Image(mono)
    }

    /** Drops blank rows above and below the content so documents don't waste receipt paper. */
    private fun ArgbImage.trimmedVertically(): ArgbImage? {
        fun hasInk(y: Int): Boolean = (0 until width).any { x -> isInk(pixels[y * width + x]) }
        val top = (0 until height).firstOrNull(::hasInk) ?: return null
        val bottom = (height - 1 downTo top).first(::hasInk)
        if (top == 0 && bottom == height - 1) return this
        return ArgbImage(width, bottom - top + 1, pixels.copyOfRange(top * width, (bottom + 1) * width))
    }

    private fun isInk(argb: Int): Boolean {
        if ((argb ushr 24) < MIN_INK_ALPHA) return false
        val luma = (((argb shr 16) and 0xFF) + ((argb shr 8) and 0xFF) + (argb and 0xFF)) / 3
        return luma < MAX_PAPER_LUMA
    }

    private fun assemble(elements: List<PrintElement>, copies: Int): PrintDocument? {
        if (elements.isEmpty()) return null
        val current = settings
        val single = buildList {
            addAll(elements)
            if (current.feedLinesAfterPrint > 0) add(PrintElement.Feed(current.feedLinesAfterPrint))
            if (current.autoCut) add(PrintElement.Cut)
        }
        return PrintDocument(List(copies.coerceIn(1, MAX_COPIES)) { single }.flatten())
    }

    private fun open(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri) ?: throw NothingToPrintException()

    private inline fun <T> withTempCopy(input: InputStream, block: (File) -> T): T {
        val file = File.createTempFile(TEMP_PREFIX, PDF_SUFFIX, context.cacheDir)
        try {
            input.use { source -> file.outputStream().use { source.copyTo(it) } }
            return block(file)
        } finally {
            file.delete()
        }
    }

    private suspend fun toast(message: String) = withContext(Dispatchers.Main) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val PDF_MIME = "application/pdf"
        const val PDF_SUFFIX = ".pdf"
        const val TEXT_MIME_PREFIX = "text/"
        const val TEMP_PREFIX = "external_print"
        const val TAB_SPACES = "    "
        const val MAX_TEXT_CHARS = 50_000
        const val MAX_PAGE_ROWS = 20_000
        const val MAX_COPIES = 10
        const val DOCUMENT_THRESHOLD = 160
        const val MIN_INK_ALPHA = 16
        const val MAX_PAPER_LUMA = 245
    }
}
