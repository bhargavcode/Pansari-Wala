package org.bhargav.pansariwala.data.seed

import org.bhargav.pansariwala.domain.auth.Credentials
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ShopUser
import org.bhargav.pansariwala.util.MILLIS_PER_DAY

data class SeedUser(
    val user: ShopUser,
    val passwordHash: String,
)

object SeedData {
    const val DEMO_SHOP_ID: String = "shop_1"

    fun users(): List<SeedUser> = listOf(
        SeedUser(
            user = ShopUser(
                id = "user_1",
                username = "owner",
                displayName = "Bhargav Pansari",
                shopId = DEMO_SHOP_ID,
            ),
            passwordHash = Credentials.hash("1234"),
        ),
        SeedUser(
            user = ShopUser(
                id = "user_2",
                username = "cashier",
                displayName = "Ramesh Kumar",
                shopId = DEMO_SHOP_ID,
            ),
            passwordHash = Credentials.hash("1234"),
        ),
    )

    fun products(): List<Product> {
        var counter = 0
        fun p(
            name: String,
            nameHi: String,
            category: ProductCategory,
            unit: ProductUnit,
            barcode: String,
            selling: Double,
            cost: Double,
            stock: Double,
            threshold: Double,
            alias: String,
        ): Product {
            counter += 1
            return Product(
                id = "prod_" + counter.toString().padStart(4, '0'),
                shopId = DEMO_SHOP_ID,
                name = name,
                nameHi = nameHi,
                category = category,
                unit = unit,
                barcode = barcode,
                sellingPrice = selling,
                costPrice = cost,
                stockQty = stock,
                lowStockThreshold = threshold,
                voiceAlias = alias,
            )
        }

        return listOf(
            // Fortune / Branded
            p("Fortune Sunflower Oil 1L", "फॉर्च्यून सूरजमुखी तेल 1L", ProductCategory.FORTUNE_BRANDED, ProductUnit.LITRE, "8901234500011", 160.0, 140.0, 48.0, 12.0, "sunflower oil"),
            p("Fortune Mustard Oil 1L", "फॉर्च्यून सरसों तेल 1L", ProductCategory.FORTUNE_BRANDED, ProductUnit.LITRE, "8901234500028", 175.0, 150.0, 9.0, 10.0, "sarson tel"),
            p("Fortune Chakki Atta 5kg", "फॉर्च्यून चक्की आटा 5kg", ProductCategory.FORTUNE_BRANDED, ProductUnit.PACKET, "8901234500035", 245.0, 220.0, 30.0, 8.0, "atta"),
            p("Fortune Basmati Rice 5kg", "फॉर्च्यून बासमती चावल 5kg", ProductCategory.FORTUNE_BRANDED, ProductUnit.PACKET, "8901234500042", 520.0, 470.0, 6.0, 8.0, "basmati chawal"),
            p("Fortune Soyabean Oil 1L", "फॉर्च्यून सोयाबीन तेल 1L", ProductCategory.FORTUNE_BRANDED, ProductUnit.LITRE, "8901234500059", 150.0, 132.0, 20.0, 10.0, "soya oil"),

            // General grocery
            p("Sugar", "चीनी", ProductCategory.GENERAL_GROCERY, ProductUnit.KG, "8901234510015", 45.0, 40.0, 120.0, 25.0, "cheeni,chini,sugar,चीनी"),
            p("Iodised Salt 1kg", "नमक 1kg", ProductCategory.GENERAL_GROCERY, ProductUnit.PACKET, "8901234510022", 28.0, 22.0, 80.0, 20.0, "namak"),
            p("Toor Dal", "तूर दाल", ProductCategory.GENERAL_GROCERY, ProductUnit.KG, "8901234510039", 140.0, 122.0, 7.0, 10.0, "toor dal,tuar dal,tuar daal,toor daal,तूर दाल"),
            p("Tea 250g", "चाय 250g", ProductCategory.GENERAL_GROCERY, ProductUnit.PACKET, "8901234510046", 130.0, 110.0, 40.0, 12.0, "chai"),
            p("Poha 500g", "पोहा 500g", ProductCategory.GENERAL_GROCERY, ProductUnit.PACKET, "8901234510053", 35.0, 28.0, 4.0, 10.0, "poha"),

            // Puja samagri
            p("Agarbatti Pack", "अगरबत्ती", ProductCategory.PUJA_SAMAGRI, ProductUnit.PACKET, "8901234520014", 40.0, 30.0, 60.0, 15.0, "agarbatti"),
            p("Camphor 50g", "कपूर 50g", ProductCategory.PUJA_SAMAGRI, ProductUnit.PACKET, "8901234520021", 90.0, 72.0, 5.0, 8.0, "kapoor"),
            p("Diya Pack", "दीया पैक", ProductCategory.PUJA_SAMAGRI, ProductUnit.PACKET, "8901234520038", 60.0, 45.0, 25.0, 10.0, "diya"),
            p("Roli Chawal", "रोली चावल", ProductCategory.PUJA_SAMAGRI, ProductUnit.PACKET, "8901234520045", 25.0, 18.0, 3.0, 8.0, "roli chawal"),

            // Standard spices
            p("Turmeric Powder 100g", "हल्दी पाउडर 100g", ProductCategory.STANDARD_SPICES, ProductUnit.PACKET, "8901234530013", 35.0, 27.0, 55.0, 15.0, "haldi"),
            p("Red Chilli Powder 100g", "लाल मिर्च 100g", ProductCategory.STANDARD_SPICES, ProductUnit.PACKET, "8901234530020", 45.0, 35.0, 6.0, 12.0, "mirchi"),
            p("Coriander Powder 100g", "धनिया पाउडर 100g", ProductCategory.STANDARD_SPICES, ProductUnit.PACKET, "8901234530037", 30.0, 23.0, 42.0, 12.0, "dhaniya,dhaniya powder,धनिया,धनिया पाउडर"),
            p("Garam Masala 50g", "गरम मसाला 50g", ProductCategory.STANDARD_SPICES, ProductUnit.PACKET, "8901234530044", 55.0, 42.0, 2.0, 8.0, "garam masala,masala,मसाला"),

            // High-value spices
            p("Saffron (Kesar) 1g", "केसर 1g", ProductCategory.HIGH_VALUE_SPICES, ProductUnit.GRAM, "8901234540012", 320.0, 260.0, 8.0, 5.0, "kesar"),
            p("Green Cardamom 50g", "हरी इलायची 50g", ProductCategory.HIGH_VALUE_SPICES, ProductUnit.PACKET, "8901234540029", 180.0, 150.0, 3.0, 6.0, "elaichi"),
            p("Black Pepper 100g", "काली मिर्च 100g", ProductCategory.HIGH_VALUE_SPICES, ProductUnit.PACKET, "8901234540036", 120.0, 95.0, 18.0, 8.0, "kali mirch"),
            p("Cloves 50g", "लौंग 50g", ProductCategory.HIGH_VALUE_SPICES, ProductUnit.PACKET, "8901234540043", 95.0, 78.0, 4.0, 6.0, "laung"),
            p("Curd (Dahi)", "दही", ProductCategory.GENERAL_GROCERY, ProductUnit.KG, "8901234510060", 80.0, 62.0, 20.0, 5.0, "dahi,curd,दही"),
        )
    }

    /**
     * A handful of recent orders. [now] is injected so a few land in "today" for the
     * dashboard sales card and others in previous days.
     */
    fun orders(now: Long, products: List<Product>): List<Order> {
        fun item(index: Int, qty: Double): OrderItem {
            val product = products[index]
            return OrderItem(
                productId = product.id,
                productName = product.name,
                unit = product.unit,
                quantity = qty,
                unitPrice = product.sellingPrice,
            )
        }
        return listOf(
            Order(
                id = "order_0001",
                shopId = DEMO_SHOP_ID,
                createdAtEpochMs = now - 1 * 60 * 60 * 1000L,
                status = OrderStatus.COMPLETED,
                customerName = "Suresh",
                items = listOf(item(0, 2.0), item(5, 3.0), item(14, 1.0)),
            ),
            Order(
                id = "order_0002",
                shopId = DEMO_SHOP_ID,
                createdAtEpochMs = now - 3 * 60 * 60 * 1000L,
                status = OrderStatus.COMPLETED,
                customerName = "Anita",
                items = listOf(item(2, 1.0), item(8, 2.0)),
            ),
            Order(
                id = "order_0003",
                shopId = DEMO_SHOP_ID,
                createdAtEpochMs = now - 5 * 60 * 60 * 1000L,
                status = OrderStatus.COMPLETED,
                customerName = "Walk-in",
                items = listOf(item(18, 1.0), item(19, 1.0)),
            ),
            Order(
                id = "order_0004",
                shopId = DEMO_SHOP_ID,
                createdAtEpochMs = now - 1 * MILLIS_PER_DAY,
                status = OrderStatus.COMPLETED,
                customerName = "Mahesh",
                items = listOf(item(3, 1.0), item(6, 2.0), item(10, 4.0)),
            ),
            Order(
                id = "order_0005",
                shopId = DEMO_SHOP_ID,
                createdAtEpochMs = now - 2 * MILLIS_PER_DAY,
                status = OrderStatus.COMPLETED,
                customerName = "Priya",
                items = listOf(item(15, 2.0), item(16, 1.0)),
            ),
        )
    }
}
