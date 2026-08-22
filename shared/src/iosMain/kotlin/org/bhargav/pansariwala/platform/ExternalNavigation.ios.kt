package org.bhargav.pansariwala.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKLaunchOptionsDirectionsModeDriving
import platform.MapKit.MKLaunchOptionsDirectionsModeKey
import platform.MapKit.MKMapItem
import platform.MapKit.MKPlacemark

@OptIn(ExperimentalForeignApi::class)
actual fun openExternalNavigation(destLat: Double, destLng: Double) {
    val coordinate = CLLocationCoordinate2DMake(destLat, destLng)
    val placemark = MKPlacemark(coordinate = coordinate, addressDictionary = null)
    val mapItem = MKMapItem(placemark = placemark)
    mapItem.openInMapsWithLaunchOptions(
        mapOf(MKLaunchOptionsDirectionsModeKey to MKLaunchOptionsDirectionsModeDriving),
    )
}
