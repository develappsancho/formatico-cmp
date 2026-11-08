package com.developersancho.formatico.server.share

import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareServer
import com.developersancho.formatico.server.FormaticoProperties
import com.developersancho.formatico.server.MutableClock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The shares over HTTP, as the app asks for them: kept, read back and deleted, and every way each is refused. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShareApiTest {
    @LocalServerPort
    private var port = 0

    /** The server's clock: [time], which the tests move. */
    @TestBean
    lateinit var clock: Clock

    @Autowired
    private lateinit var cleanup: ShareCleanup

    @Autowired
    private lateinit var properties: FormaticoProperties

    private lateinit var client: WebTestClient

    private val encrypted = ByteArray(4_096) { (it * 31).toByte() }

    @BeforeTest
    fun connect() {
        time.now = START
        client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .codecs { it.defaultCodecs().maxInMemorySize(ShareServer.MAX_SHARE_BYTES) }
            .build()
    }

    @Test
    fun keepsReadsAndDeletesAShare() {
        val kept = keep(encrypted, ShareKeep.Week)
        assertTrue(ShareServer.Id.matches(kept.id), kept.id)
        assertEquals(TOKEN_BYTES, Base64.getUrlDecoder().decode(kept.deleteToken).size)
        assertFalse(kept.deleteToken.endsWith("="))
        assertEquals(START.plus(Duration.ofDays(7)), Instant.parse(kept.expiresAt))

        val read = client.get().uri("${ShareServer.SHARES}/${kept.id}").exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .expectHeader().cacheControl(CacheControl.noStore())
            .expectBody<ByteArray>().returnResult().responseBody
        assertContentEquals(encrypted, read)

        delete(kept.id, kept.deleteToken).expectStatus().isNoContent
        client.get().uri("${ShareServer.SHARES}/${kept.id}").exchange().expectStatus().isNotFound
        delete(kept.id, kept.deleteToken).expectStatus().isNotFound
        assertFalse(Files.exists(shares.resolve(kept.id)))
    }

    @Test
    fun answersWithExactlyTheFieldsTheAppReads() {
        val body = client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, 1).build() }
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(encrypted)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<String>().returnResult().responseBody.orEmpty()
        val answer = Json.parseToJsonElement(body).jsonObject
        assertEquals(setOf("id", "deleteToken", "expiresAt"), answer.keys)
        assertEquals("2026-11-07T12:00:00Z", answer.getValue("expiresAt").jsonPrimitive.content)
    }

    @Test
    fun keepsAShareForEachOfTheDaysTheAppOffers() {
        for (keep in ShareKeep.entries) {
            assertEquals(START.plus(Duration.ofDays(keep.days.toLong())), Instant.parse(keep(encrypted, keep).expiresAt), keep.name)
        }
    }

    @Test
    fun refusesDaysItDoesNotKeepAShareFor() {
        for (days in listOf(null, "", "0", "2", "-7", "31", "365", "seven", "7.0")) {
            client.post().uri { builder ->
                builder.path(ShareServer.SHARES).also {
                    if (days !=
                        null
                    ) {
                        it.queryParam(ShareServer.DAYS, days)
                    }
                }.build()
            }
                .bodyValue(encrypted)
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Test
    fun refusesAShareWithNoBody() {
        client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, 7).build() }.exchange().expectStatus().isBadRequest
        client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, 7).build() }
            .bodyValue(ByteArray(0))
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun refusesAShareTooLargeBeforeReadingIt() {
        // Expecting 100-continue, the client sends the body only if the server asks for it: a 413 comes first.
        val request = HttpRequest.newBuilder(URI("http://localhost:$port${ShareServer.SHARES}?${ShareServer.DAYS}=7"))
            .expectContinue(true)
            .POST(HttpRequest.BodyPublishers.ofByteArray(ByteArray(ShareServer.MAX_SHARE_BYTES + 1)))
            .build()
        val response = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build().use {
            it.send(request, HttpResponse.BodyHandlers.discarding())
        }
        assertEquals(HttpStatus.CONTENT_TOO_LARGE.value(), response.statusCode())
    }

    @Test
    fun keepsTheLargestShare() {
        val largest = ByteArray(ShareServer.MAX_SHARE_BYTES) { (it % 253).toByte() }
        val kept = keep(largest, ShareKeep.Day)
        val read = client.get().uri("${ShareServer.SHARES}/${kept.id}").exchange()
            .expectStatus().isOk
            .expectBody<ByteArray>().returnResult().responseBody
        assertContentEquals(largest, read)
    }

    @Test
    fun answersOneNotFoundForEveryShareThatIsNotThere() {
        val unknown = "Zz9Zz9Zz9Z"
        val malformed = listOf("abc", "ABCDEFGHIJK", "abcdefghi!", "abcde-ghij", ".incoming", "%2E%2E%2Fetc")
        for (id in listOf(unknown) + malformed) {
            client.get().uri("${ShareServer.SHARES}/$id").exchange().expectStatus().isNotFound
            delete(id, "token").expectStatus().isNotFound
        }
    }

    @Test
    fun answersNotFoundForAShareThatHasExpiredAndDeletesIt() {
        val kept = keep(encrypted, ShareKeep.Day)
        time.advance(Duration.ofHours(23))
        client.get().uri("${ShareServer.SHARES}/${kept.id}").exchange().expectStatus().isOk
        time.advance(Duration.ofHours(1))
        client.get().uri("${ShareServer.SHARES}/${kept.id}").exchange().expectStatus().isNotFound
        assertFalse(Files.exists(shares.resolve(kept.id)))
        delete(kept.id, kept.deleteToken).expectStatus().isNotFound
    }

    @Test
    fun refusesToDeleteAShareWithAnotherToken() {
        val kept = keep(encrypted, ShareKeep.Week)
        val other = keep(encrypted, ShareKeep.Week)
        delete(kept.id, other.deleteToken).expectStatus().isForbidden
        delete(kept.id, kept.deleteToken.dropLast(1)).expectStatus().isForbidden
        delete(kept.id, "").expectStatus().isForbidden
        client.delete().uri("${ShareServer.SHARES}/${kept.id}").exchange().expectStatus().isForbidden
        client.get().uri("${ShareServer.SHARES}/${kept.id}").exchange().expectStatus().isOk
    }

    @Test
    fun deletesTheSharesThatHaveExpired() {
        val day = keep(encrypted, ShareKeep.Day)
        val month = keep(encrypted, ShareKeep.Month)
        time.advance(Duration.ofDays(2))
        cleanup.deleteExpired()
        assertFalse(Files.exists(shares.resolve(day.id)))
        assertTrue(Files.exists(shares.resolve(month.id)))
        client.get().uri("${ShareServer.SHARES}/${month.id}").exchange().expectStatus().isOk
    }

    @Test
    fun storesTheTokensHashAndNeverTheToken() {
        val kept = keep(encrypted, ShareKeep.Week)
        val folder = shares.resolve(kept.id)
        val stored = Files.list(folder).use { files -> files.toList() }.joinToString("\n") { Files.readString(it, Charsets.ISO_8859_1) }
        assertFalse(stored.contains(kept.deleteToken))
    }

    /**
     * The web app's own calls, as nginx hands them on: the browser says the site's origin, and the server, reached over
     * http on its own port, sees another one. Without the site in `formatico.allowed-origins` each is refused (403).
     */
    @Test
    fun takesTheWebAppsCallsFromTheSitesOrigin() {
        val kept = client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, ShareKeep.Day.days).build() }
            .header(HttpHeaders.ORIGIN, SITE)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(encrypted)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SITE)
            .expectBody<String>().returnResult().responseBody.orEmpty()
        val answer = Json.parseToJsonElement(kept).jsonObject
        client.delete().uri("${ShareServer.SHARES}/${answer.getValue("id").jsonPrimitive.content}")
            .header(HttpHeaders.ORIGIN, SITE)
            .header(ShareServer.DELETE_TOKEN, answer.getValue("deleteToken").jsonPrimitive.content)
            .exchange()
            .expectStatus().isNoContent
        client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, ShareKeep.Day.days).build() }
            .header(HttpHeaders.ORIGIN, "http://localhost:4005")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(encrypted)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun letsTheSiteCallTheApiFromAnotherOrigin() {
        client.options().uri("${ShareServer.SHARES}/Zz9Zz9Zz9Z")
            .header(HttpHeaders.ORIGIN, SITE)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, ShareServer.DELETE_TOKEN)
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SITE)
        client.options().uri(ShareServer.SHARES)
            .header(HttpHeaders.ORIGIN, SITE)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.CONTENT_TYPE)
            .exchange()
            .expectStatus().isOk
        client.options().uri(ShareServer.SHARES)
            .header(HttpHeaders.ORIGIN, "https://elsewhere.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .exchange()
            .expectStatus().isForbidden
        client.options().uri(ShareServer.SHARES)
            .header(HttpHeaders.ORIGIN, SITE)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun saysItIsUp() {
        client.get().uri(ShareServer.HEALTH).exchange()
            .expectStatus().isOk
            .expectBody<String>().isEqualTo("ok")
    }

    @Test
    fun reachesNoAddressOfItsOwnInItsOwnSettings() {
        assertFalse(properties.fetch.allowPrivate)
        for (url in listOf(
            "http://127.0.0.1:$port${ShareServer.HEALTH}",
            "http://localhost/",
            "http://169.254.169.254/latest/meta-data/"
        )) {
            client.get().uri { it.path(ShareServer.FETCH).queryParam(ShareServer.URL, "{url}").build(url) }.exchange()
                .expectStatus().isBadRequest
                .expectBody<String>().isEqualTo("""{"error":"not-allowed"}""")
        }
    }

    private fun keep(bytes: ByteArray, keep: ShareKeep): Kept {
        val body = client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, keep.days).build() }
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(bytes)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().cacheControl(CacheControl.noStore())
            .expectBody<String>().returnResult().responseBody.orEmpty()
        val answer = Json.parseToJsonElement(body).jsonObject
        return Kept(
            answer.getValue("id").jsonPrimitive.content,
            answer.getValue("deleteToken").jsonPrimitive.content,
            answer.getValue("expiresAt").jsonPrimitive.content
        )
    }

    private fun delete(id: String, token: String): WebTestClient.ResponseSpec =
        client.delete().uri("${ShareServer.SHARES}/$id").header(ShareServer.DELETE_TOKEN, token).exchange()

    private class Kept(val id: String, val deleteToken: String, val expiresAt: String)

    companion object {
        private const val SITE = "https://formatico.pantrixui.com"
        private const val TOKEN_BYTES = 32
        private val START: Instant = Instant.parse("2026-11-06T12:00:00Z")
        private val time = MutableClock(START)
        private val shares: Path = Files.createTempDirectory("formatico-shares")

        /** The clock [TestBean] puts in the server's place: the test moves it. */
        @JvmStatic
        fun clock(): Clock = time

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
