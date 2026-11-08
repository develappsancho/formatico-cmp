package com.developersancho.formatico.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.http.parseUrl
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray

/** What came of opening the document at a web address. */
sealed interface AddressReading {
    /** The document's text, and the [name] its address gives it. */
    data class Read(val text: String, val name: String?) : AddressReading

    /** The address is no public `http` or `https` one. */
    data object NotAllowed : AddressReading

    /** The address answered [status], which is no document's: 404 where there is none. */
    data class Answered(val status: Int) : AddressReading

    /** The document is larger than may be opened here: [bytes] where its address said. */
    data class TooLarge(val bytes: Long?) : AddressReading

    /** The document is no text: an image, an archive. */
    data object NotText : AddressReading

    /** The address could not be reached, or did not answer in time. */
    data object Unreachable : AddressReading

    /** On the web, Formatico's server, which fetches the document, could not be reached. */
    data object ServerUnreachable : AddressReading
}

/** A document downloaded, before it is read as text, or why none was. */
internal sealed interface Downloading {
    class Bytes(val bytes: ByteArray) : Downloading

    class Failed(val reading: AddressReading) : Downloading
}

/** Opens the documents at web addresses. */
fun interface WebAddresses {
    /** The document at [address], of [maxBytes] at most: the largest the platform opens. */
    suspend fun open(address: String, maxBytes: Long): AddressReading
}

/**
 * [WebAddresses] on HTTP: a document downloaded by the platform itself, or on the web through [server], since a
 * browser reads another site only where that site lets it.
 */
internal class HttpWebAddresses(
    private val client: HttpClient,
    private val server: FormaticoServer,
    private val itself: Boolean = downloadsItself
) : WebAddresses {
    override suspend fun open(address: String, maxBytes: Long): AddressReading {
        val url = parseUrl(address.trim())?.takeIf { it.host.isNotEmpty() && it.protocol in WebProtocols }
            ?: return AddressReading.NotAllowed
        val bytes = when (val downloaded = if (itself) download(url, maxBytes) else server.fetch(url.toString())) {
            is Downloading.Bytes -> downloaded.bytes
            is Downloading.Failed -> return downloaded.reading
        }
        if (bytes.size > maxBytes) return AddressReading.TooLarge(bytes.size.toLong())
        val text = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
        return text?.let { AddressReading.Read(it, nameOf(url)) } ?: AddressReading.NotText
    }

    private suspend fun download(url: Url, maxBytes: Long): Downloading =
        reaching(Downloading.Failed(AddressReading.Unreachable)) {
            client.prepareGet(url).execute { response ->
                val length = response.contentLength()
                when {
                    !response.status.isSuccess() -> Downloading.Failed(AddressReading.Answered(response.status.value))

                    length != null && length > maxBytes -> Downloading.Failed(AddressReading.TooLarge(length))

                    else -> {
                        // One byte past the most tells a document too large from one just as large.
                        val bytes = response.bodyAsChannel().readRemaining(maxBytes + 1).readByteArray()
                        if (bytes.size > maxBytes) Downloading.Failed(AddressReading.TooLarge(null)) else Downloading.Bytes(bytes)
                    }
                }
            }
        }

    private companion object {
        val WebProtocols = setOf(URLProtocol.HTTP, URLProtocol.HTTPS)
    }
}

/** The name of the document at [url]: its last part, where it has one. */
internal fun nameOf(url: Url): String? = url.segments.lastOrNull { it.isNotBlank() }
