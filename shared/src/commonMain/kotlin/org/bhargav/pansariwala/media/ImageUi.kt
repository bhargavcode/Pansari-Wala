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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.delay
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
    var bitmap by remember(url, localPreviewBytes?.size) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url, localPreviewBytes?.size) {
        bitmap = null
        localPreviewBytes?.takeIf { it.isNotEmpty() }?.let {
            bitmap = runCatching { it.decodeToImageBitmap() }.getOrNull()
            if (bitmap != null) return@LaunchedEffect
        }
        val target = url?.trim()?.takeIf { isHttpImageUrl(it) } ?: return@LaunchedEffect
        bitmap = runCatching {
            createPlatformHttpClient().get(target).readRawBytes().decodeToImageBitmap()
        }.getOrNull()
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
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
    onAdd: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onClear: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    onAdd = { onAdd(index) },
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
            is ImageSlotState.Compressing, is ImageSlotState.Uploading -> CircularProgressIndicator(
                Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
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
