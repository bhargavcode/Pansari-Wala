package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.platform.AndroidPhoneAuthGateway
import org.bhargav.pansariwala.platform.AndroidDeviceLocation
import org.bhargav.pansariwala.platform.AndroidImagePicker
import org.bhargav.pansariwala.platform.AndroidRazorpayCheckout
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { AndroidDeviceLocation(androidContext()) }
    single<DeviceLocation> { get<AndroidDeviceLocation>() }
    single<PhoneAuthGateway> { AndroidPhoneAuthGateway(get()) }
    single<ImagePicker> { AndroidImagePicker() }
    single<RazorpayCheckout> { AndroidRazorpayCheckout() }
}
