package com.developersancho.formatico.server.fetch

import com.developersancho.formatico.core.share.FetchError
import com.developersancho.formatico.core.share.ShareServer
import kotlinx.serialization.Serializable
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.queryParamOrNull

/**
 * `GET /api/fetch?url=…`: the document at `url`, fetched by [fetcher], or one of [FetchError]'s names in `{"error": …}`,
 * with the address's `status` or the document's `bytes` where they say why.
 */
class FetchHandler(private val fetcher: DocumentFetcher) {
    /** Answers a fetch. */
    suspend fun fetch(request: ServerRequest): ServerResponse {
        val url = request.queryParamOrNull(ShareServer.URL) ?: return refusal(Fetched.NotAllowed)
        return when (val fetched = fetcher.fetch(url)) {
            is Fetched.Document -> document(fetched)
            is Fetched.Failure -> refusal(fetched)
        }
    }

    // The document is someone else's, served from Formatico's own origin: a browser sent to this address downloads
    // it, sandboxed, and never runs it as a page of Formatico's.
    private suspend fun document(document: Fetched.Document): ServerResponse = ServerResponse.ok()
        .contentType(document.type)
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().build().toString())
        .header(CONTENT_SECURITY_POLICY, "sandbox; default-src 'none'")
        .header(CONTENT_TYPE_OPTIONS, "nosniff")
        .header(RESOURCE_POLICY, "same-origin")
        .bodyValueAndAwait(document.bytes)

    private suspend fun refusal(failure: Fetched.Failure): ServerResponse {
        val (status, answer) = when (failure) {
            Fetched.NotAllowed -> HttpStatus.BAD_REQUEST to FetchRefusal(failure.error.code)
            is Fetched.TooLarge -> HttpStatus.CONTENT_TOO_LARGE to FetchRefusal(failure.error.code, bytes = failure.bytes)
            is Fetched.Status -> HttpStatus.BAD_GATEWAY to FetchRefusal(failure.error.code, status = failure.status)
            Fetched.Unreachable -> HttpStatus.BAD_GATEWAY to FetchRefusal(failure.error.code)
        }
        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .cacheControl(CacheControl.noStore())
            .bodyValueAndAwait(answer)
    }

    /** A fetch's refusal, as the app reads it: the [error]'s name, and the address's [status] or the document's [bytes]. */
    @Serializable
    data class FetchRefusal(val error: String, val status: Int? = null, val bytes: Long? = null)

    private companion object {
        const val CONTENT_SECURITY_POLICY = "Content-Security-Policy"
        const val CONTENT_TYPE_OPTIONS = "X-Content-Type-Options"
        const val RESOURCE_POLICY = "Cross-Origin-Resource-Policy"
    }
}
