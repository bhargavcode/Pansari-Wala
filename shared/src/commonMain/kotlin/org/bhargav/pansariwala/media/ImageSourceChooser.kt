package org.bhargav.pansariwala.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.image_source_camera
import pansariwala.shared.generated.resources.image_source_camera_hint
import pansariwala.shared.generated.resources.image_source_gallery
import pansariwala.shared.generated.resources.image_source_gallery_hint
import pansariwala.shared.generated.resources.image_source_title

/**
 * Returns a click handler for an image field. Uses [ImageUploadFeature.sources]:
 * a single source launches immediately, multiple sources open a camera/gallery sheet.
 */
@Composable
fun rememberImageSourceLauncher(
    feature: ImageUploadFeature,
    onSource: (ImageSource) -> Unit,
): () -> Unit {
    val currentOnSource by rememberUpdatedState(onSource)
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet) {
        ImageSourceSheet(
            sources = feature.sources,
            onDismiss = { showSheet = false },
            onSelect = { source ->
                showSheet = false
                currentOnSource(source)
            },
        )
    }
    return remember(feature) {
        {
            val single = feature.sources.singleOrNull()
            if (single != null) currentOnSource(single) else showSheet = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageSourceSheet(
    sources: List<ImageSource>,
    onDismiss: () -> Unit,
    onSelect: (ImageSource) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(Res.string.image_source_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            sources.forEach { source ->
                val (title, hint, icon) = when (source) {
                    ImageSource.CAMERA -> Triple(
                        Res.string.image_source_camera,
                        Res.string.image_source_camera_hint,
                        Icons.Default.PhotoCamera,
                    )
                    ImageSource.GALLERY -> Triple(
                        Res.string.image_source_gallery,
                        Res.string.image_source_gallery_hint,
                        Icons.Default.PhotoLibrary,
                    )
                }
                ListItem(
                    headlineContent = { Text(stringResource(title)) },
                    supportingContent = { Text(stringResource(hint)) },
                    leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(source) }
                    },
                )
            }
        }
    }
}
