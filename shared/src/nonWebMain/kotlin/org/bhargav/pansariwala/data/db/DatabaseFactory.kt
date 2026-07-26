package org.bhargav.pansariwala.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

internal const val SHOP_DB_FILE = "pansari_shop.db"

/** Platform-provided builder (Android needs a Context, iOS needs a file path). */
expect fun shopDatabaseBuilder(): RoomDatabase.Builder<ShopDatabase>

fun buildShopDatabase(): ShopDatabase =
    shopDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .build()

actual fun createShopRepository(): ShopRepository = RoomShopRepository(buildShopDatabase())
