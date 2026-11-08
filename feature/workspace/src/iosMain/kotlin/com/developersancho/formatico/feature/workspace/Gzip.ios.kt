package com.developersancho.formatico.feature.workspace

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import platform.zlib.ZLIB_VERSION
import platform.zlib.Z_DEFAULT_COMPRESSION
import platform.zlib.Z_DEFAULT_STRATEGY
import platform.zlib.Z_DEFLATED
import platform.zlib.Z_FINISH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.deflate
import platform.zlib.deflateBound
import platform.zlib.deflateEnd
import platform.zlib.deflateInit2_
import platform.zlib.z_stream

/** zlib's window of 2^15 bytes, and 16 more for a gzip header and trailer in place of zlib's own. */
private const val GZIP_WINDOW_BITS = 15 + 16

/** zlib's default memory for its state. */
private const val MEMORY_LEVEL = 8

/** Room past zlib's own bound, for the gzip header and trailer that bound may leave out. */
private const val GZIP_WRAPPER = 32

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun gzippedSize(text: String): Long? = memScoped {
    val input = text.encodeToByteArray()
    val stream = alloc<z_stream>()
    val started = deflateInit2_(
        stream.ptr,
        Z_DEFAULT_COMPRESSION,
        Z_DEFLATED,
        GZIP_WINDOW_BITS,
        MEMORY_LEVEL,
        Z_DEFAULT_STRATEGY,
        ZLIB_VERSION,
        sizeOf<z_stream>().convert()
    )
    if (started != Z_OK) return@memScoped null
    try {
        val output = ByteArray(deflateBound(stream.ptr, input.size.convert()).toInt() + GZIP_WRAPPER)
        // An empty array has no address to pin: an empty text is given a byte of room, none of which is read.
        val ended = (if (input.isEmpty()) ByteArray(1) else input).usePinned { source ->
            output.usePinned { target ->
                stream.next_in = source.addressOf(0).reinterpret()
                stream.avail_in = input.size.convert()
                stream.next_out = target.addressOf(0).reinterpret()
                stream.avail_out = output.size.convert()
                deflate(stream.ptr, Z_FINISH)
            }
        }
        if (ended == Z_STREAM_END) stream.total_out.toLong() else null
    } finally {
        deflateEnd(stream.ptr)
    }
}
