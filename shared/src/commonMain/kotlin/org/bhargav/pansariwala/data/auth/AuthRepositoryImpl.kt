package org.bhargav.pansariwala.data.auth

import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.crash.CrashReporter
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.remote.ApiConfig
import org.bhargav.pansariwala.data.remote.AuthApi
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.auth.LoginCredentials
import org.bhargav.pansariwala.domain.auth.Session

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
) : AuthRepository {

    override suspend fun hasSession(): Boolean = preferences.hasSession()

    override suspend fun login(credentials: LoginCredentials): Result<Session> {
        return runCatching {
            val session = if (ApiConfig.USE_DEMO_AUTH) {
                demoSession(credentials)
            } else {
                authApi.login(credentials)
            }
            preferences.saveSession(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                userId = session.userId,
                shopId = session.shopId,
                displayName = session.displayName,
            )
            session
        }.onFailure { error ->
            analytics.log(
                AnalyticsEvent.Error(
                    code = "login_failed",
                    message = error.message.orEmpty(),
                    payload = mapOf(
                        "identifier" to credentials.identifier.take(3) + "***",
                    ),
                ),
            )
            crashReporter.recordException(
                error,
                mapOf(
                    "screen" to "login",
                    "action" to "submit",
                ),
            )
        }
    }

    override suspend fun logout() {
        preferences.clearSession()
    }

    private fun demoSession(credentials: LoginCredentials): Session =
        Session(
            accessToken = "demo_token_${credentials.identifier.hashCode()}",
            refreshToken = "demo_refresh",
            userId = "demo_user",
            shopId = "demo_shop",
            displayName = credentials.identifier.substringBefore("@").ifBlank { "Shopkeeper" },
        )
}
