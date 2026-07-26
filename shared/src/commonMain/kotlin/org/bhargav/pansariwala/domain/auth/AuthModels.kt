package org.bhargav.pansariwala.domain.auth

data class Session(
    val accessToken: String,
    val refreshToken: String?,
    val userId: String,
    val shopId: String?,
    val displayName: String?,
)

data class LoginCredentials(
    val identifier: String,
    val password: String,
)

interface AuthRepository {
    suspend fun hasSession(): Boolean
    suspend fun login(credentials: LoginCredentials): Result<Session>
    suspend fun logout()
}
