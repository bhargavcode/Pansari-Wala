package org.bhargav.pansariwala

import android.os.Bundle
import org.bhargav.pansariwala.platform.AndroidActivityHolder
import org.bhargav.pansariwala.platform.RazorpayCheckoutActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : RazorpayCheckoutActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidActivityHolder.activity = this
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        if (AndroidActivityHolder.activity === this) {
            AndroidActivityHolder.activity = null
        }
        super.onDestroy()
    }
}
