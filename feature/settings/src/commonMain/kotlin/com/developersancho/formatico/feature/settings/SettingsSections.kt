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
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.link.PantrixLink
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.overlay.select.PantrixSelect
import com.pantrixui.components.overlay.select.SelectOption
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.settings.generated.resources.Res
import formatico.feature.settings.generated.resources.settings_about
import formatico.feature.settings.generated.resources.settings_built_with
import formatico.feature.settings.generated.resources.settings_clear
import formatico.feature.settings.generated.resources.settings_clear_body
import formatico.feature.settings.generated.resources.settings_clear_body_phone
import formatico.feature.settings.generated.resources.settings_clear_short
import formatico.feature.settings.generated.resources.settings_editor
import formatico.feature.settings.generated.resources.settings_formats
import formatico.feature.settings.generated.resources.settings_general
import formatico.feature.settings.generated.resources.settings_history
import formatico.feature.settings.generated.resources.settings_history_empty
import formatico.feature.settings.generated.resources.settings_history_empty_phone
import formatico.feature.settings.generated.resources.settings_history_kept
import formatico.feature.settings.generated.resources.settings_history_kept_phone
import formatico.feature.settings.generated.resources.settings_history_off
import formatico.feature.settings.generated.resources.settings_indent_four
import formatico.feature.settings.generated.resources.settings_indent_tabs
import formatico.feature.settings.generated.resources.settings_indent_two
import formatico.feature.settings.generated.resources.settings_indentation
import formatico.feature.settings.generated.resources.settings_indentation_body
import formatico.feature.settings.generated.resources.settings_keep_30
import formatico.feature.settings.generated.resources.settings_keep_7
import formatico.feature.settings.generated.resources.settings_keep_always
import formatico.feature.settings.generated.resources.settings_keep_for
import formatico.feature.settings.generated.resources.settings_keep_for_body
import formatico.feature.settings.generated.resources.settings_keep_history
import formatico.feature.settings.generated.resources.settings_keep_history_short
import formatico.feature.settings.generated.resources.settings_language
import formatico.feature.settings.generated.resources.settings_language_body
import formatico.feature.settings.generated.resources.settings_licences
import formatico.feature.settings.generated.resources.settings_sort_keys
import formatico.feature.settings.generated.resources.settings_sort_keys_body
import formatico.feature.settings.generated.resources.settings_theme
import formatico.feature.settings.generated.resources.settings_theme_body
import formatico.feature.settings.generated.resources.settings_theme_dark
import formatico.feature.settings.generated.resources.settings_theme_light
import formatico.feature.settings.generated.resources.settings_theme_system
import formatico.feature.settings.generated.resources.settings_unknown
import formatico.feature.settings.generated.resources.settings_unknown_ask
import formatico.feature.settings.generated.resources.settings_unknown_body
import formatico.feature.settings.generated.resources.settings_unknown_json
import formatico.feature.settings.generated.resources.settings_unknown_text
import formatico.feature.settings.generated.resources.settings_version
import formatico.feature.settings.generated.resources.settings_wrap_lines
import formatico.feature.settings.generated.resources.settings_wrap_lines_body
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** How wide a select of Settings is: the boards' 200, and 160 on a phone. */
private fun SettingsLayout.selectWidth() = if (this == SettingsLayout.Phone) 160.dp else 200.dp

/**
 * General: the theme and the language. A phone has the room for the theme's three choices under its name alone, each
 * as wide as its words: "As the system" does not fit a third of a phone's row.
 */
@Composable
internal fun GeneralSection(state: SettingsState, language: AppLanguage, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    val phone = layout == SettingsLayout.Phone
    SettingsSection(title = stringResource(Res.string.settings_general), layout = layout) {
        SettingRow(stringResource(Res.string.settings_theme), layout, stringResource(Res.string.settings_theme_body), stacked = phone) {
            PantrixSegmentButton(
                options = listOf(
                    SegmentOption(ThemeChoice.Light, stringResource(Res.string.settings_theme_light), tagKey = "light"),
                    SegmentOption(ThemeChoice.Dark, stringResource(Res.string.settings_theme_dark), tagKey = "dark"),
                    SegmentOption(ThemeChoice.System, stringResource(Res.string.settings_theme_system), tagKey = "system")
                ),
                selected = state.preferences.theme,
                onSelect = { onIntent(SettingsIntent.SetTheme(it)) },
                fullWidth = false,
                tagKey = "theme"
            )
        }
        RowDivider()
        SettingRow(
            stringResource(Res.string.settings_language),
            layout,
            stringResource(Res.string.settings_language_body).takeUnless { phone }
        ) {
            PantrixSelect(
                // A language is named as it names itself, in every language.
                options = AppLanguage.entries.map { SelectOption(it, it.ownName, tagKey = it.tag) },
                selected = language,
                onSelect = { onIntent(SettingsIntent.SetLanguage(it)) },
                modifier = Modifier.width(layout.selectWidth()),
                contentDescription = stringResource(Res.string.settings_language),
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
    SettingsSection(title = stringResource(Res.string.settings_editor), layout = layout) {
        SettingRow(
            stringResource(Res.string.settings_indentation),
            layout,
            stringResource(Res.string.settings_indentation_body),
            stacked = phone
        ) {
            PantrixSegmentButton(
                options = listOf(
                    SegmentOption(IndentChoice.TwoSpaces, stringResource(Res.string.settings_indent_two), tagKey = "two"),
                    SegmentOption(IndentChoice.FourSpaces, stringResource(Res.string.settings_indent_four), tagKey = "four"),
                    SegmentOption(IndentChoice.Tabs, stringResource(Res.string.settings_indent_tabs), tagKey = "tabs")
                ),
                selected = state.preferences.indentation,
                onSelect = { onIntent(SettingsIntent.SetIndentation(it)) },
                fullWidth = false,
                tagKey = "indentation"
            )
        }
        RowDivider()
        SwitchRow(
            label = stringResource(Res.string.settings_wrap_lines),
            checked = state.preferences.wrapLines,
            onCheckedChange = { onIntent(SettingsIntent.SetWrapLines(it)) },
            layout = layout,
            tagKey = "wrap_lines",
            description = stringResource(Res.string.settings_wrap_lines_body).takeUnless { phone }
        )
        RowDivider()
        SwitchRow(
            label = stringResource(Res.string.settings_sort_keys),
            checked = state.preferences.sortKeys,
            onCheckedChange = { onIntent(SettingsIntent.SetSortKeys(it)) },
            layout = layout,
            tagKey = "sort_keys",
            description = stringResource(Res.string.settings_sort_keys_body).takeUnless { phone }
        )
    }
}

/** Formats: what is done with a text whose format cannot be told. */
@Composable
internal fun FormatsSection(state: SettingsState, layout: SettingsLayout, onIntent: (SettingsIntent) -> Unit) {
    SettingsSection(title = stringResource(Res.string.settings_formats), layout = layout) {
        SettingRow(stringResource(Res.string.settings_unknown), layout, stringResource(Res.string.settings_unknown_body)) {
            PantrixSelect(
                options = listOf(
                    SelectOption(UnknownFormat.Ask, stringResource(Res.string.settings_unknown_ask), tagKey = "ask"),
                    SelectOption(UnknownFormat.Json, stringResource(Res.string.settings_unknown_json), tagKey = "json"),
                    SelectOption(UnknownFormat.Text, stringResource(Res.string.settings_unknown_text), tagKey = "text")
                ),
                selected = state.preferences.unknownFormat,
                onSelect = { onIntent(SettingsIntent.SetUnknownFormat(it)) },
                modifier = Modifier.width(layout.selectWidth()),
                contentDescription = stringResource(Res.string.settings_unknown),
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
    SettingsSection(title = stringResource(Res.string.settings_history), layout = layout) {
        KeepHistoryRow(state, layout, onIntent)
        if (!phone) {
            RowDivider()
            SettingRow(stringResource(Res.string.settings_keep_for), layout, stringResource(Res.string.settings_keep_for_body)) {
                PantrixSelect(
                    options = listOf(
                        SelectOption(HistoryKeep.Days7, stringResource(Res.string.settings_keep_7), tagKey = "days_7"),
                        SelectOption(HistoryKeep.Days30, stringResource(Res.string.settings_keep_30), tagKey = "days_30"),
                        SelectOption(HistoryKeep.UntilCleared, stringResource(Res.string.settings_keep_always), tagKey = "always")
                    ),
                    selected = state.preferences.keepFor,
                    onSelect = { onIntent(SettingsIntent.SetKeepFor(it)) },
                    modifier = Modifier.width(layout.selectWidth()),
                    enabled = state.preferences.keepHistory,
                    contentDescription = stringResource(Res.string.settings_keep_for),
                    tagKey = "keep_for"
                )
            }
        }
        RowDivider()
        SettingRow(
            stringResource(Res.string.settings_clear),
            layout,
            stringResource(if (phone) Res.string.settings_clear_body_phone else Res.string.settings_clear_body)
        ) {
            PantrixButton(
                text = stringResource(if (phone) Res.string.settings_clear_short else Res.string.settings_clear),
                onClick = { onIntent(SettingsIntent.ClearHistory) },
                variant = ButtonVariant.Destructive,
                size = if (layout == SettingsLayout.Wide) ButtonSize.Small else ButtonSize.Medium,
                enabled = state.history.documents > 0,
                contentDescription = stringResource(Res.string.settings_clear).takeIf { phone },
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
        !state.preferences.keepHistory -> stringResource(Res.string.settings_history_off)

        history.documents == 0 -> stringResource(if (phone) Res.string.settings_history_empty_phone else Res.string.settings_history_empty)

        else -> pluralStringResource(
            if (phone) Res.plurals.settings_history_kept_phone else Res.plurals.settings_history_kept,
            history.documents,
            history.documents,
            PantrixTheme.strings.fileSize(history.bytes)
        )
    }
    SwitchRow(
        label = stringResource(if (phone) Res.string.settings_keep_history_short else Res.string.settings_keep_history),
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
    SettingsSection(title = stringResource(Res.string.settings_about), layout = layout) {
        SettingRow(
            stringResource(Res.string.settings_version, FormaticoVersions.APP),
            layout,
            stringResource(Res.string.settings_built_with, FormaticoVersions.PANTRIX),
            stacked = layout == SettingsLayout.Phone
        ) {
            PantrixLink(
                text = stringResource(Res.string.settings_licences),
                onClick = { onIntent(SettingsIntent.ShowLicences) },
                style = PantrixTheme.typography.label,
                tagKey = "licences"
            )
        }
    }
}
