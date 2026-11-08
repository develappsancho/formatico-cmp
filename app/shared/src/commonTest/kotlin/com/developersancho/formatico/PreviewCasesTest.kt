package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.core.testing.checkPreviewCases
import kotlin.test.Test

/**
 * Every preview case of the app's own bar and its palette, drawn as their previews draw them: what each shows, and
 * that Pantrix's diagnostics find nothing in it. What pressing does is in their own tests.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewCasesTest {
    @Test
    fun theBarShowsTheOpenDocuments() = checkPreviewCases(
        AppTopBarCases(),
        boardsOf = { listOf(it.board) },
        content = { AppTopBarPreview(it) }
    ) { case, board ->
        val documents = case.value.workbench.documents
        when {
            // A phone's bar counts the open documents on its switcher's button.
            board == Board.Phone -> if (documents.isNotEmpty()) awaitTexts("${documents.size}")

            documents.isEmpty() -> awaitTexts("Formatico", "New tab")

            else -> awaitTexts(*documents.map { it.name ?: "Untitled" }.toTypedArray())
        }
    }

    @Test
    fun thePaletteListsTheCommandsOfWhereItOpens() = checkPreviewCases(
        PaletteCases(),
        boardsOf = { Board.entries },
        content = { PalettePreview(it) }
    ) { case, _ ->
        if (case.value.settingsShown || case.value.workbench.selected == null) {
            awaitTexts("Formatico", "Open a file", "Settings")
        } else {
            awaitTexts("Document", "Format document", "Copy the document")
        }
    }
}
