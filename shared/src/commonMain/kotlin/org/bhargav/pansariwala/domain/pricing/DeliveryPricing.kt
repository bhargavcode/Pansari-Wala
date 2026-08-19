package org.bhargav.pansariwala.domain.pricing

import org.bhargav.pansariwala.domain.model.CheckoutQuote
import org.bhargav.pansariwala.util.AppConstants
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DeliveryPricing {
    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthKm = 6371.0
        val dLat = degToRad(lat2 - lat1)
        val dLng = degToRad(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(degToRad(lat1)) * cos(degToRad(lat2)) * sin(dLng / 2).pow(2)
        return 2 * earthKm * asin(min(1.0, sqrt(a)))
    }

    fun partnerPayout(distanceFromShopKm: Double): Double =
        AppConstants.DELIVERY_BASE_PER_KM_INR * distanceFromShopKm

    fun customerDeliveryCharge(distanceKm: Double): Double {
        val base = AppConstants.DELIVERY_BASE_PER_KM_INR * distanceKm
        return base + base * AppConstants.DELIVERY_SURCHARGE_RATIO
    }

    fun quote(
        itemsSubtotal: Double,
        discountPercent: Double,
        deliveryDistanceKm: Double,
    ): CheckoutQuote {
        val discount = itemsSubtotal * (discountPercent / 100.0)
        val platformFee = AppConstants.PLATFORM_FEE_INR
        val delivery = customerDeliveryCharge(deliveryDistanceKm)
        val payable = (itemsSubtotal - discount + platformFee + delivery).coerceAtLeast(0.0)
        return CheckoutQuote(
            itemsSubtotal = itemsSubtotal,
            discount = discount,
            platformFee = platformFee,
            deliveryCharge = delivery,
            payable = payable,
            deliveryDistanceKm = deliveryDistanceKm,
        )
    }

    private fun degToRad(deg: Double): Double = deg * (kotlin.math.PI / 180.0)
}
