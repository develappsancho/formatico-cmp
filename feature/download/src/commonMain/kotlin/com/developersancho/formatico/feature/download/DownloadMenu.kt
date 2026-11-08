package com.developersancho.formatico.feature.download

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.base.MviScreen
import com.developersancho.formatico.core.network.Release
import com.developersancho.formatico.feature.download.strings.DownloadStrings
import com.developersancho.formatico.feature.download.strings.downloadStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.components.overlay.popover.PopoverSide
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.date.PantrixDate
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * Whether the app offers its files where it runs: on the desktop and the web. A phone or a tablet that runs the Android
 * app has it already, and there is no app for iOS.
 */
fun offersDownloads(platform: PlatformType): Boolean = platform == PlatformType.Desktop || platform == PlatformType.Web

/**
 * Where a download button finds its menu. The app gives [AppDownloadMenu], which asks GitHub for the latest release; a
 * preview and a screen's test have the default, which draws the button and opens nothing, with no Koin to ask.
 */
abstract class DownloadMenuHost {
    /** The download menu, hanging from [anchor], the button that opens it with the `open` it is given. */
    @Composable
    internal abstract fun Menu(anchor: @Composable (open: () -> Unit) -> Unit)

    /** A button that opens no menu: a preview's and a test's of a screen that holds one. */
    internal object Unopened : DownloadMenuHost() {
        @Composable
        override fun Menu(anchor: @Composable (open: () -> Unit) -> Unit) = anchor {}
    }
}

/** The download menu the buttons of this place open: see [DownloadMenuHost]. */
val LocalDownloadMenu = staticCompositionLocalOf<DownloadMenuHost> { DownloadMenuHost.Unopened }

/**
 * The download menu with its view model, on the latest release GitHub gives: a file chosen is opened in the browser,
 * which downloads it, and the snackbar gives its SHA-256, which it copies.
 */
object AppDownloadMenu : DownloadMenuHost() {
    @Composable
    override fun Menu(anchor: @Composable (open: () -> Unit) -> Unit) {
        val viewModel = koinViewModel<DownloadViewModel>()
        val onEffect = rememberDownloadEffects()
        MviScreen(viewModel = viewModel, onEffect = onEffect) { state, dispatch ->
            DownloadMenu(state = state, onIntent = dispatch, anchor = anchor)
        }
    }
}

/** "Get the app", Start's button, which opens the download menu: drawn where [offersDownloads] says. */
@Composable
fun GetTheAppButton(size: ButtonSize, modifier: Modifier = Modifier) {
    val label = downloadStrings.getTheApp
    LocalDownloadMenu.current.Menu { open -> DownloadButton(label, size, open, modifier, tagKey = "get_the_app") }
}

/** "Download", at the end of Settings' "Get the app" row, which opens the download menu. */
@Composable
fun DownloadAppButton(size: ButtonSize, modifier: Modifier = Modifier) {
    val label = downloadStrings.download
    LocalDownloadMenu.current.Menu { open -> DownloadButton(label, size, open, modifier, tagKey = "download_app") }
}

/** A button that opens the download menu: outlined, its arrow down onto a line before its [label]. */
@Composable
internal fun DownloadButton(label: String, size: ButtonSize, onClick: () -> Unit, modifier: Modifier = Modifier, tagKey: String) {
    PantrixButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        variant = ButtonVariant.Outline,
        size = size,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Download, contentDescription = null) },
        tagKey = tagKey
    )
}

/**
 * The download menu, drawn from [state], hanging from [anchor] and opening over it, as the button stands at the foot of
 * Start and of Settings. In a narrow window it is a sheet, whose title is "Get the app" and the release's name the line
 * under it; elsewhere a panel, whose title is the release's name. What the user does goes to [onIntent].
 */
@Composable
internal fun DownloadMenu(
    state: DownloadState,
    onIntent: (DownloadIntent) -> Unit,
    anchor: @Composable (open: () -> Unit) -> Unit
) {
    val words = downloadStrings
    val narrow = PantrixTheme.window.isNarrow
    val release = (state.lookup as? ReleaseLookup.Found)?.release
    val name = release?.let { releaseName(it, words) }
    PantrixMenu(
        visible = state.open,
        onDismissRequest = { onIntent(DownloadIntent.Close) },
        anchor = { anchor { onIntent(DownloadIntent.Open) } },
        side = PopoverSide.Top,
        alignment = PopoverAlignment.End,
        title = if (narrow) words.getTheApp else name ?: words.formatico,
        tagKey = "download"
    ) {
        if (narrow && name != null) {
            PantrixText(
                text = name,
                modifier = Modifier.padding(horizontal = PantrixTheme.spacing.lg, vertical = PantrixTheme.spacing.xs),
                style = PantrixTheme.typography.footnote,
                color = PantrixTheme.colors.textSecondary
            )
        }
        when (val lookup = state.lookup) {
            is ReleaseLookup.Found -> {
                AppFile.entries.forEach { FileRow(it, lookup.release, words, onIntent) }
                PantrixHorizontalDivider()
                PantrixMenuItem(
                    text = words.allReleases,
                    onClick = { onIntent(DownloadIntent.OpenReleases) },
                    icon = PantrixIcons.ExternalLink,
                    supportingText = words.allReleasesBody,
                    tagKey = "all_releases"
                )
            }

            ReleaseLookup.None -> NoFiles(words.noRelease, words.noReleaseBody, words, onIntent)

            ReleaseLookup.Unreachable -> NoFiles(words.unreachable, words.unreachableBody, words, onIntent)

            ReleaseLookup.NotAsked, ReleaseLookup.Looking -> PantrixMenuItem(
                text = words.looking,
                onClick = {},
                icon = PantrixIcons.Refresh,
                enabled = false,
                supportingText = words.lookingBody,
                tagKey = "looking"
            )
        }
    }
}

/** A platform's row: its name, and under it whom its file is for, its kind and its size; or that the release has none yet. */
@Composable
private fun FileRow(app: AppFile, release: Release, words: DownloadStrings, onIntent: (DownloadIntent) -> Unit) {
    val file = release.file(app.fileName)
    val (platform, forWhom) = when (app) {
        AppFile.MacOs -> words.macOs to words.macOsFor
        AppFile.Windows -> words.windows to words.windowsFor
        AppFile.Android -> words.android to words.androidFor
    }
    PantrixMenuItem(
        text = platform,
        onClick = { if (file != null) onIntent(DownloadIntent.Choose(file)) },
        icon = PantrixIcons.Download,
        enabled = file != null,
        supportingText = file?.let { words.file(forWhom, app.kind, PantrixTheme.strings.fileSize(it.size)) } ?: words.notInRelease,
        tagKey = app.tagKey
    )
}

/** Why there are no files to give, [why] and [then], and the way to the releases page, which has them. */
@Composable
private fun NoFiles(why: String, then: String, words: DownloadStrings, onIntent: (DownloadIntent) -> Unit) {
    PantrixMenuItem(text = why, onClick = {}, icon = PantrixIcons.Warning, enabled = false, supportingText = then, tagKey = "no_files")
    PantrixHorizontalDivider()
    PantrixMenuItem(
        text = words.openReleases,
        onClick = { onIntent(DownloadIntent.OpenReleases) },
        icon = PantrixIcons.ExternalLink,
        tagKey = "releases_page"
    )
}

/** "Formatico 1.0.0 · released Nov 12, 2026": the release's version, and its day by the device's clock. */
@Composable
private fun releaseName(release: Release, words: DownloadStrings): String {
    val published = release.published ?: return words.releaseUndated(release.version)
    val day = PantrixDate.of(published, LocalPantrixClock.current.utcOffset(published))
    return words.release(release.version, PantrixTheme.strings.date(day.day, day.month, day.year))
}
