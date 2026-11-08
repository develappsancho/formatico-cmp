package com.developersancho.formatico

import com.developersancho.formatico.core.settings.SettingsStore
import com.developersancho.formatico.feature.start.DocumentHistory
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.leaveHistory
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/**
 * Keeps [history] as [settings] say, once they are read and each time they change: nothing where no history is kept,
 * and no document last opened or changed longer ago than a document is kept for.
 */
internal suspend fun keepHistory(settings: SettingsStore, history: DocumentHistory, clock: Clock) {
    combine(settings.loaded, settings.preferences) { loaded, preferences -> preferences.takeIf { loaded } }
        .filterNotNull()
        .collect { preferences ->
            val days = preferences.keepFor.days
            when {
                !preferences.keepHistory -> history.clear()
                days != null -> history.forgetBefore(clock.now() - days.days)
            }
        }
}

/** Tells [history] of each change made to a document of [workbench] that has a place in it, as it is made. */
internal suspend fun followEdits(workbench: Workbench, history: DocumentHistory, clock: Clock) {
    val texts = mutableMapOf<Long, String>()
    workbench.state.collect { state ->
        state.documents.forEach { document ->
            val before = texts.put(document.id, document.text)
            val historyId = document.historyId
            if (before != null && before != document.text && historyId != null) {
                history.edit(historyId, document.text, clock.now())
            }
        }
        texts.keys.retainAll(state.documents.map { it.id }.toSet())
    }
}

/** Keeps [history] and [workbench] in step: the changes of its documents, and which of them it forgot. */
internal suspend fun followHistory(workbench: Workbench, history: DocumentHistory, clock: Clock) = coroutineScope {
    launch { followEdits(workbench, history, clock) }
    followForgotten(workbench, history)
}

/**
 * Tells [workbench] which of its documents [history] forgot, cleared or dropped past its last, while they were open:
 * they leave the history, and keep their texts of their own. What the history lists counts once it is read.
 */
internal suspend fun followForgotten(workbench: Workbench, history: DocumentHistory) {
    combine(history.loaded, history.documents) { loaded, listed -> listed.mapTo(HashSet()) { it.id }.takeIf { loaded } }
        .filterNotNull()
        .collect { listed ->
            workbench.state.value.documents
                .filter { it.historyId != null && it.historyId !in listed }
                .forEach { workbench.leaveHistory(it.id) }
        }
}
