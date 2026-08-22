package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * @param lat destination latitude (shop or customer)
 * @param lng destination longitude
 * @param originLat optional start (partner / delivery location) for route polyline
 * @param originLng optional start longitude
 */
@Composable
expect fun PartnerLiveMap(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    originLat: Double? = null,
    originLng: Double? = null,
)
