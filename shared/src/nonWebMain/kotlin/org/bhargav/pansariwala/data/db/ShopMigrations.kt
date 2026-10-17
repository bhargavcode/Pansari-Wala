package org.bhargav.pansariwala.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import org.bhargav.pansariwala.util.AppConstants

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT '${AppConstants.PosPayment.CASH}'",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE orders ADD COLUMN discount REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE orders ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE orders ADD COLUMN syncRevision INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE products ADD COLUMN pendingSync INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE products ADD COLUMN syncRevision INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `sync_state` (`syncKey` TEXT NOT NULL, `syncValue` TEXT NOT NULL, " +
                "PRIMARY KEY(`syncKey`))",
        )
        // Demo seed bills (order_0001…) must never be uploaded as real sales.
        connection.execSQL("UPDATE orders SET pendingSync = 0 WHERE id GLOB 'order_[0-9][0-9][0-9][0-9]'")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE products ADD COLUMN variantsJson TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE order_items ADD COLUMN variantId TEXT")
        connection.execSQL("ALTER TABLE order_items ADD COLUMN variantLabel TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE products ADD COLUMN brandName TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE products ADD COLUMN masterProductId TEXT")
        connection.execSQL("ALTER TABLE products ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_products_masterProductId` ON `products` (`masterProductId`)",
        )
    }
}
