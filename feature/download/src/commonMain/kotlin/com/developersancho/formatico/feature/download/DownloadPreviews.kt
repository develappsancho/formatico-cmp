package com.developersancho.formatico.feature.download

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.network.Release
import com.developersancho.formatico.core.network.ReleaseFile
import com.developersancho.formatico.feature.download.strings.downloadStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.theme.PantrixTheme
import kotlin.time.Instant

private const val PREVIEW_DOWNLOADS = "https://github.com/develappsancho/formatico-cmp/releases/download/v1.0.0/"

/** A release as the boards draw it: 1.0.0, out on Nov 12, 2026, with its DMG, its MSI and its APK. */
internal val PreviewRelease = Release(
    version = "1.0.0",
    published = Instant.parse("2026-11-12T18:00:00Z"),
    files = listOf(
        ReleaseFile(
            name = "Formatico-macOS.dmg",
            size = 94_707_215,
            sha256 = "403d521e8c9e59182a9b151fcde884df09bb81ae928b921ea309c91d56a0a983",
            address = PREVIEW_DOWNLOADS + "Formatico-macOS.dmg"
        ),
        ReleaseFile(
            name = "Formatico-Windows.msi",
            size = 72_118_784,
            sha256 = "5f0c3b7a9d2e41c6b8a07e93d4f1c2b6a8e0d3f7c9b14a2e6d8f0b3c5a7e9d1f",
            address = PREVIEW_DOWNLOADS + "Formatico-Windows.msi"
        ),
        ReleaseFile(
            name = "Formatico-Android.apk",
            size = 41_890_030,
            sha256 = "8a239b2565fd77346c674f1572b66f04d19ceaa725bf7b9924249efd88f85091",
            address = PREVIEW_DOWNLOADS + "Formatico-Android.apk"
        )
    )
)

/**
 * The download menu's cases: the release with its three files, the release out before its Windows installer, the
 * release being looked up, GitHub out of reach, and no release out yet.
 */
internal class DownloadMenuCases :
    PreviewCases<ReleaseLookup>(
        "release" to ReleaseLookup.Found(PreviewRelease),
        "no windows" to ReleaseLookup.Found(PreviewRelease.copy(files = PreviewRelease.files.filterNot { it.name.endsWith(".msi") })),
        "looking" to ReleaseLookup.Looking,
        "unreachable" to ReleaseLookup.Unreachable,
        "no release" to ReleaseLookup.None
    )

/**
 * The download menu of [case]'s release, hanging from "Get the app" at the foot of the window's end, as on Start. A
 * preview cannot draw a menu open: the button opens it (DownloadScreenshotTest presses it).
 */
@BoardPreviews
@Composable
internal fun DownloadMenuPreview(@PreviewParameter(DownloadMenuCases::class) case: PreviewCase<ReleaseLookup>) {
    FormaticoPreview(case, Modifier.fillMaxSize()) { lookup ->
        var open by remember { mutableStateOf(false) }
        val narrow = PantrixTheme.window.isNarrow
        Box(modifier = Modifier.fillMaxSize().padding(PantrixTheme.spacing.xl), contentAlignment = Alignment.BottomEnd) {
            DownloadMenu(
                state = DownloadState(open = open, lookup = lookup),
                onIntent = {
                    when (it) {
                        DownloadIntent.Open -> open = true
                        else -> open = false
                    }
                }
            ) { opens ->
                DownloadButton(
                    label = downloadStrings.getTheApp,
                    size = if (narrow) ButtonSize.Large else ButtonSize.Small,
                    onClick = opens,
                    tagKey = "get_the_app"
                )
            }
        }
    }
}
