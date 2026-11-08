package com.developersancho.formatico.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.URLProtocol
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parseUrl
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

/** Reads the pictures at web addresses, for the thumbnails of the values that are their addresses. */
fun interface WebPictures {
    /**
     * The bytes of the picture at [address], of [maxBytes] at most, or null: for an address that is no `https` one,
     * one that could not be reached or answered no picture, and a picture larger than [maxBytes].
     */
    suspend fun read(address: String, maxBytes: Long): ByteArray?
}

/**
 * [WebPictures] on HTTP, always by the platform itself: a picture is never fetched through Formatico's server, which
 * would learn the addresses a document holds. On the web the page reaches a picture's site over https
 * (`deploy/web/headers.conf`, `connect-src https:`), and a browser hands it the picture only where that site lets a page
 * read it (CORS); a picture of any other site keeps its icon.
 */
internal class HttpWebPictures(private val client: HttpClient) : WebPictures {
    override suspend fun read(address: String, maxBytes: Long): ByteArray? {
        val url = parseUrl(address.trim())?.takeIf { it.host.isNotEmpty() && it.protocol == URLProtocol.HTTPS } ?: return null
        return reaching(null) {
            client.prepareGet(url).execute { response ->
                val length = response.contentLength()
                val type = response.contentType()
                when {
                    !response.status.isSuccess() -> null

                    length != null && length > maxBytes -> null

                    // An answer that says what it holds and holds no picture is none: a page that stands in for one.
                    type != null && !type.match(ContentType.Image.Any) -> null

                    else -> {
                        // One byte past the most tells a picture too large from one just as large.
                        val bytes = response.bodyAsChannel().readBuffer(maxBytes + 1).readByteArray()
                        bytes.takeIf { it.size <= maxBytes }
                    }
                }
            }
        }
    }
}
