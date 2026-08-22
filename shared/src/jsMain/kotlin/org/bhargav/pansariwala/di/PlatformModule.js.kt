package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.JsDeviceLocation
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.NoOpRazorpayCheckout
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.platform.ServerPhoneAuthGateway
import org.bhargav.pansariwala.platform.UnavailableImagePicker
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DeviceLocation> { JsDeviceLocation() }
    single<PhoneAuthGateway> { ServerPhoneAuthGateway(get()) }
    single<ImagePicker> { UnavailableImagePicker() }
    single<RazorpayCheckout> { NoOpRazorpayCheckout() }
}
