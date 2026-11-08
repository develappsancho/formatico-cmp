package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.selection.ControlPosition
import com.pantrixui.components.elements.switchbutton.PantrixSwitchButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.theme.PantrixTheme

/** The least height of a setting's row, as the boards draw it: 64 in a wide window, 68 on a tablet, 60 on a phone. */
private fun SettingsLayout.rowHeight() = when (this) {
    SettingsLayout.Wide -> 64.dp
    SettingsLayout.Tablet -> 68.dp
    SettingsLayout.Phone -> 60.dp
}

/** A section of Settings: its [title], and its rows in a group with a line between each and the next. */
@Composable
internal fun SettingsSection(
    title: String,
    layout: SettingsLayout,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit
) {
    val colors = PantrixTheme.colors
    val phone = layout == SettingsLayout.Phone
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (phone) 6.dp else PantrixTheme.spacing.sm)) {
        PantrixText(
            text = title,
            modifier = Modifier.padding(start = if (phone) PantrixTheme.spacing.xs else 0.dp),
            style = if (phone) {
                PantrixTheme.typography.label.copy(fontWeight = FontWeight.SemiBold)
            } else {
                PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)
            },
            color = if (phone) colors.textSecondary else colors.textPrimary,
            role = TextRole.Heading(level = 2)
        )
        SettingsGroup(content = rows)
    }
}

/** Rows in a group: a surface with an edge, round at its corners. */
@Composable
internal fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val shape = PantrixTheme.shapes.lg
    val colors = PantrixTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().clip(shape).background(colors.surface).border(1.dp, colors.divider, shape),
        content = content
    )
}

/** The line between two rows of a section. */
@Composable
internal fun RowDivider() = PantrixHorizontalDivider()

/**
 * A setting: its [label] and what it does, [description], at the start, and its [control] at the end; [stacked], the
 * control goes under them, as a phone has the room for a choice of three.
 */
@Composable
internal fun SettingRow(
    label: String,
    layout: SettingsLayout,
    description: String? = null,
    stacked: Boolean = false,
    control: @Composable () -> Unit
) {
    val spacing = PantrixTheme.spacing
    val padding = Modifier.fillMaxWidth().heightIn(min = layout.rowHeight()).padding(horizontal = spacing.lg, vertical = 10.dp)
    if (stacked) {
        Column(modifier = padding.padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingText(label, description, layout)
            control()
        }
    } else {
        Row(
            modifier = padding,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (layout == SettingsLayout.Phone) spacing.lg else 24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) { SettingText(label, description, layout) }
            control()
        }
    }
}

/** A setting's name and, under it, what it does. */
@Composable
private fun SettingText(label: String, description: String?, layout: SettingsLayout) {
    val typography = PantrixTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PantrixText(text = label, style = layout.labelStyle())
        description?.let {
            PantrixText(
                text = it,
                style = typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = PantrixTheme.colors.textSecondary
            )
        }
    }
}

/** A setting's name, a step larger on a tablet and a phone, where a finger reads it. */
@Composable
private fun SettingsLayout.labelStyle() = when (this) {
    SettingsLayout.Wide -> PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium)
    else -> PantrixTheme.typography.body.copy(fontWeight = FontWeight.Medium)
}

/** A setting that is on or off: its switch at the row's end, named by [label] and said by [description]. */
@Composable
internal fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    layout: SettingsLayout,
    tagKey: String,
    description: String? = null
) {
    PantrixSwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = layout.rowHeight())
            .padding(horizontal = PantrixTheme.spacing.lg, vertical = 10.dp),
        label = label,
        supportingText = description,
        labelStyle = layout.labelStyle(),
        controlPosition = ControlPosition.End,
        tagKey = tagKey
    )
}
