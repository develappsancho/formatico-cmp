package com.developersancho.formatico.core.storage

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EncryptedTextStoreTest {
    private val document =
        """
        {
          "customer": "Elif Yılmaz",
          "card": "4111 1111 1111 1111"
        }
        """.trimIndent() + "\n"
    private val stored = MemoryTextStore()
    private val key = Random(seed = 7).nextBytes(32)

    private fun store(key: ByteArray = this.key) = EncryptedTextStore(stored) { key }

    @Test
    fun aTextIsReadBackAsItWasWritten() = runTest {
        store().write("open-1", document)

        assertEquals(document, store().read("open-1"))
    }

    @Test
    fun whatIsKeptSaysNothingOfTheText() = runTest {
        store().write("open-1", document)

        val kept = requireNotNull(stored.read("open-1"))
        assertTrue(kept.startsWith(ENCRYPTED_TEXT))
        assertFalse("Elif" in kept || "4111" in kept, kept)
    }

    @Test
    fun theSameTextIsEncryptedDifferentlyEachTime() = runTest {
        store().write("a", document)
        store().write("b", document)

        assertNotEquals(stored.read("a"), stored.read("b"))
    }

    @Test
    fun aTextKeptBeforeTheTextsWereEncryptedIsReadAsItIs() = runTest {
        stored.write("history-3", document)

        assertEquals(document, store().read("history-3"))
    }

    @Test
    fun aTextChangedOrReadWithAnotherKeyIsReadAsNone() = runTest {
        store().write("open-1", document)
        val kept = requireNotNull(stored.read("open-1"))

        assertNull(store(Random(seed = 8).nextBytes(32)).read("open-1"))

        // One character of the ciphertext changed: the tag no longer fits.
        val at = kept.length - 5
        stored.write("open-1", kept.substring(0, at) + (if (kept[at] == 'A') 'B' else 'A') + kept.substring(at + 1))
        assertNull(store().read("open-1"))
    }

    @Test
    fun anEmptyTextAndALargeOneAreKept() = runTest {
        val large = "x".repeat(1_000_000)
        store().write("empty", "")
        store().write("large", large)

        assertEquals("", store().read("empty"))
        assertEquals(large, store().read("large"))
    }
}
