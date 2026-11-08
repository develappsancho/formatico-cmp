package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.rootTypeName
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import kotlinx.coroutines.flow.drop

/** A label before a choice of Types: "In", "From JSON, in", "Root type". */
@Composable
internal fun TypesLabel(text: String) {
    PantrixText(
        text = text,
        style = PantrixTheme.typography.label.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary
    )
}

/** The button that says the language the types are written in, [language], and opens the menu of the others. */
@Composable
internal fun LanguageMenu(language: TypeLanguage, size: ButtonSize, onChange: (TypeLanguage) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixButton(
                text = language.displayName,
                onClick = { open = true },
                variant = ButtonVariant.Outline,
                size = size,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                contentDescription = typesStrings.languageMenu(language.displayName),
                tagKey = "types_language"
            )
        },
        tagKey = "languages"
    ) {
        TypeLanguage.entries.forEach { each ->
            PantrixMenuItem(
                text = each.displayName,
                onClick = {
                    open = false
                    onChange(each)
                },
                selected = each == language,
                tagKey = each.name.lowercase()
            )
        }
    }
}

/**
 * The root type's name, as a field holds it: what the reader typed, or the name the document's gives; what is typed is
 * the choice's root, sent on as [onIntent]'s [WorkspaceIntent.Types].
 */
@Composable
internal fun rememberRootField(document: OpenDocument, onIntent: (WorkspaceIntent) -> Unit): TextFieldState {
    val field = remember(document.id) { TextFieldState(document.view.types.root ?: rootTypeName(document.name)) }
    val choice by rememberUpdatedState(document.view.types)
    val send by rememberUpdatedState(onIntent)
    LaunchedEffect(field) {
        snapshotFlow { field.text.toString() }.drop(1).collect { send(WorkspaceIntent.Types(choice.copy(root = it))) }
    }
    return field
}

/** The field of the root type's name: labelled on a phone, where it stands alone, and named for a screen reader elsewhere. */
@Composable
internal fun RootField(field: TextFieldState, modifier: Modifier = Modifier, labelled: Boolean = false) {
    PantrixTextField(
        state = field,
        modifier = modifier,
        label = if (labelled) typesStrings.rootType else null,
        contentDescription = if (labelled) null else typesStrings.rootTypeName,
        tagKey = "root_type"
    )
}

/** The button that copies [written]. */
@Composable
internal fun CopyTypesButton(written: TypesOutcome.Written, size: ButtonSize, commands: DocumentCommands) {
    PantrixIconButton(
        imageVector = PantrixIcons.Copy,
        contentDescription = typesStrings.copy(written.language.displayName),
        onClick = { commands.copyText(Copied.Types, written.text) },
        size = size,
        tagKey = "copy_types"
    )
}

/** The button that saves [written] as a file named for its root type and language: `Order.kt`, `order.ts`. */
@Composable
internal fun SaveTypesButton(written: TypesOutcome.Written, size: ButtonSize, commands: DocumentCommands) {
    PantrixButton(
        text = typesStrings.saveAs(written.fileName),
        onClick = { commands.save(OpenDocument(id = 0, name = written.fileName, text = written.text, format = null)) },
        variant = ButtonVariant.Outline,
        size = size,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Download, contentDescription = null) },
        tagKey = "save_types"
    )
}

/** Why there are no types: the tree's reason, the document's errors, which are repaired first, or that it has no object. */
@Composable
internal fun TypesMessage(state: WorkspaceState, modifier: Modifier = Modifier) {
    when (val outcome = state.types) {
        is TypesOutcome.Invalid -> PaneMessage(
            icon = PantrixIcons.Warning,
            title = workspaceStrings.statusProblems(outcome.errors, outcome.errors),
            body = typesStrings.invalid(outcome.errors, outcome.format.displayName, outcome.errors),
            modifier = modifier
        )

        is TypesOutcome.Untyped -> PaneMessage(
            icon = PantrixIcons.Info,
            title = typesStrings.untypedTitle,
            body = typesStrings.untyped(outcome.format.displayName),
            modifier = modifier
        )

        else -> TreePaneMessage(state = state, modifier = modifier)
    }
}
