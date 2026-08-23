package org.bhargav.pansariwala.domain.auth

class LoginUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(credentials: LoginCredentials): Result<Session> {
        val identifier = credentials.identifier.trim()
        val password = credentials.password.trim()
        if (identifier.isBlank() || password.length < 4) {
            return Result.failure(
                IllegalArgumentException("Enter a valid phone/email and password (min 4 chars)."),
            )
        }
        return authRepository.login(LoginCredentials(identifier, password))
    }
}

class ObserveSessionUseCase(
    private val authRepository: AuthRepository,
) {
    suspend fun hasSession(): Boolean = authRepository.hasSession()
}
