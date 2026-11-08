package com.developersancho.formatico.feature.workspace

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class GzipTest {
    @Test
    fun aTextIsGzippedByThePlatformsOwnCompressor() = runTest {
        val repeated = gzippedSize("formatico ".repeat(1_000))
        val empty = gzippedSize("")
        val turkish = gzippedSize("İzmir, Isparta, Şile")

        // 10,000 bytes that repeat come to a few dozen; nothing comes to gzip's header and trailer.
        assertTrue(repeated != null && repeated in 20L..200L, "$repeated")
        assertTrue(empty != null && empty in 18L..30L, "$empty")
        assertTrue(turkish != null && turkish > 18L, "$turkish")
    }
}
