package org.bhargav.pansariwala.platform

import kotlinx.browser.window

actual fun openExternalNavigation(destLat: Double, destLng: Double) {
    window.open(
        "https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng&travelmode=driving",
        "_blank",
    )
}
