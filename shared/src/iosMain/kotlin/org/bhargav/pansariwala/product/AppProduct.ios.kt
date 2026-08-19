package org.bhargav.pansariwala.product

import org.bhargav.pansariwala.util.AppConstants
import platform.Foundation.NSBundle

actual fun currentAppProduct(): AppProduct {
    val raw = NSBundle.mainBundle.objectForInfoDictionaryKey("PansariProduct") as? String
    return AppProduct.fromName(raw).also { AppProductHolder.current = it }
}
