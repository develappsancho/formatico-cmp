package com.developersancho.formatico.server.fetch

import com.developersancho.formatico.core.share.ShareServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * `GET /api/fetch` over HTTP, with `formatico.fetch.allow-private` on so that it reaches a server on this machine:
 * the document and its headers, and each refusal in the JSON the app reads.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["formatico.fetch.allow-private=true"])
class FetchApiTest {
    @LocalServerPort
    private var port = 0

    private val origin = MockWebServer()
    private val loopback = InetAddress.getByName("127.0.0.1")
    private lateinit var client: WebTestClient

    @BeforeTest
    fun start() {
        origin.start(loopback, 0)
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @AfterTest
    fun stop() {
        origin.close()
    }

    @Test
    fun answersWithTheDocumentInItsContentType() {
        origin.enqueue(
            MockResponse.Builder().code(200).setHeader("Content-Type", "text/csv; header=present; charset=utf-8").body("a,b\n1,2").build()
        )
        val body = client.get().uri { it.path(ShareServer.FETCH).queryParam(ShareServer.URL, "{url}").build(originUrl("/table.csv")) }
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType("text/csv;charset=UTF-8")
            .expectHeader().cacheControl(CacheControl.noStore())
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
            .expectHeader().valueEquals("Content-Security-Policy", "sandbox; default-src 'none'")
            .expectHeader().valueEquals("Content-Disposition", "attachment")
            .expectBody<ByteArray>().returnResult().responseBody
        assertContentEquals("a,b\n1,2".encodeToByteArray(), body)
    }

    @Test
    fun neverServesAPageAsOneOfFormaticosOwn() {
        origin.enqueue(MockResponse.Builder().code(200).setHeader("Content-Type", "text/html").body("<script>alert(1)</script>").build())
        client.get().uri { it.path(ShareServer.FETCH).queryParam(ShareServer.URL, "{url}").build(originUrl("/page.html")) }
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("Content-Security-Policy", "sandbox; default-src 'none'")
            .expectHeader().valueEquals("Content-Disposition", "attachment")
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
    }

    @Test
    fun refusesAnAddressTheFetchMayNotAsk() {
        assertRefusal(fetch("ftp://127.0.0.1/data.json"), HttpStatus.BAD_REQUEST, "error" to "not-allowed")
        assertRefusal(fetch("not an address"), HttpStatus.BAD_REQUEST, "error" to "not-allowed")
        assertRefusal(client.get().uri(ShareServer.FETCH).exchange(), HttpStatus.BAD_REQUEST, "error" to "not-allowed")
    }

    @Test
    fun refusesADocumentTooLargeWithTheBytesItsAddressSaid() {
        val bytes = ShareServer.MAX_FETCH_BYTES + 1
        origin.enqueue(MockResponse.Builder().code(200).body(Buffer().write(ByteArray(bytes))).build())
        assertRefusal(fetch(originUrl("/huge")), HttpStatus.CONTENT_TOO_LARGE, "error" to "too-large", "bytes" to bytes)
    }

    @Test
    fun refusesADocumentThatTurnsOutTooLargeWithoutItsBytes() {
        val bytes = ByteArray(ShareServer.MAX_FETCH_BYTES + 1)
        origin.enqueue(MockResponse.Builder().code(200).chunkedBody(Buffer().write(bytes), 256 * 1024).build())
        assertRefusal(fetch(originUrl("/endless")), HttpStatus.CONTENT_TOO_LARGE, "error" to "too-large")
    }

    @Test
    fun answersWithTheStatusOfAnAddressThatGaveNoDocument() {
        origin.enqueue(MockResponse.Builder().code(404).body("missing").build())
        assertRefusal(fetch(originUrl("/missing")), HttpStatus.BAD_GATEWAY, "error" to "status", "status" to 404)
        origin.enqueue(MockResponse.Builder().code(503).build())
        assertRefusal(fetch(originUrl("/down")), HttpStatus.BAD_GATEWAY, "error" to "status", "status" to 503)
    }

    @Test
    fun answersAnAddressThatCannotBeReachedAsUnreachable() {
        val closed = ServerSocket(0, 1, loopback).use { it.localPort }
        assertRefusal(fetch("http://127.0.0.1:$closed/"), HttpStatus.BAD_GATEWAY, "error" to "unreachable")
    }

    private fun fetch(url: String): WebTestClient.ResponseSpec =
        client.get().uri { it.path(ShareServer.FETCH).queryParam(ShareServer.URL, "{url}").build(url) }.exchange()

    private fun originUrl(path: String) = "http://127.0.0.1:${origin.port}$path"

    private fun assertRefusal(response: WebTestClient.ResponseSpec, status: HttpStatus, vararg fields: Pair<String, Any>) {
        val body = response.expectStatus().isEqualTo(status)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<String>().returnResult().responseBody.orEmpty()
        val expected = JsonObject(
            fields.associate { (name, value) -> name to if (value is Number) JsonPrimitive(value) else JsonPrimitive(value.toString()) }
        )
        // Exactly these fields: the app reads the answer with a Json that takes no other.
        assertEquals(expected, Json.parseToJsonElement(body))
    }

    companion object {
        private val shares: Path = Files.createTempDirectory("formatico-fetch-shares")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("formatico.shares.directory") { shares.toString() }
        }

        @JvmStatic
        @AfterAll
        fun deleteShares() {
            shares.toFile().deleteRecursively()
        }
    }
}
