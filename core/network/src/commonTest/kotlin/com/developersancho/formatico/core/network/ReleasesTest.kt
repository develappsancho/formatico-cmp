package com.developersancho.formatico.core.network

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

/** Formatico's latest release, as GitHub's API gives it, for the download menu. */
class ReleasesTest {
    /** GitHub's answer for the latest release, cut to what Formatico reads and a little more. */
    private val answer =
        """
        {
          "url": "https://api.github.com/repos/develappsancho/formatico-cmp/releases/1",
          "tag_name": "v1.0.0",
          "name": "Formatico 1.0.0",
          "draft": false,
          "published_at": "2026-11-12T18:00:00Z",
          "assets": [
            {
              "name": "Formatico-macOS.dmg",
              "size": 94707215,
              "digest": "sha256:403d521e8c9e59182a9b151fcde884df09bb81ae928b921ea309c91d56a0a983",
              "content_type": "application/x-apple-diskimage",
              "browser_download_url": "https://github.com/develappsancho/formatico-cmp/releases/download/v1.0.0/Formatico-macOS.dmg"
            },
            {
              "name": "Formatico-Android.apk",
              "size": 41890030,
              "digest": null,
              "browser_download_url": "https://github.com/develappsancho/formatico-cmp/releases/download/v1.0.0/Formatico-Android.apk"
            }
          ]
        }
        """.trimIndent()

    @Test
    fun theLatestReleaseIsReadWithItsVersionItsDayAndItsFiles() = runTest {
        var asked: String? = null
        var accept: String? = null
        val releases = GitHubReleases(
            clientOf {
                asked = it.url.toString()
                accept = it.headers[HttpHeaders.Accept]
                json(answer)
            }
        )

        val release = assertIs<LatestRelease.Found>(releases.latest()).release

        assertEquals("https://api.github.com/repos/develappsancho/formatico-cmp/releases/latest", asked)
        assertEquals("application/vnd.github+json", accept)
        assertEquals("1.0.0", release.version)
        assertEquals(Instant.parse("2026-11-12T18:00:00Z"), release.published)
        val dmg = release.file("Formatico-macOS.dmg")!!
        assertEquals(94_707_215L, dmg.size)
        assertEquals("403d521e8c9e59182a9b151fcde884df09bb81ae928b921ea309c91d56a0a983", dmg.sha256)
        assertEquals("https://github.com/develappsancho/formatico-cmp/releases/download/v1.0.0/Formatico-macOS.dmg", dmg.address)
        // A file GitHub has no digest of says none; one the release has not had added yet is not there.
        assertNull(release.file("Formatico-Android.apk")!!.sha256)
        assertNull(release.file("Formatico-Windows.msi"))
    }

    @Test
    fun aReleaseFoundIsKeptAndNotAskedForAgain() = runTest {
        var asks = 0
        val releases = GitHubReleases(
            clientOf {
                asks++
                json(answer)
            }
        )

        releases.latest()
        releases.latest()

        assertEquals(1, asks)
    }

    @Test
    fun noReleaseYetIsToldFromGitHubOutOfReach() = runTest {
        assertEquals(LatestRelease.None, GitHubReleases(clientOf { json("""{"message":"Not Found"}""", HttpStatusCode.NotFound) }).latest())
        // Too many asks from this address in the hour: GitHub's 403, which is GitHub out of reach for now.
        assertEquals(LatestRelease.Unreachable, GitHubReleases(clientOf { json("{}", HttpStatusCode.Forbidden) }).latest())
        assertEquals(LatestRelease.Unreachable, GitHubReleases(clientOf { respond("<html></html>") }).latest())
        assertEquals(LatestRelease.Unreachable, GitHubReleases(clientOf { error("No route to host") }).latest())
    }

    @Test
    fun aReleaseNotFoundIsAskedForAgain() = runTest {
        var asks = 0
        val releases = GitHubReleases(
            clientOf {
                asks++
                if (asks == 1) error("No route to host") else json(answer)
            }
        )

        assertEquals(LatestRelease.Unreachable, releases.latest())
        assertIs<LatestRelease.Found>(releases.latest())
        assertEquals(2, asks)
    }
}
