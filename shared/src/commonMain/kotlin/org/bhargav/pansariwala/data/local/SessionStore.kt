package org.bhargav.pansariwala.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Platform key-value store. Android/iOS use DataStore; JS/Wasm use an in-memory store for Phase 1.
 */
interface SessionStore {
    fun observeString(key: String): Flow<String?>
    suspend fun getString(key: String): String?
    suspend fun putStrings(values: Map<String, String?>)
    suspend fun remove(keys: Set<String>)
}

expect fun createSessionStore(): SessionStore
