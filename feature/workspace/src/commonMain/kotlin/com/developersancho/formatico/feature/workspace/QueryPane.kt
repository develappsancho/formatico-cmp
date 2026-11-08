package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.strings.queryStrings
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.chip.PantrixChip
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.segmentbutton.PantrixSegmentButton
import com.pantrixui.components.elements.segmentbutton.SegmentOption
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import kotlinx.coroutines.flow.drop

/**
 * The query's pane in a wide window or on a tablet: the views' choice and the query's language, the query and the
 * queries used last, and what it found, each match choosing its value in the tree and the text.
 */
@Composable
internal fun QueryPane(
    state: WorkspaceState,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val language = state.queryLanguage
    val message = treeMessage(state)
    Column(modifier = Modifier.fillMaxSize()) {
        val choices: @Composable () -> Unit = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                QueryViewChoice(state = state, onIntent = onIntent)
                Spacer(modifier = Modifier.weight(1f))
                if (language != null) LanguageChoice(language = language)
            }
        }
        if (message != null || language == null) {
            Row(modifier = Modifier.height(48.dp).padding(horizontal = spacing.md)) { choices() }
            TreePaneMessage(state = state, modifier = Modifier.fillMaxSize())
            return@Column
        }
        val field = rememberQueryField(state, onIntent)
        if (wide) {
            Row(modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = spacing.md)) { choices() }
            PantrixHorizontalDivider()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.md, end = spacing.md, top = if (wide) spacing.lg else spacing.md, bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            if (!wide) choices()
            QueryField(field = field, state = state, layout = layout, onIntent = onIntent)
            RecentQueries(queries = state.otherQueries, layout = layout, onPick = { field.setTextAndPlaceCursorAtEnd(it) })
        }
        PantrixHorizontalDivider()
        val outcome = state.query
        if (outcome is QueryOutcome.Found || outcome is QueryOutcome.Computed) {
            ResultsHeader(state = state, layout = layout, onIntent = onIntent, commands = commands)
            MatchList(state = state, layout = layout, onIntent = onIntent, modifier = Modifier.weight(1f))
        }
    }
}

/** The views of a document while its query is shown: the tree, the table, its problems where it has any, and the query. */
@Composable
internal fun QueryViewChoice(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit, withText: Boolean = false) {
    ViewChoice(
        selected = DocumentPane.Query,
        withText = withText,
        onSelect = { onIntent(WorkspaceIntent.ShowPane(it)) },
        problems = state.problems.size,
        withQuery = true
    )
}

/**
 * The query's languages, the one [language] the document is queried in chosen: a document's format says which it is,
 * the other is there to say that there is one.
 */
@Composable
internal fun LanguageChoice(language: QueryLanguage) {
    PantrixSegmentButton(
        options = QueryLanguage.entries.map { each ->
            SegmentOption(value = each, label = each.displayName, enabled = each == language, tagKey = each.name.lowercase())
        },
        selected = language,
        onSelect = {},
        fullWidth = false,
        tagKey = "query_language"
    )
}

/**
 * The query's field in [field], which writes the document's query as it is typed, named after its language and saying
 * what the query came to, or where it is no query. Its keyboard's action uses the query.
 */
@Composable
internal fun QueryField(field: TextFieldState, state: WorkspaceState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    val language = state.queryLanguage ?: return
    PantrixTextField(
        state = field,
        modifier = Modifier.fillMaxWidth(),
        label = language.displayName,
        supportingText = queryStatus(state.query, language, withOtherLanguage = layout != WorkspaceLayout.Phone),
        error = state.query is QueryOutcome.Failed,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Search, contentDescription = null) },
        showClearButton = true,
        // The action is named: the default one never reaches the handler, and Enter would use nothing.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onIntent(WorkspaceIntent.UseQuery) },
        tagKey = "query"
    )
}

/**
 * The document's query, as a field holds it: what is typed is the document's query, sent on as [onIntent]'s
 * [WorkspaceIntent.Query].
 */
@Composable
internal fun rememberQueryField(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit): TextFieldState {
    val document = state.document
    val field = remember(document?.id) { TextFieldState(document?.view?.query.orEmpty()) }
    val send by rememberUpdatedState(onIntent)
    LaunchedEffect(field) {
        snapshotFlow { field.text.toString() }.drop(1).collect { send(WorkspaceIntent.Query(it)) }
    }
    return field
}

/**
 * The [queries] used last that a query can be asked again from, each a chip: a phone's in one row, which its place
 * scrolls; a wider window's no more than three, under the word "Recent", the row wrapping.
 */
@Composable
internal fun RecentQueries(queries: List<String>, layout: WorkspaceLayout, onPick: (String) -> Unit) {
    if (queries.isEmpty()) return
    val spacing = PantrixTheme.spacing
    val shown = if (layout == WorkspaceLayout.Phone) queries else queries.take(RECENT_SHOWN)
    val chips: @Composable () -> Unit = {
        shown.forEachIndexed { index, query ->
            PantrixChip(label = query, onClick = { onPick(query) }, leadingIcon = PantrixIcons.Clock, tagKey = "recent$index")
        }
    }
    if (layout == WorkspaceLayout.Phone) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) { chips() }
        return
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        PantrixText(
            text = queryStrings.recent,
            style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
            color = PantrixTheme.colors.textSecondary
        )
        chips()
    }
}

/**
 * Over the matches: how many there are, and the copying of what was found and its opening as a document of its own; a
 * tablet copies with an icon.
 */
@Composable
private fun ResultsHeader(state: WorkspaceState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit, commands: DocumentCommands) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val document = state.document ?: return
    val results = remember(document, state.root, state.query) { queryResults(document, state.root, state.query) }
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        PantrixText(
            text = resultsTitle(state.query),
            style = PantrixTheme.typography.callout.copy(fontWeight = FontWeight.SemiBold),
            role = TextRole.Heading(level = 2)
        )
        Spacer(modifier = Modifier.weight(1f))
        val copy = {
            results?.let { commands.copyText(Copied.Results, it.text) }
            onIntent(WorkspaceIntent.UseQuery)
        }
        if (wide) {
            PantrixButton(
                text = queryStrings.copyResults,
                onClick = copy,
                variant = ButtonVariant.Text,
                size = layout.buttons,
                enabled = results != null,
                leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Copy, contentDescription = null) },
                tagKey = "copy_results"
            )
        } else {
            PantrixIconButton(
                imageVector = PantrixIcons.Copy,
                contentDescription = queryStrings.copyResults,
                onClick = copy,
                size = layout.buttons,
                enabled = results != null,
                tagKey = "copy_results"
            )
        }
        OpenResultsButton(results = results, layout = layout, onIntent = onIntent, commands = commands)
    }
}

/** Opens what the query found, [results], as a document of its own, in a tab beside this one. */
@Composable
internal fun OpenResultsButton(
    results: NewDocument?,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    PantrixButton(
        text = queryStrings.openResults,
        onClick = {
            onIntent(WorkspaceIntent.UseQuery)
            results?.let(commands.open)
        },
        variant = ButtonVariant.Outline,
        size = layout.buttons,
        enabled = results != null,
        leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Plus, contentDescription = null) },
        tagKey = "open_results"
    )
}

/**
 * The query's matches, each its path and its value, the chosen one marked; choosing one chooses its value in the tree
 * and in the text. A wide window says so under them.
 */
@Composable
private fun MatchList(state: WorkspaceState, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = PantrixTheme.spacing
    val rows = matchRows(state)
    val current = state.currentMatch
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = spacing.sm, end = spacing.sm, bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(rows, key = { index, _ -> index }) { index, row ->
            MatchRow(
                row = row,
                layout = layout,
                chosen = index == current,
                onChoose = row.key?.let { key ->
                    {
                        onIntent(WorkspaceIntent.Select(key))
                        onIntent(WorkspaceIntent.UseQuery)
                    }
                },
                tagKey = "match$index"
            )
        }
        if (layout == WorkspaceLayout.Wide && rows.isNotEmpty()) {
            item(key = "hint") {
                PantrixText(
                    text = queryStrings.chooseHint,
                    modifier = Modifier.padding(start = spacing.md, end = spacing.md, top = spacing.md),
                    style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                    color = PantrixTheme.colors.textSecondary
                )
            }
        }
    }
}

/** How wide a match's path stands beside its value: as the boards draw it, 200 in a wide window, 260 on a tablet. */
internal fun pathWidth(layout: WorkspaceLayout): Dp = if (layout == WorkspaceLayout.Wide) 200.dp else 260.dp

/** How many of the queries used last a wide window and a tablet offer. */
private const val RECENT_SHOWN = 3
