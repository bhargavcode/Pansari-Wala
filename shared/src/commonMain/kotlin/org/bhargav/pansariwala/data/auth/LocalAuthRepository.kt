package org.bhargav.pansariwala.data.auth

import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.api.ApiException
import org.bhargav.pansariwala.api.JwtAuthCache
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.crash.CrashReporter
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.sync.OrderSyncSignal
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.auth.LoginCredentials
import org.bhargav.pansariwala.domain.auth.Session
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.currentAppProduct
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId

/**
 * Authenticates against the seeded local Room database (offline-first) and
 * persists the resulting session in DataStore.
 *
 * POS always requires a server JWT so online orders / live alerts work.
 */
class LocalAuthRepository(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    private val crashReporter: CrashReporter,
    private val api: PansariApi,
    private val orderSyncSignal: OrderSyncSignal,
) : AuthRepository {

    override suspend fun hasSession(): Boolean = preferences.hasSession()

    override suspend fun login(credentials: LoginCredentials): Result<Session> {
        return runCatching {
            val identifier = credentials.identifier.trim()
            require(identifier.isNotEmpty() && credentials.password.isNotBlank()) {
                "Invalid username or password."
            }
            shopRepository.ensureSeeded()
            // Server first: shops/users created from the master portal never exist in the local seed DB.
            val remoteResult = withTimeoutOrNull(AppConstants.REMOTE_LOGIN_TIMEOUT_MS) {
                runCatching { api.shopLogin(identifier, credentials.password) }
            }
            val remote = remoteResult?.getOrNull()
            val remoteError = remoteResult?.exceptionOrNull()
            val remoteMessage = remoteError?.message.orEmpty()
            if ((remoteError as? ApiException)?.code == AppConstants.LOGIN_LOCKED_CODE) throw remoteError
            if (remoteMessage.contains("Invalid credentials", ignoreCase = true)) {
                throw IllegalStateException("Invalid username or password.")
            }
            val requireServerJwt = currentAppProduct() == AppProduct.POS
            if (remote == null && requireServerJwt) {
                throw IllegalStateException(
                    remoteMessage.takeIf { it.isNotBlank() }
                        ?: "Cannot reach the shop server.",
                )
            }
            val session = if (remote != null) {
                preferences.saveToken(remote)
                JwtAuthCache.invalidate()
                Session(
                    accessToken = remote.accessToken,
                    refreshToken = remote.refreshToken,
                    userId = remote.userId,
                    shopId = remote.shopId,
                    displayName = remote.displayName ?: identifier,
                )
            } else {
                val user = shopRepository.authenticate(identifier, credentials.password)
                    ?: throw IllegalStateException("Invalid username or password.")
                val local = Session(
                    accessToken = generateId("session"),
                    refreshToken = null,
                    userId = user.id,
                    shopId = user.shopId,
                    displayName = user.displayName,
                )
                preferences.saveSession(
                    accessToken = local.accessToken,
                    refreshToken = local.refreshToken,
                    userId = local.userId,
                    shopId = local.shopId,
                    displayName = local.displayName,
                )
                JwtAuthCache.invalidate()
                local
            }
            orderSyncSignal.kick(pull = true)
            session
        }.onFailure { error ->
            analytics.log(
                AnalyticsEvent.Error(
                    code = "login_failed",
                    message = error.message.orEmpty(),
                    payload = mapOf("identifier" to credentials.identifier.take(3) + "***"),
                ),
            )
            val expected = error.message.orEmpty().contains("Invalid username or password")
            if (!expected) {
                crashReporter.recordException(
                    error,
                    mapOf("screen" to "login", "action" to "submit"),
                )
            }
        }
    }

    override suspend fun logout() {
        preferences.clearSession()
        JwtAuthCache.invalidate()
    }
}
