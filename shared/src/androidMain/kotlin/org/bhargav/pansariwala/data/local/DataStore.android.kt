package org.bhargav.pansariwala.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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

fun createAndroidDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { context.filesDir.resolve(DATASTORE_FILE_NAME).absolutePath.toPath() },
    )

fun createAndroidSessionStore(context: Context): SessionStore =
    DataStoreSessionStore(createAndroidDataStore(context))

actual fun createSessionStore(): SessionStore {
    val holder = object : KoinComponent {
        val context: Context by inject()
    }
    return createAndroidSessionStore(holder.context)
}
