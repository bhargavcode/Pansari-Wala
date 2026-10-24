package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.printer.transport.browser.browserPrinterTransports
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.WasmJsDeviceLocation
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.NoOpRazorpayCheckout
import org.bhargav.pansariwala.platform.OtpAppVerifier
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.WebOtpAppVerifier
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.platform.ServerPhoneAuthGateway
import org.bhargav.pansariwala.platform.WasmJsImagePicker
import org.bhargav.pansariwala.printer.PrinterManager
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DeviceLocation> { WasmJsDeviceLocation() }
    single<OtpAppVerifier> { WebOtpAppVerifier() }
    single<PhoneAuthGateway> { ServerPhoneAuthGateway(get(), get()) }
    single<ImagePicker> { WasmJsImagePicker() }
    single<org.bhargav.pansariwala.platform.PhoneAutofill> { org.bhargav.pansariwala.platform.NoPhoneAutofill() }
    single<org.bhargav.pansariwala.platform.DocumentPicker> { org.bhargav.pansariwala.platform.WasmJsDocumentPicker() }
    single<RazorpayCheckout> { NoOpRazorpayCheckout() }
    single { PrinterManager(browserPrinterTransports(), get()) }
}
