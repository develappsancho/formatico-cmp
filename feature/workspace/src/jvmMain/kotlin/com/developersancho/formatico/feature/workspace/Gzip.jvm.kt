package com.developersancho.formatico.feature.workspace

import java.io.OutputStream
import java.util.zip.GZIPOutputStream

internal actual suspend fun gzippedSize(text: String): Long? {
    val counted = CountingStream()
    GZIPOutputStream(counted).use { it.write(text.encodeToByteArray()) }
    return counted.count
}

/** A stream that keeps nothing of what is written to it but how many bytes it was. */
private class CountingStream : OutputStream() {
    var count = 0L
        private set

    override fun write(b: Int) {
        count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        count += len
    }
}
