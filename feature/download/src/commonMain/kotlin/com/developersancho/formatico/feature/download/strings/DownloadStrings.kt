package com.developersancho.formatico.feature.download.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the download menu.
 *
 * In English, [DownloadStringsEnglish], and in Turkish, [DownloadStringsTurkish]. A sentence that holds values is a
 * function of them, in their order.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface DownloadStrings {
    /** Start's button, and the menu's title as a phone's sheet. */
    val getTheApp: String

    /** Settings' button, at the end of About's row. */
    val download: String

    /** The menu's title: "Formatico {first} · released {second}", the release's version and its day. */
    fun release(first: String, second: String): String

    /** The menu's title where GitHub tells no day: "Formatico {first}". */
    fun releaseUndated(first: String): String

    /** The menu's title while there is no release to name. */
    val formatico: String
    val macOs: String

    /** Whom the DMG is for. */
    val macOsFor: String
    val windows: String

    /** Whom the MSI is for. */
    val windowsFor: String
    val android: String

    /** Whom the APK is for. */
    val androidFor: String

    /** A file's row under its platform: "{first} · {second} · {third}", whom it is for, its kind and its size. */
    fun file(first: String, second: String, third: String): String

    /** A platform whose file the release has not had added yet. */
    val notInRelease: String
    val allReleases: String
    val allReleasesBody: String
    val looking: String
    val lookingBody: String
    val unreachable: String
    val unreachableBody: String
    val noRelease: String
    val noReleaseBody: String
    val openReleases: String

    /** "{first} is downloading from GitHub. Its SHA-256 is {second}.", the file and its checksum, shortened. */
    fun downloading(first: String, second: String): String

    /** "{first} is downloading from GitHub.", for a file GitHub gives no checksum of. */
    fun downloadingUnchecked(first: String): String

    val copySha: String
    val shaCopied: String
    val shaNotCopied: String
}

/** [DownloadStrings] in the theme's language. */
internal val downloadStrings: DownloadStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(DownloadStringsEnglish, DownloadStringsTurkish)
