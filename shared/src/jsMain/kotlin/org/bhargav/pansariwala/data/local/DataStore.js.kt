package org.bhargav.pansariwala.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private class InMemorySessionStore : SessionStore {
    private val state = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observeString(key: String): Flow<String?> =
        state.map { it[key] }

    override suspend fun getString(key: String): String? = state.value[key]

    override suspend fun putStrings(values: Map<String, String?>) {
        state.update { current ->
            current.toMutableMap().apply {
                values.forEach { (key, value) ->
                    if (value == null) remove(key) else put(key, value)
                }
            }
        }
    }

    override suspend fun remove(keys: Set<String>) {
        state.update { current -> current.filterKeys { it !in keys } }
    }
}

actual fun createSessionStore(): SessionStore = InMemorySessionStore()
