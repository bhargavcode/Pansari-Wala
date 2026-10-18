package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.JsDeviceLocation
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.NoOpRazorpayCheckout
import org.bhargav.pansariwala.platform.OtpAppVerifier
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.WebOtpAppVerifier
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.platform.ServerPhoneAuthGateway
import org.bhargav.pansariwala.platform.JsImagePicker
import org.bhargav.pansariwala.printer.PrinterManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DeviceLocation> { JsDeviceLocation() }
    single<OtpAppVerifier> { WebOtpAppVerifier() }
    single<PhoneAuthGateway> { ServerPhoneAuthGateway(get(), get()) }
    single<ImagePicker> { JsImagePicker() }
    single<org.bhargav.pansariwala.platform.DocumentPicker> { org.bhargav.pansariwala.platform.JsDocumentPicker() }
    single<RazorpayCheckout> { NoOpRazorpayCheckout() }
    single { PrinterManager(emptyList(), get()) }
}
