package org.bhargav.pansariwala.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.domain.auth.ObserveSessionUseCase

sealed interface SplashDestination {
    data object Login : SplashDestination
    data object Home : SplashDestination
    /** Signed in but registration (profile setup) was never finished. */
    data object CompleteProfile : SplashDestination
}

/** What the splash is doing right now; shown as a label when start-up is slow. */
enum class SplashStep { PREPARING_DATA, CHECKING_SESSION, LOADING_PROFILE, OPENING }

class SplashViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val shopRepository: ShopRepository,
    private val preferences: org.bhargav.pansariwala.data.local.AppPreferences,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    private val _step = MutableStateFlow(SplashStep.PREPARING_DATA)
    val step: StateFlow<SplashStep> = _step.asStateFlow()

    /** True once start-up has taken longer than [SLOW_HINT_MS] without navigating. */
    private val _slow = MutableStateFlow(false)
    val slow: StateFlow<Boolean> = _slow.asStateFlow()

    init {
        viewModelScope.launch {
            delay(SLOW_HINT_MS)
            if (_destination.value == null) _slow.value = true
        }
        viewModelScope.launch {
            // Home screens seed again on open, so a slow local DB must not trap the user here.
            withTimeoutOrNull(SEED_TIMEOUT_MS) { shopRepository.ensureSeeded() }
            delay(SPLASH_DURATION_MS)
            _step.value = SplashStep.CHECKING_SESSION
            val product = org.bhargav.pansariwala.product.currentAppProduct()
            val token = preferences.getAccessToken()
            if (product == org.bhargav.pansariwala.product.AppProduct.POS &&
                !token.isNullOrBlank() &&
                !token.startsWith(org.bhargav.pansariwala.util.AppConstants.JWT_PREFIX)
            ) {
                preferences.clearSession()
            }
            val hasSession = observeSession.hasSession()
            val role = preferences.getRole()
            val home = when (product) {
                org.bhargav.pansariwala.product.AppProduct.POS ->
                    hasSession
                org.bhargav.pansariwala.product.AppProduct.USER ->
                    hasSession && role == org.bhargav.pansariwala.util.AppConstants.Roles.CUSTOMER
                org.bhargav.pansariwala.product.AppProduct.DELIVERY ->
                    hasSession && role == org.bhargav.pansariwala.util.AppConstants.Roles.PARTNER
            }
            _step.value = SplashStep.LOADING_PROFILE
            val next = when {
                !home -> SplashDestination.Login
                product == org.bhargav.pansariwala.product.AppProduct.USER &&
                    !preferences.isProfileComplete() -> SplashDestination.CompleteProfile
                else -> SplashDestination.Home
            }
            _step.value = SplashStep.OPENING
            _destination.value = next
        }
    }

    companion object {
        const val SPLASH_DURATION_MS = 400L
        private const val SLOW_HINT_MS = 3_000L
        private const val SEED_TIMEOUT_MS = 10_000L
    }
}
