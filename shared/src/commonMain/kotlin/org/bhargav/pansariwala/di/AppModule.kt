package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.createAnalytics
import org.bhargav.pansariwala.crash.CrashReporter
import org.bhargav.pansariwala.crash.createCrashReporter
import org.bhargav.pansariwala.data.auth.LocalAuthRepository
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.db.createShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.local.SessionStore
import org.bhargav.pansariwala.data.local.createSessionStore
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.auth.LoginUseCase
import org.bhargav.pansariwala.domain.auth.ObserveSessionUseCase
import org.bhargav.pansariwala.feature.dashboard.DashboardViewModel
import org.bhargav.pansariwala.feature.inventory.AddEditInventoryViewModel
import org.bhargav.pansariwala.feature.inventory.InventoryListViewModel
import org.bhargav.pansariwala.feature.login.LoginViewModel
import org.bhargav.pansariwala.feature.order.OrderEditorViewModel
import org.bhargav.pansariwala.feature.splash.SplashViewModel
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.createNotificationGateway
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.voice.createSpeechToText
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<Analytics> { createAnalytics() }
    single<CrashReporter> { createCrashReporter() }
    single<NotificationGateway> { createNotificationGateway() }
    single<SpeechToText> { createSpeechToText() }

    single<SessionStore> { createSessionStore() }
    single<ShopRepository> { createShopRepository() }
    single { AppPreferences(get()) }

    singleOf(::LocalAuthRepository) bind AuthRepository::class

    factoryOf(::LoginUseCase)
    factoryOf(::ObserveSessionUseCase)

    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::AddEditInventoryViewModel)
    viewModelOf(::InventoryListViewModel)
    viewModelOf(::OrderEditorViewModel)
}
