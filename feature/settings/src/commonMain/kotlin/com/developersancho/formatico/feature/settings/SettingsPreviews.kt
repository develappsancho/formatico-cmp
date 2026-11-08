package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.developersancho.formatico.core.designsystem.BoardPreviews
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.settings.AppLanguage
import com.developersancho.formatico.core.settings.HistoryKeep
import com.developersancho.formatico.core.settings.IndentChoice
import com.developersancho.formatico.core.settings.Preferences
import com.developersancho.formatico.core.settings.ThemeChoice
import com.developersancho.formatico.core.settings.UnknownFormat

/** The history of the Settings boards: 23 documents, 6.1 MB. */
internal val PreviewHistory = HistorySummary(documents = 23, bytes = 6_100_000)

/**
 * Settings' cases: the boards' own, every setting as it is first; every one chosen otherwise; no history kept, and one
 * that holds nothing yet; the open-source licences, and the text of one of them.
 */
internal class SettingsCases :
    PreviewCases<SettingsState>(
        "settings" to SettingsState(history = PreviewHistory),
        "chosen" to SettingsState(
            preferences = Preferences(
                theme = ThemeChoice.Dark,
                language = AppLanguage.Turkish,
                indentation = IndentChoice.Tabs,
                sortKeys = true,
                unknownFormat = UnknownFormat.Json,
                keepFor = HistoryKeep.UntilCleared
            ),
            history = PreviewHistory
        ),
        "no history" to SettingsState(preferences = Preferences(keepHistory = false)),
        "nothing yet" to SettingsState(),
        "licences" to SettingsState(history = PreviewHistory, page = SettingsPage.Licences),
        "licence" to SettingsState(history = PreviewHistory, page = SettingsPage.Licence(OpenSourceLibrary.NavigationBrowser))
    )

@BoardPreviews
@Composable
internal fun SettingsPreview(@PreviewParameter(SettingsCases::class) case: PreviewCase<SettingsState>) {
    FormaticoPreview(case, Modifier.fillMaxSize()) { state -> SettingsScreen(state = state, onIntent = {}) }
}
