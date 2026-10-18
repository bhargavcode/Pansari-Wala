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
import org.bhargav.pansariwala.data.sync.OfflineOrderSync
import org.bhargav.pansariwala.data.sync.OrderSyncSignal
import org.bhargav.pansariwala.data.sync.SyncingShopRepository
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.auth.LoginUseCase
import org.bhargav.pansariwala.domain.auth.ObserveSessionUseCase
import org.bhargav.pansariwala.feature.dashboard.DashboardViewModel
import org.bhargav.pansariwala.feature.profile.ShopProfileViewModel
import org.bhargav.pansariwala.feature.inventory.AddEditInventoryViewModel
import org.bhargav.pansariwala.feature.inventory.InventoryListViewModel
import org.bhargav.pansariwala.feature.inventory.MasterCatalogViewModel
import org.bhargav.pansariwala.feature.inventory.InventoryProductDetailViewModel
import org.bhargav.pansariwala.feature.login.LoginViewModel
import org.bhargav.pansariwala.feature.order.OrderEditorViewModel
import org.bhargav.pansariwala.feature.subscription.CommissionDayViewModel
import org.bhargav.pansariwala.feature.subscription.SubscriptionViewModel
import org.bhargav.pansariwala.feature.order.OrdersWorkspaceViewModel
import org.bhargav.pansariwala.feature.settings.SettingsViewModel
import org.bhargav.pansariwala.feature.splash.SplashViewModel
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.api.KtorPansariApi
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.feature.delivery.PartnerEarningsViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerHomeViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerJobViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerLoginViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterViewModel
import org.bhargav.pansariwala.feature.order.CustomerRatingsViewModel
import org.bhargav.pansariwala.feature.order.OnlineOrdersViewModel
import org.bhargav.pansariwala.feature.user.AccountViewModel
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.CartViewModel
import org.bhargav.pansariwala.feature.user.CheckoutViewModel
import org.bhargav.pansariwala.feature.user.MarketViewModel
import org.bhargav.pansariwala.feature.user.OrderDetailsViewModel
import org.bhargav.pansariwala.feature.user.PhoneAuthViewModel
import org.bhargav.pansariwala.feature.user.AddressViewModel
import org.bhargav.pansariwala.feature.user.ShopCatalogViewModel
import org.bhargav.pansariwala.feature.user.ShopOffersViewModel
import org.bhargav.pansariwala.feature.user.CategoryProductsViewModel
import org.bhargav.pansariwala.feature.user.ProductDetailViewModel
import org.bhargav.pansariwala.feature.user.ThankYouViewModel
import org.bhargav.pansariwala.feature.user.UserSettingsViewModel
import org.bhargav.pansariwala.feature.printer.PrinterSettingsViewModel
import org.bhargav.pansariwala.feature.reports.ReportsViewModel
import org.bhargav.pansariwala.platform.PartnerLocationTracker
import org.bhargav.pansariwala.printer.PrinterSettingsStore
import org.bhargav.pansariwala.printing.PosPrintService
import org.bhargav.pansariwala.printing.SessionPrinterSettingsStore
import org.bhargav.pansariwala.notification.DedupingNotificationGateway
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.PartnerOfferSocket
import org.bhargav.pansariwala.notification.PushMessageHandler
import org.bhargav.pansariwala.notification.PushRegistrar
import org.bhargav.pansariwala.notification.ShopNotifier
import org.bhargav.pansariwala.notification.createNotificationGateway
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.voice.createSpeechToText
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<Analytics> { createAnalytics() }
    single<CrashReporter> { createCrashReporter() }
    single<NotificationGateway> { DedupingNotificationGateway(createNotificationGateway()) }
    singleOf(::ShopNotifier)
    singleOf(::PushRegistrar)
    singleOf(::PushMessageHandler)
    singleOf(::PartnerOfferSocket)
    singleOf(::PartnerLocationTracker)
    single<SpeechToText> { createSpeechToText() }

    single<SessionStore> { createSessionStore() }
    single { OrderSyncSignal() }
    single<ShopRepository> { SyncingShopRepository(createShopRepository(), get()) }
    singleOf(::OfflineOrderSync)
    single { AppPreferences(get()) }
    single { CartStore(get()) }
    single<PrinterSettingsStore> { SessionPrinterSettingsStore(get()) }
    singleOf(::PosPrintService)
    single<PansariApi> { KtorPansariApi(createPlatformHttpClient(), ApiRuntime.baseUrl, get()) }

    singleOf(::LocalAuthRepository) bind AuthRepository::class

    factoryOf(::LoginUseCase)
    factoryOf(::ObserveSessionUseCase)

    viewModelOf(::SplashViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::AddEditInventoryViewModel)
    viewModelOf(::InventoryListViewModel)
    viewModelOf(::InventoryProductDetailViewModel)
    viewModelOf(::MasterCatalogViewModel)
    viewModelOf(::OrderEditorViewModel)
    viewModelOf(::SubscriptionViewModel)
    viewModelOf(::CommissionDayViewModel)
    viewModelOf(::OrdersWorkspaceViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::PhoneAuthViewModel)
    viewModelOf(::AddressViewModel)
    viewModelOf(::MarketViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::UserSettingsViewModel)
    viewModelOf(::ShopCatalogViewModel)
    viewModelOf(::ShopOffersViewModel)
    viewModel { CategoryProductsViewModel(get()) }
    viewModelOf(::ProductDetailViewModel)
    viewModelOf(::CartViewModel)
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::ThankYouViewModel)
    viewModelOf(::OrderDetailsViewModel)
    viewModelOf(::PartnerLoginViewModel)
    viewModelOf(::PartnerRegisterViewModel)
    viewModelOf(::PartnerHomeViewModel)
    viewModelOf(::PartnerJobViewModel)
    viewModelOf(::PartnerEarningsViewModel)
    viewModelOf(::OnlineOrdersViewModel)
    viewModelOf(::CustomerRatingsViewModel)
    viewModelOf(::PrinterSettingsViewModel)
    viewModelOf(::ReportsViewModel)
    viewModelOf(::ShopProfileViewModel)
}
