package com.developersancho.formatico

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.skia.Image

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Formatico",
        state = rememberWindowState(width = 1440.dp, height = 900.dp),
        icon = remember { appIcon() }
    ) {
        App()
    }
}

/** The window's icon, which the dock and the taskbar show: the app's own, drawn by tools/icons/generate.py. */
private fun appIcon(): BitmapPainter {
    val bytes = checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("formatico-icon.png")) {
        "The app's icon is in the desktop app's resources"
    }.use { it.readBytes() }
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}
