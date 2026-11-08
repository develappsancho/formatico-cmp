package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.developersancho.formatico.core.network.WebPictures
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

/** A picture value's thumbnail: its picture cut to a square, or none, where it could not be read. */
internal sealed interface Thumbnail {
    /** The picture, cut to a square from its middle and made no larger than a thumbnail needs. */
    data class Shown(val image: ImageBitmap) : Thumbnail

    /** The picture could not be read: its place keeps the picture's icon. */
    data object None : Thumbnail
}

/** Where the thumbnails of picture values come from: the app's reads them; a preview's or a test's has them at once. */
internal interface PictureThumbnails {
    /** The thumbnail of the picture at [address] where it is known already, or null while it is not. */
    fun known(address: String): Thumbnail?

    /** The thumbnail of the picture at [address], read where it is not known yet. */
    suspend fun load(address: String): Thumbnail
}

/** The thumbnails a workspace draws; none in a preview or a test that gives none, where every picture keeps its icon. */
internal val LocalPictureThumbnails = staticCompositionLocalOf<PictureThumbnails?> { null }

/**
 * The thumbnail of the picture at [address], or null while it is read: a row asks for it only while it is shown, as a
 * lazy tree composes only the rows in view. Where no thumbnails are given, as in a preview that gives none, there is
 * none. Another address, or other thumbnails, start again from what they know: a value edited into another picture's
 * address does not keep the first one's.
 */
@Composable
internal fun thumbnailOf(address: String): Thumbnail? {
    val thumbnails = LocalPictureThumbnails.current ?: return Thumbnail.None
    val thumbnail by produceState(thumbnails.known(address), thumbnails, address) {
        value = thumbnails.known(address)
        if (value == null) value = thumbnails.load(address)
    }
    return thumbnail
}

/**
 * The app's [PictureThumbnails]: each picture read once while the app runs, from its `https` address by [pictures] or
 * from a `data:` address on this device, no larger than [MAX_PICTURE_BYTES], cut to a square of [THUMBNAIL_PIXELS] at
 * most, and kept in memory, the last [MAX_KEPT] of them. A picture that could not be read is kept as none, and not read
 * again.
 *
 * A reading starts on [caller], the thread the row asks on: the network is waited for without holding it, and a small
 * picture, of [INLINE_DECODE_BYTES] at most, is cut there at once, so that a picture at hand is drawn in the frame that
 * asks for it. A larger one is decoded on [worker], one at a time: it is held whole only while it is cut.
 */
internal class LoadedThumbnails(
    private val pictures: WebPictures,
    caller: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val worker: CoroutineDispatcher = Dispatchers.Default
) : PictureThumbnails {
    private val scope = CoroutineScope(SupervisorJob() + caller)

    // In the order they were last kept, the one kept longest ago first.
    private val kept = MutableStateFlow<Map<String, Thumbnail>>(emptyMap())
    private val lock = Mutex()
    private val reading = HashMap<String, Deferred<Thumbnail>>()
    private val decoding = Semaphore(1)

    override fun known(address: String): Thumbnail? = kept.value[address]

    override suspend fun load(address: String): Thumbnail {
        kept.value[address]?.let { return it }
        // Two rows of one address wait for the same reading; it goes on where the row that asked leaves the screen.
        val read = lock.withLock { reading.getOrPut(address) { scope.async { read(address) } } }
        return read.await()
    }

    private suspend fun read(address: String): Thumbnail {
        val bytes = dataPictureBytes(address, MAX_PICTURE_BYTES) ?: pictures.read(address, MAX_PICTURE_BYTES)
        val image = when {
            bytes == null -> null
            bytes.size <= INLINE_DECODE_BYTES -> squareOf(bytes, THUMBNAIL_PIXELS)
            else -> withContext(worker) { decoding.withPermit { squareOf(bytes, THUMBNAIL_PIXELS) } }
        }
        val thumbnail = image?.let(Thumbnail::Shown) ?: Thumbnail.None
        kept.update { map ->
            LinkedHashMap(map).apply {
                remove(address)
                put(address, thumbnail)
                while (size > MAX_KEPT) remove(keys.first())
            }
        }
        lock.withLock { reading.remove(address) }
        return thumbnail
    }
}

/** The most bytes a picture is read with: a larger one keeps its icon. */
internal const val MAX_PICTURE_BYTES = 2_000_000L

/** The most bytes a picture is cut with on the thread that asked for it: an avatar's, in a moment. */
internal const val INLINE_DECODE_BYTES = 32_000

/** The longest side of a thumbnail in pixels: the phone's card's 56 at a dense screen's scale. */
internal const val THUMBNAIL_PIXELS = 192

/** How many thumbnails are kept in memory, each of [THUMBNAIL_PIXELS] squared at most. */
private const val MAX_KEPT = 64

/** A picture written into its address, in base 64: `data:image/png;base64,…`. */
private val DataPicture = Regex("""^data:image/(png|jpe?g|gif|webp);base64,""", RegexOption.IGNORE_CASE)

/** How many bytes four characters of base 64 hold. */
private const val BASE64_QUANTUM_BYTES = 3
private const val BASE64_QUANTUM_CHARS = 4

/**
 * The bytes of the picture written into [address], a `data:` address in base 64, or null for an address that is none,
 * a picture that holds more than [maxBytes] and one written wrong.
 */
internal fun dataPictureBytes(address: String, maxBytes: Long): ByteArray? {
    val written = DataPicture.find(address) ?: return null
    val encoded = address.substring(written.range.last + 1).filterNot { it.isWhitespace() }
    if (encoded.length.toLong() / BASE64_QUANTUM_CHARS * BASE64_QUANTUM_BYTES > maxBytes) return null
    return runCatching { Base64.decode(encoded) }.getOrNull()?.takeIf { it.size <= maxBytes }
}

/**
 * [bytes], a picture, cut to a square from its middle, no wider than [side] pixels: the thumbnail kept in its place.
 * Null where the platform cannot read it.
 */
internal fun squareOf(bytes: ByteArray, side: Int): ImageBitmap? {
    val picture = runCatching { bytes.decodeToImageBitmap() }.getOrNull() ?: return null
    val edge = minOf(picture.width, picture.height)
    if (edge <= 0) return null
    val size = minOf(edge, side)
    val square = ImageBitmap(size, size)
    Canvas(square).drawImageRect(
        image = picture,
        srcOffset = IntOffset((picture.width - edge) / 2, (picture.height - edge) / 2),
        srcSize = IntSize(edge, edge),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size, size),
        paint = Paint().apply { filterQuality = FilterQuality.Medium }
    )
    return square
}
