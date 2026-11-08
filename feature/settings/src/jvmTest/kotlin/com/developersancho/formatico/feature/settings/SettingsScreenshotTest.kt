package com.developersancho.formatico.feature.settings

import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.capturePreview
import kotlin.test.Test

/**
 * Settings' previews, every case at every board: `recordRoborazziJvm` writes each under `screenshots/`, named by its
 * case and its width (`Settings/licences-dark-390.png`), and `verifyRoborazziJvm` compares against them.
 */
class SettingsScreenshotTest {
    @Test
    fun settings() = SettingsCases().values.forEach { case ->
        Board.entries.forEach { board -> capturePreview("Settings", case, board) { SettingsPreview(case) } }
    }
}
