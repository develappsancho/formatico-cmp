package com.developersancho.formatico

import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.captureOverlay
import kotlin.test.Test

/**
 * The palette's previews, every case at every board: `recordRoborazziJvm` writes each under `screenshots/Palette/`,
 * named by its case and its board's width, and `verifyRoborazziJvm` compares against them. The palette is a window of
 * its own, over its scrim.
 */
class PaletteScreenshotTest {
    @Test
    fun palette() = PaletteCases().values.forEach { case ->
        Board.entries.forEach { board -> captureOverlay("Palette", case, board) { PalettePreview(case) } }
    }
}
