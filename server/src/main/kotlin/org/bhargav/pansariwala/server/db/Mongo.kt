package org.bhargav.pansariwala.server.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import java.util.concurrent.TimeUnit
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.exists
import com.mongodb.client.model.Filters.ne
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
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
data class ShopFeaturesDoc(
    val voiceSearch: Boolean = true,
    val barcodeSearch: Boolean = true,
    val reportGeneration: Boolean = true,
    val onlineOrders: Boolean = true,
    val inventoryAlerts: Boolean = true,
)

@Serializable
data class ShopHoursDayDoc(
    val day: String,
    val start: String = "09:00",
    val end: String = "21:00",
    val closed: Boolean = false,
)

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
    val joinedAt: Long = 0L,
    val features: ShopFeaturesDoc = ShopFeaturesDoc(),
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "India",
    val registrationNumber: String = "",
    val taxId: String = "",
    val operatingHours: List<ShopHoursDayDoc> = emptyList(),
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
    val imageUrl: String? = null,
    val active: Boolean = true,
    val joinedAt: Long = 0L,
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
    val vehicleName: String = "",
    val vehicleBrand: String = "",
    val vehicleColor: String = "",
    val vehicleType: String = "SCOOTY",
    val active: Boolean = true,
)

@Serializable
data class CategoryDoc(
    @SerialName("_id") val id: String,
    val name: String,
    val parentId: String? = null,
)

@Serializable
data class MasterProductVariantDoc(
    val name: String,
    val sku: String = "",
    val price: Double = 0.0,
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
    val brandName: String = "",
    val companyName: String = "",
    val subcategoryId: String? = null,
    val salePrice: Double = 0.0,
    val cost: Double = 0.0,
    val active: Boolean = true,
    val addedAt: Long = 0L,
    val description: String = "",
    val sku: String = "",
    val stockQty: Double = 0.0,
    val lowStockThreshold: Double = 0.0,
    val tags: String = "",
    val weightKg: Double = 0.0,
    val dimensions: String = "",
    val variants: List<MasterProductVariantDoc> = emptyList(),
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
    ensurePrototypeShops(db)
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
    val masterId = "admin_1"
    // Prefer rotating the canonical row; fall back to the row that already owns this username.
    val targetId = admins.find(eq("username", username)).firstOrNull()?.id ?: masterId
    admins.replaceOne(
        eq("_id", targetId),
        AdminUserDoc(targetId, username, hash),
        ReplaceOptions().upsert(true),
    )
    // Drop legacy default login and any other rows colliding on username.
    if (username != "admin") {
        admins.deleteMany(eq("username", "admin"))
    }
    admins.deleteMany(and(eq("username", username), ne("_id", targetId)))
}

private fun defaultShopTypes(): List<ShopTypeDoc> = listOf(
    ShopTypeDoc("SUPERMARKET", "Supermarket"),
    ShopTypeDoc("GROCERY", "Grocery"),
    ShopTypeDoc("BOUTIQUE", "Boutique"),
    ShopTypeDoc("GENERAL_STORE", "General Store"),
    ShopTypeDoc("MEDICAL_STORE", "Medical Store"),
    ShopTypeDoc("HARDWARE", "Hardware"),
    ShopTypeDoc("SWEET_SHOP", "Sweet Shop"),
)

/** Prototype master-portal shops (ids match mockup table). */
private fun prototypeAdminShops(): List<ShopDoc> {
    val joined = 1_687_478_400_000L // 2023-06-23 UTC
    val hours = listOf(
        ShopHoursDayDoc("Mon", "09:00", "21:00"),
        ShopHoursDayDoc("Tue", "09:00", "21:00"),
        ShopHoursDayDoc("Wed", "09:00", "21:00"),
        ShopHoursDayDoc("Thu", "09:00", "21:00"),
        ShopHoursDayDoc("Fri", "09:00", "21:00"),
        ShopHoursDayDoc("Sat", "10:00", "20:00"),
        ShopHoursDayDoc("Sun", "10:00", "18:00"),
    )
    val features = ShopFeaturesDoc()
    return listOf(
        ShopDoc(
            id = "301", name = "Ermople Shopisg", imageUrl = null, rating = 4.5, ratingCount = 128,
            lat = 41.0149426, lng = -55.4878477, isOpen = true, active = true, paymentsEnabled = true,
            discountPercent = 0.0, upiId = "success@razorpay", address = "Location", shopType = "SUPERMARKET",
            joinedAt = joined, features = features, ownerName = "Ermople Owner", ownerPhone = "+91-9800000301",
            ownerEmail = "shop301@ermople.com", city = "New York", state = "NY", zip = "10001", country = "USA",
            registrationNumber = "REG-301", taxId = "TAX-301", operatingHours = hours,
        ),
        ShopDoc(
            id = "302", name = "Ermople Stara", imageUrl = null, rating = 4.5, ratingCount = 96,
            lat = 41.0150, lng = -55.4880, isOpen = true, active = true, paymentsEnabled = true,
            discountPercent = 0.0, upiId = "success@razorpay", address = "Location", shopType = "SUPERMARKET",
            joinedAt = joined, features = features, ownerName = "Stara Owner", ownerPhone = "+91-9800000302",
            ownerEmail = "shop302@ermople.com", city = "New York", state = "NY", zip = "10002", country = "USA",
            registrationNumber = "REG-302", taxId = "TAX-302", operatingHours = hours,
        ),
        ShopDoc(
            id = "303", name = "Shop Market", imageUrl = null, rating = 4.5, ratingCount = 74,
            lat = 41.0160, lng = -55.4890, isOpen = true, active = true, paymentsEnabled = true,
            discountPercent = 0.0, upiId = "success@razorpay", address = "Location", shopType = "GROCERY",
            joinedAt = joined, features = features, ownerName = "Market Owner", ownerPhone = "+91-9800000303",
            ownerEmail = "shop303@ermople.com", city = "New York", state = "NY", zip = "10003", country = "USA",
            registrationNumber = "REG-303", taxId = "TAX-303", operatingHours = hours,
        ),
        ShopDoc(
            id = "304", name = "Farmfresh Express", imageUrl = null, rating = 4.5, ratingCount = 110,
            lat = 41.0170, lng = -55.4900, isOpen = true, active = true, paymentsEnabled = true,
            discountPercent = 0.0, upiId = "success@razorpay", address = "Location", shopType = "BOUTIQUE",
            joinedAt = joined, features = features, ownerName = "Farm Owner", ownerPhone = "+91-9800000304",
            ownerEmail = "shop304@ermople.com", city = "New York", state = "NY", zip = "10004", country = "USA",
            registrationNumber = "REG-304", taxId = "TAX-304", operatingHours = hours,
        ),
    )
}

private fun ensureShopTypes(db: MongoDatabase) {
    val col = db.getCollection<ShopTypeDoc>("master_shop_types")
    defaultShopTypes().forEach { row ->
        if (col.find(eq("_id", row.id)).firstOrNull() == null) {
            col.insertOne(row)
        }
    }
}

private fun ensurePrototypeShops(db: MongoDatabase) {
    val col = db.getCollection<ShopDoc>("shops")
    prototypeAdminShops().forEach { row ->
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
        prototypeAdminShops() + listOf(
            ShopDoc("shop_1", "Bhargav Kirana", null, 4.6, 128, 28.6139, 77.2090, true, true, true, 5.0, "success@razorpay", address = "Connaught Place, Delhi", shopType = "GENERAL_STORE", joinedAt = 1_687_478_400_000L),
            ShopDoc("shop_2", "Laxmi General Store", null, 4.3, 86, 28.6200, 77.2150, true, true, true, 0.0, "success@razorpay", address = "Karol Bagh, Delhi", shopType = "GENERAL_STORE", joinedAt = 1_687_478_400_000L),
            ShopDoc("shop_3", "Sharma Medical", null, 4.5, 64, 28.6180, 77.2120, true, true, true, 0.0, "success@razorpay", address = "Location", shopType = "MEDICAL_STORE", joinedAt = 1_687_478_400_000L),
            ShopDoc("shop_4", "Gupta Hardware", null, 4.1, 42, 28.6160, 77.2060, true, true, true, 0.0, "success@razorpay", address = "Location", shopType = "HARDWARE", joinedAt = 1_687_478_400_000L),
            ShopDoc("shop_5", "Mithai Mahal", null, 4.8, 210, 28.6145, 77.2105, true, true, true, 3.0, "success@razorpay", address = "Location", shopType = "SWEET_SHOP", joinedAt = 1_687_478_400_000L),
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
    val now = System.currentTimeMillis()
    db.getCollection<MasterProductDoc>("master_products").insertMany(
        skus.map {
            MasterProductDoc(
                id = it.id,
                name = it.name,
                nameHi = it.nameHi,
                categoryId = "cat_grocery",
                unit = "KG",
                salePrice = it.price,
                cost = it.price * 0.85,
                addedAt = now,
                sku = it.id.uppercase(),
            )
        },
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
