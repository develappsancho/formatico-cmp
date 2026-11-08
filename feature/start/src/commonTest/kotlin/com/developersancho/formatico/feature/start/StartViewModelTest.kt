package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.pantrixui.foundation.files.PantrixFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StartViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val moment = Instant.parse("2026-11-03T09:41:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = moment
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.viewModel(samples: SampleSource = SampleSource { "{}" }): StartViewModel {
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = backgroundScope)
        return StartViewModel(history = history, samples = samples, opener = DocumentOpener(history, clock, MAX_SIZE, dispatcher))
    }

    private fun TestScope.effectsOf(viewModel: StartViewModel): List<StartEffect> {
        val effects = mutableListOf<StartEffect>()
        backgroundScope.launch { viewModel.effects.collect { effects += it } }
        return effects
    }

    @Test
    fun pastedTextIsOpenedInTheFormatItIsWrittenInAndListed() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.Paste("""{"a": 1}"""))

        val document = OpenedDocument(name = null, text = """{"a": 1}""", format = Format.Json, historyId = 1)
        assertEquals(listOf<StartEffect>(StartEffect.OpenDocument(document)), effects)
        val recent = viewModel.state.value.recent.single()
        assertEquals(Format.Json, recent.format)
        assertEquals(8L, recent.size)
        assertEquals(moment, recent.at)
        assertFalse(recent.edited)
    }

    @Test
    fun anEncryptedLinkWhoseServerIsNotReachedIsSaidAndOpenedAgain() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.Paste(TestSharedLink))
        viewModel.dispatch(StartIntent.OpenLink(TestSharedLink))

        assertEquals(List<StartEffect>(2) { StartEffect.SharedLinkUnreachable(TestSharedLink) }, effects)
    }

    @Test
    fun aWebAddressIsAskedForInTheAppsDialog() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.AskAddress)

        assertEquals(listOf<StartEffect>(StartEffect.AskAddress), effects)
    }

    @Test
    fun anEmptyClipboardOpensNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.Paste(null))
        viewModel.dispatch(StartIntent.Paste("  \n"))

        assertEquals(listOf<StartEffect>(StartEffect.NothingToPaste, StartEffect.NothingToPaste), effects)
        assertTrue(viewModel.state.value.recent.isEmpty())
    }

    @Test
    fun aPastedTextLargerThanAFileMayBeIsSaidAndNotOpened() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.Paste("a".repeat(MAX_SIZE.toInt() + 1)))

        assertEquals(listOf<StartEffect>(StartEffect.PasteTooLarge(MAX_SIZE)), effects)
        assertTrue(viewModel.state.value.recent.isEmpty())
    }

    @Test
    fun aNewDocumentIsOpenedEmptyAndNotListed() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.NewDocument)

        assertEquals(listOf<StartEffect>(StartEffect.OpenDocument(OpenedDocument(null, "", null))), effects)
        assertTrue(viewModel.state.value.recent.isEmpty())
    }

    @Test
    fun aFileIsReadAndToldByItsName() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.OpenFiles(listOf(PantrixFile("values.yaml", "a: 1\n".encodeToByteArray()))))

        val document = OpenedDocument(name = "values.yaml", text = "a: 1\n", format = Format.Yaml, historyId = 1)
        assertEquals(listOf<StartEffect>(StartEffect.OpenDocument(document)), effects)
        assertEquals("values.yaml", viewModel.state.value.recent.single().name)
    }

    @Test
    fun aFileLargerThanThePlatformsMostIsRefused() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.OpenFiles(listOf(PantrixFile("big.json", ByteArray(MAX_SIZE.toInt() + 1)))))

        assertTrue(effects.isEmpty())
        assertEquals(FileRefusal.TooLarge("big.json", MAX_SIZE), viewModel.state.value.refusal)
    }

    @Test
    fun theNextDocumentOpenedClearsARefusal() = runTest(dispatcher) {
        val viewModel = viewModel()
        effectsOf(viewModel)

        viewModel.dispatch(StartIntent.RefuseFile(FileRefusal.WrongType("photo.png")))
        viewModel.dispatch(StartIntent.Paste("a = 1"))

        assertNull(viewModel.state.value.refusal)
    }

    @Test
    fun aSampleIsOpenedUnderItsFileName() = runTest(dispatcher) {
        val viewModel = viewModel(samples = { sample -> "text of ${sample.fileName}" })
        val effects = effectsOf(viewModel)

        viewModel.dispatch(StartIntent.OpenSample(Sample.Manifest))

        val document =
            OpenedDocument(name = "Cargo.toml", text = "text of Cargo.toml", format = Format.Toml, historyId = 1)
        assertEquals(listOf<StartEffect>(StartEffect.OpenDocument(document)), effects)
    }

    @Test
    fun aRecentDocumentIsOpenedAgainAndClearingForgetsEveryOne() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = effectsOf(viewModel)
        viewModel.dispatch(StartIntent.Paste("first: 1"))
        viewModel.dispatch(StartIntent.Paste("second: 2"))
        val first = viewModel.state.value.recent.last()

        viewModel.dispatch(StartIntent.OpenRecent(first.id))

        assertEquals(first.id, viewModel.state.value.recent.first().id)
        assertEquals(2, viewModel.state.value.recent.size)
        assertEquals("first: 1", (effects.last() as StartEffect.OpenDocument).document.text)

        viewModel.dispatch(StartIntent.ClearHistory)

        assertTrue(viewModel.state.value.recent.isEmpty())
    }

    private companion object {
        const val MAX_SIZE = 100L
    }
}
