package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.ShareKeep
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class FormaticoServerTest {
    @Test
    fun aShareIsSentAsItsBytesForTheDaysChosen() = runTest {
        var sent: Pair<String, ByteArray>? = null
        val server = FormaticoServer(
            clientOf { request ->
                sent = "${request.method.value} ${request.url}" to request.body.toByteArray()
                json("""{"id": "7Qm2xLp9fK", "deleteToken": "token", "expiresAt": "2026-11-13T20:30:00Z"}""", HttpStatusCode.Created)
            },
            SERVER
        )

        val kept = assertIs<Keeping.Kept>(server.keep(byteArrayOf(1, 2, 3), ShareKeep.Week))

        assertEquals(KeptShare("7Qm2xLp9fK", "token", Instant.parse("2026-11-13T20:30:00Z")), kept.share)
        // A key the server adds later is passed over.
        val later = FormaticoServer(
            clientOf {
                json(
                    """{"id": "7Qm2xLp9fK", "deleteToken": "t", "expiresAt": "2026-11-13T20:30:00Z", "region": "eu"}""",
                    HttpStatusCode.Created
                )
            },
            SERVER
        )
        assertIs<Keeping.Kept>(later.keep(byteArrayOf(1), ShareKeep.Week))
        assertEquals("POST $SERVER/api/shares?days=7", sent?.first)
        assertContentEquals(byteArrayOf(1, 2, 3), sent?.second)
    }

    @Test
    fun aShareTheServerWillNotKeepOrCannotTakeIsSaidSo() = runTest {
        for ((status, keeping) in listOf(
            HttpStatusCode.PayloadTooLarge to Keeping.Refused,
            HttpStatusCode.BadRequest to Keeping.Unreachable,
            HttpStatusCode.BadGateway to Keeping.Unreachable
        )) {
            val server = FormaticoServer(clientOf { respond("", status) }, SERVER)
            assertEquals(keeping, server.keep(byteArrayOf(1), ShareKeep.Day), "$status")
        }
        val down = FormaticoServer(clientOf { error("No route to host") }, SERVER)
        assertEquals(Keeping.Unreachable, down.keep(byteArrayOf(1), ShareKeep.Day))
        val garbled = FormaticoServer(clientOf { json("not json", HttpStatusCode.Created) }, SERVER)
        assertEquals(Keeping.Unreachable, garbled.keep(byteArrayOf(1), ShareKeep.Day))
    }

    @Test
    fun aShareIsFetchedAndDeletedWithItsToken() = runTest {
        val fake = FakeServer()
        fake.server.keep(byteArrayOf(9, 8, 7), ShareKeep.Month)

        assertContentEquals(byteArrayOf(9, 8, 7), assertIs<Fetching.Found>(fake.server.share("7Qm2xLp9fK")).bytes)
        assertEquals(Forgetting.Refused, fake.server.forget("7Qm2xLp9fK", "another"))
        assertEquals(Forgetting.Forgotten, fake.server.forget("7Qm2xLp9fK", "token"))
        assertEquals(Fetching.Gone, fake.server.share("7Qm2xLp9fK"))
        assertEquals(Forgetting.Gone, fake.server.forget("7Qm2xLp9fK", "token"))

        fake.reachable = false
        assertEquals(Fetching.Unreachable, fake.server.share("7Qm2xLp9fK"))
        assertEquals(Forgetting.Unreachable, fake.server.forget("7Qm2xLp9fK", "token"))
    }

    @Test
    fun theServerIsAskedForTheAddressItFetches() = runTest {
        var asked: String? = null
        val server = FormaticoServer(
            clientOf { request ->
                asked = request.url.toString()
                check(request.method == HttpMethod.Get)
                respond("{}", HttpStatusCode.OK)
            },
            SERVER
        )

        assertIs<Downloading.Bytes>(server.fetch("https://example.com/orders/8842.json?v=2"))
        assertEquals("$SERVER/api/fetch?url=https%3A%2F%2Fexample.com%2Forders%2F8842.json%3Fv%3D2", asked)
    }
}
