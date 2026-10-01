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
    /** Retry backoff for registering the FCM token with the server. */
    const val PUSH_REGISTER_RETRY_MS: Long = 15_000L
    /** Idle wait while partner is offline / logged out before rechecking socket duty. */
    const val PARTNER_OFFER_SOCKET_IDLE_MS: Long = 3_000L
    /** Backoff after a delivery WebSocket disconnect before reconnect. */
    const val PARTNER_OFFER_SOCKET_RETRY_MS: Long = 3_000L
    const val LOCATION_FETCH_TIMEOUT_MS: Long = 15_000L
    /** Partner GPS push to server (foreground + background). */
    const val PARTNER_LOCATION_UPDATE_MS: Long = 2 * 60_000L
    const val PARTNER_RING_RADIUS_KM: Double = 8.0
    const val ARRIVAL_PROXIMITY_RADIUS_M: Double = 100.0
    const val ARRIVAL_PROXIMITY_POLL_MS: Long = 4_000L
    /** DEV ONLY — used by DevTripleTapUnlock; remove with that helper. */
    const val DEV_ARRIVAL_UNLOCK_TAPS: Int = 3
    const val DEV_ARRIVAL_UNLOCK_WINDOW_MS: Long = 2_000L
    const val RECENT_ORDERS_CARD_LIMIT: Int = 3

    const val DEV_OTP: String = "123456"
    const val DELIVERY_OTP_LENGTH: Int = 4
    const val PUBLIC_ORDER_CODE_LENGTH: Int = 8

    object PartnerProgress {
        const val TO_STORE: String = "TO_STORE"
        const val AT_STORE: String = "AT_STORE"
        const val CAPTURE: String = "CAPTURE"
        const val TO_CUSTOMER: String = "TO_CUSTOMER"
        const val AT_CUSTOMER: String = "AT_CUSTOMER"
    }
    const val JWT_PREFIX: String = "eyJ"
    const val DEFAULT_API_PORT: Int = 8080
    const val PUBLIC_DOMAIN: String = "pansariwala.shop"
    const val WEB_BASE_URL: String = "https://pansariwala.shop"
    const val API_HOST: String = "api.pansariwala.shop"
    /** EC2 API for all platforms (debug + release). Override via Android `API_BASE_URL` env/property. */
    const val API_BASE_URL: String = "https://api.pansariwala.shop"
    const val DEFAULT_API_BASE_URL: String = API_BASE_URL
    const val IOS_API_BASE_URL: String = API_BASE_URL
    const val BRAND_TAGLINE: String = "Apka apna market"
    const val PLAY_STORE_USER_URL: String =
        "https://play.google.com/store/apps/details?id=org.bhargav.pansariwala.user"
    const val PLAY_STORE_DELIVERY_URL: String =
        "https://play.google.com/store/apps/details?id=org.bhargav.pansariwala.delivery"
    const val APP_STORE_USER_URL: String = "https://apps.apple.com/app/id000000000"
    const val APP_STORE_DELIVERY_URL: String = "https://apps.apple.com/app/id000000001"
    const val S3_BUCKET: String = "pansariwala-assets"
    const val S3_REGION: String = "ap-south-1"
    object S3Prefix {
        const val MASTER_PRODUCT_IMAGES: String = "master/product-images/"
        const val MASTER_SHOP_IMAGES: String = "master/shop-images/"
        const val MASTER_SHOP_VERIFICATION: String = "master/shop-verification/"
        const val USERS_USER_IMAGE: String = "users/user-image/"
        const val USERS_VEHICLE_IMAGE: String = "users/vehicle-image/"
        const val USERS_USER_IDS: String = "users/user-ids/"
        const val PARTNERS_USER_IMAGE: String = "partners/user-image/"
        const val PARTNERS_VEHICLE_IMAGE: String = "partners/vehicle-image/"
        const val PARTNERS_USER_IDS: String = "partners/user-ids/"
        const val SHOPS_DELIVERY_PACKETS: String = "shops/delivery-packets/"
        const val SHOPS_SHOP: String = "shops/shop/"
        const val SHOPS_PRODUCT_IMAGES: String = "shops/product-images/"
    }
    /** Allowed S3 key prefixes for authenticated multipart uploads. */
    val ALLOWED_UPLOAD_PREFIXES: Set<String> = setOf(
        S3Prefix.MASTER_PRODUCT_IMAGES,
        S3Prefix.MASTER_SHOP_IMAGES,
        S3Prefix.MASTER_SHOP_VERIFICATION,
        S3Prefix.USERS_USER_IMAGE,
        S3Prefix.USERS_VEHICLE_IMAGE,
        S3Prefix.USERS_USER_IDS,
        S3Prefix.PARTNERS_USER_IMAGE,
        S3Prefix.PARTNERS_VEHICLE_IMAGE,
        S3Prefix.PARTNERS_USER_IDS,
        S3Prefix.SHOPS_DELIVERY_PACKETS,
        S3Prefix.SHOPS_SHOP,
        S3Prefix.SHOPS_PRODUCT_IMAGES,
    )
    const val HTTP_CONNECT_TIMEOUT_MS: Long = 5_000L
    const val HTTP_REQUEST_TIMEOUT_MS: Long = 12_000L
    const val HTTP_SOCKET_TIMEOUT_MS: Long = 12_000L
    const val HTTP_EXTERNAL_TIMEOUT_MS: Long = 5_000L
    const val HTTP_UPLOAD_TIMEOUT_MS: Long = 60_000L
    const val HTTP_LOG_TAG: String = "PansariHttp"
    const val REMOTE_LOGIN_TIMEOUT_MS: Long = 15_000L
    const val DEFAULT_PHONE_COUNTRY_CODE: String = "+91"
    const val PHONE_LOCAL_DIGITS: Int = 10
    const val OTP_TIMEOUT_SEC: Long = 60L
    /** WhatsApp-style: high visual quality JPEG after downscale. */
    const val PHOTO_JPEG_QUALITY: Int = 82
    const val PHOTO_JPEG_QUALITY_MIN: Int = 55
    const val PHOTO_MAX_EDGE_PX: Int = 1600
    /** Soft cap after compress (~1MB camera shot → ~100KB). */
    const val PHOTO_TARGET_MAX_BYTES: Int = 100_000
    /** Aim for ~90% size reduction vs original when original is large. */
    const val PHOTO_TARGET_RATIO: Double = 0.10
    const val PHOTO_MAX_PRODUCT_IMAGES: Int = 4
    const val PHOTO_THUMB_EDGE_PX: Int = 256
    const val PHOTO_THUMB_JPEG_QUALITY: Int = 75
    const val IMAGE_BITMAP_CACHE_SIZE: Int = 48
    const val IMAGE_CAROUSEL_INTERVAL_MS: Long = 2_000L
    const val UPLOAD_AUTO_RETRY_COUNT: Int = 1
    const val UPLOAD_MAX_RAW_BYTES: Int = 12_000_000

    object Prefs {
        const val SEARCH_RADIUS_KM: String = "pref_search_radius_km"
        const val CUSTOMER_PHONE: String = "pref_customer_phone"
        const val CUSTOMER_NAME: String = "pref_customer_name"
        const val CUSTOMER_ADDRESS: String = "pref_customer_address"
        const val PARTNER_ID: String = "pref_partner_id"
        const val CACHED_PARTNER_PROFILE: String = "pref_cached_partner_profile"
        const val CACHED_CUSTOMER_PROFILE: String = "pref_cached_customer_profile"
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

    /** FCM data payload contract with the server (`PushService`). */
    object Push {
        const val EVENT_ORDER_UPDATE: String = "ORDER_UPDATE"
        const val ALERT_ORDER_NEW: String = "ORDER_NEW"
        const val ALERT_PARTNER_TIMEOUT: String = "PARTNER_TIMEOUT"
        const val ALERT_ACCEPTED: String = "ACCEPTED"
        const val ALERT_ON_THE_WAY: String = "ON_THE_WAY"
        const val ALERT_DELIVERED: String = "DELIVERED"
        const val ALERT_CANCELLED: String = "CANCELLED"
        const val KEY_PREFIX_STATUS: String = "ORDER_STATUS"
        const val KEY_EVENT: String = "event"
        const val KEY_ORDER_ID: String = "orderId"
        const val KEY_STATUS: String = "status"
        const val KEY_ALERT: String = "alert"
        const val KEY_SHOP_NAME: String = "shopName"
        const val KEY_CUSTOMER_NAME: String = "customerName"
        const val PLATFORM_ANDROID: String = "android"
        const val PLATFORM_IOS: String = "ios"
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

    object MasterWeb {
        const val TOKEN_STORAGE_KEY: String = "master_admin_token"
        const val REMEMBER_USER_KEY: String = "master_admin_username"
        const val SIDEBAR_WIDTH_DP: Int = 220
        const val COMPACT_BREAKPOINT_DP: Int = 840
    }

    object ShopFeature {
        const val VOICE_SEARCH: String = "voiceSearch"
        const val BARCODE_SEARCH: String = "barcodeSearch"
        const val REPORT_GENERATION: String = "reportGeneration"
        const val ONLINE_ORDERS: String = "onlineOrders"
        const val INVENTORY_ALERTS: String = "inventoryAlerts"
    }

    object DateFilter {
        const val TODAY: String = "TODAY"
        const val YESTERDAY: String = "YESTERDAY"
        const val WEEKLY: String = "WEEKLY"
        const val MONTHLY: String = "MONTHLY"
        const val YEARLY: String = "YEARLY"
        const val CUSTOM: String = "CUSTOM"
    }

    object VehicleType {
        const val SCOOTY: String = "SCOOTY"
        const val E_RIKSHAW: String = "E_RIKSHAW"
        const val BIKE: String = "BIKE"
        const val TEMPO: String = "TEMPO"
        const val VAN: String = "VAN"
    }
}
