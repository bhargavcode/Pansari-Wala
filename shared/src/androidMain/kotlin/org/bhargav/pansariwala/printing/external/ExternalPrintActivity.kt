package org.bhargav.pansariwala.printing.external

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Invisible share target: "Share → Print receipt" from any app prints text, PDFs or images. */
class ExternalPrintActivity : ComponentActivity(), KoinComponent {
    private val gateway: ExternalPrintGateway by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }
        val request = intent
        lifecycleScope.launch {
            gateway.printShared(request)
            finish()
        }
    }
}
