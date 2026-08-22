package org.bhargav.pansariwala.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.domain.auth.ObserveSessionUseCase

sealed interface SplashDestination {
    data object Login : SplashDestination
    data object Home : SplashDestination
}

class SplashViewModel(
    private val observeSession: ObserveSessionUseCase,
    private val shopRepository: ShopRepository,
    private val preferences: org.bhargav.pansariwala.data.local.AppPreferences,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            delay(SPLASH_DURATION_MS)
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
            _destination.value = if (home) SplashDestination.Home else SplashDestination.Login
        }
    }

    companion object {
        const val SPLASH_DURATION_MS = 400L
    }
}
