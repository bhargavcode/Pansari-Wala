package org.bhargav.pansariwala.util

import org.bhargav.pansariwala.domain.pricing.DeliveryPricing

object GeoDistance {
    fun metersBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double =
        DeliveryPricing.haversineKm(lat1, lng1, lat2, lng2) * 1_000.0

    fun isWithinMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        radiusMeters: Double,
    ): Boolean = metersBetween(lat1, lng1, lat2, lng2) <= radiusMeters
}
