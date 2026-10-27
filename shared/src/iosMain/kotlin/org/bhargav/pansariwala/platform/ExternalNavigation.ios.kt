package org.bhargav.pansariwala.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/** Google Maps app when installed, otherwise Google Maps directions on the web. */
actual fun openExternalNavigation(destLat: Double, destLng: Double) {
    val app = UIApplication.sharedApplication
    val web = NSURL.URLWithString("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng&travelmode=driving")
    val native = NSURL.URLWithString("comgooglemaps://?daddr=$destLat,$destLng&directionsmode=driving")
    if (native == null) {
        web?.let { app.openURL(it, emptyMap<Any?, Any>(), null) }
        return
    }
    app.openURL(native, emptyMap<Any?, Any>()) { opened ->
        if (!opened) web?.let { app.openURL(it, emptyMap<Any?, Any>(), null) }
    }
}
