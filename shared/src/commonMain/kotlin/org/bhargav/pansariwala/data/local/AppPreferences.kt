package org.bhargav.pansariwala.data.local

import kotlinx.coroutines.flow.Flow

class AppPreferences(
    private val store: SessionStore,
) {
    private object Keys {
        const val accessToken = "access_token"
        const val refreshToken = "refresh_token"
        const val userId = "user_id"
        const val shopId = "shop_id"
        const val userDisplayName = "user_display_name"
    }

    val accessToken: Flow<String?> = store.observeString(Keys.accessToken)

    suspend fun getAccessToken(): String? = store.getString(Keys.accessToken)

    suspend fun getUserId(): String? = store.getString(Keys.userId)

    suspend fun getShopId(): String? = store.getString(Keys.shopId)

    suspend fun getDisplayName(): String? = store.getString(Keys.userDisplayName)

    suspend fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: String,
        shopId: String?,
        displayName: String?,
    ) {
        store.putStrings(
            mapOf(
                Keys.accessToken to accessToken,
                Keys.refreshToken to refreshToken,
                Keys.userId to userId,
                Keys.shopId to shopId,
                Keys.userDisplayName to displayName,
            ),
        )
    }

    suspend fun clearSession() {
        store.remove(
            setOf(
                Keys.accessToken,
                Keys.refreshToken,
                Keys.userId,
                Keys.shopId,
                Keys.userDisplayName,
            ),
        )
    }
}
