package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.FetchError
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareServer
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** A share the server keeps: its [id], the [deleteToken] that takes it away, and the moment it [expiresAt]. */
data class KeptShare(val id: String, val deleteToken: String, val expiresAt: Instant)

/** What came of asking the server to keep a share. */
sealed interface Keeping {
    /** The server keeps it as [share]. */
    data class Kept(val share: KeptShare) : Keeping

    /** The server would not keep it: larger than it keeps (413). */
    data object Refused : Keeping

    /** The server takes no share now: the room it keeps for them is full (507), or too many came at once (429). */
    data object Busy : Keeping

    /** The address this comes from has left the [limit] of shares the server keeps from one a day (429). */
    data class DailyLimit(val limit: Int) : Keeping

    /** The server could not be reached, or did not answer as it does. */
    data object Unreachable : Keeping
}

/** What came of asking the server for a share. */
sealed interface Fetching {
    /** The share, still encrypted, as [bytes]. */
    class Found(val bytes: ByteArray) : Fetching

    /** The server has no such share: it expired, or was deleted, which it does not tell apart. */
    data object Gone : Fetching

    /** The server could not be reached. */
    data object Unreachable : Fetching
}

/** What came of asking the server to delete a share. */
enum class Forgetting {
    /** It is deleted. */
    Forgotten,

    /** It was gone already. */
    Gone,

    /** The token was not the share's. */
    Refused,

    /** The server could not be reached. */
    Unreachable
}

/**
 * Formatico's server, at [address], as [ShareServer] sets it out: it keeps the shares, encrypted, and on the web
 * fetches the documents of web addresses.
 */
class FormaticoServer(private val client: HttpClient, private val address: String) {
    /** Asks the server to keep [encrypted] for [keep]. */
    suspend fun keep(encrypted: ByteArray, keep: ShareKeep): Keeping = reaching(Keeping.Unreachable) {
        val response = client.post(address + ShareServer.SHARES) {
            parameter(ShareServer.DAYS, keep.days)
            contentType(ContentType.Application.OctetStream)
            setBody(encrypted)
        }
        when (response.status) {
            HttpStatusCode.Created -> Keeping.Kept(Answers.decodeFromString<KeptAnswer>(response.bodyAsText()).share())

            HttpStatusCode.PayloadTooLarge -> Keeping.Refused

            HttpStatusCode.InsufficientStorage -> Keeping.Busy

            // The server's own says the day's limit; nginx's, of too many at once, has no body the app reads.
            HttpStatusCode.TooManyRequests -> limitOf(response.bodyAsText())?.let(Keeping::DailyLimit) ?: Keeping.Busy

            else -> Keeping.Unreachable
        }
    }

    /** The share [id], still encrypted. */
    suspend fun share(id: String): Fetching = reaching(Fetching.Unreachable) {
        val response = client.get("$address${ShareServer.SHARES}/$id")
        when {
            response.status.isSuccess() -> Fetching.Found(response.readRawBytes())
            response.status == HttpStatusCode.NotFound -> Fetching.Gone
            else -> Fetching.Unreachable
        }
    }

    /** Deletes the share [id], with the [token] the server gave when it kept it. */
    suspend fun forget(id: String, token: String): Forgetting = reaching(Forgetting.Unreachable) {
        val response = client.delete("$address${ShareServer.SHARES}/$id") { header(ShareServer.DELETE_TOKEN, token) }
        when (response.status) {
            HttpStatusCode.NoContent, HttpStatusCode.OK -> Forgetting.Forgotten
            HttpStatusCode.NotFound -> Forgetting.Gone
            HttpStatusCode.Forbidden -> Forgetting.Refused
            else -> Forgetting.Unreachable
        }
    }

    /** The document at [url], as the server fetches it for a browser, which may not read another site itself. */
    internal suspend fun fetch(url: String): Downloading = reaching(Downloading.Failed(AddressReading.ServerUnreachable)) {
        val response = client.get(address + ShareServer.FETCH) { parameter(ShareServer.URL, url) }
        if (response.status.isSuccess()) {
            Downloading.Bytes(response.readRawBytes())
        } else {
            val refusal = runCatching { Answers.decodeFromString<FetchAnswer>(response.bodyAsText()) }.getOrNull()
            Downloading.Failed(
                when (refusal?.error) {
                    FetchError.NotAllowed.code -> AddressReading.NotAllowed
                    FetchError.TooLarge.code -> AddressReading.TooLarge(refusal.bytes)
                    FetchError.Status.code -> AddressReading.Answered(refusal.status ?: response.status.value)
                    FetchError.Unreachable.code -> AddressReading.Unreachable
                    else -> AddressReading.ServerUnreachable
                }
            )
        }
    }

    @Serializable
    private class KeptAnswer(val id: String, val deleteToken: String, val expiresAt: String) {
        fun share() = KeptShare(id, deleteToken, Instant.parse(expiresAt))
    }

    /** The day's limit a `429` gives, where it is the server's own: [ShareServer.DAILY_LIMIT]. */
    private fun limitOf(body: String): Int? =
        runCatching { Answers.decodeFromString<LimitAnswer>(body) }.getOrNull()?.takeIf { it.error == ShareServer.DAILY_LIMIT }?.limit

    @Serializable
    private class FetchAnswer(val error: String, val status: Int? = null, val bytes: Long? = null)

    @Serializable
    private class LimitAnswer(val error: String, val limit: Int)

    private companion object {
        /** The server's answers, read as far as the app knows them: a key it adds later is passed over. */
        val Answers = Json { ignoreUnknownKeys = true }
    }
}

/**
 * [ask]'s answer, or [unreachable] where the network failed it: each platform fails its own way (an IOException on
 * the JVM, an NSError on iOS, a JavaScript error, which is no Exception in Wasm), and an answer the app cannot read is
 * one it treats as none.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
internal suspend fun <T> reaching(unreachable: T, ask: suspend () -> T): T = try {
    ask()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failed: Throwable) {
    unreachable
}
