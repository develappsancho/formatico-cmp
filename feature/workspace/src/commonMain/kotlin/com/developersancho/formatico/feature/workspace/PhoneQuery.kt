package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * A phone's query, under the views: the query and the queries used last, what it found, each match leading to its
 * value in the text, and at the bottom the opening of what was found as a document of its own and its copying.
 */
@Composable
internal fun PhoneQuery(
    state: WorkspaceState,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands,
    modifier: Modifier = Modifier
) {
    val spacing = PantrixTheme.spacing
    if (treeMessage(state) != null || state.queryLanguage == null) {
        TreePaneMessage(state = state, modifier = modifier)
        return
    }
    val document = state.document ?: return
    val field = rememberQueryField(state, onIntent)
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            QueryField(field = field, state = state, layout = WorkspaceLayout.Phone, onIntent = onIntent)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                RecentQueries(queries = state.otherQueries, layout = WorkspaceLayout.Phone, onPick = {
                    field.setTextAndPlaceCursorAtEnd(it)
                })
            }
        }
        PantrixHorizontalDivider()
        PhoneMatches(state = state, onIntent = onIntent, modifier = Modifier.weight(1f).fillMaxWidth())
        val results = remember(document, state.root, state.query) { queryResults(document, state.root, state.query) }
        Column(modifier = Modifier.fillMaxWidth().background(PantrixTheme.colors.background)) {
            PantrixHorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                OpenResultsButton(results = results, layout = WorkspaceLayout.Phone, onIntent = onIntent, commands = commands)
                Spacer(modifier = Modifier.weight(1f))
                PantrixIconButton(
                    imageVector = PantrixIcons.Copy,
                    contentDescription = queryStrings.copyResults,
                    onClick = {
                        results?.let { commands.copyText(Copied.Results, it.text) }
                        onIntent(WorkspaceIntent.UseQuery)
                    },
                    size = ButtonSize.Large,
                    enabled = results != null,
                    tagKey = "copy_results"
                )
            }
        }
    }
}

/** A phone's matches under how many there are; choosing one shows its value in the text. */
@Composable
private fun PhoneMatches(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = PantrixTheme.spacing
    val rows = matchRows(state)
    val current = state.currentMatch
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (state.query is QueryOutcome.Found || state.query is QueryOutcome.Computed) {
            item(key = "title") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = spacing.sm, end = spacing.sm, top = spacing.xs, bottom = spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PantrixText(
                        text = resultsTitle(state.query),
                        style = PantrixTheme.typography.callout.copy(fontWeight = FontWeight.SemiBold),
                        role = TextRole.Heading(level = 2)
                    )
                    if (rows.any { it.key != null }) {
                        PantrixText(
                            text = queryStrings.seeInText,
                            style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                            color = PantrixTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
        itemsIndexed(rows, key = { index, _ -> index }) { index, row ->
            MatchRow(
                row = row,
                layout = WorkspaceLayout.Phone,
                chosen = index == current,
                onChoose = row.key?.let { key ->
                    {
                        onIntent(WorkspaceIntent.Select(key))
                        onIntent(WorkspaceIntent.UseQuery)
                        onIntent(WorkspaceIntent.ShowPane(DocumentPane.Text))
                    }
                },
                tagKey = "match$index"
            )
        }
    }
}
