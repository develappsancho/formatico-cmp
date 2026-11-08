package com.developersancho.formatico.feature.start

import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.components.overlay.snackbar.SnackbarDuration
import com.pantrixui.components.overlay.snackbar.SnackbarTone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OpeningSnackbarsTest {
    @Test
    fun anUnreachableSharedLinkIsSaidWithItsTryAgainAndLeavesByItself() = runTest {
        val snackbars = PantrixSnackbarState()
        val triedAgain = async { snackbars.showSharedLinkUnreachable("Formatico’s server could not be reached.", "Try again") }
        runCurrent()

        val shown = checkNotNull(snackbars.current)
        assertEquals("shared_link_unreachable", shown.tagKey)
        assertEquals("Try again", shown.actionLabel)
        assertEquals(SnackbarTone.Danger, shown.tone)
        assertEquals(SnackbarDuration.Short, shown.duration)
        assertTrue(shown.closeButton)

        shown.dismiss()
        assertFalse(triedAgain.await())
    }

    @Test
    fun aBrokenLinkAndAGoneShareStayLongerWithAClose() = runTest {
        val snackbars = PantrixSnackbarState()
        for (show in listOf(PantrixSnackbarState::showBrokenLink, PantrixSnackbarState::showSharedLinkGone)) {
            val said = async { show(snackbars, "This link holds no document Formatico can read.") }
            runCurrent()

            val shown = checkNotNull(snackbars.current)
            assertEquals(SnackbarDuration.Long, shown.duration)
            assertTrue(shown.closeButton)
            shown.dismiss()
            said.await()
        }
    }
}
