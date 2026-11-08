package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.developersancho.formatico.core.designsystem.FormaticoTheme
import com.developersancho.formatico.feature.workspace.editor.LocalWrapLines
import com.developersancho.formatico.feature.workspace.editor.editorLanguage
import com.developersancho.formatico.feature.workspace.strings.compareStrings
import com.pantrixui.components.data.diffview.DiffLayout
import com.pantrixui.components.data.diffview.DiffViewColors
import com.pantrixui.components.data.diffview.PantrixDiffView
import com.pantrixui.components.data.diffview.rememberDiffViewState

/**
 * A's text and B's compared, as the Compare boards draw them in Pantrix's `PantrixDiffView`: side by side, or one over
 * the other in one [layout] column; each headed by its letter and its name, the lines that differ marked and the words
 * in them, unchanged runs past a few lines folded. The difference at [current] is the change shown, barred and brought
 * into view. Two documents of one format are coloured as it is.
 */
@Composable
internal fun ComparedTexts(compared: CompareOutcome.Compared, current: Int, layout: DiffLayout, modifier: Modifier = Modifier) {
    val state = rememberDiffViewState()
    val change = compared.changes.getOrNull(current)
    LaunchedEffect(state, compared, change) {
        if (change == null) state.shownChange = null else state.scrollToChange(change)
    }
    // One highlighter reads both texts: a JSON's is no reader of a YAML.
    val format = compared.a.format.takeIf { it == compared.b.format }
    PantrixDiffView(
        lines = compared.lines,
        modifier = modifier,
        layout = layout,
        softWrap = LocalWrapLines.current,
        highlighter = format?.let { editorLanguage(it).highlighter },
        beforeTitle = compareStrings.sideShort(compareStrings.a, comparedName(compared.a)),
        afterTitle = compareStrings.sideShort(compareStrings.b, comparedName(compared.b)),
        state = state,
        colors = diffColors(),
        tagKey = "texts"
    )
}

/** The colours of a compared text's syntax, as the editor writes a document in: Formatico's own. */
@Composable
private fun diffColors(): DiffViewColors {
    val syntax = FormaticoTheme.syntax
    return DiffViewColors(
        comment = syntax.comment,
        keyword = syntax.literal,
        type = syntax.key,
        string = syntax.string,
        number = syntax.number,
        punctuation = syntax.punctuation,
        annotation = syntax.number,
        property = syntax.key,
        tag = syntax.key
    )
}
