package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.NullHandling
import com.developersancho.formatico.feature.workspace.strings.convertStrings
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.elements.switchbutton.PantrixSwitchButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** The label before the format a document is converted to: "To" in a wide window, "From JSON to" elsewhere. */
@Composable
internal fun ConvertToLabel(from: Format, layout: WorkspaceLayout) {
    PantrixText(
        text = if (layout ==
            WorkspaceLayout.Wide
        ) {
            convertStrings.to
        } else {
            convertStrings.fromTo(from.displayName)
        },
        style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary
    )
}

/** The button that says the format a document of [from] is converted to, [to], and opens the menu of the others. */
@Composable
internal fun TargetMenu(from: Format, to: Format, size: ButtonSize, onChange: (Format) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixButton(
                text = to.displayName,
                onClick = { open = true },
                variant = ButtonVariant.Outline,
                size = size,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                contentDescription = convertStrings.toMenu(to.displayName),
                tagKey = "convert_to"
            )
        },
        tagKey = "targets"
    ) {
        from.targets.forEach { target ->
            PantrixMenuItem(
                text = target.displayName,
                onClick = {
                    open = false
                    onChange(target)
                },
                selected = target == to,
                tagKey = target.name.lowercase()
            )
        }
    }
}

/** How a format written in levels is indented: two spaces or four. */
@Composable
internal fun IndentChoice(choice: ConvertChoice, onIntent: (WorkspaceIntent) -> Unit) {
    PantrixSegmentButton(
        options = IndentOptions.map { spaces ->
            SegmentOption(
                value = spaces,
                label = workspaceStrings.indentSpaces(spaces, spaces),
                tagKey = "$spaces"
            )
        },
        selected = choice.indent,
        onSelect = { onIntent(WorkspaceIntent.Convert(choice.copy(indent = it))) },
        fullWidth = false,
        contentDescription = convertStrings.indentation,
        tagKey = "indent"
    )
}

/** Whether every object's members are written in the order of their names. */
@Composable
internal fun SortKeysSwitch(choice: ConvertChoice, onIntent: (WorkspaceIntent) -> Unit) {
    PantrixSwitchButton(
        checked = choice.sortKeys,
        onCheckedChange = { onIntent(WorkspaceIntent.Convert(choice.copy(sortKeys = it))) },
        label = workspaceStrings.sortKeys,
        tagKey = "convert_sort_keys"
    )
}

/** What a null becomes in a format that has none: left out, or an empty string. */
@Composable
internal fun NullChoice(choice: ConvertChoice, onIntent: (WorkspaceIntent) -> Unit) {
    PantrixText(
        text = convertStrings.nulls,
        style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary
    )
    PantrixSegmentButton(
        options = listOf(
            SegmentOption(value = NullHandling.LeaveOut, label = convertStrings.nullsLeave, tagKey = "leave"),
            SegmentOption(value = NullHandling.EmptyString, label = convertStrings.nullsEmpty, tagKey = "empty")
        ),
        selected = choice.nulls,
        onSelect = { onIntent(WorkspaceIntent.Convert(choice.copy(nulls = it))) },
        fullWidth = false,
        contentDescription = convertStrings.nulls,
        tagKey = "nulls"
    )
}

/** The name of [converted]'s file: its own, or "Untitled" and the extension of its format. */
@Composable
internal fun convertedFileName(converted: ConvertOutcome.Converted): String =
    converted.name ?: "${workspaceStrings.untitled}.${converted.to.extensions.first()}"

/** The button that copies [converted]. */
@Composable
internal fun CopyConvertedButton(converted: ConvertOutcome.Converted, size: ButtonSize, commands: DocumentCommands) {
    PantrixIconButton(
        imageVector = PantrixIcons.Copy,
        contentDescription = convertStrings.copy(converted.to.displayName),
        onClick = { commands.copyText(Copied.Converted, converted.text) },
        size = size,
        tagKey = "copy_converted"
    )
}

/** The button that saves [converted] as a file, its name on it where [withName] says so and an icon alone otherwise. */
@Composable
internal fun SaveConvertedButton(
    converted: ConvertOutcome.Converted,
    size: ButtonSize,
    commands: DocumentCommands,
    withName: Boolean = true
) {
    val name = convertedFileName(converted)
    val save = { commands.save(OpenDocument(id = 0, name = name, text = converted.text, format = converted.to)) }
    val label = convertStrings.saveAs(name)
    if (withName) {
        PantrixButton(
            text = label,
            onClick = save,
            variant = ButtonVariant.Outline,
            size = size,
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Download, contentDescription = null) },
            tagKey = "save_converted"
        )
    } else {
        PantrixIconButton(
            imageVector = PantrixIcons.Download,
            contentDescription = label,
            onClick = save,
            size = size,
            tagKey = "save_converted"
        )
    }
}

/** The button that opens [converted] as a document of its own, in a tab. */
@Composable
internal fun OpenConvertedButton(converted: ConvertOutcome.Converted, size: ButtonSize, commands: DocumentCommands) {
    val name = convertedFileName(converted)
    PantrixButton(
        text = queryStrings.openResults,
        onClick = { commands.open(NewDocument(name = name, text = converted.text, format = converted.to)) },
        size = size,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Plus, contentDescription = null) },
        tagKey = "open_converted"
    )
}

/** The indentations a format written in levels is offered: two spaces, or four. */
private val IndentOptions = listOf(2, 4)
