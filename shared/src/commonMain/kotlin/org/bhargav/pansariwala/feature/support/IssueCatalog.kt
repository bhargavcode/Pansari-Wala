package org.bhargav.pansariwala.feature.support

import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.AppProduct.DELIVERY
import org.bhargav.pansariwala.product.AppProduct.POS
import org.bhargav.pansariwala.product.AppProduct.USER
import org.jetbrains.compose.resources.StringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.issue_feature_address
import pansariwala.shared.generated.resources.issue_feature_billing
import pansariwala.shared.generated.resources.issue_feature_cart_checkout
import pansariwala.shared.generated.resources.issue_feature_earnings
import pansariwala.shared.generated.resources.issue_feature_inventory
import pansariwala.shared.generated.resources.issue_feature_job_offers
import pansariwala.shared.generated.resources.issue_feature_login
import pansariwala.shared.generated.resources.issue_feature_my_orders
import pansariwala.shared.generated.resources.issue_feature_navigation
import pansariwala.shared.generated.resources.issue_feature_notifications
import pansariwala.shared.generated.resources.issue_feature_online_orders
import pansariwala.shared.generated.resources.issue_feature_other
import pansariwala.shared.generated.resources.issue_feature_payments
import pansariwala.shared.generated.resources.issue_feature_performance
import pansariwala.shared.generated.resources.issue_feature_pickup_delivery
import pansariwala.shared.generated.resources.issue_feature_printer
import pansariwala.shared.generated.resources.issue_feature_products
import pansariwala.shared.generated.resources.issue_feature_reports
import pansariwala.shared.generated.resources.issue_feature_shop_search
import pansariwala.shared.generated.resources.issue_feature_subscription
import pansariwala.shared.generated.resources.issue_feature_voice_scan
import pansariwala.shared.generated.resources.issue_type_action_failed
import pansariwala.shared.generated.resources.issue_type_crash
import pansariwala.shared.generated.resources.issue_type_earnings_wrong
import pansariwala.shared.generated.resources.issue_type_location_wrong
import pansariwala.shared.generated.resources.issue_type_login_failed
import pansariwala.shared.generated.resources.issue_type_map_issue
import pansariwala.shared.generated.resources.issue_type_no_notification
import pansariwala.shared.generated.resources.issue_type_not_loading
import pansariwala.shared.generated.resources.issue_type_order_missing
import pansariwala.shared.generated.resources.issue_type_order_status_stuck
import pansariwala.shared.generated.resources.issue_type_other
import pansariwala.shared.generated.resources.issue_type_otp_not_received
import pansariwala.shared.generated.resources.issue_type_payment_failed
import pansariwala.shared.generated.resources.issue_type_payment_not_confirmed
import pansariwala.shared.generated.resources.issue_type_price_wrong
import pansariwala.shared.generated.resources.issue_type_print_failed
import pansariwala.shared.generated.resources.issue_type_scan_failed
import pansariwala.shared.generated.resources.issue_type_slow
import pansariwala.shared.generated.resources.issue_type_stock_mismatch
import pansariwala.shared.generated.resources.issue_type_wrong_data

/** Radio options shown once a feature is picked; [name] is the stable key stored on the server. */
enum class IssueType(val label: StringResource) {
    NOT_LOADING(Res.string.issue_type_not_loading),
    CRASH(Res.string.issue_type_crash),
    SLOW(Res.string.issue_type_slow),
    WRONG_DATA(Res.string.issue_type_wrong_data),
    ACTION_FAILED(Res.string.issue_type_action_failed),
    PAYMENT_FAILED(Res.string.issue_type_payment_failed),
    PAYMENT_NOT_CONFIRMED(Res.string.issue_type_payment_not_confirmed),
    OTP_NOT_RECEIVED(Res.string.issue_type_otp_not_received),
    LOGIN_FAILED(Res.string.issue_type_login_failed),
    NO_NOTIFICATION(Res.string.issue_type_no_notification),
    PRINT_FAILED(Res.string.issue_type_print_failed),
    SCAN_FAILED(Res.string.issue_type_scan_failed),
    STOCK_MISMATCH(Res.string.issue_type_stock_mismatch),
    PRICE_WRONG(Res.string.issue_type_price_wrong),
    ORDER_MISSING(Res.string.issue_type_order_missing),
    ORDER_STATUS_STUCK(Res.string.issue_type_order_status_stuck),
    LOCATION_WRONG(Res.string.issue_type_location_wrong),
    MAP_ISSUE(Res.string.issue_type_map_issue),
    EARNINGS_WRONG(Res.string.issue_type_earnings_wrong),
    OTHER(Res.string.issue_type_other),
}

/** App areas a user can report against; [apps] decides which build lists it. */
enum class IssueFeature(
    val label: StringResource,
    val apps: Set<AppProduct>,
    val types: List<IssueType>,
) {
    BILLING(Res.string.issue_feature_billing, setOf(POS), listOf(IssueType.ACTION_FAILED, IssueType.PRICE_WRONG, IssueType.SCAN_FAILED, IssueType.CRASH, IssueType.OTHER)),
    INVENTORY(Res.string.issue_feature_inventory, setOf(POS), listOf(IssueType.STOCK_MISMATCH, IssueType.PRICE_WRONG, IssueType.NOT_LOADING, IssueType.ACTION_FAILED, IssueType.OTHER)),
    ONLINE_ORDERS(Res.string.issue_feature_online_orders, setOf(POS), listOf(IssueType.ORDER_MISSING, IssueType.ORDER_STATUS_STUCK, IssueType.NO_NOTIFICATION, IssueType.ACTION_FAILED, IssueType.OTHER)),
    REPORTS(Res.string.issue_feature_reports, setOf(POS), listOf(IssueType.WRONG_DATA, IssueType.NOT_LOADING, IssueType.OTHER)),
    PRINTER(Res.string.issue_feature_printer, setOf(POS), listOf(IssueType.PRINT_FAILED, IssueType.OTHER)),
    VOICE_SCAN(Res.string.issue_feature_voice_scan, setOf(POS), listOf(IssueType.SCAN_FAILED, IssueType.OTHER)),
    SUBSCRIPTION(Res.string.issue_feature_subscription, setOf(POS), listOf(IssueType.PAYMENT_FAILED, IssueType.WRONG_DATA, IssueType.OTHER)),
    SHOP_SEARCH(Res.string.issue_feature_shop_search, setOf(USER), listOf(IssueType.NOT_LOADING, IssueType.LOCATION_WRONG, IssueType.WRONG_DATA, IssueType.OTHER)),
    PRODUCTS(Res.string.issue_feature_products, setOf(USER), listOf(IssueType.PRICE_WRONG, IssueType.WRONG_DATA, IssueType.NOT_LOADING, IssueType.OTHER)),
    CART_CHECKOUT(Res.string.issue_feature_cart_checkout, setOf(USER), listOf(IssueType.ACTION_FAILED, IssueType.PRICE_WRONG, IssueType.PAYMENT_FAILED, IssueType.OTHER)),
    PAYMENTS(Res.string.issue_feature_payments, setOf(USER), listOf(IssueType.PAYMENT_FAILED, IssueType.PAYMENT_NOT_CONFIRMED, IssueType.WRONG_DATA, IssueType.OTHER)),
    MY_ORDERS(Res.string.issue_feature_my_orders, setOf(USER), listOf(IssueType.ORDER_MISSING, IssueType.ORDER_STATUS_STUCK, IssueType.OTHER)),
    ADDRESS(Res.string.issue_feature_address, setOf(USER), listOf(IssueType.LOCATION_WRONG, IssueType.ACTION_FAILED, IssueType.OTHER)),
    JOB_OFFERS(Res.string.issue_feature_job_offers, setOf(DELIVERY), listOf(IssueType.NO_NOTIFICATION, IssueType.ACTION_FAILED, IssueType.OTHER)),
    PICKUP_DELIVERY(Res.string.issue_feature_pickup_delivery, setOf(DELIVERY), listOf(IssueType.ACTION_FAILED, IssueType.ORDER_STATUS_STUCK, IssueType.OTHER)),
    NAVIGATION(Res.string.issue_feature_navigation, setOf(DELIVERY), listOf(IssueType.MAP_ISSUE, IssueType.LOCATION_WRONG, IssueType.OTHER)),
    EARNINGS(Res.string.issue_feature_earnings, setOf(DELIVERY), listOf(IssueType.EARNINGS_WRONG, IssueType.NOT_LOADING, IssueType.OTHER)),
    LOGIN(Res.string.issue_feature_login, setOf(POS, USER, DELIVERY), listOf(IssueType.OTP_NOT_RECEIVED, IssueType.LOGIN_FAILED, IssueType.CRASH, IssueType.OTHER)),
    NOTIFICATIONS(Res.string.issue_feature_notifications, setOf(POS, USER, DELIVERY), listOf(IssueType.NO_NOTIFICATION, IssueType.WRONG_DATA, IssueType.OTHER)),
    PERFORMANCE(Res.string.issue_feature_performance, setOf(POS, USER, DELIVERY), listOf(IssueType.SLOW, IssueType.CRASH, IssueType.NOT_LOADING, IssueType.OTHER)),
    OTHER(Res.string.issue_feature_other, setOf(POS, USER, DELIVERY), listOf(IssueType.OTHER)),
    ;

    companion object {
        fun forApp(app: AppProduct): List<IssueFeature> = entries.filter { app in it.apps }
    }
}
