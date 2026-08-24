package org.bhargav.pansariwala.server.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import java.util.concurrent.TimeUnit
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoDatabase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.dto.OrderItemDto
import org.bhargav.pansariwala.server.dto.QuoteDto
import org.bhargav.pansariwala.server.security.Security
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.kotlinx.KotlinSerializerCodecProvider

class MongoApp(val client: MongoClient, val db: MongoDatabase)

@Serializable
data class ShopDoc(
    @SerialName("_id") val id: String,
    val name: String,
    val imageUrl: String? = null,
    val rating: Double,
    val ratingCount: Int,
    val lat: Double,
    val lng: Double,
    val isOpen: Boolean,
    val active: Boolean,
    val paymentsEnabled: Boolean,
    val discountPercent: Double,
    val upiId: String = "success@razorpay",
    val address: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val shopType: String = "GENERAL_STORE",
)

@Serializable
data class ShopUserDoc(
    @SerialName("_id") val id: String,
    val shopId: String,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val role: String,
)

@Serializable
data class CustomerDoc(
    @SerialName("_id") val id: String,
    val phone: String,
    val name: String,
    val address: String,
    val locality: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val addresses: List<CustomerAddressDoc> = emptyList(),
)

@Serializable
data class CustomerAddressDoc(
    val id: String,
    val line: String,
    val locality: String = "",
    val lat: Double,
    val lng: Double,
    val isDefault: Boolean = false,
)

@Serializable
data class PartnerDoc(
    @SerialName("_id") val id: String,
    val phone: String,
    val name: String,
    val email: String,
    val address: String,
    val vehicleReg: String,
    val platePhoto: String = "",
    val vehiclePhoto: String = "",
    val profilePhoto: String = "",
    val dlPhoto: String = "",
    val idPhoto: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val verified: Boolean = false,
    val online: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CategoryDoc(
    @SerialName("_id") val id: String,
    val name: String,
    val parentId: String? = null,
)

@Serializable
data class MasterProductDoc(
    @SerialName("_id") val id: String,
    val name: String,
    val nameHi: String,
    val categoryId: String,
    val unit: String,
    val barcode: String? = null,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
data class ShopTypeDoc(
    @SerialName("_id") val id: String,
    val name: String,
    val active: Boolean = true,
)

@Serializable
data class ProductDoc(
    @SerialName("_id") val id: String,
    val shopId: String,
    val name: String,
    val nameHi: String,
    val category: String,
    val unit: String,
    val barcode: String? = null,
    val sellingPrice: Double,
    val costPrice: Double,
    val stockQty: Double,
    val lowStockThreshold: Double,
    val voiceAlias: String? = null,
)

@Serializable
data class OfferDoc(
    @SerialName("_id") val id: String,
    val shopId: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
)

@Serializable
data class OrderDoc(
    @SerialName("_id") val id: String,
    val shopId: String,
    val customerId: String? = null,
    val partnerId: String? = null,
    val status: String,
    val channel: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val deliveryAddress: String? = null,
    val dropoffInstructions: String? = null,
    val deliveryOtp: String? = null,
    val pickupPhotos: List<String> = emptyList(),
    val paymentId: String? = null,
    val paymentMethod: String = "ONLINE",
    val razorpayOrderId: String? = null,
    val shopUpi: String? = null,
    val createdAt: Long,
    val items: List<OrderItemDto> = emptyList(),
    val quote: QuoteDto? = null,
    val ratingStars: Int? = null,
    val ratingComment: String? = null,
    val cancelReason: String? = null,
    val refundId: String? = null,
    val totalDistanceKm: Double? = null,
    val deliveryDurationMin: Int? = null,
    val partnerPayoutInr: Double? = null,
    val partnerProgress: String = "",
)

@Serializable
data class TxnDoc(
    @SerialName("_id") val id: String,
    val orderId: String,
    val customerId: String,
    val amount: Double,
    val title: String,
    val createdAt: Long,
)

@Serializable
data class DeliveryOfferDoc(
    @SerialName("_id") val id: String,
    val orderId: String,
    val shopId: String,
    val status: String,
    val payout: Double,
    val shopDistanceKm: Double,
    val dropDistanceKm: Double,
    val expiresAt: Long,
    val acceptedBy: String? = null,
    val rejectedBy: List<String> = emptyList(),
    val dropAddress: String,
)

@Serializable
data class OtpDoc(
    @SerialName("_id") val sessionId: String,
    val phone: String,
    val codeHash: String,
    val purpose: String,
    val expiresAt: Long,
)

@Serializable
data class AdminUserDoc(
    @SerialName("_id") val id: String,
    val username: String,
    val passwordHash: String,
)

fun connectMongo(config: ServerConfig, security: Security): MongoApp {
    val codecRegistry = fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        fromProviders(KotlinSerializerCodecProvider()),
    )
    val settings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(config.mongoUri))
        .codecRegistry(codecRegistry)
        .applyToClusterSettings { it.serverSelectionTimeout(8, TimeUnit.SECONDS) }
        .applyToSocketSettings {
            it.connectTimeout(5, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
        }
        .applyToConnectionPoolSettings {
            it.minSize(2)
                .maxSize(20)
                .maxWaitTime(5, TimeUnit.SECONDS)
                .maxConnectionIdleTime(10, TimeUnit.MINUTES)
        }
        .applyToServerSettings { it.heartbeatFrequency(10, TimeUnit.SECONDS) }
        .build()
    val client = MongoClient.create(settings)
    val db = client.getDatabase(config.mongoDbName)
    ensureIndexes(db)
    if (db.getCollection<ShopDoc>("shops").countDocuments() == 0L) {
        seed(db, security, config)
    }
    ensureShopUpis(db)
    ensureDemoShopUsers(db, security)
    ensureMasterAdmin(db, security, config)
    ensureShopTypes(db)
    return MongoApp(client, db)
}

private fun ensureDemoShopUsers(db: MongoDatabase, security: Security) {
    val users = db.getCollection<ShopUserDoc>("shop_users")
    listOf(
        ShopUserDoc("user_owner", "shop_1", "owner", security.hashPassword("1234"), "Owner", "SHOP"),
        ShopUserDoc("user_cashier", "shop_1", "cashier", security.hashPassword("1234"), "Cashier", "SHOP"),
    ).forEach { demo ->
        if (users.find(eq("username", demo.username)).firstOrNull() == null) {
            users.insertOne(demo)
        }
    }
}

/** Creates or rotates master admin from ADMIN_USERNAME / ADMIN_PASSWORD. Never hardcodes credentials. */
private fun ensureMasterAdmin(db: MongoDatabase, security: Security, config: ServerConfig) {
    val username = config.adminUsername.trim().ifBlank { "bhargav" }
    val password = config.adminPassword
    if (password.isBlank()) {
        if (config.devAuth) {
            println("WARN: ADMIN_PASSWORD unset — skip master admin seed/rotate (set before production)")
            return
        }
        error("ADMIN_PASSWORD is required when AUTH_DEV_MODE=false")
    }
    val admins = db.getCollection<AdminUserDoc>("admin_users")
    val hash = security.hashPassword(password)
    val existing = admins.find(eq("username", username)).firstOrNull()
    if (existing == null) {
        admins.insertOne(AdminUserDoc("admin_1", username, hash))
    } else {
        admins.updateOne(eq("_id", existing.id), set("passwordHash", hash))
    }
    // Remove legacy default admin account if username changed
    if (username != "admin") {
        admins.deleteMany(eq("username", "admin"))
    }
}

private fun defaultShopTypes(): List<ShopTypeDoc> = listOf(
    ShopTypeDoc("GENERAL_STORE", "General Store"),
    ShopTypeDoc("MEDICAL_STORE", "Medical Store"),
    ShopTypeDoc("HARDWARE", "Hardware"),
    ShopTypeDoc("SWEET_SHOP", "Sweet Shop"),
)

private fun ensureShopTypes(db: MongoDatabase) {
    val col = db.getCollection<ShopTypeDoc>("master_shop_types")
    defaultShopTypes().forEach { row ->
        if (col.find(eq("_id", row.id)).firstOrNull() == null) {
            col.insertOne(row)
        }
    }
}

private fun ensureShopUpis(db: MongoDatabase) {
    db.getCollection<ShopDoc>("shops").updateMany(
        or(exists("upiId", false), eq("upiId", "")),
        set("upiId", "success@razorpay"),
    )
}

private fun ensureIndexes(db: MongoDatabase) {
    db.getCollection<ShopUserDoc>("shop_users")
        .createIndex(Indexes.ascending("username"), IndexOptions().unique(true))
    db.getCollection<CustomerDoc>("customers")
        .createIndex(Indexes.ascending("phone"), IndexOptions().unique(true))
    db.getCollection<PartnerDoc>("partners")
        .createIndex(Indexes.ascending("phone"), IndexOptions().unique(true))
    db.getCollection<AdminUserDoc>("admin_users")
        .createIndex(Indexes.ascending("username"), IndexOptions().unique(true))
    db.getCollection<OrderDoc>("orders").apply {
        createIndex(Indexes.compoundIndex(Indexes.ascending("customerId"), Indexes.descending("createdAt")))
        createIndex(Indexes.compoundIndex(Indexes.ascending("shopId"), Indexes.ascending("channel"), Indexes.descending("createdAt")))
        createIndex(Indexes.ascending("partnerId"))
        createIndex(Indexes.compoundIndex(Indexes.ascending("partnerId"), Indexes.ascending("status")))
    }
    db.getCollection<TxnDoc>("transactions")
        .createIndex(Indexes.compoundIndex(Indexes.ascending("customerId"), Indexes.descending("createdAt")))
    db.getCollection<ProductDoc>("products")
        .createIndex(Indexes.ascending("shopId"))
    db.getCollection<DeliveryOfferDoc>("delivery_offers").apply {
        createIndex(Indexes.ascending("orderId"))
        createIndex(Indexes.ascending("status"))
    }
}

private fun seed(db: MongoDatabase, security: Security, config: ServerConfig) {
    db.getCollection<ShopDoc>("shops").insertMany(
        listOf(
            ShopDoc("shop_1", "Bhargav Kirana", null, 4.6, 128, 28.6139, 77.2090, true, true, true, 5.0, "success@razorpay", shopType = "GENERAL_STORE"),
            ShopDoc("shop_2", "Laxmi General Store", null, 4.3, 86, 28.6200, 77.2150, true, true, true, 0.0, "success@razorpay", shopType = "GENERAL_STORE"),
            ShopDoc("shop_3", "Sharma Medical", null, 4.5, 64, 28.6180, 77.2120, true, true, true, 0.0, "success@razorpay", shopType = "MEDICAL_STORE"),
            ShopDoc("shop_4", "Gupta Hardware", null, 4.1, 42, 28.6160, 77.2060, true, true, true, 0.0, "success@razorpay", shopType = "HARDWARE"),
            ShopDoc("shop_5", "Mithai Mahal", null, 4.8, 210, 28.6145, 77.2105, true, true, true, 3.0, "success@razorpay", shopType = "SWEET_SHOP"),
        ),
    )
    db.getCollection<ShopUserDoc>("shop_users").insertMany(
        listOf(
            ShopUserDoc("user_owner", "shop_1", "owner", security.hashPassword("1234"), "Owner", "SHOP"),
            ShopUserDoc("user_cashier", "shop_1", "cashier", security.hashPassword("1234"), "Cashier", "SHOP"),
        ),
    )
    ensureMasterAdmin(db, security, config)
    db.getCollection<CategoryDoc>("master_categories").insertMany(
        listOf(
            CategoryDoc("cat_grocery", "General Grocery"),
            CategoryDoc("cat_spices", "Spices"),
            CategoryDoc("cat_grocery_oil", "Oils", "cat_grocery"),
            CategoryDoc("cat_spices_std", "Standard Spices", "cat_spices"),
        ),
    )
    data class SeedSku(val id: String, val name: String, val nameHi: String, val cat: String, val price: Double, val alias: String)
    val skus = listOf(
        SeedSku("mp_chini", "Sugar", "चीनी", "GENERAL_GROCERY", 48.0, "chini"),
        SeedSku("mp_aata", "Wheat Flour", "आटा", "GENERAL_GROCERY", 42.0, "aata"),
        SeedSku("mp_daal", "Toor Dal", "तूर दाल", "GENERAL_GROCERY", 140.0, "tuar daal"),
        SeedSku("mp_oil", "Mustard Oil", "सरसों तेल", "FORTUNE_BRANDED", 160.0, "sarson tel"),
        SeedSku("mp_haldi", "Turmeric", "हल्दी", "STANDARD_SPICES", 220.0, "haldi"),
        SeedSku("mp_namak", "Salt", "नमक", "GENERAL_GROCERY", 22.0, "namak"),
    )
    db.getCollection<ShopTypeDoc>("master_shop_types").insertMany(defaultShopTypes())
    db.getCollection<MasterProductDoc>("master_products").insertMany(
        skus.map { MasterProductDoc(it.id, it.name, it.nameHi, "cat_grocery", "KG") },
    )
    db.getCollection<ProductDoc>("products").insertMany(
        skus.flatMap { sku ->
            listOf("shop_1", "shop_2").map { sid ->
                ProductDoc(
                    id = "${sid}_${sku.id}",
                    shopId = sid,
                    name = sku.name,
                    nameHi = sku.nameHi,
                    category = sku.cat,
                    unit = "KG",
                    sellingPrice = sku.price,
                    costPrice = sku.price * 0.85,
                    stockQty = 50.0,
                    lowStockThreshold = 5.0,
                    voiceAlias = sku.alias,
                )
            }
        },
    )
    db.getCollection<OfferDoc>("offers").insertOne(
        OfferDoc("off_1", "shop_1", "Festival 5% off", "5% off on grocery basket", 5.0),
    )
}
