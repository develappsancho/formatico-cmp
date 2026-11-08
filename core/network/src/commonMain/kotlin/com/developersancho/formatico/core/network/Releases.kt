package com.developersancho.formatico.core.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** The repository Formatico is released from, on GitHub. */
const val FORMATICO_REPOSITORY = "develappsancho/formatico-cmp"

/** The page of every release of Formatico, with its notes and its files' SHA-256 checksums. */
const val FORMATICO_RELEASES_PAGE = "https://github.com/$FORMATICO_REPOSITORY/releases"

/** GitHub's answer for the latest release that is out: no draft, no prerelease. */
private const val LATEST_RELEASE = "https://api.github.com/repos/$FORMATICO_REPOSITORY/releases/latest"

/**
 * A file of a release: its [name] (`Formatico-macOS.dmg`), its [size] in bytes, its [sha256] in hexadecimal where GitHub
 * gives it, and the [address] it downloads from.
 */
data class ReleaseFile(val name: String, val size: Long, val sha256: String?, val address: String)

/**
 * A release of Formatico: its [version] (`1.0.0`), the moment it was [published], where GitHub tells it, and its
 * [files]. A file the release has not had added yet, as a Windows installer built later, is not among them.
 */
data class Release(val version: String, val published: Instant?, val files: List<ReleaseFile>) {
    /** The file named [name], or null where the release has none. */
    fun file(name: String): ReleaseFile? = files.firstOrNull { it.name == name }
}

/** What came of asking GitHub for Formatico's latest release. */
sealed interface LatestRelease {
    /** The latest [release]. */
    data class Found(val release: Release) : LatestRelease

    /** GitHub answered that Formatico has no release out yet. */
    data object None : LatestRelease

    /** GitHub could not be reached, or did not answer as it does: too many asks from this address among them. */
    data object Unreachable : LatestRelease
}

/** Formatico's releases on GitHub, for the menu that downloads the app. */
fun interface Releases {
    /** The latest release that is out. */
    suspend fun latest(): LatestRelease
}

/**
 * [Releases] from GitHub's API, by the platform itself: in a browser the page reaches `api.github.com` over https
 * (`deploy/web/headers.conf`, `connect-src https:`), which every page may read. The release found is kept for as long
 * as the app runs, as GitHub answers an address only 60 times an hour; one not found is asked for again next time.
 */
internal class GitHubReleases(private val client: HttpClient) : Releases {
    private val lock = Mutex()
    private var found: LatestRelease.Found? = null

    override suspend fun latest(): LatestRelease = lock.withLock {
        found ?: ask().also { if (it is LatestRelease.Found) found = it }
    }

    private suspend fun ask(): LatestRelease = reaching(LatestRelease.Unreachable) {
        // A header GitHub asks for, and none a browser would first have to ask leave to send.
        val response = client.get(LATEST_RELEASE) { header(HttpHeaders.Accept, "application/vnd.github+json") }
        when {
            response.status == HttpStatusCode.NotFound -> LatestRelease.None

            !response.status.isSuccess() -> LatestRelease.Unreachable

            else -> runCatching { ReleaseAnswers.decodeFromString<ReleaseAnswer>(response.bodyAsText()) }
                .map { LatestRelease.Found(it.release()) }
                .getOrDefault(LatestRelease.Unreachable)
        }
    }

    /** GitHub's release, of what Formatico reads from it. */
    @Serializable
    private class ReleaseAnswer(
        @SerialName("tag_name") val tag: String,
        @SerialName("published_at") val published: String? = null,
        val assets: List<AssetAnswer> = emptyList()
    ) {
        fun release() = Release(
            version = tag.removePrefix("v"),
            published = published?.let { runCatching { Instant.parse(it) }.getOrNull() },
            files = assets.map { it.file() }
        )
    }

    /** A file of GitHub's release; its digest, where GitHub has one, is `sha256:` and the hexadecimal. */
    @Serializable
    private class AssetAnswer(
        val name: String,
        val size: Long,
        val digest: String? = null,
        @SerialName("browser_download_url") val address: String
    ) {
        fun file() = ReleaseFile(
            name = name,
            size = size,
            sha256 = digest?.takeIf { it.startsWith(SHA256) }?.removePrefix(SHA256),
            address = address
        )
    }

    private companion object {
        const val SHA256 = "sha256:"
        val ReleaseAnswers = Json { ignoreUnknownKeys = true }
    }
}
