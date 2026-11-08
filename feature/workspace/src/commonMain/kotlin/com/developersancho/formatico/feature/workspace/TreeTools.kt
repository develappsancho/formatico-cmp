package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pantrixui.components.elements.buttongroup.GroupButton
import com.pantrixui.components.elements.buttongroup.PantrixButtonGroup
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_close_search
import formatico.feature.workspace.generated.resources.workspace_find
import formatico.feature.workspace.generated.resources.workspace_match
import formatico.feature.workspace.generated.resources.workspace_open_all
import formatico.feature.workspace.generated.resources.workspace_open_all_levels
import formatico.feature.workspace.generated.resources.workspace_open_to
import formatico.feature.workspace.generated.resources.workspace_open_to_level
import org.jetbrains.compose.resources.stringResource

/** The levels the tree is opened to by a press, from the root's: the root alone open, its children too, and theirs. */
private val Levels = 1..3

/**
 * The branches of [root] that are open where the tree is opened to [level]: those fewer than [level] steps from the
 * root, the root itself among them, or every one where [level] is null.
 */
internal fun expandedTo(root: ValueNode?, level: Int?): Set<String> {
    if (root == null) return emptySet()
    val opened = mutableSetOf<String>()

    fun open(node: ValueNode) {
        if (node.children.isEmpty() || (level != null && node.path.size >= level)) return
        opened += node.key
        node.children.forEach(::open)
    }
    open(root)
    return opened
}

/**
 * The values of [root] that [query] is found in, in the order the tree shows them: in a key or an index's place, or in
 * a value that holds no others, as the tree writes it. Letters are matched small as [language] writes them, so that in
 * Turkish "i" finds "İzmir" and "ı" finds "Isparta". The root itself is never one, nor is anything for a blank query.
 */
internal fun matchesOf(root: ValueNode?, query: String, language: String): List<ValueNode> {
    if (root == null || query.isBlank()) return emptyList()
    val sought = folded(query.trim(), language)
    val found = mutableListOf<ValueNode>()

    fun look(node: ValueNode) {
        val inName = node.path.isNotEmpty() && sought in folded(node.name, language)
        val inValue = !node.kind.isContainer && sought in folded(node.value ?: node.text, language)
        if (inName || inValue) found += node
        node.children.forEach(::look)
    }
    look(root)
    return found
}

/** [text] in small letters as [language] writes them: Turkish and Azerbaijani have a dotted and a dotless i. */
internal fun folded(text: String, language: String): String = if (language == "tr" || language == "az") {
    text.replace('I', 'ı').replace('İ', 'i').lowercase()
} else {
    text.lowercase()
}

/** "Open to" and the levels the tree is opened to by a press: 1, 2, 3 and all of it. */
@Composable
internal fun TreeLevels(root: ValueNode, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit) {
    PantrixText(
        text = stringResource(Res.string.workspace_open_to),
        style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
        color = PantrixTheme.colors.textSecondary,
        maxLines = 1
    )
    val levels = Levels.map { level ->
        GroupButton(
            text = "$level",
            onClick = { onIntent(WorkspaceIntent.Expand(expandedTo(root, level))) },
            contentDescription = stringResource(Res.string.workspace_open_to_level, level),
            tagKey = "level_$level"
        )
    }
    val all = GroupButton(
        text = stringResource(Res.string.workspace_open_all),
        onClick = { onIntent(WorkspaceIntent.Expand(expandedTo(root, level = null))) },
        contentDescription = stringResource(Res.string.workspace_open_all_levels),
        tagKey = "level_all"
    )
    PantrixButtonGroup(buttons = levels + all, size = layout.buttons, tagKey = "levels")
}

/**
 * The bar the tree is searched in: the field, which match of how many is chosen ([position]), the previous and the
 * next one, and closing it. Enter goes to the next match and Escape closes the bar. [focus] puts the caret in its field.
 */
@Composable
internal fun TreeSearchBar(
    query: TextFieldState,
    position: String?,
    layout: WorkspaceLayout,
    onStep: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusRequester = remember { FocusRequester() }
) {
    val spacing = PantrixTheme.spacing
    val strings = PantrixTheme.strings
    val wide = layout == WorkspaceLayout.Wide
    val phone = layout == WorkspaceLayout.Phone
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (wide) 48.dp else 56.dp)
            .background(if (phone) PantrixTheme.colors.surface else PantrixTheme.colors.background)
            .padding(start = spacing.md, end = if (phone) spacing.xs else spacing.sm)
            .onPreviewKeyEvent { event ->
                val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                if (escape) onClose()
                escape
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        PantrixTextField(
            state = query,
            // A phone's field takes what the buttons leave; a wider window's is as wide as its board draws it.
            modifier = (if (phone) Modifier.weight(1f) else Modifier.width(if (wide) 240.dp else 280.dp)).focusRequester(focus),
            leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = { onStep(1) },
            contentDescription = stringResource(Res.string.workspace_find),
            tagKey = "find"
        )
        position?.let {
            PantrixText(
                text = it,
                modifier = Modifier.padding(horizontal = spacing.sm),
                style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
                color = PantrixTheme.colors.textSecondary,
                maxLines = 1
            )
        }
        val size = layout.buttons
        PantrixIconButton(PantrixIcons.ChevronBack, strings.previousMatch, onClick = { onStep(-1) }, size = size, tagKey = "previous_match")
        PantrixIconButton(PantrixIcons.ChevronForward, strings.nextMatch, onClick = { onStep(1) }, size = size, tagKey = "next_match")
        if (!phone) Spacer(modifier = Modifier.weight(1f))
        PantrixIconButton(
            imageVector = PantrixIcons.Close,
            contentDescription = stringResource(Res.string.workspace_close_search),
            onClick = onClose,
            size = size,
            tagKey = "close_search"
        )
    }
}

/**
 * A search in the tree: whether its bar is [open], what is looked for in [query], the values it is [found] in and which
 * of them is chosen. Opening and closing the bar goes to [onOpen]; going to a match chooses it through [onSelect],
 * which opens the branches it is in.
 */
@Stable
internal class TreeSearch(
    val open: Boolean,
    private val chosen: MutableIntState,
    val query: TextFieldState,
    private val found: State<List<ValueNode>>,
    private val onOpen: (Boolean) -> Unit,
    private val onSelect: (String) -> Unit
) {
    val matches: List<ValueNode> get() = found.value

    /** Which of the [matches] is chosen, from 0. */
    val current: Int get() = chosen.intValue

    /** Opens the bar, or closes it and forgets what was looked for. */
    fun toggle() = if (open) close() else onOpen(true)

    fun close() {
        onOpen(false)
        query.clearText()
    }

    /** Goes [by] matches on, round from the last to the first, and chooses the one it comes to. */
    fun step(by: Int) {
        if (matches.isEmpty()) return
        chosen.intValue = (chosen.intValue + by).mod(matches.size)
        onSelect(matches[chosen.intValue].key)
    }

    /** Goes to the first match of a new search. */
    fun restart() {
        chosen.intValue = 0
        matches.firstOrNull()?.let { onSelect(it.key) }
    }

    /** Which match of how many is chosen, "No matches", or nothing while nothing is looked for. */
    @Composable
    fun position(): String? = searchPosition(query.text.toString(), chosen.intValue, matches.size)

    /** "match 1 of 4", where [node] is the chosen match, as a phone's card says it; or null. */
    @Composable
    fun matchOf(node: ValueNode): String? {
        if (matches.getOrNull(chosen.intValue)?.key != node.key) return null
        return stringResource(Res.string.workspace_match, PantrixTheme.strings.matchPosition(chosen.intValue + 1, matches.size))
    }
}

/** The search in [state]'s tree, its bar open as the document's view says; what it does goes to [onIntent]. */
@Composable
internal fun rememberTreeSearch(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit): TreeSearch {
    val query = rememberTextFieldState()
    val chosen = rememberSaveable { mutableIntStateOf(0) }
    val language = PantrixTheme.locale.language
    val text = query.text.toString()
    val found = rememberUpdatedState(remember(state.root, text, language) { matchesOf(state.root, text, language) })
    val intents = rememberUpdatedState(onIntent)
    val open = state.document?.view?.searching == true
    val search = remember(open, query, chosen, found) {
        TreeSearch(
            open = open,
            chosen = chosen,
            query = query,
            found = found,
            onOpen = { intents.value(WorkspaceIntent.Search(it)) },
            onSelect = { intents.value(WorkspaceIntent.Select(it)) }
        )
    }
    // A new search goes to its first match.
    LaunchedEffect(query, text) { if (text.isNotBlank()) search.restart() }
    return search
}

/** Which match of [count], the one at [current], a search for [query] has chosen: "1 of 4", "No matches", or null. */
@Composable
internal fun searchPosition(query: String, current: Int, count: Int): String? {
    val strings = PantrixTheme.strings
    return when {
        query.isBlank() -> null
        count == 0 -> strings.noMatches
        else -> strings.matchPosition(current + 1, count)
    }
}
