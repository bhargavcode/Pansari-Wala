package org.bhargav.pansariwala.platform

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

object AndroidActivityHolder {
    @Volatile
    private var ref: WeakReference<ComponentActivity>? = null

    var activity: ComponentActivity?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}
