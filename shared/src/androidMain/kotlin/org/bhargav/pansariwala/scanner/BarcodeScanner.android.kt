package org.bhargav.pansariwala.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import org.koin.core.context.GlobalContext

private val ANALYSIS_SIZE = Size(640, 480)
private const val MIN_FRAME_INTERVAL_MS = 120L
private const val BEEP_DURATION_MS = 80
private const val VIBRATE_DURATION_MS = 40L

actual val barcodeScannerSupported: Boolean = true

@Composable
actual fun BarcodeScannerPreview(
    settings: ScannerSettings,
    active: Boolean,
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnBarcode by rememberUpdatedState(onBarcode)
    val latestOnDenied by rememberUpdatedState(onPermissionDenied)
    var granted by remember { mutableStateOf(context.hasCameraPermission()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) latestOnDenied()
    }
    LaunchedEffect(active, granted) {
        if (active && !granted) launcher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember {
        PreviewView(context).apply {
            // TextureView-backed so the preview can be clipped to the round toolbar avatar.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)

    if (!active || !granted) return

    DisposableEffect(lifecycleOwner, settings.lens, settings.retailFormatsOnly, settings.repeatDelayMs) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(scannerOptions(settings.retailFormatsOnly))
        val analyzer = ThrottledBarcodeAnalyzer(scanner, settings.repeatDelayMs) { code ->
            ContextCompat.getMainExecutor(context).execute { latestOnBarcode(code) }
        }
        var provider: ProcessCameraProvider? = null
        var camera: Camera? = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = runCatching { future.get() }.getOrNull() ?: return@addListener
            provider = cameraProvider
            val resolution = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                )
                .build()
            val preview = Preview.Builder().setResolutionSelector(resolution).build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(resolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, analyzer) }
            val selector = cameraSelector(cameraProvider, settings.lens)
            camera = runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }.getOrNull()
            camera?.cameraControl?.enableTorch(settings.torch && camera?.cameraInfo?.hasFlashUnit() == true)
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            runCatching { provider?.unbindAll() }
            camera = null
            scanner.close()
            executor.shutdown()
        }
    }
}

private fun cameraSelector(provider: ProcessCameraProvider, lens: ScannerLens): CameraSelector {
    val wanted = if (lens == ScannerLens.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    val fallback = if (lens == ScannerLens.FRONT) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
    return if (runCatching { provider.hasCamera(wanted) }.getOrDefault(false)) wanted else fallback
}

private fun scannerOptions(retailOnly: Boolean): BarcodeScannerOptions {
    val builder = BarcodeScannerOptions.Builder()
    if (retailOnly) {
        builder.setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
        )
    } else {
        builder.setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
    }
    return builder.build()
}

/** Decodes at most one frame at a time, no faster than [MIN_FRAME_INTERVAL_MS], and suppresses repeats. */
private class ThrottledBarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val repeatDelayMs: Long,
    private val onCode: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    private val busy = AtomicBoolean(false)
    private var lastFrameAt = 0L
    private var lastCode: String? = null
    private var lastCodeAt = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        val media = image.image
        if (media == null || now - lastFrameAt < MIN_FRAME_INTERVAL_MS || !busy.compareAndSet(false, true)) {
            image.close()
            return
        }
        lastFrameAt = now
        scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes ->
                val value = codes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }
                    ?: return@addOnSuccessListener
                val at = SystemClock.elapsedRealtime()
                if (value == lastCode && at - lastCodeAt < repeatDelayMs) return@addOnSuccessListener
                lastCode = value
                lastCodeAt = at
                onCode(value)
            }
            .addOnCompleteListener {
                image.close()
                busy.set(false)
            }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private val toneGenerator: ToneGenerator? by lazy {
    runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME / 2) }.getOrNull()
}

actual fun scanFeedback(beep: Boolean, vibrate: Boolean) {
    if (beep) toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)
    if (!vibrate) return
    val context = runCatching { GlobalContext.get().get<Context>() }.getOrNull() ?: return
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    runCatching {
        vibrator?.vibrate(VibrationEffect.createOneShot(VIBRATE_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
