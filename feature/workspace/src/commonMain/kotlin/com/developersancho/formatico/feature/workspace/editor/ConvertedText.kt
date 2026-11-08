package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.data.codeblock.CodeMark
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.components.data.codeeditor.PantrixCodeEditor

/**
 * A converted document's [text], named [name], to be read, selected and copied but not changed: coloured as [format]
 * is, its lines numbered, in the editor of a pane, and the lines of [marked], counted from nought, on a warning's band:
 * the comments that mark where a null was left out.
 */
@Composable
internal fun ConvertedText(text: String, format: Format, name: String, modifier: Modifier = Modifier, marked: List<Int> = emptyList()) {
    val language = editorLanguage(format)
    val editor = remember(text) { CodeEditorState(text) }
    PantrixCodeEditor(
        state = editor,
        modifier = modifier,
        highlighter = language.highlighter,
        syntax = language.syntax,
        lineMarks = remember(marked) { marked.associateWith { CodeMark(CodeTone.Warning) } },
        readOnly = true,
        style = PaneEditorStyle,
        colors = editorColors(),
        contentDescription = workspaceStrings.textOf(name),
        softWrap = LocalWrapLines.current,
        tagKey = "converted"
    )
}
