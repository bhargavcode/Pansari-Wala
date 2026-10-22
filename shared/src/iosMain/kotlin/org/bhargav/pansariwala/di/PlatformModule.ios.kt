package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.IosDeviceLocation
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.IosImagePicker
import org.bhargav.pansariwala.platform.IosOtpAppVerifier
import org.bhargav.pansariwala.platform.OtpAppVerifier
import org.bhargav.pansariwala.platform.IosRazorpayCheckout
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.platform.ServerPhoneAuthGateway
import org.bhargav.pansariwala.printer.IosPrinterTransports
import org.bhargav.pansariwala.printer.PrinterManager
import org.bhargav.pansariwala.printer.encoder.EscPosEncoder
import org.bhargav.pansariwala.printer.text.IosTextRasterizer
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DeviceLocation> { IosDeviceLocation() }
    single<OtpAppVerifier> { IosOtpAppVerifier() }
    single<PhoneAuthGateway> { ServerPhoneAuthGateway(get(), get()) }
    single<ImagePicker> { IosImagePicker() }
    single<org.bhargav.pansariwala.platform.PhoneAutofill> { org.bhargav.pansariwala.platform.NoPhoneAutofill() }
    single<org.bhargav.pansariwala.platform.DocumentPicker> { org.bhargav.pansariwala.platform.IosDocumentPicker() }
    single<RazorpayCheckout> { IosRazorpayCheckout() }
    single {
        PrinterManager(
            transports = IosPrinterTransports.create(),
            store = get(),
            encoder = EscPosEncoder(textRasterizer = IosTextRasterizer()),
        )
    }
}
