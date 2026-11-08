package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.developersancho.formatico.core.designsystem.PREVIEW_TAG
import com.pantrixui.testing.PantrixTestTheme
import io.github.takahirom.roborazzi.captureRoboImage
import org.koin.core.context.stopKoin
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The bar's previews, every case on its board: `recordRoborazziJvm` writes each under `screenshots/AppTopBar/`, named
 * by its case and its board's width, and `verifyRoborazziJvm` compares against them.
 */
@OptIn(ExperimentalTestApi::class)
class AppTopBarScreenshotTest {
    private var locale: Locale = Locale.getDefault()

    // The bar's words are Compose resources, which read the JVM's language, not the theme's.
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
    fun appTopBar() = AppTopBarCases().values.forEach { case ->
        val board = case.value.board
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            setContent {
                PantrixTestTheme(platform = board.platform, dark = case.dark, window = board.window) { AppTopBarPreview(case) }
            }
            waitForIdle()
            onNodeWithTag(PREVIEW_TAG).captureRoboImage("screenshots/AppTopBar/${case.fileName}-${board.width}.png")
        }
        // KoinApplication keeps its context once the preview is gone: each case starts its own.
        stopKoin()
    }
}
