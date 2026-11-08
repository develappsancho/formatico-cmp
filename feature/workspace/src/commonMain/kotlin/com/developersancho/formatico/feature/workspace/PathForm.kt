package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.json.jqPath
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.json.jsonPointer
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** The ways a value's path is copied: as JSONPath, as a JSON Pointer and as jq writes it. Their names are not translated. */
internal enum class PathForm(val label: String, val tagKey: String, private val write: (List<PathStep>) -> String) {
    JsonPath("JSONPath", "jsonpath", ::jsonPath),
    JsonPointer("JSON Pointer", "json_pointer", ::jsonPointer),
    Jq("jq", "jq", ::jqPath);

    /** [node]'s path in this form. */
    fun of(node: ValueNode): String = write(node.path)
}

/**
 * The button that copies [node]'s path, and the menu of the forms it is copied in, each with the path it gives: in a
 * wide window a small "Copy path" button, elsewhere an icon; on a phone the menu is a sheet. A form chosen goes to
 * [onCopy].
 */
@Composable
internal fun CopyPathMenu(node: ValueNode, layout: WorkspaceLayout, onCopy: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val name = stepLabel(node)
    val description = workspaceStrings.copyPathOf(name)
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            if (layout == WorkspaceLayout.Wide) {
                PantrixButton(
                    text = workspaceStrings.copyPath,
                    onClick = { open = true },
                    modifier = modifier,
                    variant = ButtonVariant.Text,
                    size = ButtonSize.ExtraSmall,
                    leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Copy, contentDescription = null) },
                    trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                    contentDescription = description,
                    tagKey = "copy_path"
                )
            } else {
                PantrixIconButton(
                    imageVector = PantrixIcons.Copy,
                    contentDescription = description,
                    onClick = { open = true },
                    modifier = modifier,
                    size = if (layout == WorkspaceLayout.Tablet) ButtonSize.Small else ButtonSize.Medium,
                    tagKey = "copy_path"
                )
            }
        },
        alignment = PopoverAlignment.End,
        tagKey = "path_forms"
    ) {
        val spacing = PantrixTheme.spacing
        PantrixText(
            text = workspaceStrings.copyPathAs(name),
            modifier = Modifier.padding(start = spacing.md, end = spacing.md, top = spacing.sm, bottom = spacing.xs),
            style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = PantrixTheme.colors.textSecondary
        )
        PathForm.entries.forEach { form ->
            val path = form.of(node)
            PantrixMenuItem(
                text = form.label,
                onClick = { onCopy(path) },
                supportingText = path,
                supportingTextStyle = PantrixTheme.typography.code,
                tagKey = form.tagKey
            )
        }
    }
}
