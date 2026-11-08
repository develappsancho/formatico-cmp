package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.settings.AppLanguage
import com.developersancho.formatico.core.settings.HistoryKeep
import com.developersancho.formatico.core.settings.IndentChoice
import com.developersancho.formatico.core.settings.ThemeChoice
import com.developersancho.formatico.core.settings.UnknownFormat
import com.developersancho.formatico.feature.settings.strings.settingsStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.link.PantrixLink
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.overlay.select.PantrixSelect
import com.pantrixui.components.overlay.select.SelectOption
import com.pantrixui.foundation.theme.PantrixTheme

/** How wide a select of Settings is: the boards' 200, and 160 on a phone. */
private fun SettingsLayout.selectWidth() = if (this == SettingsLayout.Phone) 160.dp else 200.dp

/**
 * General: the theme and the language. A phone has the room for the theme's three choices under its name alone, each
 * as wide as its words: "As the system" does not fit a third of a phone's row.
 */
@Composable
internal fun GeneralSection(state: SettingsState, language: AppLanguage, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val phone = layout == SettingsLayout.Phone
    SettingsSection(title = settingsStrings.general, layout = layout) {
        SettingRow(settingsStrings.theme, layout, settingsStrings.themeBody, stacked = phone) {
            PantrixSegmentButton(
                options = listOf(
                    SegmentOption(ThemeChoice.Light, settingsStrings.themeLight, tagKey = "light"),
                    SegmentOption(ThemeChoice.Dark, settingsStrings.themeDark, tagKey = "dark"),
                    SegmentOption(ThemeChoice.System, settingsStrings.themeSystem, tagKey = "system")
                ),
                selected = state.preferences.theme,
                onSelect = { onIntent(SettingsIntent.SetTheme(it)) },
                fullWidth = false,
                tagKey = "theme"
            )
        }
        RowDivider()
        SettingRow(
            settingsStrings.language,
            layout,
            settingsStrings.languageBody.takeUnless { phone }
        ) {
            PantrixSelect(
                // A language is named as it names itself, in every language.
                options = AppLanguage.entries.map { SelectOption(it, it.ownName, tagKey = it.tag) },
                selected = language,
                onSelect = { onIntent(SettingsIntent.SetLanguage(it)) },
                modifier = Modifier.width(layout.selectWidth()),
                contentDescription = settingsStrings.language,
                tagKey = "language"
            )
        }
    }
}

/** What a language calls itself. */
private val AppLanguage.ownName: String
    get() = when (this) {
        AppLanguage.English -> "English"
        AppLanguage.Turkish -> "Türkçe"
    }

/** Editor: what Format indents with, whether a long line goes on under itself, and whether Format sorts the keys. */
@Composable
internal fun EditorSection(state: SettingsState, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val phone = layout == SettingsLayout.Phone
    SettingsSection(title = settingsStrings.editor, layout = layout) {
        SettingRow(
            settingsStrings.indentation,
            layout,
            settingsStrings.indentationBody,
            stacked = phone
        ) {
            PantrixSegmentButton(
                options = listOf(
                    SegmentOption(IndentChoice.TwoSpaces, settingsStrings.indentTwo, tagKey = "two"),
                    SegmentOption(IndentChoice.FourSpaces, settingsStrings.indentFour, tagKey = "four"),
                    SegmentOption(IndentChoice.Tabs, settingsStrings.indentTabs, tagKey = "tabs")
                ),
                selected = state.preferences.indentation,
                onSelect = { onIntent(SettingsIntent.SetIndentation(it)) },
                fullWidth = false,
                tagKey = "indentation"
            )
        }
        RowDivider()
        SwitchRow(
            label = settingsStrings.wrapLines,
            checked = state.preferences.wrapLines,
            onCheckedChange = { onIntent(SettingsIntent.SetWrapLines(it)) },
            layout = layout,
            tagKey = "wrap_lines",
            description = settingsStrings.wrapLinesBody.takeUnless { phone }
        )
        RowDivider()
        SwitchRow(
            label = settingsStrings.sortKeys,
            checked = state.preferences.sortKeys,
            onCheckedChange = { onIntent(SettingsIntent.SetSortKeys(it)) },
            layout = layout,
            tagKey = "sort_keys",
            description = settingsStrings.sortKeysBody.takeUnless { phone }
        )
    }
}

/** Formats: what is done with a text whose format cannot be told. */
@Composable
internal fun FormatsSection(state: SettingsState, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    SettingsSection(title = settingsStrings.formats, layout = layout) {
        SettingRow(settingsStrings.unknown, layout, settingsStrings.unknownBody) {
            PantrixSelect(
                options = listOf(
                    SelectOption(UnknownFormat.Ask, settingsStrings.unknownAsk, tagKey = "ask"),
                    SelectOption(UnknownFormat.Json, settingsStrings.unknownJson, tagKey = "json"),
                    SelectOption(UnknownFormat.Text, settingsStrings.unknownText, tagKey = "text")
                ),
                selected = state.preferences.unknownFormat,
                onSelect = { onIntent(SettingsIntent.SetUnknownFormat(it)) },
                modifier = Modifier.width(layout.selectWidth()),
                contentDescription = settingsStrings.unknown,
                tagKey = "unknown_format"
            )
        }
    }
}

/**
 * History: whether to keep one, for how long, and clearing it. A phone keeps it until it is cleared, as its board has
 * no choice of how long.
 */
@Composable
internal fun HistorySection(state: SettingsState, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val phone = layout == SettingsLayout.Phone
    SettingsSection(title = settingsStrings.history, layout = layout) {
        KeepHistoryRow(state, layout, onIntent)
        if (!phone) {
            RowDivider()
            SettingRow(settingsStrings.keepFor, layout, settingsStrings.keepForBody) {
                PantrixSelect(
                    options = listOf(
                        SelectOption(HistoryKeep.Days7, settingsStrings.keep7, tagKey = "days_7"),
                        SelectOption(HistoryKeep.Days30, settingsStrings.keep30, tagKey = "days_30"),
                        SelectOption(HistoryKeep.UntilCleared, settingsStrings.keepAlways, tagKey = "always")
                    ),
                    selected = state.preferences.keepFor,
                    onSelect = { onIntent(SettingsIntent.SetKeepFor(it)) },
                    modifier = Modifier.width(layout.selectWidth()),
                    enabled = state.preferences.keepHistory,
                    contentDescription = settingsStrings.keepFor,
                    tagKey = "keep_for"
                )
            }
        }
        RowDivider()
        SettingRow(
            settingsStrings.clear,
            layout,
            if (phone) settingsStrings.clearBodyPhone else settingsStrings.clearBody
        ) {
            PantrixButton(
                text = if (phone) settingsStrings.clearShort else settingsStrings.clear,
                onClick = { onIntent(SettingsIntent.ClearHistory) },
                variant = ButtonVariant.Destructive,
                size = if (layout == SettingsLayout.Wide) ButtonSize.Small else ButtonSize.Medium,
                enabled = state.history.documents > 0,
                contentDescription = settingsStrings.clear.takeIf { phone },
                tagKey = "clear_history"
            )
        }
    }
}

/**
 * Whether a history is kept, said with what it holds: how many documents and how many bytes, on this device alone; or
 * that nothing is kept.
 */
@Composable
private fun KeepHistoryRow(state: SettingsState, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val phone = layout == SettingsLayout.Phone
    val history = state.history
    val description = when {
        !state.preferences.keepHistory -> settingsStrings.historyOff

        history.documents == 0 -> if (phone) settingsStrings.historyEmptyPhone else settingsStrings.historyEmpty

        else -> {
            val kept = if (phone) settingsStrings::historyKeptPhone else settingsStrings::historyKept
            kept(history.documents, history.documents, PantrixTheme.strings.fileSize(history.bytes))
        }
    }
    SwitchRow(
        label = if (phone) settingsStrings.keepHistoryShort else settingsStrings.keepHistory,
        checked = state.preferences.keepHistory,
        onCheckedChange = { onIntent(SettingsIntent.SetKeepHistory(it)) },
        layout = layout,
        tagKey = "keep_history",
        description = description
    )
}

/** About: Formatico's version, what it is built with and set in, and the way to the open-source licences. */
@Composable
internal fun AboutSection(layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    SettingsSection(title = settingsStrings.about, layout = layout) {
        SettingRow(
            settingsStrings.version(FormaticoVersions.APP),
            layout,
            settingsStrings.builtWith(FormaticoVersions.PANTRIX),
            stacked = layout == SettingsLayout.Phone
        ) {
            PantrixLink(
                text = settingsStrings.licences,
                onClick = { onIntent(SettingsIntent.ShowLicences) },
                style = PantrixTheme.typography.label,
                tagKey = "licences"
            )
        }
    }
}
