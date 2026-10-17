package org.bhargav.pansariwala.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import org.bhargav.pansariwala.util.AppConstants
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKOverlayLevelAboveRoads
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPointAnnotation
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.MapKit.addOverlay
import platform.MapKit.overlays
import platform.MapKit.removeOverlay
import platform.UIKit.UIColor
import platform.darwin.NSObject
import kotlin.math.abs

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PartnerLiveMap(
    lat: Double,
    lng: Double,
    modifier: Modifier,
    originLat: Double?,
    originLng: Double?,
) {
    val delegate = remember { PartnerMapDelegate() }
    var routePoints by remember(originLat, originLng, lat, lng) {
        mutableStateOf(
            if (originLat != null && originLng != null) {
                listOf(LatLngPoint(originLat, originLng), LatLngPoint(lat, lng))
            } else {
                emptyList()
            },
        )
    }
    LaunchedEffect(originLat, originLng, lat, lng) {
        if (originLat != null && originLng != null) {
            routePoints = fetchDrivingRoute(originLat, originLng, lat, lng)
        } else {
            routePoints = emptyList()
        }
    }
    UIKitView(
        factory = {
            MKMapView().apply {
                showsUserLocation = false
                this.delegate = delegate
                updateMap(this, lat, lng, originLat, originLng, routePoints)
            }
        },
        modifier = modifier,
        update = { mapView ->
            mapView.delegate = delegate
            updateMap(mapView, lat, lng, originLat, originLng, routePoints)
        },
    )
}

@OptIn(ExperimentalForeignApi::class)
private class PartnerMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(mapView: MKMapView, rendererForOverlay: MKOverlayProtocol): MKOverlayRenderer {
        val renderer = MKPolylineRenderer(overlay = rendererForOverlay as MKPolyline)
        renderer.strokeColor = UIColor.colorWithRed(0.05, green = 0.45, blue = 0.47, alpha = 1.0)
        renderer.lineWidth = 5.0
        return renderer
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun updateMap(
    mapView: MKMapView,
    lat: Double,
    lng: Double,
    originLat: Double?,
    originLng: Double?,
    routePoints: List<LatLngPoint>,
) {
    mapView.annotations.filterIsInstance<MKPointAnnotation>().forEach { mapView.removeAnnotation(it) }
    mapView.overlays.filterIsInstance<MKPolyline>().forEach { mapView.removeOverlay(it) }

    val dest = CLLocationCoordinate2DMake(lat, lng)
    val destMarker = MKPointAnnotation()
    destMarker.setCoordinate(dest)
    destMarker.setTitle("Destination")
    mapView.addAnnotation(destMarker)

    if (originLat != null && originLng != null) {
        val origin = CLLocationCoordinate2DMake(originLat, originLng)
        val originMarker = MKPointAnnotation()
        originMarker.setCoordinate(origin)
        originMarker.setTitle("Partner")
        mapView.addAnnotation(originMarker)

        val points = routePoints.ifEmpty {
            listOf(LatLngPoint(originLat, originLng), LatLngPoint(lat, lng))
        }
        memScoped {
            val coords = allocArray<CLLocationCoordinate2D>(points.size) { index ->
                latitude = points[index].lat
                longitude = points[index].lng
            }
            val polyline = MKPolyline.polylineWithCoordinates(coords, points.size.toULong())
            mapView.addOverlay(polyline, MKOverlayLevelAboveRoads)
        }

        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }
        val midLat = (minLat + maxLat) / 2.0
        val midLng = (minLng + maxLng) / 2.0
        val latDelta = (abs(maxLat - minLat) * 1.4).coerceAtLeast(0.02)
        val lngDelta = (abs(maxLng - minLng) * 1.4).coerceAtLeast(0.02)
        mapView.setRegion(
            MKCoordinateRegionMake(
                CLLocationCoordinate2DMake(midLat, midLng),
                MKCoordinateSpanMake(latDelta, lngDelta),
            ),
            animated = true,
        )
    } else {
        mapView.setRegion(
            MKCoordinateRegionMakeWithDistance(
                dest,
                AppConstants.PARTNER_MAP_DEFAULT_SPAN_M,
                AppConstants.PARTNER_MAP_DEFAULT_SPAN_M,
            ),
            animated = true,
        )
    }
}
