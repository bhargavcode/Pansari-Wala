package org.bhargav.pansariwala.platform

actual val mapLocationPickerSupported: Boolean = false

actual suspend fun pickLocationOnMap(lat: Double, lng: Double, zoom: Int): MapPickedLocation? = null
