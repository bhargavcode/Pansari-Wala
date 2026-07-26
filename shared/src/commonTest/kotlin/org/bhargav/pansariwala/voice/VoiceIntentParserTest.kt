package org.bhargav.pansariwala.voice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.ProductUnit

class VoiceIntentParserTest {

    @Test
    fun parsesHinglishMultiItemUtterance() {
        val items = VoiceIntentParser.parse("1 kilo chini, 2 kilo tuar daal, 50 gram masala")
        assertEquals(3, items.size)
        assertEquals(1.0, items[0].quantity)
        assertEquals(ProductUnit.KG, items[0].unit)
        assertEquals("chini", items[0].productQuery)
        assertEquals(2.0, items[1].quantity)
        assertEquals("tuar daal", items[1].productQuery)
        assertEquals(50.0, items[2].quantity)
        assertEquals(ProductUnit.GRAM, items[2].unit)
        assertEquals("masala", items[2].productQuery)
    }

    @Test
    fun parsesHindiProductThenQuantity() {
        val items = VoiceIntentParser.parse("धनिया पाउडर दो पैकेट")
        assertEquals(1, items.size)
        assertEquals(2.0, items[0].quantity)
        assertEquals(ProductUnit.PACKET, items[0].unit)
        assertEquals("धनिया पाउडर", items[0].productQuery)
    }

    @Test
    fun parsesQuantityOnEitherSideOfProduct() {
        val phrases = listOf(
            "1 packet dhaniya" to Triple(1.0, ProductUnit.PACKET, "dhaniya"),
            "dhaniya 1 packet" to Triple(1.0, ProductUnit.PACKET, "dhaniya"),
            "chini 1 kilo" to Triple(1.0, ProductUnit.KG, "chini"),
            "1 kilo chini" to Triple(1.0, ProductUnit.KG, "chini"),
            "adha kilo dahi" to Triple(0.5, ProductUnit.KG, "dahi"),
            "आधा किलो दही" to Triple(0.5, ProductUnit.KG, "दही"),
            "1 packet dahi" to Triple(1.0, ProductUnit.PACKET, "dahi"),
        )

        phrases.forEach { (phrase, expected) ->
            val item = VoiceIntentParser.parse(phrase).single()
            assertEquals(expected.first, item.quantity, phrase)
            assertEquals(expected.second, item.unit, phrase)
            assertEquals(expected.third, item.productQuery, phrase)
        }
    }

    @Test
    fun matchesSeedCatalogForSampleUtterance() {
        val catalog = SeedData.products()
        val items = VoiceIntentParser.parse("1 kilo chini, 2 kilo tuar daal, 50 gram masala")
        val matched = items.mapNotNull { line ->
            ProductFuzzyMatcher.findBest(line.productQuery, catalog)?.product?.name
        }
        assertTrue(matched.any { it.contains("Sugar", ignoreCase = true) })
        assertTrue(matched.any { it.contains("Toor", ignoreCase = true) })
        assertTrue(matched.any { it.contains("Masala", ignoreCase = true) })
    }

    @Test
    fun matchesDhaniyaHindiUtterance() {
        val catalog = SeedData.products()
        val items = VoiceIntentParser.parse("धनिया पाउडर दो पैकेट")
        val match = ProductFuzzyMatcher.findBest(items.single().productQuery, catalog)
        assertTrue(match != null)
        assertTrue(match!!.product.name.contains("Coriander", ignoreCase = true))
        val qty = ProductFuzzyMatcher.convertQuantity(
            items.single().quantity,
            items.single().unit,
            match.product.unit,
        )
        assertEquals(2.0, qty)
    }
}
