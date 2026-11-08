package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.ShareServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** The address the tests' server answers at. */
internal const val SERVER = "https://formatico.test"

/** A client whose every request [answer] answers, as Formatico's client reads answers: of any status. */
internal fun clientOf(answer: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
    HttpClient(MockEngine { answer(it) }) { expectSuccess = false }

/** A JSON answer of [status]. */
internal fun MockRequestHandleScope.json(text: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(text, status, headersOf(HttpHeaders.ContentType, "application/json"))

/**
 * Formatico's server as the tests need it: it keeps what is posted under one id, gives it back and deletes it with
 * its token, and keeps the bytes it was given, to show what it can see.
 */
internal class FakeServer {
    var kept: ByteArray? = null
    var reachable = true

    val client = clientOf { request ->
        check(reachable) { "No route to host" }
        val path = request.url.encodedPath
        when {
            request.method == HttpMethod.Post && path == ShareServer.SHARES -> {
                kept = request.body.toByteArray()
                json("""{"id": "7Qm2xLp9fK", "deleteToken": "token", "expiresAt": "2026-11-13T20:30:00Z"}""", HttpStatusCode.Created)
            }

            request.method == HttpMethod.Get && path == "${ShareServer.SHARES}/7Qm2xLp9fK" ->
                kept?.let { respond(it, HttpStatusCode.OK) } ?: respond("", HttpStatusCode.NotFound)

            request.method == HttpMethod.Delete && path == "${ShareServer.SHARES}/7Qm2xLp9fK" ->
                if (kept == null) {
                    respond("", HttpStatusCode.NotFound)
                } else if (request.headers[ShareServer.DELETE_TOKEN] == "token") {
                    kept = null
                    respond("", HttpStatusCode.NoContent)
                } else {
                    respond("", HttpStatusCode.Forbidden)
                }

            else -> respond("", HttpStatusCode.NotFound)
        }
    }

    val server = FormaticoServer(client, SERVER)
}
