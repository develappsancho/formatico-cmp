package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
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
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.foundation.keyboard.PantrixShortcut

/** The history of the Settings boards: 23 documents, 6.1 MB. */
internal val PreviewHistory = HistorySummary(documents = 23, bytes = 6_100_000)

/**
 * The commands of the Settings boards' Keyboard shortcuts, under their groups, as the app's palette lists them: its own
 * under Formatico's name, and a document's under Document and View.
 */
internal val PreviewShortcuts = listOf(
    PantrixCommand("palette", "Search or run a command", "Formatico", shortcut = PantrixShortcut(Key.K, command = true)),
    PantrixCommand("new_tab", "New tab", "Formatico", shortcut = PantrixShortcut(Key.T, command = true)),
    PantrixCommand("open", "Open a file", "Formatico", shortcut = PantrixShortcut(Key.O, command = true)),
    PantrixCommand("paste", "Paste as a new document", "Formatico", shortcut = PantrixShortcut(Key.V, command = true, shift = true)),
    PantrixCommand("settings", "Settings", "Formatico", shortcut = PantrixShortcut(Key.Comma, command = true)),
    PantrixCommand("format", "Format document", "Document", shortcut = PantrixShortcut(Key.F, shift = true, alt = true)),
    PantrixCommand("minify", "Minify", "Document", shortcut = PantrixShortcut(Key.M, shift = true, alt = true)),
    PantrixCommand("copy", "Copy the document", "Document", shortcut = PantrixShortcut(Key.C, command = true, shift = true)),
    PantrixCommand("save", "Save as a file", "Document", shortcut = PantrixShortcut(Key.S, command = true)),
    PantrixCommand("tree", "Show the tree", "View", shortcut = PantrixShortcut(Key.One, command = true)),
    PantrixCommand("table", "Show the table", "View", shortcut = PantrixShortcut(Key.Two, command = true)),
    PantrixCommand("graph", "Show the graph", "View", shortcut = PantrixShortcut(Key.Three, command = true)),
    PantrixCommand("query", "Query with JSONPath", "View", shortcut = PantrixShortcut(Key.F, command = true, shift = true))
)

/** One case of Settings: what it shows, [state], and the section it is opened scrolled to, [at], where not the top. */
internal data class SettingsCase(val state: SettingsState, val at: SettingsSection? = null)

/**
 * Settings' cases: the boards' own, every setting as it is first; every one chosen otherwise; no history kept, and one
 * that holds nothing yet; the boards' further down, at the keyboard's shortcuts (on a phone, which has none, at the
 * history); the open-source licences, and the text of one of them.
 */
internal class SettingsCases :
    PreviewCases<SettingsCase>(
        "settings" to SettingsCase(SettingsState(history = PreviewHistory)),
        "chosen" to SettingsCase(
            SettingsState(
                preferences = Preferences(
                    theme = ThemeChoice.Dark,
                    language = AppLanguage.Turkish,
                    indentation = IndentChoice.Tabs,
                    sortKeys = true,
                    unknownFormat = UnknownFormat.Json,
                    keepFor = HistoryKeep.UntilCleared
                ),
                history = PreviewHistory
            )
        ),
        "no history" to SettingsCase(SettingsState(preferences = Preferences(keepHistory = false))),
        "nothing yet" to SettingsCase(SettingsState()),
        "shortcuts" to SettingsCase(SettingsState(history = PreviewHistory), at = SettingsSection.Shortcuts),
        "licences" to SettingsCase(SettingsState(history = PreviewHistory, page = SettingsPage.Licences)),
        "licence" to SettingsCase(SettingsState(history = PreviewHistory, page = SettingsPage.Licence(OpenSourceLibrary.NavigationBrowser)))
    )

@BoardPreviews
@Composable
internal fun SettingsPreview(@PreviewParameter(SettingsCases::class) case: PreviewCase<SettingsCase>) {
    FormaticoPreview(case, Modifier.fillMaxSize()) { (state, at) ->
        // Opened over the order, as the boards draw it.
        SettingsScreen(state = state, onIntent = {}, shortcuts = PreviewShortcuts, at = at, backTo = "order.json")
    }
}
