package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitTexts
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.files.PantrixFile
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.assertNoFindings
import kotlin.test.Test
import kotlin.test.assertTrue

/** The cover a file dragged over an open document shows, and what it says. */
@OptIn(ExperimentalTestApi::class)
class DocumentDropTest {
    /** A drop that takes files of [maxFileSize] at most, and opens none. */
    private class Drop(override val maxFileSize: Long) : DocumentDrop {
        override fun open(files: List<PantrixFile>) = Unit
    }

    @Test
    fun aFileDraggedOverADocumentSaysItOpensInANewTabAndHowLargeItMayBe() = runComposeUiTest {
        val findings = PantrixFindings()
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(834.dp, 1194.dp)), findings = findings) {
                CompositionLocalProvider(LocalDocumentDrop provides Drop(maxFileSize = 10_000_000)) {
                    WorkspaceScreen(state = OrderUnchosen, onIntent = {}, commands = rememberDocumentCommands(Workbench()), dropping = true)
                }
            }
        }

        awaitText("Drop to open in a new tab")
        awaitText("JSON, JSONC, JSON5, NDJSON, XML, YAML, TOML, CSV, TSV, properties and INI, up to 10 MB")
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneShowsNoCover() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(390.dp, 844.dp))) {
                CompositionLocalProvider(LocalDocumentDrop provides Drop(maxFileSize = 10_000_000)) {
                    WorkspaceScreen(state = OrderUnchosen, onIntent = {}, commands = rememberDocumentCommands(Workbench()), dropping = true)
                }
            }
        }

        awaitTexts("Valid JSON")
        assertTrue(onAllNodes(hasText("Drop to open in a new tab")).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aDocumentWithNothingOverItShowsNoCover() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                CompositionLocalProvider(LocalDocumentDrop provides Drop(maxFileSize = 50_000_000)) {
                    WorkspaceScreen(state = OrderUnchosen, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitTexts("Valid JSON")
        assertTrue(onAllNodes(hasText("Drop to open in a new tab")).fetchSemanticsNodes().isEmpty())
    }
}
