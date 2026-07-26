package org.bhargav.pansariwala.voice

import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductUnit
import kotlin.math.min

data class ProductMatch(
    val product: Product,
    val score: Double,
)

/**
 * Phonetic-ish + Levenshtein matcher over name / nameHi / voiceAlias.
 */
object ProductFuzzyMatcher {

    private const val MIN_SCORE = 0.42

    fun findBest(query: String, catalog: List<Product>): ProductMatch? {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank() || catalog.isEmpty()) return null

        var best: ProductMatch? = null
        for (product in catalog) {
            val score = scoreProduct(normalizedQuery, product)
            if (score >= MIN_SCORE && (best == null || score > best.score)) {
                best = ProductMatch(product, score)
            }
        }
        return best
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

    private fun scoreProduct(query: String, product: Product): Double {
        val candidates = buildList {
            add(product.name)
            add(product.nameHi)
            product.voiceAlias?.let { add(it) }
            // Also try individual alias tokens separated by /
            product.voiceAlias?.split(',', '/', '|')?.forEach { add(it) }
        }.map { normalize(it) }.filter { it.isNotBlank() }

        return candidates.maxOf { candidate ->
            when {
                candidate == query -> 1.0
                candidate.contains(query) || query.contains(candidate) -> 0.92
                tokensOverlap(query, candidate) -> 0.85
                else -> 1.0 - (levenshtein(query, candidate).toDouble() / maxOf(query.length, candidate.length, 1))
            }
        }
    }

    private fun tokensOverlap(a: String, b: String): Boolean {
        val ta = a.split(' ').filter { it.length >= 2 }.toSet()
        val tb = b.split(' ').filter { it.length >= 2 }.toSet()
        if (ta.isEmpty() || tb.isEmpty()) return false
        return ta.any { it in tb } || tb.any { t -> ta.any { it.startsWith(t) || t.startsWith(it) } }
    }

    private fun normalize(value: String): String {
        return value
            .lowercase()
            .replace('ँ', 'ं')
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .let { applyAliases(it) }
    }

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
