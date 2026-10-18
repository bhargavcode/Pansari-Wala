package org.bhargav.pansariwala.server.service

import org.bhargav.pansariwala.server.dto.ProductVariantDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductSheetTest {
    @Test
    fun templateRoundTripsIntoProductsWithVariants() {
        val groups = ProductSheet.group(ProductSheet.read(ProductSheet.template(), "sample.xlsx"))
        assertEquals(3, groups.size)
        val parle = groups.first()
        assertEquals(4, parle.variantRows.size)
        val merged = ProductSheet.mergeVariants(emptyList(), parle.variantRows)
        assertEquals(4, merged.created)
        val carton = merged.variants.first { it.sellMode == "BUNDLE" }
        assertEquals(merged.variants.first { it.name == "Medium" }.id, carton.baseVariantId)
        assertEquals("8901719110016", merged.variants.first().barcode)
        assertEquals(25.0, merged.variants.first().packSize)
    }

    @Test
    fun stockAddUpdatesExistingVariantAndKeepsOthers() {
        val current = listOf(
            ProductVariantDto(name = "Small", id = "v1", price = 5.0, stockQty = 10.0),
            ProductVariantDto(name = "Large", id = "v2", price = 50.0, stockQty = 3.0),
        )
        val csv = "product_id,variant_id,stock_add\np1,v1,24\n".toByteArray()
        val rows = ProductSheet.group(ProductSheet.read(csv, "stock.csv")).single().variantRows
        val merged = ProductSheet.mergeVariants(current, rows)
        assertEquals(34.0, merged.variants.first { it.id == "v1" }.stockQty)
        assertEquals(3.0, merged.variants.first { it.id == "v2" }.stockQty)
        assertEquals(1, merged.updated)
    }

    @Test
    fun newVariantWithoutPriceIsRejected() {
        val csv = "name,variant_label\nParle-G,Small\n".toByteArray()
        val rows = ProductSheet.group(ProductSheet.read(csv, "x.csv")).single().variantRows
        assertFailsWith<IllegalArgumentException> { ProductSheet.mergeVariants(emptyList(), rows) }
    }
}
