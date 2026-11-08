package com.developersancho.formatico.core.share

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZlibTest {
    /** zlib's own streams (Python's `zlib.compress`), one of each kind of block. */
    private val stored = "eAEBJgDZ/0Zvcm1hdGljbyBrZWVwcyB0aGUgb3JkZXIgb2YgdGhlIGtleXMuFF8N2g=="
    private val fixed = "eNqrVspMUbJSUMovSom3sDAxUtJRUCouSSwpLQaJFmdkFhSkpijVAgDhDgvh"
    private val repeated = "eNqrVkosUbJSUDIyMDLTNTTQNbIMMbC0MjCwMjSKUtJRUEotS80DK8gvSM0DCSRVlqQWAwUMjQwMarmqR7WPah/VPqp9VPuo9lHto9pHtQ8O" +
        "7a45mWkKkUc25uQmVukoHNlQlZtZpPBh/owGAIEUg5I="
    private val dynamic = "eNqV1N9q2zAUBvD7PoXIdTk4duIku2pGx8qawhiFMcYuTuNTR7X+BElO15a+0/YOfbDKjtOMSbj4xuAjf+jHhzlPJ4yN1rV1WpKxow/s" +
        "px8w9tQ+/REv/Cw5PbwqlOQHo0+C37IfL3+FxMfR2+mau4fm9OXPo+TmOCeJXDQH5GPwsI+d0W+UW0Gw1vL4qdMOm08TOF66xVbhTE3t6Pk0Yhw" +
        "HxmWBbKV3JHBNoXGlVaFVxIgFguhi/cZxBtnsf+UtCtvDTAPmZ+MvYhd6uyUTMpdGcFW6qLRskrBpk/3SNIfZZFifWdinQMWua+M9ofM7F9IKfR8r" +
        "1OfAtbl+5SSB8Xhgn5OAeYluQ/4yYl/0Rtl/mztgLzwiXml1CMPdPtwvnmYwmQ/rdRqAV1zVll1rs0NR2IiWhOWq4hGuaKLgumi/Nc9hPh3Ybh5" +
        "gr9CUaMgx3yEXLtbuV9QihpVdFDZdtN87TyBNh3U7C/dUYUsy7JzfVdYZDLHftHNkCpSxbdWGoejC/dxFBtPFwHrngfcjmhs0fm9xW+ldbGtZtl" +
        "QlCbIR8M0+DaJNv7O8khwW+bB+F4H3XCv/57FLVbtNqL3i4h7riihiLdokVE3yHWmaQJb1VOufv06eXwGOvr1g"
    private val event = """{"at": "2026-10-29T09:00:12Z", "event": "open", "bytes": 1200}""" + "\n"

    private fun unpacked(base64: String, limit: Int = ShareLinks.MAX_TEXT) = Zlib.decompress(Base64.decode(base64), limit)

    @Test
    fun zlibsOwnStreamsOfEachKindOfBlockAreReadBack() {
        assertEquals("Formatico keeps the order of the keys.", unpacked(stored)?.decodeToString())
        assertEquals("""{"id": "ord_8842", "status": "shipped"}""", unpacked(fixed)?.decodeToString())
        assertEquals(event.repeat(40) + "Elif Yılmaz, İzmir 😀", unpacked(repeated)?.decodeToString())
        val customers = unpacked(dynamic)?.decodeToString().orEmpty()
        assertEquals(1738, customers.encodeToByteArray().size)
        assertTrue("\"name\": \"Elif Yılmaz\"" in customers && "Donald Knuth" in customers, customers)
    }

    @Test
    fun whatIsCompressedIsReadBackAsItWas() {
        val texts = listOf(
            ByteArray(0),
            byteArrayOf(42),
            ByteArray(256) { it.toByte() },
            "Elif Yılmaz, İzmir, ğüşöç 😀".encodeToByteArray(),
            event.repeat(5_000).encodeToByteArray(),
            pseudoRandom(50_000),
            unpacked(dynamic)!!
        )
        for (text in texts) assertContentEquals(text, Zlib.decompress(Zlib.compress(text), ShareLinks.MAX_TEXT), "${text.size} bytes")
    }

    @Test
    fun repeatsAreCompressedAsZlibDoesAndTheSameTextGivesTheSameBytes() {
        val customers = unpacked(dynamic)!!
        // zlib's best is 477 bytes.
        assertTrue(Zlib.compress(customers).size < 520, "${Zlib.compress(customers).size} bytes")
        // zlib's best is 1001 bytes.
        val events = Zlib.compress(event.repeat(5_000).encodeToByteArray()).size
        assertTrue(events <= 1_050, "$events bytes")
        assertContentEquals(Zlib.compress(customers), Zlib.compress(customers.copyOf()))
        assertEquals("eJ", Base64.encode(Zlib.compress(customers)).take(2))
    }

    @Test
    fun aStreamChangedOrCutShortOrTooLargeIsNotRead() {
        val packed = Zlib.compress(event.repeat(100).encodeToByteArray())
        assertNull(Zlib.decompress(packed.copyOf(packed.size - 1), ShareLinks.MAX_TEXT), "cut short")
        assertNull(Zlib.decompress(packed.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }, ShareLinks.MAX_TEXT))
        assertNull(Zlib.decompress(packed.copyOf().also { it[0] = 0x79 }, ShareLinks.MAX_TEXT), "not deflate")
        assertNull(Zlib.decompress(packed, limit = 1_000), "more than the limit")
        assertNull(Zlib.decompress(ByteArray(64) { 0xFF.toByte() }, ShareLinks.MAX_TEXT))
    }

    @Test
    fun adler32IsZlibs() {
        assertEquals(0x11E60398, adler32("Wikipedia".encodeToByteArray()))
        assertEquals(1, adler32(ByteArray(0)))
    }
}

/** [size] bytes that hardly compress, the same in every run: a linear congruential generator's. */
internal fun pseudoRandom(size: Int): ByteArray {
    var seed = 20_261_106L
    return ByteArray(size) {
        seed = (seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L)
        (seed ushr 56).toByte()
    }
}
