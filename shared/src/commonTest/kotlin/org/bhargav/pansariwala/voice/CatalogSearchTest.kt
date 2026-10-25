package org.bhargav.pansariwala.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.bhargav.pansariwala.data.seed.SeedData

class CatalogSearchTest {

    private val base = SeedData.products().first()
    private val catalog = listOf(
        base.copy(id = "p1", name = "Parle-G Biscuit", nameHi = "", voiceAlias = null, brandName = "Parle"),
        base.copy(id = "p2", name = "Monaco Biscuit", nameHi = "", voiceAlias = null, brandName = "Parle"),
        base.copy(id = "p3", name = "Soyabean Oil", nameHi = "", voiceAlias = null, brandName = "Fortune"),
        base.copy(id = "p4", name = "Cheeni", nameHi = "चीनी", voiceAlias = null, brandName = ""),
    )

    @Test
    fun brandNameListsAllItsProducts() {
        val result = ProductFuzzyMatcher.search("parle", catalog)!!
        assertEquals(CatalogMatchKind.BRAND, result.kind)
        assertEquals(setOf("p1", "p2"), result.products.map { it.id }.toSet())
    }

    @Test
    fun brandPlusProductPicksTheProduct() {
        val result = ProductFuzzyMatcher.search("parle g", catalog)!!
        assertEquals(CatalogMatchKind.PRODUCT, result.kind)
        assertEquals("p1", result.products.first().id)
        assertTrue(ProductFuzzyMatcher.isConfident(result.matches))
    }

    @Test
    fun wordOrderAndFillerDoNotMatter() {
        val best = ProductFuzzyMatcher.findBest("oil fortune wala", catalog)
        assertEquals("p3", best?.product?.id)
    }

    @Test
    fun soundAlikeSpellingMatches() {
        assertEquals("p4", ProductFuzzyMatcher.findBest("chini", catalog)?.product?.id)
        assertEquals("p4", ProductFuzzyMatcher.findBest("चीनी", catalog)?.product?.id)
    }
}
