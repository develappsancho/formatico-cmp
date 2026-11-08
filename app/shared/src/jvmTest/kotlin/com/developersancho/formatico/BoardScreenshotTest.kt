package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import io.github.takahirom.roborazzi.captureRoboImage
import org.koin.core.context.stopKoin
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The app as the canvas's boards draw it, at their three widths: `recordRoborazziJvm` writes each under
 * `screenshots/`, named as its board is (`Workspace-1440.png`), and `verifyRoborazziJvm` compares against them.
 */
@OptIn(ExperimentalTestApi::class)
class BoardScreenshotTest {
    /** A board's window: its width, its height and the platform its controls are sized for. */
    private enum class Board(val width: Int, val height: Int, val platform: PlatformType) {
        Desktop(1440, 900, PlatformType.Desktop),
        Tablet(834, 1194, PlatformType.Android),
        Phone(390, 844, PlatformType.Android)
    }

    private var locale: Locale = Locale.getDefault()

    // The app's words are Compose resources, which read the JVM's language, not the theme's.
    @BeforeTest
    fun inEnglish() {
        locale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    // KoinApplication keeps its context once the app is gone: each board starts with nothing open.
    @AfterTest
    fun closeTheApp() {
        stopKoin()
        Locale.setDefault(locale)
    }

    @Test
    fun start() = Board.entries.forEach { capture("Start", it) }

    @Test
    fun workspace() = Board.entries.forEach { capture("Workspace", it, sample = "order", shows = "customer") }

    @Test
    fun yaml() = Board.entries.forEach { capture("Yaml", it, sample = "deployment", shows = "apiVersion") }

    @Test
    fun table() = Board.entries.forEach { capture("Table", it, sample = "countries", shows = "Afghanistan") }

    /**
     * Draws the app on [board], opens [sample] where one is named and waits for what it [shows], and writes the picture
     * as `[name]-width.png`.
     */
    private fun capture(name: String, board: Board, sample: String? = null, shows: String? = null) {
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            setContent {
                PantrixTestTheme(
                    platform = board.platform,
                    window = PantrixWindow(DpSize(board.width.dp, board.height.dp))
                ) {
                    App(systemDark = false)
                }
            }
            waitUntilExactlyOneExists(hasText("Paste anything"), timeoutMillis = 10_000)
            if (sample != null && shows != null) {
                if (board == Board.Phone) onPantrixNode("Link", "all_samples").performClick()
                onPantrixNode("ListItem", "sample_$sample").performScrollTo().performClick()
                waitUntilAtLeastOneExists(hasText(shows, substring = true), timeoutMillis = 10_000)
            }
            onRoot().captureRoboImage("screenshots/$name-${board.width}.png")
        }
        stopKoin()
    }
}
