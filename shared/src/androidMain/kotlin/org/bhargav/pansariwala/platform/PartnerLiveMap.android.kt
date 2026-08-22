package org.bhargav.pansariwala.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import org.bhargav.pansariwala.util.AppConstants

@Composable
actual fun PartnerLiveMap(
    lat: Double,
    lng: Double,
    modifier: Modifier,
    originLat: Double?,
    originLng: Double?,
) {
    val dest = LatLng(lat, lng)
    val origin = if (originLat != null && originLng != null) {
        LatLng(originLat, originLng)
    } else {
        null
    }
    var routePoints by remember(originLat, originLng, lat, lng) {
        mutableStateOf(
            if (origin != null) listOf(origin, dest) else emptyList(),
        )
    }
    LaunchedEffect(originLat, originLng, lat, lng) {
        if (originLat != null && originLng != null) {
            val points = fetchDrivingRoute(originLat, originLng, lat, lng)
            routePoints = points.map { LatLng(it.lat, it.lng) }
        } else {
            routePoints = emptyList()
        }
    }

    // Bitmaps are fine early; BitmapDescriptorFactory needs Maps SDK init (after map load).
    val density = LocalDensity.current
    val partnerBitmap = remember(density) {
        createPartnerMarkerBitmap(with(density) { 48.dp.roundToPx() })
    }
    val destinationBitmap = remember(density) {
        createDestinationMarkerBitmap(with(density) { 44.dp.roundToPx() })
    }
    var partnerIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
    var destinationIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(dest, 14f)
    }
    LaunchedEffect(routePoints) {
        if (routePoints.size >= 2) {
            val boundsBuilder = LatLngBounds.builder()
            routePoints.forEach { boundsBuilder.include(it) }
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
        } else {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(dest, 14f)
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
        ),
        onMapLoaded = {
            partnerIcon = BitmapDescriptorFactory.fromBitmap(partnerBitmap)
            destinationIcon = BitmapDescriptorFactory.fromBitmap(destinationBitmap)
        },
    ) {
        if (origin != null && routePoints.isNotEmpty()) {
            Marker(
                state = MarkerState(position = origin),
                icon = partnerIcon,
                anchor = Offset(0.5f, 0.5f),
            )
            Polyline(
                points = routePoints,
                color = Color(AppConstants.PARTNER_MAP_ROUTE_COLOR),
                width = AppConstants.PARTNER_MAP_ROUTE_WIDTH,
            )
        }
        Marker(
            state = MarkerState(position = dest),
            icon = destinationIcon,
            anchor = Offset(0.5f, 1f),
        )
    }
}

private fun createPartnerMarkerBitmap(sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val teal = 0xFF0D7377.toInt()
    val white = 0xFFFFFFFF.toInt()

    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx * 0.52f, sizePx * 0.54f, sizePx * 0.42f, shadow)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = teal
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx * 0.5f, sizePx * 0.5f, sizePx * 0.42f, fill)

    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.06f
    }
    canvas.drawCircle(sizePx * 0.5f, sizePx * 0.5f, sizePx * 0.36f, ring)

    val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = sizePx * 0.055f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.FILL
    }

    // Scooter wheels
    canvas.drawCircle(sizePx * 0.32f, sizePx * 0.62f, sizePx * 0.09f, ink)
    canvas.drawCircle(sizePx * 0.68f, sizePx * 0.62f, sizePx * 0.09f, ink)

    // Deck + stem
    canvas.drawLine(sizePx * 0.32f, sizePx * 0.62f, sizePx * 0.62f, sizePx * 0.62f, ink)
    canvas.drawLine(sizePx * 0.62f, sizePx * 0.62f, sizePx * 0.70f, sizePx * 0.40f, ink)
    canvas.drawLine(sizePx * 0.62f, sizePx * 0.40f, sizePx * 0.78f, sizePx * 0.40f, ink)

    // Rider torso
    canvas.drawCircle(sizePx * 0.48f, sizePx * 0.34f, sizePx * 0.07f, body)
    val torso = Path().apply {
        moveTo(sizePx * 0.42f, sizePx * 0.40f)
        lineTo(sizePx * 0.54f, sizePx * 0.40f)
        lineTo(sizePx * 0.56f, sizePx * 0.56f)
        lineTo(sizePx * 0.40f, sizePx * 0.56f)
        close()
    }
    canvas.drawPath(torso, body)

    // Delivery box behind rider
    val box = RectF(sizePx * 0.28f, sizePx * 0.42f, sizePx * 0.42f, sizePx * 0.56f)
    canvas.drawRoundRect(box, sizePx * 0.03f, sizePx * 0.03f, body)
    return bitmap
}

private fun createDestinationMarkerBitmap(sizePx: Int): Bitmap {
    val width = sizePx
    val height = (sizePx * 1.25f).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val teal = 0xFF0D7377.toInt()
    val white = 0xFFFFFFFF.toInt()

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = teal
        style = Paint.Style.FILL
    }
    val cx = width / 2f
    val cy = height * 0.38f
    val radius = width * 0.38f
    canvas.drawCircle(cx, cy, radius, fill)

    val tip = Path().apply {
        moveTo(cx - radius * 0.72f, cy + radius * 0.35f)
        lineTo(cx, height * 0.92f)
        lineTo(cx + radius * 0.72f, cy + radius * 0.35f)
        close()
    }
    canvas.drawPath(tip, fill)

    val home = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.FILL
    }
    val roof = Path().apply {
        moveTo(cx, cy - radius * 0.45f)
        lineTo(cx - radius * 0.38f, cy - radius * 0.05f)
        lineTo(cx + radius * 0.38f, cy - radius * 0.05f)
        close()
    }
    canvas.drawPath(roof, home)
    canvas.drawRect(
        cx - radius * 0.28f,
        cy - radius * 0.05f,
        cx + radius * 0.28f,
        cy + radius * 0.35f,
        home,
    )
    return bitmap
}
