package org.bhargav.pansariwala.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.context.GlobalContext

actual fun openExternalNavigation(destLat: Double, destLng: Double) {
    val context = GlobalContext.get().get<Context>()
    val navUri = Uri.parse("google.navigation:q=$destLat,$destLng&mode=d")
    val mapsIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val canLaunchMaps = mapsIntent.resolveActivity(context.packageManager) != null
    if (canLaunchMaps) {
        context.startActivity(mapsIntent)
        return
    }
    val webUri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng&travelmode=driving",
    )
    context.startActivity(
        Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
