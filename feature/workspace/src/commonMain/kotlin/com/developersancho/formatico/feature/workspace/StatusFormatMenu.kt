package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.testing.pantrixTestTag
import com.pantrixui.foundation.testing.rememberPantrixTestTag
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A phone's status beside the trail, pressed to choose how the document is read: the formats of the wide toolbar's menu,
 * the one it is read as checked, as Workspace-390's `format` state draws it (the owner's decision, Faz 21). A phone has
 * no toolbar for [FormatMenu], and a text typed into a new document stays plain text until it is read as a format.
 */
@Composable
internal fun StatusFormatMenu(state: WorkspaceState, onChange: (Format?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val (text, tone) = statusOf(state)
    val tag = rememberPantrixTestTag("Button", "status_format")
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            // A tag is read and never pressed, and a chip has no tone, dot or arrow at its end: the status is pressed
            // through a row of its own (PANTRIX_FINDINGS.md, "Bir menü açan durum etiketi yok").
            Row(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(PantrixTheme.shapes.sm)
                    // Heard as the status, a button, and what pressing it does: the tag keeps its words to itself.
                    .clickable(onClickLabel = workspaceStrings.changeFormat, role = Role.Button) { open = true }
                    .semantics { contentDescription = text }
                    .pantrixTestTag(tag)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PantrixTag(text = text, tone = tone, dot = true, tagKey = "status")
                PantrixIcon(
                    imageVector = PantrixIcons.ChevronDown,
                    contentDescription = null,
                    size = 16.dp,
                    tint = PantrixTheme.colors.textSecondary
                )
            }
        },
        alignment = PopoverAlignment.End,
        tagKey = "status_formats"
    ) {
        FormatMenuItems(format = state.document?.format, onChange = onChange)
    }
}

/** The formats a document can be read as, then plain text, the one it is read as checked: [FormatMenu]'s and the phone's. */
@Composable
internal fun ColumnScope.FormatMenuItems(format: Format?, onChange: (Format?) -> Unit) {
    Format.entries.forEach { entry ->
        PantrixMenuItem(
            text = entry.displayName,
            onClick = { onChange(entry) },
            selected = entry == format,
            tagKey = entry.name.lowercase()
        )
    }
    PantrixMenuItem(
        text = workspaceStrings.plainText,
        onClick = { onChange(null) },
        selected = format == null,
        tagKey = "plain"
    )
}
