package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.font.FontWeight
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.buttongroup.GroupButton
import com.pantrixui.components.elements.buttongroup.PantrixButtonGroup
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.l10n.PantrixLocale
import com.pantrixui.foundation.theme.PantrixTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
 * a value that holds no others, as the tree writes it. Letters are matched small as [locale] writes them, so that in
 * Turkish "i" finds "İzmir" and "ı" finds "Isparta". The root itself is never one, nor is anything for a blank query.
 */
internal fun matchesOf(root: ValueNode?, query: String, locale: PantrixLocale): List<ValueNode> {
    if (root == null || query.isBlank()) return emptyList()
    val sought = locale.fold(query.trim())
    val found = mutableListOf<ValueNode>()

    fun look(node: ValueNode) {
        if (nameHolds(node, sought, locale) || valueHolds(node, sought, locale)) found += node
        node.children.forEach(::look)
    }
    look(root)
    return found
}

/** Whether [sought], in small letters as [locale] writes them, is in [node]'s key or its index; never the root's. */
internal fun nameHolds(node: ValueNode, sought: String, locale: PantrixLocale): Boolean =
    node.path.isNotEmpty() && sought in locale.fold(node.name)

/** Whether [sought], in small letters as [locale] writes them, is in a value of [node]'s that holds no others. */
internal fun valueHolds(node: ValueNode, sought: String, locale: PantrixLocale): Boolean =
    !node.kind.isContainer && sought in locale.fold(node.value ?: node.text)

/**
 * "Open to", where [withLabel] says there is room for it, and the levels the tree is opened to by a press: 1, 2, 3 and
 * all of it. Each level says what it does to a screen reader without the label.
 */
@Composable
internal fun TreeLevels(root: ValueNode, layout: WorkspaceLayout, onIntent: (WorkspaceIntent) -> Unit, withLabel: Boolean = true) {
    if (withLabel) {
        PantrixText(
            text = workspaceStrings.openTo,
            style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
            color = PantrixTheme.colors.textSecondary,
            maxLines = 1
        )
    }
    val levels = Levels.map { level ->
        GroupButton(
            text = "$level",
            onClick = { onIntent(WorkspaceIntent.Expand(expandedTo(root, level))) },
            contentDescription = workspaceStrings.openToLevel(level),
            tagKey = "level_$level"
        )
    }
    val all = GroupButton(
        text = workspaceStrings.openAll,
        onClick = { onIntent(WorkspaceIntent.Expand(expandedTo(root, level = null))) },
        contentDescription = workspaceStrings.openAllLevels,
        tagKey = "level_all"
    )
    PantrixButtonGroup(buttons = levels + all, size = layout.buttons, tagKey = "levels")
}

/**
 * A search in the tree: whether its bar is [open], what is looked for in [query], matched in [locale]'s small
 * letters, the values it is [found] in and which of them is chosen. Opening and closing the bar and choosing a match,
 * which opens the branches it is in, go to [onIntent].
 */
@Stable
internal class TreeSearch(
    val open: Boolean,
    private val chosen: MutableIntState,
    val query: TextFieldState,
    private val locale: PantrixLocale,
    private val found: State<Searched>,
    private val onIntent: (WorkspaceIntent) -> Unit
) {
    val matches: List<ValueNode> get() = found.value.matches

    /** Which of the [matches] is chosen, from 0. */
    val current: Int get() = chosen.intValue

    /** Opens the bar, or closes it and forgets what was looked for. */
    fun toggle() = if (open) close() else onIntent(WorkspaceIntent.Search(open = true))

    fun close() {
        onIntent(WorkspaceIntent.Search(open = false))
        query.clearText()
    }

    /** Goes [by] matches on, round from the last to the first, and chooses the one it comes to. */
    fun step(by: Int) {
        if (matches.isEmpty()) return
        chosen.intValue = (chosen.intValue + by).mod(matches.size)
        onIntent(WorkspaceIntent.Select(matches[chosen.intValue].key))
    }

    /** Goes to the first match of a new search. */
    fun restart() {
        chosen.intValue = 0
        matches.firstOrNull()?.let { onIntent(WorkspaceIntent.Select(it.key)) }
    }

    /** What the tree's rows mark of this search, or null while nothing is looked for. */
    fun marks(): TreeMarks? {
        val text = query.text.toString()
        if (!open || text.isBlank()) return null
        return TreeMarks(sought = locale.fold(text.trim()), locale = locale, current = matches.getOrNull(current)?.key)
    }

    /**
     * Which match of how many is chosen, "No matches", or nothing while nothing is looked for or what is typed is still
     * looked for: a large document's search takes a moment, and its matches are not yet none.
     */
    @Composable
    fun position(): String? {
        val text = query.text.toString()
        if (found.value.query != text) return null
        return searchPosition(text, chosen.intValue, matches.size)
    }

    /** "match 1 of 4", where [node] is the chosen match, as a phone's card says it; or null. */
    @Composable
    fun matchOf(node: ValueNode): String? {
        if (matches.getOrNull(chosen.intValue)?.key != node.key) return null
        return workspaceStrings.match(PantrixTheme.strings.matchPosition(chosen.intValue + 1, matches.size))
    }
}

/** The search in [state]'s tree, its bar open as the document's view says; what it does goes to [onIntent]. */
@Composable
internal fun rememberTreeSearch(state: WorkspaceState, onIntent: (WorkspaceIntent) -> Unit): TreeSearch {
    val query = rememberTextFieldState()
    val chosen = rememberSaveable { mutableIntStateOf(0) }
    val locale = PantrixTheme.locale
    val text = query.text.toString()
    val root = state.root
    // Looked for off the main thread, as a large document has a million values to fold to small letters; a letter
    // typed before the search ends sets it aside for the next. Letters typed one after another wait for the typing to
    // pause: every search marks the tree's rows again.
    val search = produceState(Searched(), root, text, locale) {
        if (text.isNotBlank() && value.query.isNotBlank() && value.query != text) delay(TYPING_PAUSE)
        value = Searched(text, withContext(Dispatchers.Default) { matchesOf(root, text, locale) })
    }
    val intents = rememberUpdatedState(onIntent)
    val open = state.document?.view?.searching == true
    val tree = remember(open, query, chosen, search, locale) {
        TreeSearch(
            open = open,
            chosen = chosen,
            query = query,
            locale = locale,
            found = search,
            onIntent = { intents.value(it) }
        )
    }
    // A new search goes to its first match, once its matches are found.
    val done = search.value
    LaunchedEffect(done) { if (done.query == text && text.isNotBlank()) tree.restart() }
    return tree
}

/** The [matches] of a search for [query]: what the tree's search found last, and what it looked for. */
internal class Searched(val query: String = "", val matches: List<ValueNode> = emptyList())

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
