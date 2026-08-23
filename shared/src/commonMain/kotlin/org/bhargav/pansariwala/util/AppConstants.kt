package org.bhargav.pansariwala.util

object AppConstants {
    const val DEFAULT_LANGUAGE: String = "en"
    const val DEMO_SHOP_ID: String = "shop_1"
    const val IOS_APPLE_LANGUAGES_KEY: String = "AppleLanguages"

    const val DEFAULT_SEARCH_RADIUS_KM: Double = 20.0
    const val MIN_SEARCH_RADIUS_KM: Double = 10.0
    const val MAX_SEARCH_RADIUS_KM: Double = 50.0

    const val PLATFORM_FEE_INR: Double = 10.0
    const val DELIVERY_BASE_PER_KM_INR: Double = 8.0
    const val DELIVERY_SURCHARGE_RATIO: Double = 0.30

    const val DEFAULT_MAP_LAT: Double = 28.6139
    const val DEFAULT_MAP_LNG: Double = 77.2090
    const val GOOGLE_MAPS_API_KEY: String = "AIzaSyCd8M0FflE0xOkx_Yy3LwHAHR6CyY0JpRI"
    const val GOOGLE_DIRECTIONS_URL: String = "https://maps.googleapis.com/maps/api/directions/json"
    const val GOOGLE_PLACES_AUTOCOMPLETE_URL: String = "https://maps.googleapis.com/maps/api/place/autocomplete/json"
    const val GOOGLE_PLACES_DETAILS_URL: String = "https://maps.googleapis.com/maps/api/place/details/json"
    const val GOOGLE_GEOCODE_URL: String = "https://maps.googleapis.com/maps/api/geocode/json"
    const val PLACE_SEARCH_DEBOUNCE_MS: Long = 350L
    const val DEFAULT_SHOP_DELIVERY_RADIUS_KM: Double = 20.0
    const val OSRM_ROUTE_URL: String = "https://router.project-osrm.org/route/v1/driving"
    const val PARTNER_MAP_ROUTE_COLOR: Long = 0xFF0D7377
    const val PARTNER_MAP_ROUTE_WIDTH: Float = 12f

    const val THANK_YOU_DELAY_MS: Long = 800L
    const val DELIVERY_RING_TIMEOUT_MS: Long = 15 * 60_000L
    const val PARTNER_OFFER_ACCEPT_MS: Long = 15_000L
    const val LIVE_ALERT_POLL_MS: Long = 5_000L
    const val LOCATION_FETCH_TIMEOUT_MS: Long = 4_000L
    /** Partner GPS push to server while online (foreground + background). */
    const val PARTNER_LOCATION_UPDATE_MS: Long = 2 * 60_000L
    const val PARTNER_RING_RADIUS_KM: Double = 8.0
    const val ARRIVAL_PROXIMITY_RADIUS_M: Double = 100.0
    const val ARRIVAL_PROXIMITY_POLL_MS: Long = 4_000L
    /** DEV ONLY — used by DevTripleTapUnlock; remove with that helper. */
    const val DEV_ARRIVAL_UNLOCK_TAPS: Int = 3
    const val DEV_ARRIVAL_UNLOCK_WINDOW_MS: Long = 2_000L
    const val RECENT_ORDERS_CARD_LIMIT: Int = 3

    const val DEV_OTP: String = "123456"
    const val JWT_PREFIX: String = "eyJ"
    const val DEFAULT_API_PORT: Int = 8080
    /** Shared API URL for debug/release and all platforms (override via Android `API_BASE_URL` env in CI). */
    const val API_BASE_URL: String = "http://35.172.232.196:8080"
    const val DEFAULT_API_BASE_URL: String = API_BASE_URL
    const val IOS_API_BASE_URL: String = API_BASE_URL
    const val HTTP_CONNECT_TIMEOUT_MS: Long = 5_000L
    const val HTTP_REQUEST_TIMEOUT_MS: Long = 12_000L
    const val HTTP_SOCKET_TIMEOUT_MS: Long = 12_000L
    const val HTTP_EXTERNAL_TIMEOUT_MS: Long = 5_000L
    const val REMOTE_LOGIN_TIMEOUT_MS: Long = 15_000L
    const val DEFAULT_PHONE_COUNTRY_CODE: String = "+91"
    const val PHONE_LOCAL_DIGITS: Int = 10
    const val OTP_TIMEOUT_SEC: Long = 60L
    const val PHOTO_JPEG_QUALITY: Int = 70
    const val PHOTO_MAX_EDGE_PX: Int = 1280

    object Prefs {
        const val SEARCH_RADIUS_KM: String = "pref_search_radius_km"
        const val CUSTOMER_PHONE: String = "pref_customer_phone"
        const val CUSTOMER_NAME: String = "pref_customer_name"
        const val CUSTOMER_ADDRESS: String = "pref_customer_address"
        const val PARTNER_ID: String = "pref_partner_id"
        const val PARTNER_ONLINE_DUTY: String = "pref_partner_online_duty"
        const val ROLE: String = "pref_auth_role"
        const val FCM_TOKEN: String = "pref_fcm_token"
        const val NOTIFY_OFFERS: String = "pref_notify_offers"
        const val NOTIFY_DELIVERY: String = "pref_notify_delivery"
    }

    object Roles {
        const val SHOP: String = "SHOP"
        const val CUSTOMER: String = "CUSTOMER"
        const val PARTNER: String = "PARTNER"
        const val ADMIN: String = "ADMIN"
    }

    object JwtClaim {
        const val ROLE: String = "role"
        const val SUBJECT_TYPE: String = "sub_type"
    }

    object Notification {
        const val CHANNEL_ORDERS: String = "orders"
        const val CHANNEL_DELIVERY: String = "delivery"
        const val CHANNEL_LOCATION: String = "partner_location"
        const val LOCATION_SERVICE_ID: Int = 42_081
        const val TYPE_ORDER: String = "order"
        const val TYPE_ONLINE_ORDER: String = "online_order"
        const val TYPE_DELIVERY_OFFER: String = "delivery_offer"
    }

    object Razorpay {
        const val MERCHANT_NAME: String = "Pansari Wala"
        const val CURRENCY: String = "INR"
        const val DEV_KEY_ID: String = "rzp_test_dev"
        const val TEST_KEY_PREFIX: String = "rzp_test_"
        const val TEST_UPI_VPA: String = "success@razorpay"
        const val DEV_PAYMENT_ID: String = "pay_dev"
        const val DEV_SIGNATURE: String = "dev"
        const val DEV_ORDER_PREFIX: String = "order_dev_"
        const val ERROR_CANCELLED: String = "razorpay_cancelled"
        const val ERROR_UNAVAILABLE: String = "razorpay_unavailable"
        const val ERROR_FAILED: String = "razorpay_failed"
    }

    object Checkout {
        const val ERROR_PROFILE: String = "Complete your profile"
        const val ERROR_EMPTY_CART: String = "Cart is empty"
        const val ERROR_MISSING_QUOTE: String = "Missing quote"
        const val ERROR_OUT_OF_RANGE: String = "Out of shop delivery range"
        const val ERROR_ADDRESS_REQUIRED: String = "Address is required"
    }
}
