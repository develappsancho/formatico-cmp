package com.developersancho.formatico.feature.download

import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.captureOpenMenu
import kotlin.test.Test

/**
 * The download menu's previews, every case at every board, opened by their button: `recordRoborazziJvm` writes each
 * under `screenshots/`, named by its case and its width (`DownloadMenu/release-dark-1440.png`), and
 * `verifyRoborazziJvm` compares against them. The phone's is the sheet a phone's browser shows.
 */
class DownloadScreenshotTest {
    @Test
    fun downloadMenu() = DownloadMenuCases().values.forEach { case ->
        Board.entries.forEach { board ->
            captureOpenMenu("DownloadMenu", case, board, opener = "get_the_app") { DownloadMenuPreview(case) }
        }
    }
}
