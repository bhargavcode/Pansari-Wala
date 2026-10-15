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
