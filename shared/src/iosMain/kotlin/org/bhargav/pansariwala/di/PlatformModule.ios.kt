package org.bhargav.pansariwala.di

import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.FallbackLocation
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.IosFirebasePhoneAuth
import org.bhargav.pansariwala.platform.IosImagePicker
import org.bhargav.pansariwala.platform.IosRazorpayCheckout
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DeviceLocation> { FallbackLocation() }
    single<PhoneAuthGateway> { IosFirebasePhoneAuth() }
    single<ImagePicker> { IosImagePicker() }
    single<RazorpayCheckout> { IosRazorpayCheckout() }
}
