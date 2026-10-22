package org.bhargav.pansariwala.voice

import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ProductVariant
import kotlin.math.abs
import kotlin.math.min

data class ProductMatch(
    val product: Product,
    val score: Double,
)

/**
 * Phonetic-ish + Levenshtein matcher over name / nameHi / voiceAlias. Every name is compared as spoken
 * and transliterated to Latin, so English names match Devanagari STT output and vice versa.
 */
object ProductFuzzyMatcher {

    private const val MIN_SCORE = 0.42
    private const val TOKEN_OVERLAP_SCORE = 0.75

    /** Auto-add to cart only at or above this score; weaker matches need the cashier to confirm. */
    const val CONFIDENT_SCORE = 0.8

    /** A different product scoring within this of the best makes the match ambiguous. */
    const val AMBIGUITY_MARGIN = 0.05

    fun findBest(query: String, catalog: List<Product>): ProductMatch? = findCandidates(query, catalog, 1).firstOrNull()

    /** Best [limit] matches above the minimum score, highest first. */
    fun findCandidates(query: String, catalog: List<Product>, limit: Int): List<ProductMatch> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank() || catalog.isEmpty()) return emptyList()
        val latinQuery = latin(clean(query))
        return catalog.asSequence()
            .map { ProductMatch(it, scoreProduct(normalizedQuery, latinQuery, it)) }
            .filter { it.score >= MIN_SCORE }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    /** True when [best] is confident and no other candidate is nearly as good. */
    fun isConfident(candidates: List<ProductMatch>): Boolean {
        val best = candidates.firstOrNull() ?: return false
        if (best.score < CONFIDENT_SCORE) return false
        val runnerUp = candidates.getOrNull(1) ?: return true
        return best.score - runnerUp.score > AMBIGUITY_MARGIN
    }

    fun convertQuantity(
        spokenQty: Double,
        spokenUnit: ProductUnit?,
        productUnit: ProductUnit,
    ): Double {
        if (spokenUnit == null) return spokenQty
        if (spokenUnit == productUnit) return spokenQty

        // Weight family
        if (spokenUnit == ProductUnit.GRAM && productUnit == ProductUnit.KG) return spokenQty / 1000.0
        if (spokenUnit == ProductUnit.KG && productUnit == ProductUnit.GRAM) return spokenQty * 1000.0

        // Packaged goods spoken by weight → one pack
        if (productUnit == ProductUnit.PACKET || productUnit == ProductUnit.PIECE) {
            if (spokenUnit == ProductUnit.GRAM || spokenUnit == ProductUnit.KG || spokenUnit == ProductUnit.LITRE) {
                return 1.0
            }
        }
        return spokenQty
    }

    /**
     * The active variant the speaker meant: a spoken pack size ("parle g 100 gram" → the 100 g pack)
     * or label words ("parle g bada" → "Large"). Null when there are no variants or none fits.
     */
    fun findVariant(query: String, spokenQty: Double, spokenUnit: ProductUnit?, product: Product): ProductVariant? {
        val variants = product.activeVariants
        if (variants.isEmpty()) return null
        spokenUnit?.let { unit ->
            variants.firstOrNull { it.hasPackSize && sameAmount(it.packSize, it.packUnit, spokenQty, unit) }?.let { return it }
        }
        val nameTokens = normalize(product.name).split(' ').toSet()
        val tokens = normalize(query).split(' ')
            .filter { it.length >= 2 && it !in nameTokens }
            .map { SIZE_WORDS[it] ?: it }
            .toSet()
        if (tokens.isEmpty()) return null
        return variants
            .map { v -> v to normalize((listOf(v.label) + v.attributes.values).joinToString(" ")) }
            .map { (v, label) -> v to tokens.count { label.contains(it) } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    /** True when the spoken amount is this pack's net content ("100 gram" → the 100 g pack). */
    fun matchesPackSize(variant: ProductVariant, spokenQty: Double, spokenUnit: ProductUnit?): Boolean =
        spokenUnit != null && variant.hasPackSize && sameAmount(variant.packSize, variant.packUnit, spokenQty, spokenUnit)

    /** Spoken unit can be converted exactly into [target] (same unit, or g ↔ kg). */
    fun unitsCompatible(spoken: ProductUnit, target: ProductUnit): Boolean =
        spoken == target || setOf(spoken, target) == setOf(ProductUnit.GRAM, ProductUnit.KG)

    private fun sameAmount(packSize: Double, packUnit: ProductUnit, spokenQty: Double, spokenUnit: ProductUnit): Boolean {
        val a = baseAmount(packSize, packUnit) ?: return false
        val b = baseAmount(spokenQty, spokenUnit) ?: return false
        return abs(a - b) < AMOUNT_EPSILON
    }

    /** Grams (or ml) for weight/volume units; null for counts. */
    private fun baseAmount(value: Double, unit: ProductUnit): Double? = when (unit) {
        ProductUnit.GRAM -> value
        ProductUnit.KG, ProductUnit.LITRE -> value * GRAMS_PER_KG
        else -> null
    }

    private const val GRAMS_PER_KG = 1000.0
    private const val AMOUNT_EPSILON = 0.001

    /** Spoken size words mapped onto the English labels used for pack variants. */
    private val SIZE_WORDS = mapOf(
        "chota" to "small", "chhota" to "small", "chote" to "small", "chhote" to "small", "छोटा" to "small",
        "bada" to "large", "bara" to "large", "bade" to "large", "बड़ा" to "large", "big" to "large",
        "beech" to "medium", "मीडियम" to "medium", "mid" to "medium",
    )

    private fun scoreProduct(query: String, latinQuery: String, product: Product): Double {
        val raw = buildList {
            add(product.name)
            add(product.nameHi)
            addAll(product.translations.values)
            product.voiceAlias?.let { add(it) }
            // Also try individual alias tokens separated by /
            product.voiceAlias?.split(',', '/', '|')?.forEach { add(it) }
        }
        val candidates = raw.map { normalize(it) }.filter { it.isNotBlank() }
        if (candidates.isEmpty()) return 0.0

        val native = candidates.maxOf { similarity(query, it) }
        // Transliterated forms skip the Hinglish alias table so "शुगर" → "shugar" still meets "sugar".
        val latinCandidates = raw.map { latin(clean(it)) }.filter { it.isNotBlank() }
        val transliterated = if (latinQuery.isBlank()) 0.0
        else latinCandidates.maxOfOrNull { similarity(latinQuery, it) } ?: 0.0
        return maxOf(native, transliterated)
    }

    private fun similarity(query: String, candidate: String): Double {
        if (candidate == query) return 1.0
        if (candidate.contains(query) || query.contains(candidate)) return 0.92
        val edit = 1.0 - (levenshtein(query, candidate).toDouble() / maxOf(query.length, candidate.length, 1))
        // One shared word ("toor dal" vs "moong dal") is a lead, not proof: below CONFIDENT_SCORE.
        return if (tokensOverlap(query, candidate)) maxOf(edit, TOKEN_OVERLAP_SCORE) else edit
    }

    private fun latin(value: String): String = Transliterator.fold(Transliterator.toLatin(value))

    private fun tokensOverlap(a: String, b: String): Boolean {
        val ta = a.split(' ').filter { it.length >= 2 }.toSet()
        val tb = b.split(' ').filter { it.length >= 2 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return false
        return ta.any { it in tb } || tb.any { t -> ta.any { it.startsWith(t) || t.startsWith(it) } }
    }

    private fun normalize(value: String): String = applyAliases(clean(value, keepMarks = false))

    /**
     * Lowercase, punctuation stripped, whitespace collapsed; no alias rewriting. [keepMarks] keeps Devanagari
     * vowel signs, which transliteration needs ("शुगर" must not become "श गर").
     */
    private fun clean(value: String, keepMarks: Boolean = true): String = value
        .lowercase()
        .replace('ँ', 'ं')
        .replace(if (keepMarks) KEEP_WITH_MARKS else KEEP_LETTERS, " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private val KEEP_LETTERS = Regex("""[^\p{L}\p{N}\s]""")
    private val KEEP_WITH_MARKS = Regex("""[^\p{L}\p{M}\p{N}\s]""")

    /** Common Hinglish spelling variants → canonical forms used in seed aliases. */
    private fun applyAliases(value: String): String {
        var v = value
        val replacements = listOf(
            Regex("""\btuar\s*daals?\b""") to "toor dal",
            Regex("""\btuar\s*dals?\b""") to "toor dal",
            Regex("""\btoor\s*daals?\b""") to "toor dal",
            Regex("""\bchini\b""") to "cheeni",
            Regex("""\bsugar\b""") to "cheeni",
            Regex("""\bचीनी\b""") to "cheeni",
            Regex("""\bतूर\s*दाल\b""") to "toor dal",
            Regex("""\bdaal\b""") to "dal",
            Regex("""\bmasala\b""") to "garam masala",
            Regex("""\bमसाला\b""") to "garam masala",
            Regex("""\bआटा\b""") to "atta",
        )
        for ((pattern, replacement) in replacements) {
            v = pattern.replace(v, replacement)
        }
        return v
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            for (j in prev.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}
