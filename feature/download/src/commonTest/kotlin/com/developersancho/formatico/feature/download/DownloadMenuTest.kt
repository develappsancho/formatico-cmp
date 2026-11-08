package com.developersancho.formatico.feature.download

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.developersancho.formatico.core.network.FORMATICO_RELEASES_PAGE
import com.developersancho.formatico.core.network.LatestRelease
import com.developersancho.formatico.core.network.Releases
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.developersancho.formatico.feature.download.strings.DownloadStringsEnglish
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.overlay.snackbar.LocalPantrixSnackbars
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarHost
import com.pantrixui.components.overlay.snackbar.PantrixSnackbarState
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import org.koin.compose.KoinApplication
import org.koin.core.context.stopKoin
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DownloadMenuTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val dmg = PreviewRelease.file("Formatico-macOS.dmg")!!

    /** The addresses opened, as the browser would be asked to. */
    private class TestLinks : UriHandler {
        val opened = mutableListOf<String>()

        override fun openUri(uri: String) {
            opened += uri
        }
    }

    // KoinApplication keeps its context once the test is gone.
    @AfterTest
    fun closeKoin() {
        stopKoin()
    }

    /** The menu as the app draws it, its state held here, from [lookup]: what the user does goes to [intents] too. */
    @Composable
    private fun HeldMenu(lookup: ReleaseLookup, intents: MutableList<DownloadIntent>) {
        var state by remember { mutableStateOf(DownloadState(lookup = lookup)) }
        DownloadMenu(
            state = state,
            onIntent = {
                intents += it
                state = state.copy(open = it == DownloadIntent.Open)
            }
        ) { open -> DownloadButton(label = "Get the app", size = ButtonSize.Small, onClick = open, tagKey = "get_the_app") }
    }

    @Test
    fun theMenuNamesTheReleaseAndEachFileWithWhomItIsForAndItsSize() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<DownloadIntent>()
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = desktop, findings = findings) {
                HeldMenu(ReleaseLookup.Found(PreviewRelease), intents)
            }
        }

        onPantrixNode("Button", "get_the_app").performClick()
        awaitText("Formatico 1.0.0 · released Nov 12, 2026")
        onPantrixNode("MenuItem", "macos", scope = "Menu_download").assert(hasText("Macs with Apple silicon · DMG · 95 MB"))
        onPantrixNode("MenuItem", "windows", scope = "Menu_download").assert(hasText("64-bit Windows 10 or later · MSI · 72 MB"))
        onPantrixNode("MenuItem", "android", scope = "Menu_download").assert(hasText("Android 11 or later · APK · 42 MB"))
        onPantrixNode("MenuItem", "all_releases", scope = "Menu_download").assert(hasText("Every release on GitHub"))

        onPantrixNode("MenuItem", "macos", scope = "Menu_download").performClick()

        awaitUntil("The file chosen") { DownloadIntent.Choose(dmg) in intents }
        assertEquals(DownloadIntent.Open, intents.first())
        assertNoFindings(findings)
    }

    @Test
    fun aReleaseOutBeforeItsWindowsInstallerSaysSoAndOffersNone() = runComposeUiTest {
        val intents = mutableListOf<DownloadIntent>()
        val noWindows = PreviewRelease.copy(files = PreviewRelease.files.filterNot { it.name.endsWith(".msi") })
        setContent {
            PantrixTestTheme(platform = PlatformType.Web, window = desktop) { HeldMenu(ReleaseLookup.Found(noWindows), intents) }
        }

        onPantrixNode("Button", "get_the_app").performClick()
        awaitText("Not in this release yet")
        onPantrixNode("MenuItem", "windows", scope = "Menu_download").assertIsNotEnabled()
    }

    @Test
    fun gitHubOutOfReachLeadsToTheReleasesPage() = runComposeUiTest {
        val intents = mutableListOf<DownloadIntent>()
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = desktop) { HeldMenu(ReleaseLookup.Unreachable, intents) }
        }

        onPantrixNode("Button", "get_the_app").performClick()
        awaitText("GitHub could not be reached")
        onPantrixNode("MenuItem", "releases_page", scope = "Menu_download").performClick()

        awaitUntil("The releases page") { DownloadIntent.OpenReleases in intents }
    }

    @Test
    fun noReleaseOutAndOneBeingLookedUpAreSaid() = runComposeUiTest {
        var lookup by mutableStateOf<ReleaseLookup>(ReleaseLookup.Looking)
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = desktop) {
                DownloadMenu(state = DownloadState(open = true, lookup = lookup), onIntent = {}) { open ->
                    DownloadButton(label = "Get the app", size = ButtonSize.Small, onClick = open, tagKey = "get_the_app")
                }
            }
        }

        awaitText("Looking up the latest release")
        onPantrixNode("MenuItem", "looking", scope = "Menu_download").assertIsNotEnabled()
        lookup = ReleaseLookup.None
        awaitText("No release on GitHub yet")
    }

    @Test
    fun onAPhonesBrowserTheMenuIsASheetCalledGetTheApp() = runComposeUiTest {
        val intents = mutableListOf<DownloadIntent>()
        setContent {
            PantrixTestTheme(platform = PlatformType.Web, window = phone) { HeldMenu(ReleaseLookup.Found(PreviewRelease), intents) }
        }

        onPantrixNode("Button", "get_the_app").performClick()
        // The sheet's title, and the release's name on the line under it.
        awaitText("Formatico 1.0.0 · released Nov 12, 2026")
        assertTrue(intents.isNotEmpty())
        onPantrixNode("MenuItem", "android", scope = "Menu_download").assert(hasText("Android 11 or later · APK · 42 MB"))
    }

    @Test
    fun aButtonWithNoMenuGivenOpensNothing() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = desktop) { GetTheAppButton(ButtonSize.Small) }
        }

        awaitText("Get the app")
        onPantrixNode("Button", "get_the_app").performClick()
        waitForIdle()
        onPantrixNode("MenuItem", "macos", scope = "Menu_download").assertDoesNotExist()
    }

    @Test
    fun theAppsMenuOpensTheFileChosenAndCopiesItsChecksum() = runComposeUiTest {
        val links = TestLinks()
        val clipboard = TestClipboard()
        val snackbars = PantrixSnackbarState()
        val releases = Releases { LatestRelease.Found(PreviewRelease) }
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        setContent {
            KoinApplication(configuration = koinConfiguration { modules(module { single { releases } }, downloadModule) }) {
                PantrixTestTheme(platform = PlatformType.Desktop, window = desktop, clipboard = clipboard) {
                    CompositionLocalProvider(
                        LocalUriHandler provides links,
                        LocalPantrixSnackbars provides snackbars,
                        LocalViewModelStoreOwner provides owner,
                        LocalDownloadMenu provides AppDownloadMenu
                    ) {
                        Box {
                            GetTheAppButton(ButtonSize.Small)
                            PantrixSnackbarHost(state = snackbars)
                        }
                    }
                }
            }
        }

        onPantrixNode("Button", "get_the_app").performClick()
        awaitText("Formatico 1.0.0 · released Nov 12, 2026")
        onPantrixNode("MenuItem", "macos", scope = "Menu_download").performClick()

        awaitUntil("The DMG opened") { links.opened == listOf(dmg.address) }
        awaitUntil("The snackbar") { snackbars.current?.tagKey == "downloading" }
        assertEquals("Formatico-macOS.dmg is downloading from GitHub. Its SHA-256 is 403d521e…a0a983.", snackbars.current?.message)
        onNodeWithText("Copy SHA-256").performClick()
        awaitUntil("The checksum copied") { clipboard.entries.isNotEmpty() }
        assertEquals(dmg.sha256, clipboard.text())

        // The releases page, from the menu opened again: GitHub is not asked a second time.
        onPantrixNode("Button", "get_the_app").performClick()
        onPantrixNode("MenuItem", "all_releases", scope = "Menu_download").performClick()
        awaitUntil("The releases page opened") { links.opened.last() == FORMATICO_RELEASES_PAGE }
    }

    @Test
    fun theAppIsOfferedOnTheDesktopAndTheWebAlone() {
        assertEquals(setOf(PlatformType.Desktop, PlatformType.Web), PlatformType.entries.filter(::offersDownloads).toSet())
    }

    @Test
    fun theSnackbarShortensTheChecksumAndSaysNoneWhereGitHubGivesNone() {
        assertEquals("403d521e…a0a983", shortSha(dmg.sha256!!))
        assertEquals(
            "Formatico-macOS.dmg is downloading from GitHub. Its SHA-256 is 403d521e…a0a983.",
            downloadingMessage(dmg, DownloadStringsEnglish)
        )
        assertEquals(
            "Formatico-macOS.dmg is downloading from GitHub.",
            downloadingMessage(dmg.copy(sha256 = null), DownloadStringsEnglish)
        )
    }
}
