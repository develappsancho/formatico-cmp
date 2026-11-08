package com.developersancho.formatico.feature.download.strings

/** [DownloadStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object DownloadStringsEnglish : DownloadStrings {
    override val getTheApp = "Get the app"
    override val download = "Download"

    override fun release(first: String, second: String) = "Formatico $first · released $second"

    override fun releaseUndated(first: String) = "Formatico $first"

    override val formatico = "Formatico"
    override val macOs = "macOS"
    override val macOsFor = "Macs with Apple silicon"
    override val windows = "Windows"
    override val windowsFor = "64-bit Windows 10 or later"
    override val android = "Android"
    override val androidFor = "Android 11 or later"

    override fun file(first: String, second: String, third: String) = "$first · $second · $third"

    override val notInRelease = "Not in this release yet"
    override val allReleases = "Every release on GitHub"
    override val allReleasesBody = "With their notes and SHA-256 checksums"
    override val looking = "Looking up the latest release"
    override val lookingBody = "On GitHub"
    override val unreachable = "GitHub could not be reached"
    override val unreachableBody = "Its releases page has every file"
    override val noRelease = "No release on GitHub yet"
    override val noReleaseBody = "Its releases page shows the first once it is out"
    override val openReleases = "Open the releases page"

    override fun downloading(first: String, second: String) = "$first is downloading from GitHub. Its SHA-256 is $second."

    override fun downloadingUnchecked(first: String) = "$first is downloading from GitHub."

    override val copySha = "Copy SHA-256"
    override val shaCopied = "SHA-256 copied"
    override val shaNotCopied = "The SHA-256 could not be copied"
}
