package org.bhargav.pansariwala

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.bhargav.pansariwala.navigation.AppNavGraph
import org.bhargav.pansariwala.theme.PansariTheme

@Composable
fun App() {
    PansariTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavGraph()
        }
    }
}
