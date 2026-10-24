package org.bhargav.pansariwala.master

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan
import kotlin.time.TimeSource
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_latitude
import pansariwala.shared.generated.resources.master_longitude
import pansariwala.shared.generated.resources.master_map_attribution
import pansariwala.shared.generated.resources.master_map_location
import pansariwala.shared.generated.resources.master_map_not_found
import pansariwala.shared.generated.resources.master_map_recenter
import pansariwala.shared.generated.resources.master_map_search_hint
import pansariwala.shared.generated.resources.master_map_tap_hint
import pansariwala.shared.generated.resources.master_map_zoom_in
import pansariwala.shared.generated.resources.master_map_zoom_out
import pansariwala.shared.generated.resources.master_select_on_map

private const val TILE_URL = "https://tile.openstreetmap.org"
private const val GEOCODER_URL = "https://nominatim.openstreetmap.org"
private const val GEOCODER_COUNTRY = "in"
private const val GEOCODER_LANGUAGE = "en"
private const val TILE_SIZE = 256
private const val MIN_ZOOM = 3
private const val MAX_ZOOM = 19
private const val INITIAL_ZOOM = 15
private const val SEARCH_ZOOM = 16
private const val MAX_LAT = 85.05112878
private const val WHEEL_ZOOM_COOLDOWN_MS = 250L
private const val COORD_DECIMALS = 1_000_000.0

/** Address parts reverse-geocoded from the dropped pin; blank when unknown. */
data class PickedAddress(
    val street: String,
    val city: String,
    val state: String,
    val zip: String,
    val country: String,
)

private val geoJson = Json { ignoreUnknownKeys = true }

/**
 * OpenStreetMap picker: drag to pan, wheel/buttons to zoom, tap to drop the pin.
 * [onAddressResolved] gets the pin's address so the form can fill empty fields.
 */
@Composable
fun MapLocationPicker(
    lat: Double,
    lng: Double,
    onLatChange: (Double) -> Unit,
    onLngChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    onAddressResolved: (PickedAddress) -> Unit = {},
) {
    val client = remember { createPlatformHttpClient() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val tilePx = TILE_SIZE * density
    val tiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val loading = remember { mutableSetOf<String>() }
    var zoom by remember { mutableStateOf(INITIAL_ZOOM) }
    var centerX by remember { mutableStateOf(lngToX(lng, INITIAL_ZOOM, tilePx)) }
    var centerY by remember { mutableStateOf(latToY(lat, INITIAL_ZOOM, tilePx)) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }
    var latText by remember { mutableStateOf(lat.toString()) }
    var lngText by remember { mutableStateOf(lng.toString()) }
    val lastWheelZoom = remember { mutableStateOf(TimeSource.Monotonic.markNow()) }

    fun centerOn(newLat: Double, newLng: Double) {
        centerX = lngToX(newLng, zoom, tilePx)
        centerY = latToY(newLat, zoom, tilePx)
    }

    fun setPin(newLat: Double, newLng: Double, resolveAddress: Boolean) {
        val roundedLat = (newLat * COORD_DECIMALS).roundToInt() / COORD_DECIMALS
        val roundedLng = (newLng * COORD_DECIMALS).roundToInt() / COORD_DECIMALS
        onLatChange(roundedLat)
        onLngChange(roundedLng)
        if (resolveAddress) {
            scope.launch {
                runCatching { reverseGeocode(client, roundedLat, roundedLng) }.getOrNull()?.let(onAddressResolved)
            }
        }
    }

    // Zooms around a screen point so the spot under the cursor stays put.
    fun zoomBy(step: Int, anchorX: Float = viewSize.width / 2f, anchorY: Float = viewSize.height / 2f) {
        val target = (zoom + step).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (target == zoom) return
        val dx = anchorX - viewSize.width / 2f
        val dy = anchorY - viewSize.height / 2f
        val factor = if (target > zoom) 2.0 else 0.5
        centerX = (centerX + dx) * factor - dx
        centerY = (centerY + dy) * factor - dy
        zoom = target
    }

    LaunchedEffect(lat, lng) {
        latText = lat.toString()
        lngText = lng.toString()
        val px = lngToX(lng, zoom, tilePx) - centerX
        val py = latToY(lat, zoom, tilePx) - centerY
        val offScreen = kotlin.math.abs(px) > viewSize.width / 2.0 || kotlin.math.abs(py) > viewSize.height / 2.0
        if (offScreen) centerOn(lat, lng)
    }

    val worldTiles = 1 shl zoom
    val visibleKeys = if (viewSize == IntSize.Zero) emptyList() else {
        val left = centerX - viewSize.width / 2.0
        val top = centerY - viewSize.height / 2.0
        val x0 = floor(left / tilePx).toInt()
        val x1 = floor((left + viewSize.width) / tilePx).toInt()
        val y0 = floor(top / tilePx).toInt().coerceAtLeast(0)
        val y1 = floor((top + viewSize.height) / tilePx).toInt().coerceAtMost(worldTiles - 1)
        buildList {
            for (ty in y0..y1) for (tx in x0..x1) add(TileRef(zoom, tx, ty, ((tx % worldTiles) + worldTiles) % worldTiles))
        }
    }

    LaunchedEffect(visibleKeys.map { it.key }) {
        visibleKeys.filter { it.key !in tiles && it.key !in loading }.forEach { tile ->
            loading += tile.key
            launch {
                try {
                    runCatching {
                        client.get("$TILE_URL/${tile.z}/${tile.wrappedX}/${tile.y}.png").readRawBytes().decodeToImageBitmap()
                    }.getOrNull()?.let { tiles[tile.key] = it }
                } finally {
                    loading -= tile.key
                }
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_map_location), fontWeight = FontWeight.SemiBold)
        val notFound = stringResource(Res.string.master_map_not_found)
        fun runSearch() {
            val q = query.trim()
            if (q.isEmpty() || searching) return
            searching = true
            searchError = false
            scope.launch {
                val hit = runCatching { forwardGeocode(client, q) }.getOrNull()
                searching = false
                if (hit == null) {
                    searchError = true
                } else {
                    zoom = SEARCH_ZOOM
                    centerOn(hit.first, hit.second)
                    setPin(hit.first, hit.second, resolveAddress = true)
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                searchError = false
            },
            placeholder = { Text(stringResource(Res.string.master_map_search_hint), style = MaterialTheme.typography.bodySmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = searchError,
            supportingText = if (searchError) ({ Text(notFound) }) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
            trailingIcon = {
                if (searching) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { runSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.master_select_on_map))
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodySmall,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .onSizeChanged { viewSize = it }
                .pointerInput(tilePx) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        centerX -= drag.x
                        centerY = (centerY - drag.y).coerceIn(0.0, (1 shl zoom) * tilePx.toDouble())
                    }
                }
                .pointerInput(tilePx) {
                    detectTapGestures { pos ->
                        val wx = centerX - viewSize.width / 2.0 + pos.x
                        val wy = centerY - viewSize.height / 2.0 + pos.y
                        setPin(yToLat(wy, zoom, tilePx), xToLng(wx, zoom, tilePx), resolveAddress = true)
                    }
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type != PointerEventType.Scroll) continue
                            val change = event.changes.firstOrNull() ?: continue
                            val dy = change.scrollDelta.y
                            if (dy == 0f) continue
                            event.changes.forEach { it.consume() }
                            if (lastWheelZoom.value.elapsedNow().inWholeMilliseconds < WHEEL_ZOOM_COOLDOWN_MS) continue
                            lastWheelZoom.value = TimeSource.Monotonic.markNow()
                            zoomBy(if (dy < 0) 1 else -1, change.position.x, change.position.y)
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val left = centerX - size.width / 2.0
                val top = centerY - size.height / 2.0
                val side = tilePx.roundToInt()
                visibleKeys.forEach { tile ->
                    val image = tiles[tile.key] ?: return@forEach
                    drawImage(
                        image = image,
                        dstOffset = IntOffset((tile.x * tilePx - left).roundToInt(), (tile.y * tilePx - top).roundToInt()),
                        dstSize = IntSize(side, side),
                    )
                }
            }
            val pinSize = 36.dp
            val pinPx = pinSize.value * density
            val pinX = lngToX(lng, zoom, tilePx) - centerX + viewSize.width / 2.0
            val pinY = latToY(lat, zoom, tilePx) - centerY + viewSize.height / 2.0
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .offset { IntOffset((pinX - pinPx / 2).roundToInt(), (pinY - pinPx).roundToInt()) }
                    .size(pinSize),
            )
            Column(
                Modifier.align(Alignment.TopEnd).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MapButton(Icons.Default.Add, stringResource(Res.string.master_map_zoom_in)) { zoomBy(1) }
                MapButton(Icons.Default.Remove, stringResource(Res.string.master_map_zoom_out)) { zoomBy(-1) }
                MapButton(Icons.Default.MyLocation, stringResource(Res.string.master_map_recenter)) { centerOn(lat, lng) }
            }
            Text(
                stringResource(Res.string.master_map_attribution),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        Text(
            stringResource(Res.string.master_map_tap_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = {
                    latText = it
                    it.toDoubleOrNull()?.takeIf { v -> v in -90.0..90.0 }?.let(onLatChange)
                },
                label = { Text(stringResource(Res.string.master_latitude)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = {
                    lngText = it
                    it.toDoubleOrNull()?.takeIf { v -> v in -180.0..180.0 }?.let(onLngChange)
                },
                label = { Text(stringResource(Res.string.master_longitude)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun MapButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(20.dp))
        }
    }
}

private data class TileRef(val z: Int, val x: Int, val y: Int, val wrappedX: Int) {
    val key = "$z/$wrappedX/$y"
}

private fun worldPx(zoom: Int, tilePx: Float): Double = (1 shl zoom) * tilePx.toDouble()

private fun lngToX(lng: Double, zoom: Int, tilePx: Float): Double = (lng + 180.0) / 360.0 * worldPx(zoom, tilePx)

private fun latToY(lat: Double, zoom: Int, tilePx: Float): Double {
    val rad = lat.coerceIn(-MAX_LAT, MAX_LAT) * PI / 180.0
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * worldPx(zoom, tilePx)
}

private fun xToLng(x: Double, zoom: Int, tilePx: Float): Double {
    val lng = x / worldPx(zoom, tilePx) * 360.0 - 180.0
    return ((lng + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
}

private fun yToLat(y: Double, zoom: Int, tilePx: Float): Double =
    atan(sinh(PI * (1.0 - 2.0 * y / worldPx(zoom, tilePx)))) * 180.0 / PI

private suspend fun forwardGeocode(client: io.ktor.client.HttpClient, query: String): Pair<Double, Double>? {
    val body = client.get("$GEOCODER_URL/search") {
        parameter("format", "json")
        parameter("limit", 1)
        parameter("countrycodes", GEOCODER_COUNTRY)
        parameter("accept-language", GEOCODER_LANGUAGE)
        parameter("q", query)
    }.bodyAsText()
    val first = (geoJson.parseToJsonElement(body) as? JsonArray)?.firstOrNull()?.jsonObject ?: return null
    val lat = first["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
    val lng = first["lon"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
    return lat to lng
}

private suspend fun reverseGeocode(client: io.ktor.client.HttpClient, lat: Double, lng: Double): PickedAddress? {
    val body = client.get("$GEOCODER_URL/reverse") {
        parameter("format", "json")
        parameter("accept-language", GEOCODER_LANGUAGE)
        parameter("lat", lat)
        parameter("lon", lng)
    }.bodyAsText()
    val address = (geoJson.parseToJsonElement(body) as? JsonObject)?.get("address") as? JsonObject ?: return null
    fun part(vararg keys: String) = keys.firstNotNullOfOrNull { k -> address[k]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } }.orEmpty()
    val street = listOf(part("house_number"), part("road", "pedestrian"), part("neighbourhood", "suburb"))
        .filter { it.isNotBlank() }
        .joinToString(", ")
    return PickedAddress(
        street = street,
        city = part("city", "town", "village", "county", "state_district"),
        state = part("state"),
        zip = part("postcode"),
        country = part("country"),
    )
}
