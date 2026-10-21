package org.bhargav.pansariwala.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserRoute : NavKey {
    @Serializable data object Splash : UserRoute
    @Serializable data object PhoneAuth : UserRoute
    @Serializable data object PhoneOtp : UserRoute
    /** Profile step 1 (name); [ProfileAddress] is step 2. */
    @Serializable data object ProfileSetup : UserRoute
    @Serializable data class ProfileAddress(val name: String) : UserRoute
    @Serializable data object Address : UserRoute
    @Serializable data object Shell : UserRoute
    @Serializable data class ShopCatalog(val shopId: String) : UserRoute
    /** [categoryId] is a master category id; null means search across all categories. */
    @Serializable data class ShopCategory(
        val shopId: String,
        val categoryId: String?,
        val categoryName: String = "",
        val query: String = "",
    ) : UserRoute
    @Serializable data class ShopProduct(val shopId: String, val productId: String) : UserRoute
    @Serializable data class ShopOffers(val shopId: String) : UserRoute
    @Serializable data class Checkout(val shopId: String) : UserRoute
    @Serializable data class ThankYou(val orderId: String) : UserRoute
    /** Waiting / result screen for payment transaction [intentId]; [autoLaunch] opens the UPI app once. */
    @Serializable data class PaymentStatus(val intentId: String, val autoLaunch: Boolean = false) : UserRoute
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
    @Serializable data object LoginOtp : DeliveryRoute
    /** Registration steps: details → address → vehicle & documents → OTP. */
    @Serializable data object Register : DeliveryRoute
    @Serializable data object RegisterAddress : DeliveryRoute
    @Serializable data object RegisterVehicle : DeliveryRoute
    @Serializable data object RegisterOtp : DeliveryRoute
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