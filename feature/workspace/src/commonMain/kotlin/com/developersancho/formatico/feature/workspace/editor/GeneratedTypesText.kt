package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.codeeditor.PantrixCodeEditor

/**
 * The types written of a document, [text] in [language], named [name]: to be read, selected and copied but not
 * changed, coloured as the language is, its lines numbered, in the editor of a pane.
 */
@Composable
internal fun GeneratedTypesText(text: String, language: TypeLanguage, name: String, modifier: Modifier = Modifier) {
    val code = editorLanguage(language)
    val editor = remember(text) { CodeEditorState(text) }
    PantrixCodeEditor(
        state = editor,
        modifier = modifier,
        highlighter = code.highlighter,
        syntax = code.syntax,
        readOnly = true,
        style = PaneEditorStyle,
        colors = editorColors(),
        contentDescription = workspaceStrings.textOf(name),
        softWrap = LocalWrapLines.current,
        tagKey = "types"
    )
}
