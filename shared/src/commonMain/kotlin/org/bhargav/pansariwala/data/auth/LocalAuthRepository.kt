package org.bhargav.pansariwala.data.auth

import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.crash.CrashReporter
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.auth.LoginCredentials
import org.bhargav.pansariwala.domain.auth.Session
import org.bhargav.pansariwala.util.generateId

/**
 * Authenticates against the seeded local Room database (offline-first) and
 * persists the resulting session in DataStore.
 */
class LocalAuthRepository(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
) : AuthRepository {

    override suspend fun hasSession(): Boolean = preferences.hasSession()

    override suspend fun login(credentials: LoginCredentials): Result<Session> {
        return runCatching {
            shopRepository.ensureSeeded()
            val user = shopRepository.authenticate(credentials.identifier, credentials.password)
                ?: throw IllegalStateException("Invalid username or password.")
            val session = Session(
                accessToken = generateId("session"),
                refreshToken = null,
                userId = user.id,
                shopId = user.shopId,
                displayName = user.displayName,
            )
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
                    payload = mapOf("identifier" to credentials.identifier.take(3) + "***"),
                ),
            )
            crashReporter.recordException(
                error,
                mapOf("screen" to "login", "action" to "submit"),
            )
        }
    }

    override suspend fun logout() {
        preferences.clearSession()
    }
}
