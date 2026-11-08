package com.developersancho.formatico.feature.settings

import androidx.lifecycle.viewModelScope
import com.developersancho.formatico.core.base.MviViewModel
import com.developersancho.formatico.core.settings.Preferences
import com.developersancho.formatico.core.settings.SettingsStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Settings' view model: what the reader chooses goes to [settings], which keeps it; what the history holds is read from
 * [history], which is cleared from here too.
 */
class SettingsViewModel(
    private val settings: SettingsStore,
    private val history: SettingsHistory
) : MviViewModel<SettingsState, SettingsIntent, SettingsEffect>(SettingsState()) {
    init {
        viewModelScope.launch {
            combine(settings.preferences, history.summary, ::Pair).collect { (preferences, summary) ->
                setState { copy(preferences = preferences, history = summary) }
            }
        }
    }

    override fun handle(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme -> change { copy(theme = intent.theme) }

            is SettingsIntent.SetLanguage -> change { copy(language = intent.language) }

            is SettingsIntent.SetIndentation -> change { copy(indentation = intent.indentation) }

            is SettingsIntent.SetWrapLines -> change { copy(wrapLines = intent.wrapLines) }

            is SettingsIntent.SetSortKeys -> change { copy(sortKeys = intent.sortKeys) }

            is SettingsIntent.SetUnknownFormat -> change { copy(unknownFormat = intent.unknownFormat) }

            is SettingsIntent.SetKeepHistory -> {
                change { copy(keepHistory = intent.keep) }
                if (!intent.keep) viewModelScope.launch { history.clear() }
            }

            is SettingsIntent.SetKeepFor -> change { copy(keepFor = intent.keepFor) }

            SettingsIntent.ClearHistory -> viewModelScope.launch { history.clear() }

            SettingsIntent.ShowLicences -> setState { copy(page = SettingsPage.Licences) }

            is SettingsIntent.ShowLicence -> setState { copy(page = SettingsPage.Licence(intent.library)) }

            SettingsIntent.Back -> back()
        }
    }

    private fun change(change: Preferences.() -> Preferences) = settings.update { it.change() }

    private fun back() {
        when (state.value.page) {
            SettingsPage.Settings -> sendEffect(SettingsEffect.Close)
            SettingsPage.Licences -> setState { copy(page = SettingsPage.Settings) }
            is SettingsPage.Licence -> setState { copy(page = SettingsPage.Licences) }
        }
    }
}
