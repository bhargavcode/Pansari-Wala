package org.bhargav.pansariwala.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.util.AppConstants

/**
 * Lifecycle-aware image upload feature.
 *
 * Bind to a [CoroutineScope] that dies with the host (typically `viewModelScope`).
 * Config-change survives via ViewModel; process death cancels in-flight uploads.
 * Failures keep compressed bytes for explicit [retry]; one automatic retry on transient errors,
 * then surface Failed so UI can show "Upload failed. Try again."
 */
class LifecycleImageUploader(
    private val scope: CoroutineScope,
    private val picker: ImagePicker,
    private val upload: suspend (prefix: String, compressed: CompressedImage) -> UploadedImage,
    private val slotCount: Int = 1,
) {
    private val _slots = MutableStateFlow(List(slotCount) { ImageSlotState.Empty as ImageSlotState })
    val slots: StateFlow<List<ImageSlotState>> = _slots.asStateFlow()

    private val jobs = mutableMapOf<Int, Job>()

    fun seedUrls(urls: List<String>) {
        _slots.value = List(slotCount) { index ->
            val url = urls.getOrNull(index)?.takeIf { it.isNotBlank() }
            if (url != null) ImageSlotState.Ready(url = url) else ImageSlotState.Empty
        }
    }

    fun readyUrls(): List<String> =
        _slots.value.mapNotNull { (it as? ImageSlotState.Ready)?.url }

    fun localBytes(slot: Int): ByteArray? =
        when (val state = _slots.value.getOrNull(slot)) {
            is ImageSlotState.Ready -> state.localPreviewBytes
            is ImageSlotState.Uploading -> state.previewBytes
            is ImageSlotState.Failed -> state.compressed.previewBytes
            else -> null
        }

    fun hasInFlight(): Boolean =
        _slots.value.any { it is ImageSlotState.Compressing || it is ImageSlotState.Uploading }

    fun hasFailures(): Boolean = _slots.value.any { it is ImageSlotState.Failed }

    fun pickAndUpload(slot: Int, prefix: String, source: ImageSource = ImageSource.GALLERY) {
        require(slot in 0 until slotCount)
        jobs[slot]?.cancel()
        jobs[slot] = scope.launch {
            val picked = runCatching { picker.pickImage(source) }.getOrNull()
            if (picked == null) return@launch
            _slots.updateSlot(slot) { ImageSlotState.Compressing(picked.displayName) }
            val compressed = runCatching { prepareImageForUpload(picked) }.getOrElse { err ->
                _slots.updateSlot(slot) {
                    ImageSlotState.Failed(
                        message = err.message ?: "Compression failed",
                        compressed = CompressedImage(picked.displayName, picked.bytes, picked.mimeType, picked.bytes.size),
                        prefix = prefix,
                    )
                }
                return@launch
            }
            uploadWithRetry(slot, prefix, compressed)
        }
    }

    fun retry(slot: Int) {
        val failed = _slots.value.getOrNull(slot) as? ImageSlotState.Failed ?: return
        jobs[slot]?.cancel()
        jobs[slot] = scope.launch {
            uploadWithRetry(slot, failed.prefix, failed.compressed)
        }
    }

    fun clear(slot: Int) {
        jobs[slot]?.cancel()
        _slots.updateSlot(slot) { ImageSlotState.Empty }
    }

    fun clearAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _slots.value = List(slotCount) { ImageSlotState.Empty }
    }

    private suspend fun uploadWithRetry(slot: Int, prefix: String, compressed: CompressedImage) {
        _slots.updateSlot(slot) {
            ImageSlotState.Uploading(compressed.displayName, compressed.previewBytes)
        }
        var lastError: Throwable? = null
        var attempt = 0
        val maxAttempts = AppConstants.UPLOAD_AUTO_RETRY_COUNT + 1
        while (attempt < maxAttempts) {
            val result = runCatching { upload(ensureUploadPrefix(prefix), compressed) }
            result.onSuccess { uploaded ->
                _slots.updateSlot(slot) {
                    ImageSlotState.Ready(
                        url = uploaded.url,
                        thumbnailUrl = uploaded.thumbnailUrl,
                        localPreviewBytes = compressed.previewBytes,
                    )
                }
                return
            }
            lastError = result.exceptionOrNull()
            attempt++
        }
        _slots.updateSlot(slot) {
            ImageSlotState.Failed(
                message = lastError?.message?.takeIf { it.isNotBlank() }
                    ?: "Upload failed. Try again.",
                compressed = compressed,
                prefix = prefix,
            )
        }
    }

    private fun MutableStateFlow<List<ImageSlotState>>.updateSlot(
        slot: Int,
        transform: (ImageSlotState) -> ImageSlotState,
    ) {
        update { list ->
            list.toMutableList().also { it[slot] = transform(it[slot]) }
        }
    }
}
