package com.developersancho.formatico.core.share

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShareEnvelopeTest {
    @Test
    fun aDocumentIsPackedWithItsNameAndUnpackedAsItWas() {
        for (document in listOf(
            SharedDocument("""{"id": "ord_8842", "customer": {"name": "Elif Yılmaz"}}""", "order.json"),
            SharedDocument("a: 1\nb: [2, 3]\n", "Sipariş listesi & notlar #2.yaml"),
            SharedDocument("plain text with no name"),
            SharedDocument("")
        )) {
            assertEquals(document, ShareEnvelope.unpack(ShareEnvelope.pack(document)), document.name)
        }
    }

    @Test
    fun aTextThatCompressesIsPackedSmaller() {
        val rows = (1..2_000).joinToString("\n") { """{"at": "2026-10-29T09:00:00Z", "event": "open", "bytes": $it}""" }

        assertTrue(ShareEnvelope.pack(SharedDocument(rows, "events.ndjson")).size < rows.length / 4)
    }

    @Test
    fun whatIsNoEnvelopeOfFormaticosIsNone() {
        val packed = ShareEnvelope.pack(SharedDocument("{}", "a.json"))
        for (bytes in listOf(
            byteArrayOf(),
            packed.copyOf(packed.size - 3),
            packed.copyOf().also { it[it.size / 2] = (it[it.size / 2] + 1).toByte() },
            Zlib.compress("not an envelope".encodeToByteArray()),
            Zlib.compress("formatico:share:1\nno line after the name".encodeToByteArray())
        )) {
            assertNull(ShareEnvelope.unpack(bytes))
        }
    }
}
