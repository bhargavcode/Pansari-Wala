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
import org.bhargav.pansariwala.feature.order.OrdersWorkspaceViewModel
import org.bhargav.pansariwala.feature.settings.SettingsViewModel
import org.bhargav.pansariwala.feature.splash.SplashViewModel
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.api.KtorPansariApi
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.feature.delivery.JobsViewModel
import org.bhargav.pansariwala.feature.delivery.OfferViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerDashboardViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerLoginViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterViewModel
import org.bhargav.pansariwala.feature.order.OnlineOrdersViewModel
import org.bhargav.pansariwala.feature.user.AccountViewModel
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.CheckoutViewModel
import org.bhargav.pansariwala.feature.user.MarketViewModel
import org.bhargav.pansariwala.feature.user.OrderDetailsViewModel
import org.bhargav.pansariwala.feature.user.PhoneAuthViewModel
import org.bhargav.pansariwala.feature.user.ProfileSetupViewModel
import org.bhargav.pansariwala.feature.user.ShopCatalogViewModel
import org.bhargav.pansariwala.feature.user.ThankYouViewModel
import org.bhargav.pansariwala.feature.user.UserSettingsViewModel
import org.bhargav.pansariwala.notification.LiveAlerts
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.ShopNotifier
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
    singleOf(::ShopNotifier)
    singleOf(::LiveAlerts)
    single<SpeechToText> { createSpeechToText() }

    single<SessionStore> { createSessionStore() }
    single<ShopRepository> { createShopRepository() }
    single { AppPreferences(get()) }
    single { CartStore() }
    single<PansariApi> { KtorPansariApi(createPlatformHttpClient(), ApiRuntime.baseUrl, get()) }

    singleOf(::LocalAuthRepository) bind AuthRepository::class

    factoryOf(::LoginUseCase)
    factoryOf(::ObserveSessionUseCase)

    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::AddEditInventoryViewModel)
    viewModelOf(::InventoryListViewModel)
    viewModelOf(::OrderEditorViewModel)
    viewModelOf(::OrdersWorkspaceViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PhoneAuthViewModel)
    viewModelOf(::ProfileSetupViewModel)
    viewModelOf(::MarketViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::UserSettingsViewModel)
    viewModelOf(::ShopCatalogViewModel)
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::ThankYouViewModel)
    viewModelOf(::OrderDetailsViewModel)
    viewModelOf(::PartnerLoginViewModel)
    viewModelOf(::PartnerRegisterViewModel)
    viewModelOf(::PartnerDashboardViewModel)
    viewModelOf(::OfferViewModel)
    viewModelOf(::JobsViewModel)
    viewModelOf(::OnlineOrdersViewModel)
}
