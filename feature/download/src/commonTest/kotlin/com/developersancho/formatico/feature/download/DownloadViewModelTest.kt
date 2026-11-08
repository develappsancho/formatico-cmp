package com.developersancho.formatico.feature.download

import com.developersancho.formatico.core.network.FORMATICO_RELEASES_PAGE
import com.developersancho.formatico.core.network.LatestRelease
import com.developersancho.formatico.core.network.Releases
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    /** GitHub as a test has it: each ask answered by the next of [answers], counted in [asks]. */
    private class FakeReleases(vararg answers: LatestRelease) : Releases {
        private val left = answers.toMutableList()
        var asks = 0
        var held: CompletableDeferred<Unit>? = null

        override suspend fun latest(): LatestRelease {
            asks++
            held?.await()
            return left.removeFirst()
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openedTheMenuLooksUpTheLatestReleaseAndShowsIt() = runTest {
        val releases = FakeReleases(LatestRelease.Found(PreviewRelease)).apply { held = CompletableDeferred() }
        val viewModel = DownloadViewModel(releases)
        assertEquals(DownloadState(), viewModel.state.value)

        viewModel.dispatch(DownloadIntent.Open)
        assertEquals(DownloadState(open = true, lookup = ReleaseLookup.Looking), viewModel.state.value)

        releases.held?.complete(Unit)
        assertEquals(DownloadState(open = true, lookup = ReleaseLookup.Found(PreviewRelease)), viewModel.state.value)
    }

    @Test
    fun aReleaseFoundIsNotAskedForAgainOneNotFoundIs() = runTest {
        val releases = FakeReleases(LatestRelease.Unreachable, LatestRelease.Found(PreviewRelease))
        val viewModel = DownloadViewModel(releases)

        viewModel.dispatch(DownloadIntent.Open)
        assertEquals(ReleaseLookup.Unreachable, viewModel.state.value.lookup)
        viewModel.dispatch(DownloadIntent.Close)
        assertFalse(viewModel.state.value.open)

        viewModel.dispatch(DownloadIntent.Open)
        assertEquals(ReleaseLookup.Found(PreviewRelease), viewModel.state.value.lookup)
        viewModel.dispatch(DownloadIntent.Close)
        viewModel.dispatch(DownloadIntent.Open)

        assertEquals(2, releases.asks)
        assertTrue(viewModel.state.value.open)
    }

    @Test
    fun noReleaseOutIsToldApart() = runTest {
        val viewModel = DownloadViewModel(FakeReleases(LatestRelease.None))
        viewModel.dispatch(DownloadIntent.Open)
        assertEquals(ReleaseLookup.None, viewModel.state.value.lookup)
    }

    @Test
    fun aFileChosenClosesTheMenuAndIsDownloaded() = runTest {
        val viewModel = DownloadViewModel(FakeReleases(LatestRelease.Found(PreviewRelease)))
        viewModel.dispatch(DownloadIntent.Open)
        val dmg = PreviewRelease.file("Formatico-macOS.dmg")!!

        viewModel.dispatch(DownloadIntent.Choose(dmg))

        assertFalse(viewModel.state.value.open)
        assertEquals(DownloadEffect.Download(dmg), viewModel.effects.first())
    }

    @Test
    fun theReleasesPageIsOpenedFromTheMenu() = runTest {
        val viewModel = DownloadViewModel(FakeReleases(LatestRelease.Unreachable))
        viewModel.dispatch(DownloadIntent.Open)

        viewModel.dispatch(DownloadIntent.OpenReleases)

        assertFalse(viewModel.state.value.open)
        assertEquals(DownloadEffect.OpenAddress(FORMATICO_RELEASES_PAGE), viewModel.effects.first())
    }
}
