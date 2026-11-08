package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.captureOverlay
import com.developersancho.formatico.core.testing.capturePreview
import kotlin.test.Test

/**
 * Start's previews, every case at every board: `recordRoborazziJvm` writes each under `screenshots/`, named by what it
 * shows, its case and its width (`StartScreen/recent-dark-390.png`), and `verifyRoborazziJvm` compares against them.
 */
class StartScreenshotTest {
    @Test
    fun startScreen() = StartScreenCases().values.forEach { case ->
        Board.entries.forEach { board -> capturePreview("StartScreen", case, board) { StartScreenPreview(case) } }
    }

    @Test
    fun unknownFormat() = UnknownFormatCases().values.forEach { case ->
        Board.entries.forEach { board -> captureOverlay("UnknownFormat", case, board) { UnknownFormatPreview(case) } }
    }
}
