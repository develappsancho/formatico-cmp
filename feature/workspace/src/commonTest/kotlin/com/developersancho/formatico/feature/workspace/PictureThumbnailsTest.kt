package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.network.WebPictures
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.testing.PantrixTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A picture value's thumbnail: read once from its address, cut to a square, and only for the rows shown. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
class PictureThumbnailsTest {
    /** A picture of 4 by 2 pixels: its left half red, its right half blue. */
    private val redAndBlue = "iVBORw0KGgoAAAANSUhEUgAAAAQAAAACCAIAAADwyuo0AAAAE0lEQVR4nGP4z8AARGDiPwMyBwBnsgf5Q2OaZwAAAABJRU5ErkJggg=="
    private val pngBytes = Base64.decode(redAndBlue)

    /** Web pictures that hand out [bytes] for every address, and keep the addresses asked for, from any thread. */
    private class Pictures(private val bytes: ByteArray?) : WebPictures {
        private val addresses = MutableStateFlow<List<String>>(emptyList())

        val asked: List<String> get() = addresses.value

        override suspend fun read(address: String, maxBytes: Long): ByteArray? {
            addresses.update { it + address }
            return bytes
        }
    }

    @Test
    fun aPictureWrittenIntoItsAddressIsAPicture() {
        val written = "data:image/png;base64,$redAndBlue"
        assertEquals(ValuePreview.Picture(written), previewOf(written))
        assertEquals(ValuePreview.Picture("https://images.example.com/a.png"), previewOf("https://images.example.com/a.png"))
        assertNull(previewOf("data:text/plain;base64,aGVsbG8="))
    }

    @Test
    fun aDataAddressIsReadOnThisDeviceUpToItsLimit() {
        val written = "data:image/png;base64,$redAndBlue"
        assertTrue(pngBytes.contentEquals(dataPictureBytes(written, MAX_PICTURE_BYTES)))
        assertNull(dataPictureBytes(written, maxBytes = 10))
        assertNull(dataPictureBytes("data:image/png;base64,***", MAX_PICTURE_BYTES))
        assertNull(dataPictureBytes("https://images.example.com/a.png", MAX_PICTURE_BYTES))
    }

    @Test
    fun aPictureIsCutToASquareFromItsMiddle() {
        val square = assertNotNull(squareOf(pngBytes, side = THUMBNAIL_PIXELS))
        assertEquals(2, square.width)
        assertEquals(2, square.height)
        val pixels = square.toPixelMap()
        assertEquals(Color.Red, pixels[0, 0])
        assertEquals(Color.Blue, pixels[1, 0])
        assertNull(squareOf("no picture".encodeToByteArray(), side = THUMBNAIL_PIXELS))
    }

    @Test
    fun aPictureIsReadOnceAndKept() = runTest {
        val pictures = Pictures(pngBytes)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val thumbnails = LoadedThumbnails(pictures, caller = dispatcher, worker = dispatcher)
        val address = "https://images.example.com/u/elif.png"

        assertNull(thumbnails.known(address))
        val first = assertIs<Thumbnail.Shown>(thumbnails.load(address))
        val second = thumbnails.load(address)

        assertEquals(first, second)
        assertEquals(first, thumbnails.known(address))
        assertEquals(listOf(address), pictures.asked)
    }

    @Test
    fun aPictureThatCouldNotBeReadIsNoneAndIsNotReadAgain() = runTest {
        val pictures = Pictures(null)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val thumbnails = LoadedThumbnails(pictures, caller = dispatcher, worker = dispatcher)
        val address = "https://images.example.com/gone.png"

        assertEquals(Thumbnail.None, thumbnails.load(address))
        assertEquals(Thumbnail.None, thumbnails.load(address))
        assertEquals(1, pictures.asked.size)
    }

    @Test
    fun aLargePictureIsCutOnTheWorker() = runTest {
        // The picture's bytes, padded past what is cut at once: a PNG reads its chunks, and ignores what follows its end.
        val pictures = Pictures(pngBytes + ByteArray(INLINE_DECODE_BYTES))
        val worker = StandardTestDispatcher(testScheduler)
        val thumbnails = LoadedThumbnails(pictures, caller = UnconfinedTestDispatcher(testScheduler), worker = worker)

        val loading = async { thumbnails.load("https://images.example.com/large.png") }
        runCurrent()
        assertIs<Thumbnail.Shown>(loading.await())
    }

    @Test
    fun aDataAddressNeverReachesTheNetwork() = runTest {
        val pictures = Pictures(null)
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val thumbnails = LoadedThumbnails(pictures, caller = dispatcher, worker = dispatcher)

        assertIs<Thumbnail.Shown>(thumbnails.load("data:image/png;base64,$redAndBlue"))
        assertTrue(pictures.asked.isEmpty())
    }

    @Test
    fun aThumbnailIsShownOnceReadAndNotWhereItCouldNotBe() = runComposeUiTest {
        val address = "https://images.example.com/u/elif.png"
        setContent {
            PantrixTestTheme {
                PreviewPictures(read = true) {
                    PantrixText(text = if (thumbnailOf(address) is Thumbnail.Shown) "read" else "none")
                }
                PreviewPictures(read = false) {
                    PantrixText(text = if (thumbnailOf(address) is Thumbnail.Shown) "read too" else "not read")
                }
            }
        }

        awaitText("read")
        awaitText("not read")
    }

    @Test
    fun aTreeRowDrawsThePictureOnceReadAndItsIconWhereItCouldNotBe() = runComposeUiTest {
        var read by mutableStateOf(true)
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                PreviewPictures(read = read) {
                    WorkspaceScreen(state = Order, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        fun drawn(state: ThumbnailState): Boolean {
            val nodes = onAllNodes(SemanticsMatcher.expectValue(PictureThumbnailState, state), useUnmergedTree = true)
            return nodes.fetchSemanticsNodes().isNotEmpty()
        }
        awaitUntil("The avatar's thumbnail was drawn") { drawn(ThumbnailState.Shown) }
        read = false
        awaitUntil("The avatar's icon was drawn") { drawn(ThumbnailState.None) }
    }

    @Test
    fun onlyThePicturesOfTheRowsShownAreRead() = runComposeUiTest {
        val count = 300
        val text = (0 until count).joinToString(",\n", "[\n", "\n]\n") { "  \"https://images.example.com/$it.png\"" }
        val state = readState(OpenDocument(id = 1, name = "pictures.json", text = text, format = Format.Json))
        val pictures = Pictures(pngBytes)
        val thumbnails = LoadedThumbnails(pictures, caller = Dispatchers.Default)
        setContent {
            PantrixTestTheme(window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                CompositionLocalProvider(LocalPictureThumbnails provides thumbnails) {
                    WorkspaceScreen(state = state, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitUntil("The first row's picture was read") { "https://images.example.com/0.png" in pictures.asked }
        awaitIdle()
        assertTrue("https://images.example.com/0.png" in pictures.asked)
        assertTrue(pictures.asked.size < count, "Only the rows shown read their pictures: ${pictures.asked.size} were read")
    }
}
