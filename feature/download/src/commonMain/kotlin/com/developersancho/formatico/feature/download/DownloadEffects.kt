package com.developersancho.formatico.feature.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.developersancho.formatico.core.network.ReleaseFile
import com.developersancho.formatico.feature.download.strings.DownloadStrings
import com.developersancho.formatico.feature.download.strings.downloadStrings
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.SnackbarResult
import com.pantrixui.components.overlay.snackbar.SnackbarTone
import com.pantrixui.foundation.platform.rememberPantrixClipboard
import kotlinx.coroutines.launch

/**
 * What the menu's effects do here: a file is opened in the browser, which downloads it, and the snackbar says so with
 * the file's SHA-256, shortened, which its action copies whole; a page is opened in the browser.
 */
@Composable
internal fun rememberDownloadEffects(): suspend (DownloadEffect) -> Unit {
    val links = LocalUriHandler.current
    val snackbars = LocalPantrixSnackbars.current
    val clipboard = rememberPantrixClipboard()
    val words = downloadStrings
    val scope = rememberCoroutineScope()
    return { effect ->
        when (effect) {
            is DownloadEffect.OpenAddress -> links.open(effect.address)

            is DownloadEffect.Download -> {
                links.open(effect.file.address)
                // The snackbar stays while it is read: the menu's next effects are not kept waiting for it.
                scope.launch {
                    val sha = effect.file.sha256
                    val result = snackbars?.show(
                        message = downloadingMessage(effect.file, words),
                        actionLabel = words.copySha.takeIf { sha != null },
                        tone = SnackbarTone.Info,
                        tagKey = "downloading"
                    )
                    if (sha != null && result == SnackbarResult.ActionPerformed) {
                        val copied = clipboard.copyText(sha, label = "SHA-256")
                        // A system that shows the copy itself is not echoed.
                        if (!copied || !clipboard.traits.announcedBySystem) {
                            snackbars.show(
                                message = if (copied) words.shaCopied else words.shaNotCopied,
                                tone = if (copied) SnackbarTone.Neutral else SnackbarTone.Danger,
                                tagKey = "sha_copied"
                            )
                        }
                    }
                }
            }
        }
    }
}

/** What the snackbar says of [file] as it downloads: its name, and its SHA-256 where GitHub gives one. */
internal fun downloadingMessage(file: ReleaseFile, words: DownloadStrings): String =
    file.sha256?.let { words.downloading(file.name, shortSha(it)) } ?: words.downloadingUnchecked(file.name)

/** A SHA-256 as a sentence holds it: its first eight digits and its last six, `403d521e…a0a983`. */
internal fun shortSha(sha: String): String = if (sha.length > SHORT_SHA) sha.take(SHA_START) + "…" + sha.takeLast(SHA_END) else sha

private const val SHA_START = 8
private const val SHA_END = 6
private const val SHORT_SHA = SHA_START + SHA_END + 1

/** Opens [url] in the browser; an address nothing on the device opens is let go, as Pantrix's own links do. */
private fun UriHandler.open(url: String) {
    try {
        openUri(url)
    } catch (@Suppress("SwallowedException") unopened: IllegalArgumentException) {
        // Nothing opens the address: the press does nothing rather than end the app.
    }
}
