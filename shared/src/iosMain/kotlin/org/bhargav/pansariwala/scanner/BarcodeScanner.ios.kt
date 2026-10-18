package org.bhargav.pansariwala.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlin.time.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPreset640x480
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.defaultDeviceWithDeviceType
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.torchMode
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIView
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue

private const val SCAN_BEEP_SOUND_ID: UInt = 1057u

actual val barcodeScannerSupported: Boolean = true

@OptIn(ExperimentalForeignApi::class)
private class ScannerPreviewView(val previewLayer: AVCaptureVideoPreviewLayer) : UIView(frame = CGRectZero.readValue()) {
    init {
        clipsToBounds = true
        layer.addSublayer(previewLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }
}

private class MetadataDelegate(
    private val repeatDelayMs: Long,
    private val onCode: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
    private var lastCode: String? = null
    private var lastMark = TimeSource.Monotonic.markNow()

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val value = didOutputMetadataObjects
            .firstNotNullOfOrNull { (it as? AVMetadataMachineReadableCodeObject)?.stringValue?.trim()?.takeIf(String::isNotEmpty) }
            ?: return
        if (value == lastCode && lastMark.elapsedNow().inWholeMilliseconds < repeatDelayMs) return
        lastCode = value
        lastMark = TimeSource.Monotonic.markNow()
        onCode(value)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun BarcodeScannerPreview(
    settings: ScannerSettings,
    active: Boolean,
    onBarcode: (String) -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier,
) {
    val latestOnBarcode by rememberUpdatedState(onBarcode)
    val latestOnDenied by rememberUpdatedState(onPermissionDenied)
    var granted by remember {
        mutableStateOf(AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized)
    }
    LaunchedEffect(active) {
        if (!active || granted) return@LaunchedEffect
        if (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { ok ->
                dispatch_async(dispatch_get_main_queue()) {
                    granted = ok
                    if (!ok) latestOnDenied()
                }
            }
        } else {
            latestOnDenied()
        }
    }

    val session = remember { AVCaptureSession().apply { sessionPreset = AVCaptureSessionPreset640x480 } }
    val previewLayer = remember {
        AVCaptureVideoPreviewLayer(session = session).apply { videoGravity = AVLayerVideoGravityResizeAspectFill }
    }
    UIKitView(factory = { ScannerPreviewView(previewLayer) }, modifier = modifier)

    if (!active || !granted) return

    DisposableEffect(settings.lens, settings.retailFormatsOnly, settings.repeatDelayMs, settings.torch) {
        val delegate = MetadataDelegate(settings.repeatDelayMs) { latestOnBarcode(it) }
        val position = if (settings.lens == ScannerLens.FRONT) AVCaptureDevicePositionFront else AVCaptureDevicePositionBack
        val device = AVCaptureDevice.defaultDeviceWithDeviceType(AVCaptureDeviceTypeBuiltInWideAngleCamera, AVMediaTypeVideo, position)
            ?: AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
        val output = AVCaptureMetadataOutput()
        session.beginConfiguration()
        session.inputs.toList().forEach { session.removeInput(it as platform.AVFoundation.AVCaptureInput) }
        session.outputs.toList().forEach { session.removeOutput(it as AVCaptureOutput) }
        val input = device?.let { AVCaptureDeviceInput.deviceInputWithDevice(it, null) }
        if (input != null && session.canAddInput(input)) session.addInput(input)
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())
            val wanted = metadataTypes(settings.retailFormatsOnly)
            output.metadataObjectTypes = output.availableMetadataObjectTypes.filter { it in wanted }
        }
        session.commitConfiguration()
        dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
            session.startRunning()
            device?.applyTorch(settings.torch)
        }
        onDispose {
            output.setMetadataObjectsDelegate(null, null)
            dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)) {
                device?.applyTorch(false)
                session.stopRunning()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun AVCaptureDevice.applyTorch(on: Boolean) {
    if (!hasTorch) return
    if (lockForConfiguration(null)) {
        torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
        unlockForConfiguration()
    }
}

private fun metadataTypes(retailOnly: Boolean): Set<Any?> {
    val retail = setOf(
        AVMetadataObjectTypeEAN13Code,
        AVMetadataObjectTypeEAN8Code,
        AVMetadataObjectTypeUPCECode,
        AVMetadataObjectTypeCode128Code,
    )
    if (retailOnly) return retail
    return retail + setOf(
        AVMetadataObjectTypeQRCode,
        AVMetadataObjectTypeCode39Code,
        AVMetadataObjectTypeCode93Code,
        AVMetadataObjectTypeITF14Code,
        AVMetadataObjectTypeDataMatrixCode,
        AVMetadataObjectTypePDF417Code,
        AVMetadataObjectTypeAztecCode,
    )
}

actual fun scanFeedback(beep: Boolean, vibrate: Boolean) {
    if (beep) AudioServicesPlaySystemSound(SCAN_BEEP_SOUND_ID)
    if (vibrate) UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleLight).impactOccurred()
}
