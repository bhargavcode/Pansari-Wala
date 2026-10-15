package org.bhargav.pansariwala.printer.text

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.bhargav.pansariwala.printer.model.MonoBitmap
import org.bhargav.pansariwala.printer.model.PrintAlignment
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetGrayFillColor
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.NSUnderlineStyleAttributeName
import platform.UIKit.NSUnderlineStyleSingle
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIFontWeightBold
import platform.UIKit.UIFontWeightRegular
import platform.UIKit.UIGraphicsPopContext
import platform.UIKit.UIGraphicsPushContext
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes
import kotlin.math.ceil

@OptIn(ExperimentalForeignApi::class)
class IosTextRasterizer : TextRasterizer {

    override fun measure(text: String, font: RasterFont): Float =
        NSString.create(string = text).sizeWithAttributes(attributes(font)).useContents { width.toFloat() }

    override fun renderLine(cells: List<RasterCell>, widthPx: Int, font: RasterFont): MonoBitmap {
        val attrs = attributes(font)
        val sizes = cells.map { NSString.create(string = it.text).sizeWithAttributes(attrs).useContents { width to height } }
        val lineHeight = uiFont(font).lineHeight
        val height = (ceil(maxOf(lineHeight, sizes.maxOfOrNull { it.second } ?: 0.0)).toInt() + LINE_PADDING_PX)
            .coerceAtLeast(1)
        val pixels = ByteArray(widthPx * height)
        pixels.usePinned { pinned ->
            val colorSpace = CGColorSpaceCreateDeviceGray()
            val context = CGBitmapContextCreate(
                pinned.addressOf(0), widthPx.toULong(), height.toULong(), 8u, widthPx.toULong(),
                colorSpace, CGImageAlphaInfo.kCGImageAlphaNone.value,
            )
            CGContextSetGrayFillColor(context, 1.0, 1.0)
            CGContextFillRect(context, CGRectMake(0.0, 0.0, widthPx.toDouble(), height.toDouble()))
            CGContextTranslateCTM(context, 0.0, height.toDouble())
            CGContextScaleCTM(context, 1.0, -1.0)
            UIGraphicsPushContext(context)
            cells.forEachIndexed { i, cell ->
                val textWidth = sizes[i].first
                val x = when (cell.alignment) {
                    PrintAlignment.LEFT -> cell.left.toDouble()
                    PrintAlignment.CENTER -> cell.left + (cell.width - textWidth) / 2.0
                    PrintAlignment.RIGHT -> cell.left + cell.width - textWidth
                }
                NSString.create(string = cell.text)
                    .drawAtPoint(CGPointMake(x.coerceAtLeast(0.0), LINE_PADDING_PX / 2.0), withAttributes = attrs)
            }
            UIGraphicsPopContext()
            CGContextRelease(context)
            CGColorSpaceRelease(colorSpace)
        }
        return monoBitmapOf(widthPx, height) { x, y -> (pixels[y * widthPx + x].toInt() and 0xFF) < DARK_THRESHOLD }
    }

    private fun attributes(font: RasterFont): Map<Any?, Any?> = buildMap {
        put(NSFontAttributeName, uiFont(font))
        put(NSForegroundColorAttributeName, UIColor.blackColor)
        if (font.underline) put(NSUnderlineStyleAttributeName, NSUnderlineStyleSingle)
    }

    private fun uiFont(font: RasterFont): UIFont {
        val size = font.sizePx.toDouble()
        val system = if (font.bold) UIFont.boldSystemFontOfSize(size) else UIFont.systemFontOfSize(size)
        return when (font.typeface) {
            RasterTypeface.SANS -> system
            RasterTypeface.SERIF -> UIFont.fontWithName(if (font.bold) SERIF_BOLD else SERIF_REGULAR, size) ?: system
            RasterTypeface.MONOSPACE ->
                UIFont.monospacedSystemFontOfSize(size, if (font.bold) UIFontWeightBold else UIFontWeightRegular)
        }
    }

    private companion object {
        const val LINE_PADDING_PX = 4
        const val DARK_THRESHOLD = 140
        const val SERIF_REGULAR = "TimesNewRomanPSMT"
        const val SERIF_BOLD = "TimesNewRomanPS-BoldMT"
    }
}
