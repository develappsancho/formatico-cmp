package com.developersancho.formatico

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.settings.HistoryKeep
import com.developersancho.formatico.core.settings.SettingsStore
import com.developersancho.formatico.core.storage.MemorySmallStore
import com.developersancho.formatico.core.storage.MemoryTextStore
import com.developersancho.formatico.feature.settings.HistorySummary
import com.developersancho.formatico.feature.start.OpenedDocument
import com.developersancho.formatico.feature.start.StoredDocumentHistory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppHistoryTest {
    private val now = Instant.parse("2026-11-05T21:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }
    private val order = OpenedDocument(name = "order.json", text = "{}", format = Format.Json)

    @Test
    fun theHistoryKeepsADocumentAsLongAsSettingsSayAndNoneWhereNoneIsKept() = runTest {
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = backgroundScope)
        val settings = SettingsStore(MemorySmallStore(), backgroundScope)
        history.record(order, now - 10.days)
        history.record(order.copy(name = "new.json"), now)
        runCurrent()

        backgroundScope.launch { keepHistory(settings, history, clock) }
        runCurrent()
        // Thirty days, the boards' choice, keep both.
        assertEquals(2, history.documents.value.size)

        settings.update { it.copy(keepFor = HistoryKeep.Days7) }
        runCurrent()
        assertEquals(listOf("new.json"), history.documents.value.map { it.name })

        settings.update { it.copy(keepHistory = false) }
        runCurrent()
        assertEquals(emptyList(), history.documents.value)
    }

    @Test
    fun settingsAreToldHowManyDocumentsTheHistoryHoldsAndHowLarge() = runTest {
        val history = StoredDocumentHistory(small = MemorySmallStore(), texts = MemoryTextStore(), scope = backgroundScope)
        val forSettings = HistoryForSettings(history, backgroundScope)
        history.record(order, now)
        history.record(OpenedDocument(name = "b.json", text = "[1, 2]", format = Format.Json), now)
        runCurrent()

        assertEquals(HistorySummary(documents = 2, bytes = 8), forSettings.summary.value)
        forSettings.clear()
        runCurrent()
        assertEquals(HistorySummary(), forSettings.summary.value)
    }
}
