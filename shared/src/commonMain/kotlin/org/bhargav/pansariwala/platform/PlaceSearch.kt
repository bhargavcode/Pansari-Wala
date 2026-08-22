package org.bhargav.pansariwala.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.util.AppConstants

data class PlacePrediction(
    val placeId: String,
    val description: String,
)

data class PlaceDetails(
    val formattedAddress: String,
    val locality: String,
    val lat: Double,
    val lng: Double,
)

suspend fun searchPlaces(query: String): List<PlacePrediction> {
    if (query.isBlank()) return emptyList()
    val client = placesHttpClient()
    return try {
        val body = client.get(AppConstants.GOOGLE_PLACES_AUTOCOMPLETE_URL) {
            parameter("input", query)
            parameter("components", "country:in")
            parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
        }.body<PlacesAutocompleteResponse>()
        if (body.status != "OK" && body.status != "ZERO_RESULTS") return emptyList()
        body.predictions.map { PlacePrediction(it.placeId, it.description) }
    } catch (_: Throwable) {
        emptyList()
    } finally {
        client.close()
    }
}

suspend fun fetchPlaceDetails(placeId: String): PlaceDetails? {
    val client = placesHttpClient()
    return try {
        val body = client.get(AppConstants.GOOGLE_PLACES_DETAILS_URL) {
            parameter("place_id", placeId)
            parameter("fields", "formatted_address,geometry,address_components,name")
            parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
        }.body<PlacesDetailsResponse>()
        if (body.status != "OK") return null
        val result = body.result ?: return null
        val loc = result.geometry?.location ?: return null
        PlaceDetails(
            formattedAddress = result.formattedAddress.ifBlank { result.name.orEmpty() },
            locality = localityFrom(result.addressComponents),
            lat = loc.lat,
            lng = loc.lng,
        )
    } catch (_: Throwable) {
        null
    } finally {
        client.close()
    }
}

suspend fun geocodeAddress(query: String): PlaceDetails? {
    if (query.isBlank()) return null
    val client = placesHttpClient()
    return try {
        val body = client.get(AppConstants.GOOGLE_GEOCODE_URL) {
            parameter("address", query)
            parameter("components", "country:IN")
            parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
        }.body<GeocodeResponse>()
        if (body.status != "OK") return null
        val result = body.results.firstOrNull() ?: return null
        val loc = result.geometry?.location ?: return null
        PlaceDetails(
            formattedAddress = result.formattedAddress.ifBlank { query },
            locality = localityFrom(result.addressComponents),
            lat = loc.lat,
            lng = loc.lng,
        )
    } catch (_: Throwable) {
        null
    } finally {
        client.close()
    }
}

private fun placesHttpClient(): HttpClient = createPlatformHttpClient().config {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
}

private fun localityFrom(components: List<PlaceAddressComponent>): String {
    val preferred = listOf("locality", "sublocality", "administrative_area_level_3", "administrative_area_level_2")
    for (type in preferred) {
        val match = components.firstOrNull { type in it.types }?.longName
        if (!match.isNullOrBlank()) return match
    }
    return ""
}

@Serializable
private data class PlacesAutocompleteResponse(
    val status: String = "",
    val predictions: List<PlacePredictionDto> = emptyList(),
)

@Serializable
private data class PlacePredictionDto(
    val description: String = "",
    @SerialName("place_id") val placeId: String = "",
)

@Serializable
private data class PlacesDetailsResponse(
    val status: String = "",
    val result: PlaceDetailsResult? = null,
)

@Serializable
private data class PlaceDetailsResult(
    val name: String? = null,
    @SerialName("formatted_address") val formattedAddress: String = "",
    val geometry: PlaceGeometry? = null,
    @SerialName("address_components") val addressComponents: List<PlaceAddressComponent> = emptyList(),
)

@Serializable
private data class GeocodeResponse(
    val status: String = "",
    val results: List<GeocodeResult> = emptyList(),
)

@Serializable
private data class GeocodeResult(
    @SerialName("formatted_address") val formattedAddress: String = "",
    val geometry: PlaceGeometry? = null,
    @SerialName("address_components") val addressComponents: List<PlaceAddressComponent> = emptyList(),
)

@Serializable
private data class PlaceGeometry(
    val location: PlaceLatLng? = null,
)

@Serializable
private data class PlaceLatLng(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

@Serializable
private data class PlaceAddressComponent(
    @SerialName("long_name") val longName: String = "",
    val types: List<String> = emptyList(),
)
