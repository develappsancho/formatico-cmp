package com.developersancho.formatico.core.network

import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WebAddressesTest {
    private val address = "https://example.com/orders/8842.json"

    private fun itself(answer: suspend MockRequestHandleScope.() -> HttpResponseData) =
        HttpWebAddresses(clientOf { answer() }, FormaticoServer(clientOf { error("not asked") }, SERVER), itself = true)

    private fun throughServer(answer: suspend MockRequestHandleScope.() -> HttpResponseData) =
        HttpWebAddresses(clientOf { error("not asked") }, FormaticoServer(clientOf { answer() }, SERVER), itself = false)

    @Test
    fun aDocumentIsReadWithTheNameItsAddressGivesIt() = runTest {
        val read = itself { respond("""{"id": "ord_8842"}""", HttpStatusCode.OK) }.open(address, maxBytes = 1_000)
        assertEquals(AddressReading.Read("""{"id": "ord_8842"}""", "8842.json"), read)

        val bare = itself { respond("a: 1", HttpStatusCode.OK) }.open("https://example.com", maxBytes = 1_000)
        assertEquals(AddressReading.Read("a: 1", null), bare)
    }

    @Test
    fun onlyAWebAddressIsOpened() = runTest {
        val addresses = itself { error("not asked") }
        for (text in listOf("ftp://example.com/a.json", "file:///etc/passwd", "example.com/a.json", "javascript:alert(1)", "")) {
            assertEquals(AddressReading.NotAllowed, addresses.open(text, maxBytes = 1_000), text)
        }
    }

    @Test
    fun aStatusALargeDocumentAndOneThatIsNoTextAreSaidSo() = runTest {
        assertEquals(AddressReading.Answered(404), itself { respond("", HttpStatusCode.NotFound) }.open(address, 1_000))
        assertEquals(
            AddressReading.TooLarge(2_000),
            itself { respond("x".repeat(2_000), HttpStatusCode.OK, headersOf(HttpHeaders.ContentLength, "2000")) }.open(address, 1_000)
        )
        assertEquals(
            AddressReading.TooLarge(null),
            itself { respond(ByteReadChannel("x".repeat(2_000)), HttpStatusCode.OK) }.open(address, 1_000)
        )
        assertEquals(AddressReading.NotText, itself { respond(byteArrayOf(0xC3.toByte(), 0x28), HttpStatusCode.OK) }.open(address, 1_000))
        assertEquals(AddressReading.Unreachable, itself { error("No route to host") }.open(address, 1_000))
    }

    @Test
    fun onTheWebTheServerFetchesAndSaysWhyItDidNot() = runTest {
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        assertEquals(AddressReading.Read("a: 1", "8842.json"), throughServer { respond("a: 1", HttpStatusCode.OK) }.open(address, 1_000))
        for ((answer, reading) in listOf(
            """{"error": "not-allowed"}""" to AddressReading.NotAllowed,
            """{"error": "too-large", "bytes": 25165824}""" to AddressReading.TooLarge(25_165_824),
            """{"error": "status", "status": 404}""" to AddressReading.Answered(404),
            """{"error": "unreachable"}""" to AddressReading.Unreachable,
            "<html>Bad gateway</html>" to AddressReading.ServerUnreachable
        )) {
            assertEquals(reading, throughServer { respond(answer, HttpStatusCode.BadGateway, json) }.open(address, 1_000), answer)
        }
        assertEquals(AddressReading.ServerUnreachable, throughServer { error("No route to host") }.open(address, 1_000))
    }
}
