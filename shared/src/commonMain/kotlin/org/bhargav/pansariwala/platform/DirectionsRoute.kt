package org.bhargav.pansariwala.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.util.AppConstants

data class LatLngPoint(val lat: Double, val lng: Double)

/**
 * Driving route following roads (Google Directions).
 * Falls back to a straight segment if the request fails.
 */
suspend fun fetchDrivingRoute(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
): List<LatLngPoint> {
    val straight = listOf(LatLngPoint(originLat, originLng), LatLngPoint(destLat, destLng))
    val client = createPlatformHttpClient().config {
        install(HttpTimeout) {
            connectTimeoutMillis = AppConstants.HTTP_EXTERNAL_TIMEOUT_MS
            requestTimeoutMillis = AppConstants.HTTP_EXTERNAL_TIMEOUT_MS
            socketTimeoutMillis = AppConstants.HTTP_EXTERNAL_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
    return try {
        fetchGoogleDrivingRoute(client, originLat, originLng, destLat, destLng) ?: straight
    } catch (_: Throwable) {
        straight
    } finally {
        client.close()
    }
}

private suspend fun fetchGoogleDrivingRoute(
    client: HttpClient,
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
): List<LatLngPoint>? {
    val response = client.get(AppConstants.GOOGLE_DIRECTIONS_URL) {
        parameter("origin", "$originLat,$originLng")
        parameter("destination", "$destLat,$destLng")
        parameter("mode", "driving")
        parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
    }.body<GoogleDirectionsResponse>()
    if (response.status != "OK") return null
    val route = response.routes.firstOrNull() ?: return null
    // Step polylines follow every road bend; overview_polyline is simplified and cuts corners.
    val detailed = route.legs.flatMap { leg -> leg.steps.flatMap { decodePolyline(it.polyline?.points.orEmpty()) } }
    val points = detailed.ifEmpty { decodePolyline(route.overviewPolyline?.points.orEmpty()) }
    if (points.size < 2) return null
    // Snap ends to the exact partner / destination so the line starts and ends on the markers.
    return listOf(LatLngPoint(originLat, originLng)) + points + LatLngPoint(destLat, destLng)
}

fun decodePolyline(encoded: String): List<LatLngPoint> {
    val poly = ArrayList<LatLngPoint>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        result = 0
        shift = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        poly.add(LatLngPoint(lat / 1e5, lng / 1e5))
    }
    return poly
}

@Serializable
private data class GoogleDirectionsResponse(
    val status: String = "",
    val routes: List<GoogleRoute> = emptyList(),
)

@Serializable
private data class GoogleRoute(
    @SerialName("overview_polyline") val overviewPolyline: GooglePolyline? = null,
    val legs: List<GoogleLeg> = emptyList(),
)

@Serializable
private data class GoogleLeg(
    val steps: List<GoogleStep> = emptyList(),
)

@Serializable
private data class GoogleStep(
    val polyline: GooglePolyline? = null,
)

@Serializable
private data class GooglePolyline(
    val points: String = "",
)
