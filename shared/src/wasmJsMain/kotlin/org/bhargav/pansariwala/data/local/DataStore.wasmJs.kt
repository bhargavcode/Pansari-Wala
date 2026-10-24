package org.bhargav.pansariwala.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private fun storageGet(key: String): String? = js("window.sessionStorage.getItem(key)")
private fun storageSet(key: String, value: String): Unit = js("window.sessionStorage.setItem(key, value)")
private fun storageRemove(key: String): Unit = js("window.sessionStorage.removeItem(key)")

/**
 * Tab-scoped session: survives a page reload but is dropped when the tab closes and is never shared with
 * other tabs (unlike localStorage), which limits token theft on shared POS counters.
 */
private class BrowserSessionStore : SessionStore {
    private val state = MutableStateFlow<Map<String, String>>(emptyMap())
    private val hydrated = mutableSetOf<String>()

    private fun hydrate(key: String) {
        if (!hydrated.add(key)) return
        val stored = runCatching { storageGet(STORAGE_PREFIX + key) }.getOrNull() ?: return
        state.update { it + (key to stored) }
    }

    override fun observeString(key: String): Flow<String?> {
        hydrate(key)
        return state.map { it[key] }.distinctUntilChanged()
    }

    override suspend fun getString(key: String): String? {
        hydrate(key)
        return state.value[key]
    }

    override suspend fun putStrings(values: Map<String, String?>) {
        hydrated.addAll(values.keys)
        values.forEach { (key, value) ->
            runCatching {
                if (value == null) storageRemove(STORAGE_PREFIX + key) else storageSet(STORAGE_PREFIX + key, value)
            }
        }
        state.update { current ->
            current.toMutableMap().apply {
                values.forEach { (key, value) ->
                    if (value == null) remove(key) else put(key, value)
                }
            }
        }
    }

    override suspend fun remove(keys: Set<String>) {
        hydrated.addAll(keys)
        keys.forEach { runCatching { storageRemove(STORAGE_PREFIX + it) } }
        state.update { current -> current.filterKeys { it !in keys } }
    }
}

private const val STORAGE_PREFIX = "pw.session."

actual fun createSessionStore(): SessionStore = BrowserSessionStore()
