package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pantrixui.components.elements.keycaps.PantrixKeycaps
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.settings.generated.resources.Res
import formatico.feature.settings.generated.resources.settings_shortcuts
import formatico.feature.settings.generated.resources.settings_shortcuts_body
import org.jetbrains.compose.resources.stringResource

/** The least height of a shortcut's row, as the boards draw it at 1440 and 834. */
private val ShortcutRowHeight = 44.dp

/**
 * Keyboard shortcuts: the palette's [commands] that have one, under their groups in the order they come, each named at
 * its row's start with its keys at the end, as the system the app runs on writes them. The commands are the app's,
 * which lists them in its palette too; a command with no shortcut is left out.
 */
@Composable
internal fun ShortcutsSection(commands: List<PantrixCommand>, layout: SettingsLayout) {
    val colors = PantrixTheme.colors
    val typography = PantrixTheme.typography
    Column(modifier = Modifier.fillMaxWidth()) {
        PantrixText(
            text = stringResource(Res.string.settings_shortcuts),
            style = typography.body.copy(fontWeight = FontWeight.SemiBold),
            role = TextRole.Heading(level = 2)
        )
        PantrixText(
            text = stringResource(Res.string.settings_shortcuts_body),
            modifier = Modifier.padding(top = PantrixTheme.spacing.sm),
            style = typography.label.copy(fontWeight = FontWeight.Medium),
            color = colors.textSecondary
        )
        commands.filter { it.shortcut != null }.groupBy { it.group.orEmpty() }.forEach { (group, shortcuts) ->
            PantrixText(
                text = group,
                modifier = Modifier.padding(top = PantrixTheme.spacing.lg, bottom = 6.dp),
                style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textSecondary,
                role = TextRole.Heading(level = 3)
            )
            SettingsGroup {
                shortcuts.forEachIndexed { index, command ->
                    if (index > 0) RowDivider()
                    ShortcutRow(command, layout)
                }
            }
        }
    }
}

/** A command's name at the start of its row, and its shortcut's keys at the end. */
@Composable
private fun ShortcutRow(command: PantrixCommand, layout: SettingsLayout) {
    val shortcut = command.shortcut ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ShortcutRowHeight)
            .padding(horizontal = PantrixTheme.spacing.lg, vertical = PantrixTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        PantrixText(text = command.label, modifier = Modifier.weight(1f), style = layout.labelStyle())
        PantrixKeycaps(shortcut = shortcut, tagKey = command.tagKey ?: command.key)
    }
}
