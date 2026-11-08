package com.developersancho.formatico.feature.settings

import com.developersancho.formatico.core.settings.AppLanguage
import com.developersancho.formatico.core.settings.HistoryKeep
import com.developersancho.formatico.core.settings.IndentChoice
import com.developersancho.formatico.core.settings.SettingsStore
import com.developersancho.formatico.core.settings.ThemeChoice
import com.developersancho.formatico.core.settings.UnknownFormat
import com.developersancho.formatico.core.storage.MemorySmallStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    /** A history of [documents] documents of [bytes] bytes, which clearing empties. */
    private class FakeHistory(documents: Int, bytes: Long) : SettingsHistory {
        val held = MutableStateFlow(HistorySummary(documents, bytes))
        override val summary: StateFlow<HistorySummary> = held

        override suspend fun clear() {
            held.value = HistorySummary()
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

    private fun TestScope.viewModel(history: SettingsHistory = FakeHistory(23, 6_100_000)): Pair<SettingsViewModel, SettingsStore> {
        val settings = SettingsStore(MemorySmallStore(), backgroundScope)
        return SettingsViewModel(settings, history) to settings
    }

    @Test
    fun whatIsChosenIsKeptAndShown() = runTest(dispatcher) {
        val (viewModel, settings) = viewModel()

        viewModel.dispatch(SettingsIntent.SetTheme(ThemeChoice.Dark))
        viewModel.dispatch(SettingsIntent.SetLanguage(AppLanguage.Turkish))
        viewModel.dispatch(SettingsIntent.SetIndentation(IndentChoice.FourSpaces))
        viewModel.dispatch(SettingsIntent.SetWrapLines(false))
        viewModel.dispatch(SettingsIntent.SetSortKeys(true))
        viewModel.dispatch(SettingsIntent.SetUnknownFormat(UnknownFormat.Text))
        viewModel.dispatch(SettingsIntent.SetKeepFor(HistoryKeep.Days7))

        val chosen = settings.preferences.value
        assertEquals(ThemeChoice.Dark, chosen.theme)
        assertEquals(AppLanguage.Turkish, chosen.language)
        assertEquals(IndentChoice.FourSpaces, chosen.indentation)
        assertEquals(false, chosen.wrapLines)
        assertEquals(true, chosen.sortKeys)
        assertEquals(UnknownFormat.Text, chosen.unknownFormat)
        assertEquals(HistoryKeep.Days7, chosen.keepFor)
        assertEquals(chosen, viewModel.state.value.preferences)
        assertEquals(HistorySummary(23, 6_100_000), viewModel.state.value.history)
    }

    @Test
    fun aLanguageChosenIsLeftForTheSystemsAgain() = runTest(dispatcher) {
        val (viewModel, settings) = viewModel()
        viewModel.dispatch(SettingsIntent.SetLanguage(AppLanguage.Turkish))

        viewModel.dispatch(SettingsIntent.SetLanguage(null))

        assertEquals(null, settings.preferences.value.language)
        assertEquals(null, viewModel.state.value.preferences.language)
    }

    @Test
    fun keepingNoHistoryForgetsWhatItHeldAndClearingDoesToo() = runTest(dispatcher) {
        val history = FakeHistory(3, 100)
        val (viewModel, settings) = viewModel(history)

        viewModel.dispatch(SettingsIntent.SetKeepHistory(false))

        assertEquals(false, settings.preferences.value.keepHistory)
        assertEquals(HistorySummary(), viewModel.state.value.history)

        history.held.value = HistorySummary(1, 10)
        viewModel.dispatch(SettingsIntent.ClearHistory)
        assertEquals(HistorySummary(), viewModel.state.value.history)
    }

    @Test
    fun backGoesAPageAtATimeAndLeavesFromTheSections() = runTest(dispatcher) {
        val (viewModel, _) = viewModel()
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }

        viewModel.dispatch(SettingsIntent.ShowLicences)
        viewModel.dispatch(SettingsIntent.ShowLicence(OpenSourceLibrary.Koin))
        assertEquals(SettingsPage.Licence(OpenSourceLibrary.Koin), viewModel.state.value.page)

        viewModel.dispatch(SettingsIntent.Back)
        assertEquals(SettingsPage.Licences, viewModel.state.value.page)
        viewModel.dispatch(SettingsIntent.Back)
        assertEquals(SettingsPage.Settings, viewModel.state.value.page)
        assertEquals(emptyList(), effects)
        viewModel.dispatch(SettingsIntent.Back)
        assertEquals(listOf<SettingsEffect>(SettingsEffect.Close), effects)
    }
}
