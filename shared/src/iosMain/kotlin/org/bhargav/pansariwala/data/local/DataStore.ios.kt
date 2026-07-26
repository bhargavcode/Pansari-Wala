package org.bhargav.pansariwala.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

private const val DATASTORE_FILE_NAME = "pansari_wala.preferences_pb"

private class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>,
) : SessionStore {
    override fun observeString(key: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey(key)] }

    override suspend fun getString(key: String): String? =
        dataStore.data.map { it[stringPreferencesKey(key)] }.first()

    override suspend fun putStrings(values: Map<String, String?>) {
        dataStore.edit { prefs ->
            values.forEach { (key, value) ->
                val prefKey = stringPreferencesKey(key)
                if (value == null) prefs.remove(prefKey) else prefs[prefKey] = value
            }
        }
    }

    override suspend fun remove(keys: Set<String>) {
        dataStore.edit { prefs ->
            keys.forEach { prefs.remove(stringPreferencesKey(it)) }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun createSessionStore(): SessionStore {
    val dataStore = PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            (requireNotNull(documentDirectory).path + "/$DATASTORE_FILE_NAME").toPath()
        },
    )
    return DataStoreSessionStore(dataStore)
}
