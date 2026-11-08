package com.developersancho.formatico.feature.start

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.files.PantrixFile
import com.pantrixui.foundation.files.rememberFilePicker
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import com.pantrixui.testing.PantrixTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/** A file dropped on an open document, opened as the Start screen's zone opens one, or refused as it refuses one. */
@OptIn(ExperimentalTestApi::class)
class DroppedFileTest {
    /** A [DocumentOpening] that hands what it opens to [opened], and keeps what it would say in [said]. */
    private fun ComposeUiTest.opening(opened: MutableList<OpenedDocument>, said: MutableState<OpeningMessage?>): DocumentOpening {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = scope)
        val opener = DocumentOpener(history, Clock.System, maxFileSize = 100)
        var opening: DocumentOpening? = null
        setContent {
            PantrixTestTheme {
                val clipboard = rememberPantrixClipboard()
                val picker = rememberFilePicker()
                opening = remember {
                    DocumentOpening(scope, opener, clipboard, picker, said, mutableStateOf({ it: OpenedDocument -> opened += it }))
                }
            }
        }
        waitForIdle()
        return requireNotNull(opening)
    }

    @Test
    fun aDocumentDroppedOpensAsStartOpensIt() = runComposeUiTest {
        val opened = mutableListOf<OpenedDocument>()
        val said = mutableStateOf<OpeningMessage?>(null)
        val opening = opening(opened, said)

        opening.openDropped(listOf(PantrixFile("order.json", "{\"a\": 1}".encodeToByteArray()), PantrixFile("b.json", ByteArray(2))))

        awaitUntil("The first file was opened") { opened.isNotEmpty() }
        assertEquals("order.json", opened.single().name)
        assertEquals(Format.Json, opened.single().format)
        assertEquals(100, opening.maxFileSize)
        assertNull(said.value)
    }

    @Test
    fun aFileOfAnotherKindOrTooLargeIsRefusedAndSaidSo() = runComposeUiTest {
        val opened = mutableListOf<OpenedDocument>()
        val said = mutableStateOf<OpeningMessage?>(null)
        val opening = opening(opened, said)

        opening.openDropped(listOf(PantrixFile("photo.png", ByteArray(10))))
        assertEquals(OpeningMessage.Refused(FileRefusal.WrongType("photo.png")), said.value)

        opening.openDropped(listOf(PantrixFile("large.json", ByteArray(200))))
        val tooLarge = OpeningMessage.Refused(FileRefusal.TooLarge("large.json", 100))
        awaitUntil("The large file was refused") { said.value == tooLarge }
        assertTrue(opened.isEmpty())
    }
}
