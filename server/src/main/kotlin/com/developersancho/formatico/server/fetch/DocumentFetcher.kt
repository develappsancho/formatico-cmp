package com.developersancho.formatico.server.fetch

import com.developersancho.formatico.core.share.FetchError
import com.developersancho.formatico.core.share.ShareServer
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelOption
import io.netty.handler.codec.http.HttpHeaderNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import reactor.core.publisher.Mono
import reactor.netty.ByteBufFlux
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientResponse
import reactor.netty.resources.ConnectionProvider
import java.io.ByteArrayOutputStream
import java.net.URI
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** What came of a fetch: the [Document], or a [Failure] that says why there is none. */
sealed interface Fetched {
    /** The document: its [bytes], in the content [type] its address gave. */
    class Document(val bytes: ByteArray, val type: MediaType) : Fetched

    /** Why the fetch read no document: [error], as [ShareServer]'s answers name it. */
    sealed interface Failure : Fetched {
        val error: FetchError
    }

    /** The address is not a public `http` or `https` one, or it redirected to one that is not. */
    data object NotAllowed : Failure {
        override val error = FetchError.NotAllowed
    }

    /** The document is larger than [ShareServer.MAX_FETCH_BYTES]: [bytes] long, where its address said. */
    data class TooLarge(val bytes: Long?) : Failure {
        override val error = FetchError.TooLarge
    }

    /** The address answered with [status], which is no document's. */
    data class Status(val status: Int) : Failure {
        override val error = FetchError.Status
    }

    /** The address could not be reached: its name has no address, it refused the connection or did not answer in time. */
    data object Unreachable : Failure {
        override val error = FetchError.Unreachable
    }
}

/**
 * Fetches the document at a web address for the web app, whose browser may not read another site: only an address
 * [guard] allows, connected to only at the addresses [guard] allows (see [GuardedResolverGroup]), following at most
 * [MAX_REDIRECTS] redirects, each checked as the first address is, and reading at most [ShareServer.MAX_FETCH_BYTES].
 * It gives up on a connection after [connectTimeout], and on the whole fetch after [timeout].
 *
 * The request carries nothing of the user's: no cookie, no credential, and the User-Agent [USER_AGENT]. Neither the
 * address nor the document is ever logged.
 */
class DocumentFetcher(
    private val guard: FetchGuard,
    lookup: HostLookup = HostLookup.System,
    connectTimeout: Duration = CONNECT_TIMEOUT,
    private val timeout: Duration = TIMEOUT
) : AutoCloseable {
    private val resolver = GuardedResolverGroup(lookup, guard)

    private val client = HttpClient.create(ConnectionProvider.newConnection())
        .resolver(resolver)
        // The last word on every connection, made to an address written in the link too, which no resolver is asked about.
        .resolvedAddressesSelector { _, addresses -> guard.checked(addresses) }
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.inWholeMilliseconds.toInt())
        .followRedirect(false)
        .disableRetry(true)
        .headers { it.set(HttpHeaderNames.USER_AGENT, USER_AGENT) }

    /** The document at [url], or why there is none. */
    suspend fun fetch(url: String): Fetched {
        val target = guard.target(url) ?: return Fetched.NotAllowed
        return withTimeoutOrNull(timeout) { follow(target) } ?: Fetched.Unreachable
    }

    /** What [target] answers, after the redirects it gives, [followed] of which lead to it. */
    private suspend fun follow(target: URI, followed: Int = 0): Fetched = when (val answer = ask(target)) {
        is Answer.Read -> answer.fetched

        // One redirect more than a fetch follows is the address's last answer.
        is Answer.Redirect -> if (followed == MAX_REDIRECTS) Fetched.Status(answer.status) else redirected(target, answer, followed)
    }

    private suspend fun redirected(from: URI, redirect: Answer.Redirect, followed: Int): Fetched =
        when (val next = guard.redirect(from, redirect.location)) {
            is FetchGuard.Redirect.To -> follow(next.uri, followed + 1)
            FetchGuard.Redirect.Refused -> Fetched.NotAllowed
            FetchGuard.Redirect.Unreadable -> Fetched.Status(redirect.status)
        }

    // Netty, TLS and the resolver fail with every kind of exception; each is an address that could not be read.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun ask(target: URI): Answer = try {
        client.get().uri(target).response(::answer).awaitSingle()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failed: Exception) {
        Answer.Read(failureOf(failed))
    }

    private fun answer(response: HttpClientResponse, body: ByteBufFlux): Mono<Answer> {
        val status = response.status().code()
        val headers = response.responseHeaders()
        val location = headers.get(HttpHeaderNames.LOCATION)
        val declared = headers.get(HttpHeaderNames.CONTENT_LENGTH)?.toLongOrNull()
        return when {
            status in REDIRECTS && location != null -> Mono.just(Answer.Redirect(status, location))
            status !in SUCCESS -> Mono.just(Answer.Read(Fetched.Status(status)))
            declared != null && declared > ShareServer.MAX_FETCH_BYTES -> Mono.just(Answer.Read(Fetched.TooLarge(declared)))
            else -> document(body, declared, documentType(headers.get(HttpHeaderNames.CONTENT_TYPE)))
        }
    }

    /** The document [body] reads to, [declared] bytes long where its address said, until it turns out too large. */
    private fun document(body: ByteBufFlux, declared: Long?, type: MediaType): Mono<Answer> =
        body.collect({ Bounded(declared) }, Bounded::add).map { Answer.Read(Fetched.Document(it.bytes(), type)) }

    private fun failureOf(failed: Throwable): Fetched.Failure {
        val causes = generateSequence(failed) { it.cause }.take(MAX_CAUSES).toList()
        return when {
            causes.any { it is RefusedAddressException } -> Fetched.NotAllowed
            causes.any { it is TooLargeException } -> Fetched.TooLarge(null)
            else -> Fetched.Unreachable
        }
    }

    override fun close() {
        resolver.close()
    }

    /** What one request came to: a redirect to follow, or what the fetch read. */
    private sealed interface Answer {
        class Redirect(val status: Int, val location: String) : Answer

        class Read(val fetched: Fetched) : Answer
    }

    /** The body read so far, which throws a [TooLargeException] once it would hold more than a fetch reads. */
    private class Bounded(declared: Long?) {
        private val bytes = ByteArrayOutputStream(declared?.takeIf { it >= 0 }?.toInt() ?: INITIAL_BYTES)

        fun add(chunk: ByteBuf) {
            if (bytes.size().toLong() + chunk.readableBytes() > ShareServer.MAX_FETCH_BYTES) throw TooLargeException()
            chunk.readBytes(bytes, chunk.readableBytes())
        }

        fun bytes(): ByteArray = bytes.toByteArray()
    }

    private class TooLargeException : IllegalStateException("The document is larger than a fetch reads")

    companion object {
        /** The most redirects a fetch follows. */
        const val MAX_REDIRECTS = 5

        /** What the fetch's requests call themselves: nothing of the user's. */
        const val USER_AGENT = "Formatico"

        private val CONNECT_TIMEOUT = 5.seconds
        private val TIMEOUT = 15.seconds
        private const val INITIAL_BYTES = 64 * 1024
        private const val MAX_CAUSES = 16
        private val SUCCESS = 200..299
        private val REDIRECTS = setOf(301, 302, 303, 307, 308)

        /**
         * The content type of a document whose address said [header]: its type and subtype, and its charset where it
         * names one Java knows; `application/octet-stream` where it says none that can be read.
         */
        fun documentType(header: String?): MediaType {
            val parsed = header?.let { parsedType(it) ?: parsedType(it.substringBefore(';')) }
            if (parsed == null || parsed.isWildcardType || parsed.isWildcardSubtype) return MediaType.APPLICATION_OCTET_STREAM
            val charset = parsed.charset
            return if (charset != null) MediaType(parsed.type, parsed.subtype, charset) else MediaType(parsed.type, parsed.subtype)
        }

        private fun parsedType(header: String): MediaType? = try {
            MediaType.parseMediaType(header)
        } catch (_: InvalidMediaTypeException) {
            null
        }
    }
}
