package org.bhargav.pansariwala.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bhargav.pansariwala.domain.auth.LoginCredentials
import org.bhargav.pansariwala.domain.auth.Session

@Serializable
data class LoginRequestDto(
    val identifier: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("shop_id") val shopId: String? = null,
    @SerialName("display_name") val displayName: String? = null,
)

class AuthApi(
    private val client: HttpClient,
) {
    suspend fun login(credentials: LoginCredentials): Session {
        val response = client.post("v1/auth/login") {
            setBody(LoginRequestDto(credentials.identifier, credentials.password))
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("Login failed (${response.status.value})")
        }
        val body = response.body<LoginResponseDto>()
        return Session(
            accessToken = body.accessToken,
            refreshToken = body.refreshToken,
            userId = body.userId,
            shopId = body.shopId,
            displayName = body.displayName,
        )
    }
}
