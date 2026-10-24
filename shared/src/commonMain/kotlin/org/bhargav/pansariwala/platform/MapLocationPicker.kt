package org.bhargav.pansariwala.platform

/** Point chosen on the map; address parts are blank when reverse geocoding failed. */
data class MapPickedLocation(
    val lat: Double,
    val lng: Double,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
)

class MapPickerUnavailableException : Exception("Map could not be loaded")

/** Whether [pickLocationOnMap] can show a map on this platform. */
expect val mapLocationPickerSupported: Boolean

/**
 * Full-screen map with Google place search and a centre pin. Returns the pinned point, or null when
 * the user cancelled; throws [MapPickerUnavailableException] when the map failed to load.
 */
expect suspend fun pickLocationOnMap(lat: Double, lng: Double, zoom: Int): MapPickedLocation?
