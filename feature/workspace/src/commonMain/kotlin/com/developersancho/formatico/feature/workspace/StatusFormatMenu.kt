package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.foundation.icons.PantrixIcons

/**
 * A phone's status beside the trail, pressed to choose how the document is read: the formats of the wide toolbar's menu,
 * the one it is read as checked, as Workspace-390's `format` state draws it (the owner's decision, Faz 21). A phone has
 * no toolbar for [WorkspaceToolbar]'s format button, and a text typed into a new document stays plain text until it is
 * read as a format.
 */
@Composable
internal fun StatusFormatMenu(state: WorkspaceState, onChange: (Format?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val (text, tone) = statusOf(state)
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            // Heard as the status, a button, and what pressing it does.
            PantrixTag(
                text = text,
                modifier = Modifier.padding(horizontal = 4.dp),
                tone = tone,
                dot = true,
                contentDescription = workspaceStrings.statusButton(text),
                trailingIcon = PantrixIcons.ChevronDown,
                onClick = { open = true },
                tagKey = "status"
            )
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
