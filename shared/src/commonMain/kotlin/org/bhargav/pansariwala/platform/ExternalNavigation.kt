package org.bhargav.pansariwala.platform

/** Opens the platform maps app with turn-by-turn navigation to [destLat], [destLng]. */
expect fun openExternalNavigation(destLat: Double, destLng: Double)
