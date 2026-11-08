package com.developersancho.formatico.server.share

import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareServer
import kotlinx.serialization.Serializable
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.queryParamOrNull
import java.net.URI

/**
 * The shares' endpoints, as [ShareServer] sets them out: `POST /api/shares?days=N` keeps one, `GET /api/shares/{id}`
 * gives it back, `DELETE /api/shares/{id}` with its delete token deletes it. A share that is not there, has expired
 * or was deleted is one `404` for all three.
 */
class ShareHandler(private val store: ShareStore) {
    /** Keeps the request's body, as it is, for the days its `days` asks. */
    suspend fun keep(request: ServerRequest): ServerResponse {
        val days = request.queryParamOrNull(ShareServer.DAYS)?.toIntOrNull()
        val keep = ShareKeep.entries.firstOrNull { it.days == days } ?: return status(HttpStatus.BAD_REQUEST)
        val declared = request.headers().contentLength()
        if (declared.isPresent && declared.asLong > ShareServer.MAX_SHARE_BYTES) return status(HttpStatus.CONTENT_TOO_LARGE)
        return when (val kept = store.keep(request.bodyToFlux<DataBuffer>(), keep)) {
            is Keeping.Kept -> kept(kept.share)
            Keeping.Empty -> status(HttpStatus.BAD_REQUEST)
            Keeping.TooLarge -> status(HttpStatus.CONTENT_TOO_LARGE)
        }
    }

    /** The share the path names, still encrypted. */
    suspend fun read(request: ServerRequest): ServerResponse {
        val bytes = store.read(request.pathVariable(ID)) ?: return status(HttpStatus.NOT_FOUND)
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .cacheControl(CacheControl.noStore())
            .header(CONTENT_TYPE_OPTIONS, "nosniff")
            .bodyValueAndAwait(bytes)
    }

    /** Deletes the share the path names, with the token in [ShareServer.DELETE_TOKEN]. */
    suspend fun delete(request: ServerRequest): ServerResponse {
        val token = request.headers().firstHeader(ShareServer.DELETE_TOKEN)
        return when (store.delete(request.pathVariable(ID), token)) {
            Deleting.Deleted -> status(HttpStatus.NO_CONTENT)
            Deleting.Refused -> status(HttpStatus.FORBIDDEN)
            Deleting.Missing -> status(HttpStatus.NOT_FOUND)
        }
    }

    private suspend fun kept(share: KeptShare): ServerResponse = ServerResponse.created(URI.create("${ShareServer.SHARES}/${share.id}"))
        .contentType(MediaType.APPLICATION_JSON)
        .cacheControl(CacheControl.noStore())
        .bodyValueAndAwait(KeptAnswer(share.id, share.deleteToken, share.expiresAt.toString()))

    private suspend fun status(status: HttpStatus): ServerResponse = ServerResponse.status(status).buildAndAwait()

    /** A kept share, as the app reads it: its [id], its [deleteToken] and when it [expiresAt], in ISO-8601 and UTC. */
    @Serializable
    data class KeptAnswer(val id: String, val deleteToken: String, val expiresAt: String)

    companion object {
        /** The path variable that holds a share's id. */
        const val ID = "id"

        private const val CONTENT_TYPE_OPTIONS = "X-Content-Type-Options"
    }
}
