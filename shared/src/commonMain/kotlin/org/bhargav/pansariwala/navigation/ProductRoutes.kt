package org.bhargav.pansariwala.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserRoute : NavKey {
    @Serializable data object Splash : UserRoute
    @Serializable data object PhoneAuth : UserRoute
    @Serializable data object ProfileSetup : UserRoute
    @Serializable data object Shell : UserRoute
    @Serializable data class ShopCatalog(val shopId: String) : UserRoute
    @Serializable data class Checkout(val shopId: String) : UserRoute
    @Serializable data class ThankYou(val orderId: String) : UserRoute
    @Serializable data class OrderDetails(val orderId: String) : UserRoute
    @Serializable data object OrdersList : UserRoute
    @Serializable data object Transactions : UserRoute
    @Serializable data object Help : UserRoute
    @Serializable data object NotificationPrefs : UserRoute
    @Serializable data object ThemePrefs : UserRoute
}

@Serializable
sealed interface DeliveryRoute : NavKey {
    @Serializable data object Splash : DeliveryRoute
    @Serializable data object Login : DeliveryRoute
    @Serializable data object Register : DeliveryRoute
    @Serializable data object Dashboard : DeliveryRoute
    @Serializable data class IncomingOffer(val offerId: String? = null) : DeliveryRoute
    @Serializable data object AcceptedJobs : DeliveryRoute
    @Serializable data object DeliveredJobs : DeliveryRoute
    @Serializable data object Earnings : DeliveryRoute
    @Serializable data class Pickup(val orderId: String) : DeliveryRoute
}