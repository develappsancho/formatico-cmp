package com.developersancho.formatico.feature.start

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.FormatDetector
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class UnknownFormatDialogTest {
    private val pasted = OpenedDocument(name = null, text = PREVIEW_UNTOLD, format = null, historyId = 4)

    @Test
    fun theTextOfTheBoardsIsOneNoFormatIsToldOf() {
        assertEquals(null, FormatDetector.detect(PREVIEW_UNTOLD))
    }

    @Test
    fun theFormatChosenFirstIsTheOneTheTextLooksMostLike() {
        assertEquals(Format.Yaml, likelyFormat("endpoint: https://api.example.com\nretries = 3\ntimeout 30"))
        assertEquals(Format.Properties, likelyFormat("# a comment\nname = Elif\nlanguage=tr"))
        assertEquals(Format.Json, likelyFormat("just some words"))
    }

    @Test
    fun theQuestionShowsTheStartOfTheTextCutShort() {
        val long = "x".repeat(200)
        val shown = startOf("one\ntwo\nthree\nfour\nfive\n$long")
        assertEquals("one\ntwo\nthree\nfour", shown)
        assertEquals(80, startOf(long).length)
    }

    @Test
    fun openingReadsTheTextAsTheFormatChosenAndLeavingItKeepsItText() = runComposeUiTest {
        val answers = mutableListOf<Format?>()
        setContent { PantrixTestTheme { UnknownFormatDialog(document = pasted, onOpen = { answers += it }, onDismiss = {}) } }

        awaitText("What format is this?")
        awaitText("Formatico could not tell the format of the text you pasted. Choose how to read it.")
        onPantrixNode("Button", "open_as").performClick()
        onPantrixNode("Button", "leave_as_text").performClick()

        // Log lines look like none of the formats: JSON is chosen first.
        assertEquals(listOf(Format.Json, null), answers)
    }

    @Test
    fun aFilesQuestionNamesTheFile() = runComposeUiTest {
        setContent {
            PantrixTestTheme {
                UnknownFormatDialog(document = pasted.copy(name = "notes.txt"), onOpen = {}, onDismiss = {})
            }
        }

        awaitText("Formatico could not tell the format of notes.txt. Choose how to read it.")
    }
}
