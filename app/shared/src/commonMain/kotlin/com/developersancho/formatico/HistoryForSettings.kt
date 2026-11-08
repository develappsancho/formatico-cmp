package com.developersancho.formatico

import com.developersancho.formatico.feature.settings.HistorySummary
import com.developersancho.formatico.feature.settings.SettingsHistory
import com.developersancho.formatico.feature.start.DocumentHistory
import com.developersancho.formatico.feature.start.RecentDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.module.Module
import org.koin.dsl.module

/** The history as Settings sees it: how many documents [history] holds and how large their texts are, and clearing it. */
internal class HistoryForSettings(private val history: DocumentHistory, scope: CoroutineScope) : SettingsHistory {
    override val summary: StateFlow<HistorySummary> = history.documents
        .map { documents -> HistorySummary(documents = documents.size, bytes = documents.sumOf(RecentDocument::size)) }
        .stateIn(scope, SharingStarted.Eagerly, HistorySummary())

    override suspend fun clear() = history.clear()
}

/** What the app gives Settings of the features it is built from: the history, as Settings sees it. */
internal val appModule: Module = module {
    single<SettingsHistory> { HistoryForSettings(history = get(), scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
