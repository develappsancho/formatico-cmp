package com.developersancho.formatico.core.share

import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** Formatico's zlib against Java's, which is zlib itself: each reads what the other writes. */
class ZlibJvmTest {
    private val texts = listOf(
        ByteArray(0),
        "a".encodeToByteArray(),
        """{"id": "ord_8842", "status": "shipped", "total": 129.90, "paid": true}""".encodeToByteArray(),
        ("""{"at": "2026-10-29T09:00:12Z", "event": "open", "bytes": 1200}""" + "\n").repeat(3_000).encodeToByteArray(),
        pseudoRandom(70_000),
        ByteArray(100_000) { (it % 7).toByte() }
    )

    @Test
    fun javaReadsWhatFormaticoWrites() {
        for (text in texts) assertContentEquals(text, javaInflated(Zlib.compress(text)), "${text.size} bytes")
    }

    @Test
    fun formaticoReadsWhatJavaWritesAtEveryLevel() {
        for (level in listOf(0, 1, 6, 9)) {
            for (text in texts) {
                assertContentEquals(
                    text,
                    Zlib.decompress(javaDeflated(text, level), ShareLinks.MAX_TEXT),
                    "level $level, ${text.size} bytes"
                )
            }
        }
    }

    private fun javaInflated(packed: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(packed)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            check(read > 0 || inflater.finished() || !inflater.needsInput()) { "Java needs more than the stream" }
            out.write(buffer, 0, read)
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun javaDeflated(text: ByteArray, level: Int): ByteArray {
        val deflater = Deflater(level)
        deflater.setInput(text)
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8_192)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        return out.toByteArray()
    }
}
