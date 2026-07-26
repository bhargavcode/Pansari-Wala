package org.bhargav.pansariwala.data.db

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}

actual fun shopDatabaseBuilder(): RoomDatabase.Builder<ShopDatabase> {
    val dbFilePath = documentDirectory() + "/" + SHOP_DB_FILE
    return Room.databaseBuilder<ShopDatabase>(name = dbFilePath)
}
