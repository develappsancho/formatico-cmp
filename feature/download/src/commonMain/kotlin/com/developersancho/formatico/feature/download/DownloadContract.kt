package com.developersancho.formatico.feature.download

import com.developersancho.formatico.core.network.Release
import com.developersancho.formatico.core.network.ReleaseFile

/**
 * The app's files a release holds, by the names `deploy/deploy.sh` and `deploy/windows.sh` give them, in the menu's
 * order; [kind] is the file's kind as the menu writes it, and [tagKey] its row's automation key.
 */
internal enum class AppFile(val fileName: String, val kind: String, val tagKey: String) {
    MacOs("Formatico-macOS.dmg", "DMG", "macos"),
    Windows("Formatico-Windows.msi", "MSI", "windows"),
    Android("Formatico-Android.apk", "APK", "android")
}

/** What the menu knows of the latest release. */
internal sealed interface ReleaseLookup {
    /** Nothing asked yet: the menu has not been opened. */
    data object NotAsked : ReleaseLookup

    /** Being asked of GitHub. */
    data object Looking : ReleaseLookup

    /** The latest [release]. */
    data class Found(val release: Release) : ReleaseLookup

    /** GitHub has no release of Formatico out yet. */
    data object None : ReleaseLookup

    /** GitHub could not be reached. */
    data object Unreachable : ReleaseLookup
}

/** The download menu: whether it is [open], and what it knows of the latest release, [lookup]. */
internal data class DownloadState(val open: Boolean = false, val lookup: ReleaseLookup = ReleaseLookup.NotAsked)

/** What the user does with the download menu. */
internal sealed interface DownloadIntent {
    /** Opens the menu, and asks GitHub for the latest release where none is known. */
    data object Open : DownloadIntent

    /** Closes it: a press outside, Escape, back, or a row chosen. */
    data object Close : DownloadIntent

    /** Downloads [file]. */
    data class Choose(val file: ReleaseFile) : DownloadIntent

    /** Opens the page of every release. */
    data object OpenReleases : DownloadIntent
}

/** What the menu does once. */
internal sealed interface DownloadEffect {
    /** Downloads [file]: the browser fetches it from GitHub, and the snackbar gives its checksum. */
    data class Download(val file: ReleaseFile) : DownloadEffect

    /** Opens the web page at [address]. */
    data class OpenAddress(val address: String) : DownloadEffect
}
