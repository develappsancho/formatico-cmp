package com.developersancho.formatico.feature.start

import androidx.compose.ui.test.ExperimentalTestApi
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.awaitTexts
import com.developersancho.formatico.core.testing.checkPreviewCases
import kotlin.test.Test
import kotlin.test.fail

/**
 * Every preview case of Start, drawn as its preview draws it at every board: what each shows, and that Pantrix's
 * diagnostics find nothing in it. What pressing does is in each screen's own tests.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewCasesTest {
    @Test
    fun startShowsWhatWasOpenedAndWhatWasRefused() = checkPreviewCases(
        StartScreenCases(),
        boardsOf = { Board.entries },
        content = { StartScreenPreview(it) }
    ) { case, board ->
        awaitTexts("Paste from clipboard")
        when (case.name) {
            "first start" -> awaitTexts("Nothing opened yet")

            "recent" -> awaitTexts("order.json", "Untitled")

            // A phone has no place to drop a file on, so nothing is said of one refused there.
            else -> if (board != Board.Phone) awaitTexts(RefusalTexts[case.name] ?: fail("No refusal for ${case.name}"))
        }
    }

    @Test
    fun theQuestionNamesWhatItCouldNotRead() = checkPreviewCases(
        UnknownFormatCases(),
        boardsOf = { Board.entries },
        content = { UnknownFormatPreview(it) }
    ) { case, _ ->
        awaitTexts("What format is this?", "Leave it as text")
        awaitTexts(if (case.value.name == null) "the text you pasted" else "the format of notes.txt", substring = true)
    }

    @Test
    fun theAddressSaysWhyItOpenedNothing() = checkPreviewCases(
        WebAddressCases(),
        boardsOf = { Board.entries },
        content = { WebAddressPreview(it) }
    ) { case, _ ->
        awaitTexts("Formatico opens the document at a public http or https address.", "Address")
        awaitTexts(AddressTexts[case.name] ?: fail("No text for ${case.name}"), substring = true)
    }

    @Test
    fun aSharedLinkIsWaitedForOverStart() = checkPreviewCases(
        SharedLinkWaitCases(),
        boardsOf = { Board.entries },
        content = { SharedLinkWaitPreview(it) }
    ) { _, _ ->
        awaitTexts("Opening a shared document", "Cancel")
    }

    private companion object {
        val RefusalTexts = mapOf(
            "refused file" to "logs.json is larger than 10 MB",
            "wrong type" to "invoice.pdf is not a kind of text Formatico reads",
            "unreadable" to "orders.json could not be read"
        )

        val AddressTexts = mapOf(
            "empty" to "Formatico downloads it itself.",
            "typed" to "Formatico downloads it itself.",
            "opening" to "Opening",
            "not there" to "The address answered 404: there is no document there.",
            "answered" to "The address answered 503, and gave no document.",
            "not allowed" to "Formatico opens only public http and https addresses.",
            "too large" to "The document is 24 MB",
            "too large untold" to "The document is larger than Formatico opens here",
            "not text" to "What is at this address is not text, and Formatico opens only text.",
            "unreachable" to "The address could not be reached. Check it, or try again.",
            "server unreachable" to "Formatico’s server, which fetches it on the web, could not be reached."
        )
    }
}
