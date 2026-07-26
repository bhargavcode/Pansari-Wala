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
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination?>(null)
    val destination: StateFlow<SplashDestination?> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            delay(SPLASH_DURATION_MS)
            val hasSession = observeSession.hasSession()
            _destination.value = if (hasSession) {
                SplashDestination.Home
            } else {
                SplashDestination.Login
            }
        }
    }

    companion object {
        const val SPLASH_DURATION_MS = 3_000L
    }
}
