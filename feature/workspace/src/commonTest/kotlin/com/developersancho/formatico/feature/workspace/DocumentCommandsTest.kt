package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarHost
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.components.overlay.snackbar.SnackbarDuration
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DocumentCommandsTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))

    @Test
    fun anEditsSnackbarOffersItsUndoAndLeavesByItself() = runComposeUiTest {
        val snackbars = PantrixSnackbarState()
        var commands: DocumentCommands? = null
        setContent {
            PantrixTestTheme(window = desktop) {
                CompositionLocalProvider(LocalPantrixSnackbars provides snackbars) {
                    commands = rememberDocumentCommands(Workbench())
                    PantrixSnackbarHost(state = snackbars)
                }
            }
        }

        awaitUntil("The commands") { commands != null }
        val previous = OpenDocument(id = 1, name = "order.json", text = "{\"id\":\"ord_8842\"}", format = Format.Json)
        commands?.report(DocumentEdit.Format, EditResult.Changed(previous))

        awaitUntil("The snackbar") { snackbars.current?.tagKey == "edited" }
        val shown = snackbars.current
        assertEquals("Undo", shown?.actionLabel)
        assertEquals(SnackbarDuration.Short, shown?.duration)
        assertTrue(shown?.closeButton == true)

        mainClock.advanceTimeBy(SHORT_SNACKBAR_AND_MORE)
        awaitUntil("The snackbar left by itself") { snackbars.current == null }
    }
}

/** Longer than Pantrix's short snackbar stays, four seconds. */
private const val SHORT_SNACKBAR_AND_MORE = 5_000L
