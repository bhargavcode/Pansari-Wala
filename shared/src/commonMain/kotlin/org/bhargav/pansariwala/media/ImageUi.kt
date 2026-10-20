package org.bhargav.pansariwala.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_retry
import pansariwala.shared.generated.resources.action_remove_photo
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.product_images_hint

@Composable
fun NetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    localPreviewBytes: ByteArray? = null,
) {
    val target = url?.trim()?.takeIf { isHttpImageUrl(it) }
    var bitmap by remember(target, localPreviewBytes) {
        mutableStateOf(target?.let { ImageBitmapCache.get(it) })
    }
    var loading by remember(target, localPreviewBytes) { mutableStateOf(false) }
    LaunchedEffect(target, localPreviewBytes) {
        if (bitmap != null) return@LaunchedEffect
        localPreviewBytes?.takeIf { it.isNotEmpty() }?.let {
            bitmap = decodeImageOffMain(it)
            if (bitmap != null) return@LaunchedEffect
        }
        if (target == null) return@LaunchedEffect
        loading = true
        bitmap = ImageBitmapCache.load(target)
        loading = false
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        when {
            bmp != null -> Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            else -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

suspend fun decodeImageOffMain(bytes: ByteArray): ImageBitmap? =
    withContext(Dispatchers.Default) {
        // Android wraps a null Bitmap for undecodable bytes; touching width surfaces that as a failure.
        runCatching { bytes.decodeToImageBitmap().takeIf { it.width > 0 && it.height > 0 } }.getOrNull()
    }

/**
 * Main-thread-only image cache: size-bounded LRU of decoded bitmaps, then the platform disk cache,
 * then the network. Concurrent requests for one URL share a single download, and loads outlive the
 * requesting item so scrolling/refreshing/filtering a list never restarts them.
 */
private object ImageBitmapCache {
    private val client by lazy { createPlatformHttpClient() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val entries = LinkedHashMap<String, ImageBitmap>()
    private val inFlight = HashMap<String, Deferred<ImageBitmap?>>()
    private var totalBytes = 0L

    fun get(url: String): ImageBitmap? = entries.remove(url)?.also { entries[url] = it }

    suspend fun load(url: String): ImageBitmap? {
        get(url)?.let { return it }
        val job = inFlight.getOrPut(url) {
            scope.async {
                try {
                    fetch(url)?.also { put(url, it) }
                } finally {
                    inFlight.remove(url)
                }
            }
        }
        return runCatching { job.await() }.getOrNull()
    }

    private suspend fun fetch(url: String): ImageBitmap? {
        val key = imageCacheKey(url)
        readImageDiskCache(key)?.let { cached -> decodeImageOffMain(cached)?.let { return it } }
        return runCatching {
            val response = client.get(imageFetchUrl(url))
            if (!response.status.isSuccess()) return@runCatching null
            val bytes = response.readRawBytes()
            decodeImageOffMain(bytes)?.also { writeImageDiskCache(key, bytes) }
        }.getOrNull()
    }

    private fun put(url: String, bitmap: ImageBitmap) {
        entries.remove(url)?.let { totalBytes -= it.byteSize() }
        entries[url] = bitmap
        totalBytes += bitmap.byteSize()
        while (totalBytes > AppConstants.IMAGE_MEMORY_CACHE_BYTES && entries.size > 1) {
            val eldest = entries.keys.first()
            entries.remove(eldest)?.let { totalBytes -= it.byteSize() }
        }
    }

    private fun ImageBitmap.byteSize(): Long = width.toLong() * height * BYTES_PER_PIXEL

    private const val BYTES_PER_PIXEL = 4
}

/** Translucent spinner overlay drawn over a local preview while it uploads. */
@Composable
fun UploadingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier.background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
    }
}

@Composable
fun ProductImageCarousel(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 96.dp,
) {
    val urls = imageUrls.filter { isHttpImageUrl(it) }
    if (urls.isEmpty()) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    var index by remember(urls) { mutableIntStateOf(0) }
    LaunchedEffect(urls) {
        if (urls.size <= 1) return@LaunchedEffect
        while (true) {
            delay(AppConstants.IMAGE_CAROUSEL_INTERVAL_MS)
            index = (index + 1) % urls.size
        }
    }
    NetworkImage(
        url = urls[index.coerceIn(0, urls.lastIndex)],
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
fun ProductImageSlots(
    slots: List<ImageSlotState>,
    feature: ImageUploadFeature,
    onAdd: (Int, ImageSource) -> Unit,
    onRetry: (Int) -> Unit,
    onClear: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingSlot by remember { mutableIntStateOf(0) }
    val launchPicker = rememberImageSourceLauncher(feature) { source -> onAdd(pendingSlot, source) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.product_images_hint, AppConstants.PHOTO_MAX_PRODUCT_IMAGES),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            slots.forEachIndexed { index, state ->
                ImageSlotTile(
                    state = state,
                    onAdd = {
                        pendingSlot = index
                        launchPicker()
                    },
                    onRetry = { onRetry(index) },
                    onClear = { onClear(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (slots.any { it is ImageSlotState.Failed }) {
            Text(
                stringResource(Res.string.error_image_upload_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ImageSlotTile(
    state: ImageSlotState,
    onAdd: () -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(88.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = state is ImageSlotState.Empty || state is ImageSlotState.Failed) {
                when (state) {
                    is ImageSlotState.Empty -> onAdd()
                    is ImageSlotState.Failed -> onRetry()
                    else -> Unit
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ImageSlotState.Empty -> Icon(
                Icons.Default.AddAPhoto,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            is ImageSlotState.Compressing -> CircularProgressIndicator(
                Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
            is ImageSlotState.Uploading -> {
                NetworkImage(
                    url = null,
                    localPreviewBytes = state.previewBytes,
                    modifier = Modifier.fillMaxSize(),
                )
                UploadingOverlay(Modifier.fillMaxSize())
            }
            is ImageSlotState.Ready -> {
                NetworkImage(
                    url = state.thumbnailUrl.ifBlank { state.url },
                    localPreviewBytes = state.localPreviewBytes,
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_remove_photo))
                }
            }
            is ImageSlotState.Failed -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }
    }
}
