package com.developersancho.formatico.feature.start

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.pantrixui.testing.PantrixTestTheme
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Start's previews, every case at every board: `recordRoborazziJvm` writes each under `screenshots/`, named by what it
 * shows, its case and its width (`StartScreen/recent-dark-390.png`), and `verifyRoborazziJvm` compares against them.
 */
@OptIn(ExperimentalTestApi::class)
class StartScreenshotTest {
    private var locale: Locale = Locale.getDefault()

    // The screen's words are Compose resources, which read the JVM's language, not the theme's.
    @BeforeTest
    fun inEnglish() {
        locale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun inTheMachinesLanguage() {
        Locale.setDefault(locale)
    }

    @Test
    fun startScreen() = StartScreenCases().values.forEach { case ->
        Board.entries.forEach { board -> capture("StartScreen", case, board) { StartScreenPreview(case) } }
    }

    /** Draws [content] in [board]'s window and platform, light or dark as [case] is, and writes it under [name]. */
    private fun capture(name: String, case: PreviewCase<*>, board: Board, content: @Composable () -> Unit) =
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            setContent {
                PantrixTestTheme(platform = board.platform, dark = case.dark, window = board.window) { content() }
            }
            waitForIdle()
            onRoot().captureRoboImage("screenshots/$name/${case.fileName}-${board.width}.png")
        }
}
