package com.developersancho.formatico.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.core.testing.checkPreviewCases
import kotlin.test.Test

/**
 * Every preview case of Settings, drawn as its preview draws it at every board: what each shows, and that Pantrix's
 * diagnostics find nothing in it. What pressing does is in Settings' own tests.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewCasesTest {
    @Test
    fun settingsShowEachChoiceAndPage() = checkPreviewCases(
        SettingsCases(),
        boardsOf = { Board.entries },
        content = { SettingsPreview(it) }
    ) { case, board ->
        when (case.name) {
            "settings" -> awaitTexts("Theme", "As the system", "23 documents, 6.1 MB.", substring = true)

            "chosen" -> awaitTexts("Türkçe", "Tabs")

            "no history" -> awaitTexts("Nothing is kept.")

            "nothing yet" -> awaitTexts("nothing yet.", substring = true)

            // A phone has no keyboard's shortcuts: it opens at the history instead.
            "shortcuts" -> awaitTexts(if (board == Board.Phone) "Clear history" else "Keyboard shortcuts")

            "licences" -> awaitTexts("Open-source licences", "Compose Multiplatform", "Ktor", "OkHttp")

            else -> awaitTexts("navigation3-browser")
        }
    }
}
