package com.developersancho.formatico

import com.developersancho.formatico.core.network.LatestRelease
import com.developersancho.formatico.core.network.Releases
import com.developersancho.formatico.core.network.WebPictures
import com.developersancho.formatico.core.network.networkModule
import org.koin.core.module.Module
import kotlin.io.encoding.Base64

/**
 * The portrait the boards draw for the order's avatar, 48 pixels square: a head and shoulders on a light ground, as
 * the workspace's previews draw theirs.
 */
private val Portrait = Base64.decode(
    "iVBORw0KGgoAAAANSUhEUgAAADAAAAAwCAIAAADYYG7QAAAAo0lEQVR42u3WSw2AQAyE4XGGI5SgACd4QAMXTrjgQEIIj033UVrY" +
        "SUbAd2jSH+M8uRoIIogggggqCFqG/nYGoCdKPgtKmmQT9DRppo+DYjUJJoIKgtI0sSaCeNSVgzy+DnfP1WN+SEyWCVuqFRn5FYGa" +
        "thNOESRHZOKg7YiV4U2KhAUTTcAEE0qABVvN1QRzzckED5qjCU40uwl+NNsIIuh3oBWIaFhM4MBCPgAAAABJRU5ErkJggg=="
)

/** The pictures at web addresses in the app's tests: every one is the [Portrait], read on this machine. */
private object BoardPictures : WebPictures {
    override suspend fun read(address: String, maxBytes: Long): ByteArray = Portrait
}

/** Formatico's releases in the app's tests: none out, so that a download menu opened asks no one. */
private val BoardReleases = Releases { LatestRelease.None }

/**
 * The app's network in its tests: Formatico's own, but for the pictures at web addresses, which the order's avatar
 * would otherwise ask the network for as its workspace opens, and for the releases a download menu would ask GitHub for.
 */
internal val boardNetworkModule: Module = networkModule(pictures = BoardPictures, releases = BoardReleases)
