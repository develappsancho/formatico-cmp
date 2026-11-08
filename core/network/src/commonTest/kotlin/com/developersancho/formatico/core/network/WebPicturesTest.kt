package com.developersancho.formatico.core.network

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A picture at a web address, read by the platform itself for a value's thumbnail, or none. */
class WebPicturesTest {
    private val address = "https://images.example.com/u/elif.png"
    private val bytes = ByteArray(16) { it.toByte() }

    /**
     * Pictures read through a client whose every request [answer] answers, by the platform [itself] or not; [asked]
     * says whether one was sent.
     */
    private class Answered(itself: Boolean = true, answer: suspend MockRequestHandleScope.() -> HttpResponseData) {
        var asked = false
        val pictures = HttpWebPictures(
            clientOf {
                asked = true
                answer()
            },
            itself = itself
        )
    }

    private fun MockRequestHandleScope.picture(bytes: ByteArray, type: String = "image/png") =
        respond(bytes, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, type))

    @Test
    fun aPictureIsReadWithItsBytes() = runTest {
        val read = Answered { picture(bytes) }.pictures.read(address, maxBytes = 100)
        assertContentEquals(bytes, read)
    }

    @Test
    fun anAddressThatIsNoHttpsOneIsNeverAsked() = runTest {
        val answered = Answered { picture(bytes) }
        assertNull(answered.pictures.read("http://images.example.com/u/elif.png", maxBytes = 100))
        assertNull(answered.pictures.read("file:///Users/elif/elif.png", maxBytes = 100))
        assertNull(answered.pictures.read("not an address", maxBytes = 100))
        assertTrue(!answered.asked)
    }

    @Test
    fun theWebAsksForNoPicture() = runTest {
        val answered = Answered(itself = false) { picture(bytes) }
        assertNull(answered.pictures.read(address, maxBytes = 100))
        assertTrue(!answered.asked)
    }

    @Test
    fun aPictureLargerThanItsLimitIsNone() = runTest {
        assertNull(Answered { picture(ByteArray(101)) }.pictures.read(address, maxBytes = 100))
    }

    @Test
    fun anAnswerThatHoldsNoPictureIsNone() = runTest {
        assertNull(Answered { picture("<html></html>".encodeToByteArray(), type = "text/html") }.pictures.read(address, maxBytes = 100))
        assertNull(Answered { respond("", HttpStatusCode.NotFound) }.pictures.read(address, maxBytes = 100))
        assertNull(Answered { error("No route to host") }.pictures.read(address, maxBytes = 100))
    }
}
