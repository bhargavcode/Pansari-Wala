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

enum class CatalogMatchKind { PRODUCT, BRAND, CATEGORY }

/** What a spoken/typed query refers to: a whole brand or category, or ranked product matches. */
data class CatalogSearch(
    val kind: CatalogMatchKind,
    val label: String,
    val matches: List<ProductMatch>,
) {
    val products: List<Product> get() = matches.map { it.product }
}

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

    /** A brand / category is meant when the whole query is (nearly) its name. */
    private const val GROUP_SCORE = 0.86

    /** Matches this far below the best are noise for the results list. */
    private const val LIST_WINDOW = 0.3

    fun findBest(query: String, catalog: List<Product>): ProductMatch? = findCandidates(query, catalog, 1).firstOrNull()

    /** Best [limit] matches above the minimum score, highest first. */
    fun findCandidates(query: String, catalog: List<Product>, limit: Int): List<ProductMatch> {
        val q = Query.of(query) ?: return emptyList()
        if (catalog.isEmpty()) return emptyList()
        val index = indexFor(catalog)
        return catalog.asSequence()
            .map { ProductMatch(it, index[it.id]?.score(q) ?: 0.0) }
            .filter { it.score >= MIN_SCORE }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    /**
     * Brand ("fortune") → all its products, category ("masale") → all in it, else ranked product matches
     * within [LIST_WINDOW] of the best. Null when nothing is close.
     */
    fun search(query: String, catalog: List<Product>, limit: Int = MAX_LIST): CatalogSearch? {
        val q = Query.of(query) ?: return null
        if (catalog.isEmpty()) return null
        val matches = findCandidates(query, catalog, limit)
        val bestScore = matches.firstOrNull()?.score ?: 0.0
        val brands = catalog.flatMap { p ->
            (listOf(p.brandName) + p.brandTranslations.values).map { it.trim() }.filter { it.isNotEmpty() }.map { it to p }
        }
        bestGroup(q, brands, bestScore)?.let { (label, products) -> return group(CatalogMatchKind.BRAND, label, products) }
        val categories = catalog.flatMap { p -> listOf(p.category.displayName to p, p.category.displayNameHi to p) }
        bestGroup(q, categories, bestScore)?.let { (label, products) -> return group(CatalogMatchKind.CATEGORY, label, products) }
        val best = matches.firstOrNull() ?: return null
        val shown = matches.filter { it.score >= best.score - LIST_WINDOW }
        return CatalogSearch(CatalogMatchKind.PRODUCT, query.trim(), shown)
    }

    private fun group(kind: CatalogMatchKind, label: String, products: List<Product>) = CatalogSearch(
        kind,
        label,
        products.distinctBy { it.id }.sortedBy { it.name.lowercase() }.map { ProductMatch(it, 1.0) },
    )

    /** A group wins only when it fits the query better than any single product ("parle" vs "parle g"). */
    private fun bestGroup(q: Query, named: List<Pair<String, Product>>, bestProduct: Double): Pair<String, List<Product>>? {
        if (named.isEmpty()) return null
        val byName = named.groupBy({ it.first }, { it.second })
        val (label, score) = byName.keys.map { it to Forms.of(listOf(it)).fullScore(q) }.maxByOrNull { it.second } ?: return null
        return if (score >= GROUP_SCORE && score > bestProduct + GROUP_MARGIN) label to byName.getValue(label) else null
    }

    private const val MAX_LIST = 200
    private const val GROUP_MARGIN = 0.02

    /** Words around an item name that carry no product meaning ("do", "dena", "wala"). */
    private val FILLER = setOf(
        "wala", "wali", "vala", "vali", "ka", "ki", "ke", "de", "do", "dena", "dedo", "dijiye", "dijie", "chahiye",
        "chahie", "please", "bhai", "bhaiya", "bhaiyya", "ji", "ek", "the", "a", "of",
    )

    private class Query(val native: String, val latin: String, val phon: String, val tokens: List<String>) {
        companion object {
            fun of(raw: String): Query? {
                val native = normalize(raw)
                if (native.isBlank()) return null
                val latinForm = latin(clean(raw))
                val tokens = latinForm.split(' ')
                    .filter { it.isNotBlank() && it !in FILLER }
                    .map { phonetic(it) }
                    .filter { it.isNotEmpty() }
                return Query(native, latinForm, phonetic(latinForm), tokens)
            }
        }
    }

    /** Precomputed spellings of one product's names (native, Latin, phonetic) and its phonetic words. */
    private class Forms(val native: List<String>, val latin: List<String>, val phon: List<String>, val tokens: Set<String>) {
        fun fullScore(q: Query): Double {
            val n = native.maxOfOrNull { similarity(q.native, it) } ?: 0.0
            val l = if (q.latin.isBlank()) 0.0 else latin.maxOfOrNull { similarity(q.latin, it) } ?: 0.0
            val p = if (q.phon.isBlank()) 0.0 else phon.maxOfOrNull { similarity(q.phon, it) } ?: 0.0
            return maxOf(n, l, p)
        }

        /** Every spoken word found in the names (any order, extra name words allowed). */
        fun coverage(q: Query): Double {
            if (q.tokens.isEmpty() || tokens.isEmpty()) return 0.0
            return q.tokens.sumOf { qt -> tokens.maxOf { tokenSimilarity(qt, it) } } / q.tokens.size
        }

        fun score(q: Query): Double {
            val full = fullScore(q)
            val cov = coverage(q)
            // All words matched strongly → as good as an exact name; partial cover stays below CONFIDENT_SCORE.
            val covered = if (cov >= TOKEN_STRONG) cov * COVERAGE_WEIGHT else cov * PARTIAL_COVERAGE_WEIGHT
            return maxOf(full, covered)
        }

        companion object {
            fun of(raw: List<String>): Forms {
                val names = raw.filter { it.isNotBlank() }
                val latinForms = names.map { latin(clean(it)) }.filter { it.isNotBlank() }
                val phon = latinForms.map { phonetic(it) }.filter { it.isNotBlank() }
                return Forms(
                    native = names.map { normalize(it) }.filter { it.isNotBlank() },
                    latin = latinForms,
                    phon = phon,
                    tokens = phon.flatMap { it.split(' ') }.filter { it.isNotEmpty() }.toSet(),
                )
            }
        }
    }

    private const val TOKEN_STRONG = 0.85
    private const val COVERAGE_WEIGHT = 0.95
    private const val PARTIAL_COVERAGE_WEIGHT = 0.7

    private fun tokenSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length >= 3 && b.startsWith(a)) return 0.9
        if (b.length >= 3 && a.startsWith(b)) return 0.85
        return 1.0 - levenshtein(a, b).toDouble() / maxOf(a.length, b.length, 1)
    }

    private var indexedCatalog: List<Product>? = null
    private var index: Map<String, Forms> = emptyMap()

    /** Built once per catalog list instance; repeated voice phrases reuse it. */
    private fun indexFor(catalog: List<Product>): Map<String, Forms> {
        if (indexedCatalog !== catalog) {
            index = catalog.associate { it.id to Forms.of(productNames(it)) }
            indexedCatalog = catalog
        }
        return index
    }

    private fun productNames(product: Product): List<String> = buildList {
        add(product.name)
        add(product.nameHi)
        addAll(product.translations.values)
        product.voiceAlias?.let { alias ->
            add(alias)
            addAll(alias.split(',', '/', '|'))
        }
        if (product.brandName.isNotBlank()) {
            add("${product.brandName} ${product.name}")
            product.brandTranslations.values.forEach { add("$it ${product.name}") }
        }
    }

    /** Sound-alike key: "chini"/"cheeni", "shakkar"/"sakar", "jeera"/"zira" collapse to one spelling. */
    internal fun phonetic(latin: String): String {
        var v = latin.lowercase()
        for ((from, to) in PHONETIC_RULES) v = v.replace(from, to)
        val out = StringBuilder()
        for (c in v) if (out.isEmpty() || out.last() != c || c == ' ') out.append(c)
        return out.toString().trim()
    }

    private val PHONETIC_RULES = listOf(
        Regex("chh|ch") to "x",
        Regex("c(?=[eiy])") to "s",
        Regex("ck|c|q") to "k",
        Regex("sh") to "s",
        Regex("kh") to "k",
        Regex("gh") to "g",
        Regex("th") to "t",
        Regex("dh") to "d",
        Regex("bh") to "b",
        Regex("jh|z") to "j",
        Regex("ph") to "f",
        Regex("w") to "v",
        Regex("ee|ii|y") to "i",
        Regex("oo|uu") to "u",
        Regex("aa") to "a",
        Regex("(?<=[a-z])h(?=\\s|$)") to "",
    )

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
