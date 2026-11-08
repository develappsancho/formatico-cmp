package com.developersancho.formatico.core.designsystem

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pantrixui.foundation.adaptive.LocalPantrixWindow
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.testing.PantrixTestTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class FormaticoPreviewTest {
    /** What a preview's content is shown with: its platform, the size of the window it is told, the clock's moment. */
    private class Seen {
        var platform: PlatformType? = null
        var window: DpSize? = null
        var now: Instant? = null
    }

    /** What a preview drawn in a room [width] by [height] wide, in a test theme on [around], shows its content with. */
    private fun seenAt(width: Int, height: Int, around: PlatformType): Seen {
        val seen = Seen()
        runDesktopComposeUiTest(width = width, height = height) {
            setContent {
                PantrixTestTheme(platform = around) {
                    FormaticoPreview(PreviewCase("order", Unit, dark = false)) {
                        seen.platform = PantrixTheme.platform.type
                        seen.window = LocalPantrixWindow.current.size
                        seen.now = LocalPantrixClock.current.clock.now()
                    }
                }
            }
            waitForIdle()
        }
        return seen
    }

    @Test
    fun aPreviewAtABoardsSizeIsOnItsPlatformInItsWindowAtThePreviewsMoment() {
        val tablet = seenAt(Board.Tablet.width, Board.Tablet.height, around = PlatformType.Desktop)
        assertEquals(PlatformType.Android, tablet.platform)
        assertEquals(DpSize(834.dp, 1194.dp), tablet.window)
        assertEquals(PreviewNow, tablet.now)

        val desktop = seenAt(Board.Desktop.width, Board.Desktop.height, around = PlatformType.Android)
        assertEquals(PlatformType.Desktop, desktop.platform)
        assertEquals(DpSize(1440.dp, 900.dp), desktop.window)
    }

    @Test
    fun aPreviewAtAnotherSizeKeepsThePlatformOfTheThemeAroundIt() {
        val seen = seenAt(1000, 700, around = PlatformType.Ios)
        assertEquals(PlatformType.Ios, seen.platform)
        assertEquals(PreviewNow, seen.now)
    }
}
