package com.developersancho.formatico.core.share

/**
 * What the app and Formatico's server agree on (Faz 16): the server keeps encrypted shares, which it cannot read, and
 * fetches a document from an address for the web, whose browser may not read another site.
 *
 * - `POST /api/shares?days=7`, the encrypted share as its body (`application/octet-stream`): `201`, and
 *   `{"id": …, "deleteToken": …, "expiresAt": …}`; `400` for days the server does not keep a share for, or no body;
 *   `413` for a body over [MAX_SHARE_BYTES]; `429` and `{"error": "daily-limit", "limit": 50}` ([DAILY_LIMIT]) once
 *   the address it comes from has left as many shares today, until midnight UTC (`Retry-After`); `507` while the room
 *   the server keeps for shares is full. A `429` without that body is nginx's: too many at once.
 * - `GET /api/shares/{id}`: `200` and the encrypted share; `404` for a share that is not there, has expired or was
 *   deleted, which the server does not tell apart.
 * - `DELETE /api/shares/{id}` with its delete token in [DELETE_TOKEN]: `204`; `403` for another token; `404` as above.
 * - `GET /api/fetch?url=…`: `200` and the document, in the content type its address gave; otherwise one of
 *   [FetchError]'s names in `{"error": …}`, with the address's `status` or the document's `bytes` where they say why.
 * - `GET /api/health`: `200`.
 */
object ShareServer {
    /** The path of the shares. */
    const val SHARES = "/api/shares"

    /** The path of the server's fetch. */
    const val FETCH = "/api/fetch"

    /** The path the server says it is up at. */
    const val HEALTH = "/api/health"

    /** The query parameter that says how many days a share is kept: [ShareKeep.days]. */
    const val DAYS = "days"

    /** The query parameter that holds the address to fetch. */
    const val URL = "url"

    /** The header a share is deleted with. */
    const val DELETE_TOKEN = "X-Delete-Token"

    /** The `error` of a `429` the server gives when an address has left as many shares as it keeps from one a day. */
    const val DAILY_LIMIT = "daily-limit"

    /**
     * The most bytes an encrypted share may have: a document of [ShareLinks.MAX_TEXT], with what compressing a text
     * that does not compress adds, and the cipher's nonce and tag.
     */
    const val MAX_SHARE_BYTES = ShareLinks.MAX_TEXT + 64 * 1024

    /** The most bytes the server fetches from an address: the largest document a browser opens. */
    const val MAX_FETCH_BYTES = ShareLinks.MAX_TEXT

    /** A share's id: ten letters and digits, which the server chooses at random. */
    val Id = Regex("[A-Za-z0-9]{10}")
}

/** How long the server keeps a share: the Share boards' "Keep it for". */
enum class ShareKeep(val days: Int) {
    Day(1),
    Week(WEEK),
    Month(MONTH)
}

/** The days of a week and of a month, as Share counts them. */
private const val WEEK = 7
private const val MONTH = 30

/** Why the server did not fetch a document, by the names its answers give. */
enum class FetchError(val code: String) {
    /** The address is not a public `http` or `https` one: the server reaches no other. */
    NotAllowed("not-allowed"),

    /** The document is larger than [ShareServer.MAX_FETCH_BYTES]. */
    TooLarge("too-large"),

    /** The address answered with a status that is no document's. */
    Status("status"),

    /** The address could not be reached, or did not answer in time. */
    Unreachable("unreachable")
}
