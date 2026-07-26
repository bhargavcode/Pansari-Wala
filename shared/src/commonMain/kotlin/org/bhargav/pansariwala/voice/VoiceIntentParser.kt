package org.bhargav.pansariwala.voice

import org.bhargav.pansariwala.domain.model.ProductUnit

/**
 * One spoken line item, e.g. "1 kilo chini" or "धनिया पाउडर दो पैकेट".
 */
data class VoiceLineItem(
    val quantity: Double,
    val unit: ProductUnit?,
    val productQuery: String,
    val raw: String,
)

/**
 * Rule-based Hindi/Hinglish parser for POS utterances.
 *
 * Supports both orders:
 * - qty + unit + product: "1 kilo chini", "दो पैकेट धनिया"
 * - product + qty + unit: "धनिया पाउडर दो पैकेट"
 */
object VoiceIntentParser {

    private val segmentSplit = Regex(
        """[,;|/]|और|\baur\b|\band\b|\bplus\b""",
        setOf(RegexOption.IGNORE_CASE),
    )

    // Avoid \b — it does not treat Devanagari as word chars in Kotlin/Java regex.
    private val unitToken = Regex(
        """(?<![^\s])(kilos?|kgs?|किलो|किलोग्राम|litres?|liters?|ltrs?|लीटर|grams?|gms?|ग्राम|packets?|packs?|pkt|पैकेट|पैक|pieces?|pcs|पीस|नग)(?![^\s])""",
        RegexOption.IGNORE_CASE,
    )

    private val quantityToken = Regex(
        """(?<![^\s])(\d+(?:[.,]\d+)?|आधा|अधा|आधी|aadha|adha|half|एक|दो|तीन|चार|पाँच|पांच|छह|छे|सात|आठ|नौ|दस)(?![^\s])""",
        RegexOption.IGNORE_CASE,
    )

    private val hindiDigits = mapOf(
        "आधा" to 0.5, "अधा" to 0.5, "आधी" to 0.5,
        "aadha" to 0.5, "adha" to 0.5, "half" to 0.5,
        "एक" to 1.0, "दो" to 2.0, "तीन" to 3.0, "चार" to 4.0,
        "पाँच" to 5.0, "पांच" to 5.0, "छह" to 6.0, "छे" to 6.0,
        "सात" to 7.0, "आठ" to 8.0, "नौ" to 9.0, "दस" to 10.0,
    )

    private fun mapUnit(token: String): ProductUnit = when (token.lowercase()) {
        "kilo", "kilos", "kg", "kgs", "किलो", "किलोग्राम" -> ProductUnit.KG
        "litre", "litres", "liter", "liters", "ltr", "ltrs", "लीटर" -> ProductUnit.LITRE
        "gram", "grams", "gm", "gms", "ग्राम" -> ProductUnit.GRAM
        "packet", "packets", "pack", "packs", "pkt", "पैकेट", "पैक" -> ProductUnit.PACKET
        else -> ProductUnit.PIECE
    }

    private fun mapQty(token: String): Double =
        hindiDigits[token.lowercase()]
            ?: hindiDigits[token]
            ?: token.replace(',', '.').toDoubleOrNull()
            ?: 1.0

    fun parse(utterance: String): List<VoiceLineItem> {
        val cleaned = utterance
            .replace(Regex("""[।.!?]"""), ",")
            .replace(Regex("""\bbill\s*(me|mein)?\s*(daalo|daldo|add)?\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\b(add|daalo|daldo)\b""", RegexOption.IGNORE_CASE), "")
            .trim()
        if (cleaned.isBlank()) return emptyList()

        return segmentSplit.split(cleaned)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { parseSegment(it) }
    }

    private fun parseSegment(raw: String): VoiceLineItem? {
        var rest = raw.trim().replace(Regex("""\s+"""), " ")
        if (rest.isBlank()) return null

        var quantity = 1.0
        var unit: ProductUnit? = null
        var qtyFound = false

        // Pattern A: leading "2 पैकेट ..." or "1 kilo ..."
        val leadingQty = quantityToken.find(rest)
        if (leadingQty != null && leadingQty.range.first == 0) {
            quantity = mapQty(leadingQty.groupValues[1])
            qtyFound = true
            rest = rest.removeRange(leadingQty.range).trim()
            val leadingUnit = unitToken.find(rest)
            if (leadingUnit != null && leadingUnit.range.first == 0) {
                unit = mapUnit(leadingUnit.groupValues[1])
                rest = rest.removeRange(leadingUnit.range).trim()
            }
        }

        // Pattern B: trailing "... दो पैकेट" / "... 2 packet" (common Hindi order)
        if (!qtyFound || unit == null) {
            val trailing = Regex(
                """\s+(\d+(?:[.,]\d+)?|आधा|अधा|आधी|aadha|adha|half|एक|दो|तीन|चार|पाँच|पांच|छह|छे|सात|आठ|नौ|दस)\s+""" +
                    """(kilos?|kgs?|किलो|किलोग्राम|litres?|liters?|ltrs?|लीटर|grams?|gms?|ग्राम|packets?|packs?|pkt|पैकेट|पैक|pieces?|pcs|पीस|नग)\s*$""",
                RegexOption.IGNORE_CASE,
            ).find(rest)
            if (trailing != null) {
                if (!qtyFound) {
                    quantity = mapQty(trailing.groupValues[1])
                    qtyFound = true
                }
                if (unit == null) {
                    unit = mapUnit(trailing.groupValues[2])
                }
                rest = rest.removeRange(trailing.range).trim()
            } else {
                // Trailing unit only: "... पैकेट"
                val trailingUnit = unitToken.findAll(rest).lastOrNull()
                if (trailingUnit != null && trailingUnit.range.last == rest.lastIndex) {
                    unit = mapUnit(trailingUnit.groupValues[1])
                    rest = rest.removeRange(trailingUnit.range).trim()
                    // Optional qty just before unit: "दो" left at end
                    val trailingQty = quantityToken.findAll(rest).lastOrNull()
                    if (!qtyFound && trailingQty != null && trailingQty.range.last == rest.lastIndex) {
                        quantity = mapQty(trailingQty.groupValues[1])
                        rest = rest.removeRange(trailingQty.range).trim()
                    }
                }
            }
        }

        // Any remaining mid-string unit token (e.g. after partial parses)
        if (unit == null) {
            val midUnit = unitToken.find(rest)
            if (midUnit != null) {
                unit = mapUnit(midUnit.groupValues[1])
                rest = rest.removeRange(midUnit.range).trim()
            }
        }

        val query = rest
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim(',', '.', '-', '_')
        if (query.isBlank()) return null

        return VoiceLineItem(
            quantity = quantity,
            unit = unit,
            productQuery = query,
            raw = raw,
        )
    }
}
