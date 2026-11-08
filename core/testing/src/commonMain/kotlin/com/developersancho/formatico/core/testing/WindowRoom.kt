package com.developersancho.formatico.core.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pantrixui.foundation.adaptive.PantrixWindow

/**
 * [content] laid out at [window]'s size, from the test's top start, whatever room the test draws in: a test's window is
 * what the screen is told, not the size it is drawn at (Pantrix's testing guide, 3.3). The diagnostics then see the
 * layout the window has. What stands past the test's room is not drawn, and a press there misses: such a control is
 * pressed as a screen reader presses it.
 */
@Composable
fun AtWindowSize(window: PantrixWindow, content: @Composable () -> Unit) {
    Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).size(window.size)) { content() }
}
