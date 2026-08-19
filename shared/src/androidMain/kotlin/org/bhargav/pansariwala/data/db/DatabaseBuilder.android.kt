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
    ).fallbackToDestructiveMigration(dropAllTables = true)
}
