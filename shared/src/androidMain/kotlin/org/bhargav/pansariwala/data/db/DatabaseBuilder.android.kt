package org.bhargav.pansariwala.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun shopDatabaseBuilder(): RoomDatabase.Builder<ShopDatabase> {
    val holder = object : KoinComponent {
        val context: Context by inject()
    }
    val appContext = holder.context.applicationContext
    val dbFile = appContext.getDatabasePath(SHOP_DB_FILE)
    return Room.databaseBuilder<ShopDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
        .fallbackToDestructiveMigration(dropAllTables = true)
}
