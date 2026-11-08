package com.developersancho.formatico.server.share

import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareServer
import com.developersancho.formatico.server.MutableClock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * What the server keeps at the most, over HTTP: two shares a day from an address here, as nginx names it, and 10 KB of
 * shares together. Each test keeps its shares from addresses of its own, and deletes them.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShareLimitsApiTest {
    @LocalServerPort
    private var port = 0

    /** The server's clock: [time], which the tests move. */
    @TestBean
    lateinit var clock: Clock

    private lateinit var client: WebTestClient

    @BeforeTest
    fun connect() {
        time.now = START
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
    }

    @Test
    fun keepsTwoSharesADayFromAnAddressAndSaysWhenItTakesMore() {
        val kept = listOf(keep(from = "203.0.113.7").expectStatus().isCreated, keep(from = "203.0.113.7").expectStatus().isCreated)
        keep(from = "203.0.113.7")
            .expectStatus().isEqualTo(TOO_MANY)
            .expectHeader().valueEquals(HttpHeaders.RETRY_AFTER, Duration.ofHours(12).seconds.toString())
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody<String>().isEqualTo("""{"error":"daily-limit","limit":2}""")
        keep(from = "203.0.113.8").expectStatus().isCreated.also(::forget)
        kept.forEach(::forget)

        time.advance(Duration.ofHours(12))
        keep(from = "203.0.113.7").expectStatus().isCreated.also(::forget)
    }

    @Test
    fun countsNoShareItDidNotKeep() {
        repeat(3) {
            client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, ShareKeep.Day.days).build() }
                .header(REAL_IP, "203.0.113.20")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(ByteArray(0))
                .exchange()
                .expectStatus().isBadRequest
        }
        keep(from = "203.0.113.20").expectStatus().isCreated.also(::forget)
        keep(from = "203.0.113.20").expectStatus().isCreated.also(::forget)
    }

    @Test
    fun keepsNoShareOnceTheSharesHoldTheMost() {
        val first = keep(from = "203.0.113.30", bytes = SHARE).expectStatus().isCreated
        val second = keep(from = "203.0.113.31", bytes = SHARE).expectStatus().isCreated
        keep(from = "203.0.113.32", bytes = SHARE).expectStatus().isEqualTo(INSUFFICIENT_STORAGE)
        // The refused share was not counted against its address either.
        forget(first)
        keep(from = "203.0.113.32", bytes = SHARE).expectStatus().isCreated.also(::forget)
        keep(from = "203.0.113.32", bytes = SHARE).expectStatus().isCreated.also(::forget)
        forget(second)
    }

    private fun keep(from: String, bytes: Int = 1_024): WebTestClient.ResponseSpec =
        client.post().uri { it.path(ShareServer.SHARES).queryParam(ShareServer.DAYS, ShareKeep.Day.days).build() }
            .header(REAL_IP, from)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .bodyValue(ByteArray(bytes) { it.toByte() })
            .exchange()

    /** Deletes the share [created] answered for, so that the next test has the room. */
    private fun forget(created: WebTestClient.ResponseSpec) {
        val answer = Json.parseToJsonElement(created.expectBody<String>().returnResult().responseBody.orEmpty()).jsonObject
        val id = answer.getValue("id").jsonPrimitive.content
        client.delete().uri("${ShareServer.SHARES}/$id")
            .header(ShareServer.DELETE_TOKEN, answer.getValue("deleteToken").jsonPrimitive.content)
            .exchange()
            .expectStatus().isNoContent
    }

    companion object {
        private const val REAL_IP = "X-Real-IP"
        private const val TOO_MANY = 429
        private const val INSUFFICIENT_STORAGE = 507

        /** A share of 4 KB: two of them fit in the 10 KB the server keeps here, and a third does not. */
        private const val SHARE = 4 * 1_024
        private val START: Instant = Instant.parse("2026-11-07T12:00:00Z")
        private val time = MutableClock(START)
        private val shares: Path = Files.createTempDirectory("formatico-limits")

        /** The clock [TestBean] puts in the server's place: the test moves it. */
        @JvmStatic
        fun clock(): Clock = time

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("formatico.shares.directory") { shares.toString() }
            registry.add("formatico.shares.per-address-per-day") { "2" }
            registry.add("formatico.shares.max-total") { "10KB" }
        }

        @JvmStatic
        @AfterAll
        fun deleteShares() {
            shares.toFile().deleteRecursively()
        }
    }
}
