package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentOpenerTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val moment = Instant.parse("2026-11-05T21:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = moment
    }

    private fun TestScope.opener(): Pair<DocumentOpener, DocumentHistory> {
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = backgroundScope)
        return DocumentOpener(history, clock, maxFileSize = MAX_SIZE, reader = dispatcher) to history
    }

    @Test
    fun pastedTextIsOpenedInItsFormatAndGoesToTheHistory() = runTest(dispatcher) {
        val (opener, history) = opener()

        val opening = opener.paste("a: 1\n")

        assertEquals(Opening.Opened(OpenedDocument(name = null, text = "a: 1\n", format = Format.Yaml, historyId = 1)), opening)
        assertEquals(moment, history.documents.value.single().at)
    }

    @Test
    fun nothingOrBlanksAreNothingToPaste() = runTest(dispatcher) {
        val (opener, history) = opener()

        assertEquals(Opening.NothingToPaste, opener.paste(null))
        assertEquals(Opening.NothingToPaste, opener.paste("  \n"))
        assertEquals(emptyList(), history.documents.value)
    }

    @Test
    fun aFileIsToldByItsNameAndOneTooLargeOrUnreadableIsRefused() = runTest(dispatcher) {
        val (opener, _) = opener()

        val opened = opener.open(PantrixFile("settings.json", """{"a": 1}""".encodeToByteArray()))
        assertEquals(Format.Json, (opened as Opening.Opened).document.format)
        assertEquals("settings.json", opened.document.name)
        assertEquals(
            Opening.Refused(FileRefusal.TooLarge("big.json", MAX_SIZE)),
            opener.open(PantrixFile("big.json", ByteArray(MAX_SIZE.toInt() + 1)))
        )
    }

    @Test
    fun anEmptyDocumentIsLeftOutOfTheHistory() = runTest(dispatcher) {
        val (opener, history) = opener()

        assertNull(opener.record(OpenedDocument(name = null, text = "", format = null)).historyId)
        assertEquals(emptyList(), history.documents.value)
    }

    private companion object {
        const val MAX_SIZE = 100L
    }
}
