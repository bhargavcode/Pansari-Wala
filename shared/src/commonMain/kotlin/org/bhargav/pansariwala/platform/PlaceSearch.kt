package org.bhargav.pansariwala.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.json
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.api.installPansariHttpLogging
import org.bhargav.pansariwala.api.shouldInstallHttpLogging
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId

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

/**
 * Google Places session: autocomplete keystrokes + the final details lookup share one token so
 * Google bills them as a single session. Reset after details are fetched.
 */
private var placesSessionToken: String? = null

private fun sessionToken(): String = placesSessionToken ?: generateId("ps").also { placesSessionToken = it }

private const val GOOGLE_OK = "OK"
private const val GOOGLE_ZERO_RESULTS = "ZERO_RESULTS"
private const val PHOTON_PLACE_PREFIX = "osm:"
private const val PHOTON_COUNTRY_CODE = "IN"

/** Photon results already carry coordinates; kept here so [fetchPlaceDetails] needs no second request. */
private val photonDetailsById = mutableMapOf<String, PlaceDetails>()

/**
 * Google Places Autocomplete, restricted to India. Falls back to Photon (OpenStreetMap) when Google
 * refuses the request — e.g. REQUEST_DENIED because billing is not enabled on the Cloud project.
 */
suspend fun searchPlaces(query: String): List<PlacePrediction> {
    if (query.isBlank()) return emptyList()
    val client = placesHttpClient()
    return try {
        val body = runCatching {
            client.get(AppConstants.GOOGLE_PLACES_AUTOCOMPLETE_URL) {
                parameter("input", query)
                parameter("components", "country:in")
                parameter("sessiontoken", sessionToken())
                parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
            }.body<PlacesAutocompleteResponse>()
        }.getOrNull()
        when (body?.status) {
            GOOGLE_OK -> body.predictions.map { PlacePrediction(it.placeId, it.description) }
            GOOGLE_ZERO_RESULTS -> emptyList()
            else -> photonSearch(client, query).map { details ->
                val id = "$PHOTON_PLACE_PREFIX${details.lat},${details.lng}"
                photonDetailsById[id] = details
                PlacePrediction(id, details.formattedAddress)
            }
        }
    } catch (_: Throwable) {
        emptyList()
    } finally {
        client.close()
    }
}

suspend fun fetchPlaceDetails(placeId: String): PlaceDetails? {
    if (placeId.startsWith(PHOTON_PLACE_PREFIX)) return photonDetailsById[placeId]
    val client = placesHttpClient()
    return try {
        val body = client.get(AppConstants.GOOGLE_PLACES_DETAILS_URL) {
            parameter("place_id", placeId)
            parameter("fields", "formatted_address,geometry,address_components,name")
            parameter("sessiontoken", sessionToken())
            parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
        }.body<PlacesDetailsResponse>()
        placesSessionToken = null
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
        val body = runCatching {
            client.get(AppConstants.GOOGLE_GEOCODE_URL) {
                parameter("address", query)
                parameter("components", "country:IN")
                parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
            }.body<GeocodeResponse>()
        }.getOrNull()
        when (body?.status) {
            GOOGLE_OK -> body.results.firstOrNull()?.let { result ->
                val loc = result.geometry?.location ?: return null
                PlaceDetails(
                    formattedAddress = result.formattedAddress.ifBlank { query },
                    locality = localityFrom(result.addressComponents),
                    lat = loc.lat,
                    lng = loc.lng,
                )
            }
            GOOGLE_ZERO_RESULTS -> null
            else -> photonSearch(client, query).firstOrNull()
        }
    } catch (_: Throwable) {
        null
    } finally {
        client.close()
    }
}

suspend fun reverseGeocode(lat: Double, lng: Double): PlaceDetails? {
    val client = placesHttpClient()
    return try {
        val body = runCatching {
            client.get(AppConstants.GOOGLE_GEOCODE_URL) {
                parameter("latlng", "$lat,$lng")
                parameter("key", AppConstants.GOOGLE_MAPS_API_KEY)
            }.body<GeocodeResponse>()
        }.getOrNull()
        val google = body?.results?.firstOrNull()?.takeIf { body.status == GOOGLE_OK }?.let { result ->
            PlaceDetails(result.formattedAddress, localityFrom(result.addressComponents), lat, lng)
        }
        google ?: photonReverse(client, lat, lng)
    } catch (_: Throwable) {
        null
    } finally {
        client.close()
    }
}

private suspend fun photonSearch(client: HttpClient, query: String): List<PlaceDetails> {
    val body = client.get(AppConstants.PHOTON_SEARCH_URL) {
        parameter("q", query)
        parameter("limit", AppConstants.PHOTON_RESULT_LIMIT * 2)
        parameter("lat", AppConstants.DEFAULT_MAP_LAT)
        parameter("lon", AppConstants.DEFAULT_MAP_LNG)
    }.body<PhotonResponse>()
    return body.features
        .filter { it.properties.countryCode.equals(PHOTON_COUNTRY_CODE, ignoreCase = true) }
        .mapNotNull { it.toPlaceDetails() }
        .distinctBy { it.formattedAddress }
        .take(AppConstants.PHOTON_RESULT_LIMIT)
}

private suspend fun photonReverse(client: HttpClient, lat: Double, lng: Double): PlaceDetails? {
    val body = client.get(AppConstants.PHOTON_REVERSE_URL) {
        parameter("lat", lat)
        parameter("lon", lng)
    }.body<PhotonResponse>()
    return body.features.firstOrNull()?.toPlaceDetails()?.copy(lat = lat, lng = lng)
}

private fun PhotonFeature.toPlaceDetails(): PlaceDetails? {
    val coords = geometry?.coordinates ?: return null
    if (coords.size < 2) return null
    val p = properties
    val street = listOfNotNull(p.houseNumber, p.street).joinToString(" ").ifBlank { null }
    val locality = listOf(p.locality, p.district, p.city, p.county).firstOrNull { !it.isNullOrBlank() }.orEmpty()
    val address = listOf(p.name, street, p.locality, p.district, p.city, p.state, p.postcode)
        .filterNot { it.isNullOrBlank() }
        .distinct()
        .joinToString(", ")
    if (address.isBlank()) return null
    return PlaceDetails(formattedAddress = address, locality = locality, lat = coords[1], lng = coords[0])
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
private data class PlaceAddressComponent(
    @SerialName("long_name") val longName: String = "",
    val types: List<String> = emptyList(),
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

/** [coordinates] is GeoJSON order: longitude, latitude. */
@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double> = emptyList(),
)

@Serializable
private data class PhotonProperties(
    val name: String? = null,
    @SerialName("housenumber") val houseNumber: String? = null,
    val street: String? = null,
    val locality: String? = null,
    val district: String? = null,
    val city: String? = null,
    val county: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    @SerialName("countrycode") val countryCode: String? = null,
)
