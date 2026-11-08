package com.developersancho.formatico.core.settings

import com.developersancho.formatico.core.storage.SmallStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.serializer

/**
 * The reader's [Preferences], kept in [small] on this device: read once as the app opens, and written in [scope] each
 * time they are changed, one write after another, the last with the last change.
 *
 * A theme chosen before Settings came, by the bar's switch alone, is read once as the light or the dark one.
 */
class SettingsStore(private val small: SmallStore, private val scope: CoroutineScope) {
    private val current = MutableStateFlow(Preferences())
    private val read = MutableStateFlow(false)
    private val writing = Mutex()
    private var changedBeforeRead = false

    /** The preferences in effect: the defaults until those kept are read. */
    val preferences: StateFlow<Preferences> = current.asStateFlow()

    /** Whether the preferences kept were read. */
    val loaded: StateFlow<Boolean> = read.asStateFlow()

    init {
        scope.launch {
            val kept = runCatching { small.get(KEY, Preferences.serializer()) }.getOrNull()
                ?: Preferences(theme = keptTheme() ?: ThemeChoice.System)
            // A change made before they were read is the reader's last word.
            if (!changedBeforeRead) current.value = kept
            read.value = true
        }
    }

    /** Changes the preferences by [change], and keeps them. */
    fun update(change: (Preferences) -> Preferences) {
        changedBeforeRead = !read.value
        current.update(change)
        scope.launch { writing.withLock { small.put(KEY, current.value, Preferences.serializer()) } }
    }

    /** The theme the bar's switch chose before Settings came, where it chose one. */
    private suspend fun keptTheme(): ThemeChoice? = runCatching { small.get(OLD_THEME_KEY, Boolean.serializer()) }
        .getOrNull()
        ?.let { dark -> if (dark) ThemeChoice.Dark else ThemeChoice.Light }

    private companion object {
        const val KEY = "settings"
        const val OLD_THEME_KEY = "theme"
    }
}
