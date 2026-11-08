package com.developersancho.formatico

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.developersancho.formatico.core.designsystem.FormaticoTypefaces
import com.pantrixui.foundation.token.typography.PantrixTypefaceGate
import com.pantrixui.foundation.token.typography.areTypefacesLoaded
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Before the first resource, the typefaces, is read (WasmMemory.wasmJs.kt).
    shareWasmMemoryWithResources()
    ComposeViewport(viewportContainerId = "app") {
        // The app is drawn once Geist and Geist Mono have come; the page's loading mark goes with its first frame.
        PantrixTypefaceGate(
            loaded = areTypefacesLoaded(FormaticoTypefaces.all),
            onShown = { document.getElementById("loading")?.remove() }
        ) {
            App()
        }
    }
}
