package org.bhargav.pansariwala.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.api.installPansariHttpLogging
import org.bhargav.pansariwala.api.shouldInstallHttpLogging
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

private val photonDetailsCache = mutableMapOf<String, PlaceDetails>()

suspend fun searchPlaces(query: String): List<PlacePrediction> {
    if (query.isBlank()) return emptyList()
    val client = placesHttpClient()
    return try {
        val google = try {
            val body = client.get(AppConstants.GOOGLE_PLACES_AUTOCOMPLETE_URL) {
                parameter("input", query)
                parameter("components", "country:in")
                parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
            }.body<PlacesAutocompleteResponse>()
            when (body.status) {
                "OK" -> body.predictions.map { PlacePrediction(it.placeId, it.description) }
                "ZERO_RESULTS" -> emptyList()
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
        google ?: photonSearch(client, query).map { (id, details) ->
            PlacePrediction(id, details.formattedAddress)
        }
    } catch (_: Throwable) {
        emptyList()
    } finally {
        client.close()
    }
}

suspend fun fetchPlaceDetails(placeId: String): PlaceDetails? {
    if (placeId.startsWith(AppConstants.PHOTON_PLACE_ID_PREFIX)) return photonDetailsCache[placeId]
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
        val result = body.results.firstOrNull()
        val loc = result?.geometry?.location
        if (body.status == "OK" && result != null && loc != null) {
            PlaceDetails(
                formattedAddress = result.formattedAddress.ifBlank { query },
                locality = localityFrom(result.addressComponents),
                lat = loc.lat,
                lng = loc.lng,
            )
        } else {
            photonSearch(client, query).firstOrNull()?.second
        }
    } catch (_: Throwable) {
        try {
            photonSearch(client, query).firstOrNull()?.second
        } catch (_: Throwable) {
            null
        }
    } finally {
        client.close()
    }
}

suspend fun reverseGeocode(lat: Double, lng: Double): PlaceDetails? {
    val client = placesHttpClient()
    return try {
        val google = try {
            val body = client.get(AppConstants.GOOGLE_GEOCODE_URL) {
                parameter("latlng", "$lat,$lng")
                parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
            }.body<GeocodeResponse>()
            body.results.firstOrNull()?.takeIf { body.status == "OK" }?.let { result ->
                PlaceDetails(result.formattedAddress, localityFrom(result.addressComponents), lat, lng)
            }
        } catch (_: Throwable) {
            null
        }
        google ?: nominatimReverse(client, lat, lng) ?: try {
            val body = client.get(AppConstants.PHOTON_REVERSE_URL) {
                parameter("lat", lat)
                parameter("lon", lng)
                parameter("radius", AppConstants.PHOTON_REVERSE_RADIUS_KM)
            }.body<PhotonResponse>()
            photonDetails(body).firstOrNull()?.second?.copy(lat = lat, lng = lng)
        } catch (_: Throwable) {
            null
        }
    } finally {
        client.close()
    }
}

private suspend fun nominatimReverse(client: HttpClient, lat: Double, lng: Double): PlaceDetails? = try {
    val body = client.get(AppConstants.NOMINATIM_REVERSE_URL) {
        parameter("format", "jsonv2")
        parameter("lat", lat)
        parameter("lon", lng)
        parameter("zoom", 18)
        parameter("addressdetails", 1)
        header(HttpHeaders.UserAgent, AppConstants.NOMINATIM_USER_AGENT)
    }.body<NominatimReverseResponse>()
    val a = body.address
    val locality = a.city ?: a.town ?: a.village ?: a.suburb ?: a.county ?: a.stateDistrict ?: a.state.orEmpty()
    body.displayName.takeIf { it.isNotBlank() }?.let { PlaceDetails(it, locality, lat, lng) }
} catch (_: Throwable) {
    null
}

private suspend fun photonSearch(client: HttpClient, query: String): List<Pair<String, PlaceDetails>> {
    val body = client.get(AppConstants.PHOTON_SEARCH_URL) {
        parameter("q", query)
        parameter("limit", AppConstants.PHOTON_RESULT_LIMIT)
        parameter("bbox", AppConstants.PHOTON_INDIA_BBOX)
    }.body<PhotonResponse>()
    return photonDetails(body)
}

private fun photonDetails(body: PhotonResponse): List<Pair<String, PlaceDetails>> {
    return body.features.mapNotNull { feature ->
        val coords = feature.geometry?.coordinates ?: return@mapNotNull null
        if (coords.size < 2) return@mapNotNull null
        val p = feature.properties
        val locality = p.city ?: p.district ?: p.county ?: p.state.orEmpty()
        val street = listOfNotNull(p.housenumber, p.street).joinToString(" ").ifBlank { null }
        val address = listOfNotNull(p.name, street, p.locality, p.district, p.city, p.state, p.postcode)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        if (address.isBlank()) return@mapNotNull null
        val id = "${AppConstants.PHOTON_PLACE_ID_PREFIX}${p.osmType.orEmpty()}${p.osmId ?: "${coords[1]},${coords[0]}"}"
        val details = PlaceDetails(address, locality, lat = coords[1], lng = coords[0])
        photonDetailsCache[id] = details
        id to details
    }
}

private fun placesHttpClient(): HttpClient = createPlatformHttpClient().config {
    install(HttpTimeout) {
        connectTimeoutMillis = AppConstants.HTTP_EXTERNAL_TIMEOUT_MS
        requestTimeoutMillis = AppConstants.HTTP_EXTERNAL_TIMEOUT_MS
        socketTimeoutMillis = AppConstants.HTTP_EXTERNAL_TIMEOUT_MS
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    if (shouldInstallHttpLogging()) {
        install(Logging) { installPansariHttpLogging() }
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
private data class NominatimReverseResponse(
    @SerialName("display_name") val displayName: String = "",
    val address: NominatimAddress = NominatimAddress(),
)

@Serializable
private data class NominatimAddress(
    val suburb: String? = null,
    val village: String? = null,
    val town: String? = null,
    val city: String? = null,
    val county: String? = null,
    @SerialName("state_district") val stateDistrict: String? = null,
    val state: String? = null,
)

@Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList(),
)

@Serializable
private data class PhotonFeature(
    val properties: PhotonProperties = PhotonProperties(),
    val geometry: PhotonGeometry? = null,
)

@Serializable
private data class PhotonProperties(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val locality: String? = null,
    val district: String? = null,
    val city: String? = null,
    val county: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    @SerialName("osm_type") val osmType: String? = null,
    @SerialName("osm_id") val osmId: Long? = null,
)

@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double> = emptyList(),
)

@Serializable
private data class PlaceAddressComponent(
    @SerialName("long_name") val longName: String = "",
    val types: List<String> = emptyList(),
)
