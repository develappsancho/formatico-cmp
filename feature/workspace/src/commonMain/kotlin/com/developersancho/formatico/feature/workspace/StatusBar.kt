package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.text.LineEnding
import com.pantrixui.components.data.tag.PantrixTag
import com.pantrixui.components.data.tag.TagTone
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_encoding
import formatico.feature.workspace.generated.resources.workspace_indent_spaces
import formatico.feature.workspace.generated.resources.workspace_indent_tabs
import formatico.feature.workspace.generated.resources.workspace_lines
import formatico.feature.workspace.generated.resources.workspace_plain_text
import formatico.feature.workspace.generated.resources.workspace_separated
import formatico.feature.workspace.generated.resources.workspace_status_not_checked
import formatico.feature.workspace.generated.resources.workspace_status_problems
import formatico.feature.workspace.generated.resources.workspace_status_reading
import formatico.feature.workspace.generated.resources.workspace_status_valid
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The bar under the panes: whether the document is valid, how long it is, the chosen value and the text's layout. */
@Composable
internal fun StatusBar(state: WorkspaceState, layout: WorkspaceLayout) {
    val spacing = PantrixTheme.spacing
    val style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium)
    val quiet = PantrixTheme.colors.textSecondary
    Column(modifier = Modifier.fillMaxWidth().background(PantrixTheme.colors.background)) {
        PantrixHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (layout == WorkspaceLayout.Wide) 28.dp else 32.dp)
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.lg)
        ) {
            StatusTag(state)
            state.stats?.let { stats ->
                PantrixText(
                    text = stringResource(
                        Res.string.workspace_separated,
                        pluralStringResource(Res.plurals.workspace_lines, stats.lines, stats.lines),
                        PantrixTheme.strings.fileSize(stats.bytes)
                    ),
                    style = style,
                    color = quiet
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            state.selected?.let { selected ->
                PantrixText(
                    text = jsonPath(selected.path),
                    style = PantrixTheme.typography.code.copy(fontSize = style.fontSize, lineHeight = style.lineHeight),
                    color = quiet,
                    maxLines = 1
                )
            }
            if (layout == WorkspaceLayout.Wide) {
                state.stats?.let { PantrixText(text = layoutOf(it), style = style, color = quiet) }
            }
        }
    }
}

/** `UTF-8 · LF · 2 spaces`: how the text is encoded, ends its lines and indents them, as far as it says. */
@Composable
private fun layoutOf(stats: TextStats): String {
    val parts = buildList {
        add(stringResource(Res.string.workspace_encoding))
        stats.layout.lineEnding?.let { add(lineEndingName(it)) }
        when (val indentation = stats.layout.indentation) {
            is Indentation.Spaces -> {
                add(pluralStringResource(Res.plurals.workspace_indent_spaces, indentation.count, indentation.count))
            }

            Indentation.Tabs -> {
                add(stringResource(Res.string.workspace_indent_tabs))
            }

            null -> {}
        }
    }
    return parts.joinToString(" · ")
}

private fun lineEndingName(lineEnding: LineEnding): String = when (lineEnding) {
    LineEnding.Lf -> "LF"
    LineEnding.Crlf -> "CRLF"
    LineEnding.Cr -> "CR"
}

/** Whether the document is valid, how many problems it has, or that it is not checked: a tag in its colour. */
@Composable
internal fun StatusTag(state: WorkspaceState) {
    val (text, tone) = when (val reading = state.reading) {
        Reading.Pending -> {
            stringResource(Res.string.workspace_status_reading) to TagTone.Neutral
        }

        is Reading.Unread -> {
            val format = reading.format
            val text = if (format == null) {
                stringResource(Res.string.workspace_plain_text)
            } else {
                stringResource(Res.string.workspace_status_not_checked, format.displayName)
            }
            text to TagTone.Neutral
        }

        is Reading.Read -> {
            if (reading.errors == 0) {
                stringResource(Res.string.workspace_status_valid, reading.format.displayName) to TagTone.Success
            } else {
                pluralStringResource(Res.plurals.workspace_status_problems, reading.errors, reading.errors) to
                    TagTone.Danger
            }
        }
    }
    PantrixTag(text = text, tone = tone, dot = true, tagKey = "status")
}
