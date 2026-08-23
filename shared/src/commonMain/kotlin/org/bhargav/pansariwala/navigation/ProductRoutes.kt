package org.bhargav.pansariwala.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserRoute : NavKey {
    @Serializable data object Splash : UserRoute
    @Serializable data object PhoneAuth : UserRoute
    @Serializable data object ProfileSetup : UserRoute
    @Serializable data object Address : UserRoute
    @Serializable data object LocationAccess : UserRoute
    @Serializable data object Shell : UserRoute
    @Serializable data class ShopCatalog(val shopId: String) : UserRoute
    @Serializable data class Checkout(val shopId: String) : UserRoute
    @Serializable data class ThankYou(val orderId: String) : UserRoute
    @Serializable data class OrderDetails(val orderId: String) : UserRoute
    @Serializable data object OrdersList : UserRoute
    @Serializable data object Transactions : UserRoute
    @Serializable data object Help : UserRoute
    @Serializable data object NotificationPrefs : UserRoute
    @Serializable data class Cart(val shopId: String) : UserRoute
    @Serializable data object LanguagePrefs : UserRoute
    @Serializable data object ThemePrefs : UserRoute
}

@Serializable
sealed interface DeliveryRoute : NavKey {
    @Serializable data object Splash : DeliveryRoute
    @Serializable data object Login : DeliveryRoute
    @Serializable data object Register : DeliveryRoute
    @Serializable data object LocationAccess : DeliveryRoute
    @Serializable data object Home : DeliveryRoute
    @Serializable data class NavigateToStore(val orderId: String) : DeliveryRoute
    @Serializable data class PickupItems(val orderId: String) : DeliveryRoute
    @Serializable data class CapturePhotos(val orderId: String) : DeliveryRoute
    @Serializable data class DeliverToCustomer(val orderId: String) : DeliveryRoute
    @Serializable data class CustomerPayment(val orderId: String) : DeliveryRoute
    @Serializable data class DeliveryComplete(val orderId: String) : DeliveryRoute
    @Serializable data object Earnings : DeliveryRoute
    @Serializable data object LanguagePrefs : DeliveryRoute
    @Serializable data object ThemePrefs : DeliveryRoute
    @Serializable data object NotificationPrefs : DeliveryRoute
}