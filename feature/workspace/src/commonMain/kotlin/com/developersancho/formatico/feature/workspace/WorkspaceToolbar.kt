package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.container.toolbar.PantrixToolbar
import com.pantrixui.components.container.toolbar.ToolbarItem
import com.pantrixui.components.container.toolbar.ToolbarMenuItem
import com.pantrixui.components.container.toolbar.ToolbarStyle
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** The design's toolbar heights: 48 in a wide window, 56 on a tablet. */
private fun toolbarHeight(layout: WorkspaceLayout): Dp = if (layout == WorkspaceLayout.Wide) 48.dp else 56.dp

/**
 * How long each of the toolbar's items stays as room runs short, the lowest going first: drawn as its icon where it
 * has one, and then folded into the "…" menu, before the next gives way. The view shown keeps its button as long as
 * Repair: the bar says which is open.
 */
private object ToolbarPriority {
    const val FORMATS = 10
    const val COPY = 10
    const val REPAIR = 9
    const val SHOWN = 9
    const val FORMAT = 8
    const val QUERY = 7
    const val CONVERT = 6
    const val MINIFY = 5
    const val TYPES = 4
    const val SCHEMA = 3
    const val SORT_KEYS = 2
    const val SAVE = 2
    const val COMPARE = 1
}

/**
 * The actions on the whole document, over its text and its tree: the format it is read as, what changes it, what is
 * made of it, and at the end copying it and the "…" menu, which holds sharing and closing it. In a wide window Sort
 * keys, Compare and Save have buttons of their own; a tablet's menu holds them, as Workspace-834 draws it. Where even so
 * the bar has no room, in Turkish or with a document to repair, Pantrix's toolbar draws the lowest button as its icon or
 * folds it into the menu, one at a time, by their [ToolbarPriority].
 */
@Composable
internal fun WorkspaceToolbar(
    state: WorkspaceState,
    document: OpenDocument,
    layout: WorkspaceLayout,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
) {
    val wide = layout == WorkspaceLayout.Wide
    val formats = formatsItem(document.format) { onIntent(WorkspaceIntent.ChangeFormat(it)) }
    val edits = editItems(state, wide, onIntent)
    val made = madeItems(document, wide, onIntent)
    val copy = ToolbarItem.IconButton(
        key = "copy",
        icon = PantrixIcons.Copy,
        label = workspaceStrings.copyDocument,
        onClick = { commands.copy(document) },
        priority = ToolbarPriority.COPY
    )
    val save = ToolbarItem.IconButton(
        key = "save",
        icon = PantrixIcons.Download,
        label = workspaceStrings.saveFile,
        onClick = { commands.save(document) },
        priority = ToolbarPriority.SAVE
    )
    PantrixToolbar(
        start = buildList {
            add(formats)
            if (wide) add(ToolbarItem.Divider(key = "reading"))
            addAll(edits)
            add(ToolbarItem.Divider(key = "made"))
            addAll(made)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarHeight(layout))
            .padding(horizontal = PantrixTheme.spacing.md),
        end = if (wide) listOf(copy, save) else listOf(copy),
        menuItems = menuRows(state, document, wide, onIntent, commands),
        buttonSize = layout.buttons,
        // As the boards draw it on every platform: a wide window's bar is used with a pointer, a tablet's by touch.
        style = if (wide) PointerToolbar else TouchToolbar,
        tagKey = "document"
    )
}

/** A wide window's toolbar, as the 1440 boards draw it: 8 between its items, its lines 24 tall. */
private val PointerToolbar = ToolbarStyle(gap = 8.dp, dividerHeight = 24.dp)

/** A tablet's toolbar, as the 834 boards draw it: 4 between its items, which take the touch target, its lines 28 tall. */
private val TouchToolbar = ToolbarStyle(gap = 4.dp, dividerHeight = 28.dp)

/** The button that says the document's format and opens the menu of the formats it can be read as. */
@Composable
private fun formatsItem(format: Format?, onChange: (Format?) -> Unit): ToolbarItem.Menu {
    val plain = workspaceStrings.plainText
    val rows = Format.entries.map { entry ->
        ToolbarMenuItem(key = entry.name.lowercase(), label = entry.displayName, onClick = { onChange(entry) }, selected = entry == format)
    } + ToolbarMenuItem(key = "plain", label = plain, onClick = { onChange(null) }, selected = format == null)
    val name = format?.displayName ?: plain
    // The button writes the format's name, and says what it is for to a screen reader, as the Workspace boards name it.
    return ToolbarItem.Menu(
        key = "format_menu",
        label = name,
        items = rows,
        variant = ButtonVariant.Outline,
        priority = ToolbarPriority.FORMATS,
        contentDescription = workspaceStrings.formatButton(name)
    )
}

/**
 * Repair where there is something to repair, Format, Minify, and in a wide window Sort keys: what changes the whole
 * document. An invalid document is repaired before it is formatted: the Invalid boards put Repair first.
 */
@Composable
private fun editItems(state: WorkspaceState, wide: Boolean, onIntent: (WorkspaceIntent) -> Unit): List<ToolbarItem> {
    val strings = workspaceStrings

    fun edit(edit: DocumentEdit) = { onIntent(WorkspaceIntent.Edit(edit)) }
    return listOfNotNull(
        ToolbarItem.Button(
            key = "repair",
            label = strings.repair,
            onClick = { onIntent(WorkspaceIntent.RepairAll) },
            variant = ButtonVariant.Solid,
            priority = ToolbarPriority.REPAIR
        ).takeIf { state.repairable > 0 },
        ToolbarItem.Button(
            key = "format",
            label = strings.format,
            onClick = edit(DocumentEdit.Format),
            variant = ButtonVariant.Solid,
            enabled = DocumentEdit.Format in state.edits,
            priority = ToolbarPriority.FORMAT
        ),
        ToolbarItem.Button(
            key = "minify",
            label = strings.minify,
            onClick = edit(DocumentEdit.Minify),
            variant = ButtonVariant.Outline,
            enabled = DocumentEdit.Minify in state.edits,
            priority = ToolbarPriority.MINIFY
        ),
        ToolbarItem.Button(
            key = "sort_keys",
            label = strings.sortKeys,
            onClick = edit(DocumentEdit.SortKeys),
            variant = ButtonVariant.Subtle,
            enabled = DocumentEdit.SortKeys in state.edits,
            priority = ToolbarPriority.SORT_KEYS
        ).takeIf { wide }
    )
}

/**
 * What is made of the document, each a view of its own: Query, Convert, Types and Schema, and in a wide window Compare.
 * Each, pressed again, gives the view the document opens with back. Schema and Compare stand in words alone.
 */
@Composable
private fun madeItems(document: OpenDocument, wide: Boolean, onIntent: (WorkspaceIntent) -> Unit): List<ToolbarItem> {
    fun pane(pane: DocumentPane, label: String, icon: ImageVector?, priority: Int): ToolbarItem.Button {
        val shown = document.view.pane == pane
        return ToolbarItem.Button(
            key = pane.name.lowercase(),
            label = label,
            onClick = { onIntent(WorkspaceIntent.ShowPane(if (shown) openingPane(document.format) else pane)) },
            icon = icon,
            variant = if (shown) ButtonVariant.Soft else ButtonVariant.Text,
            enabled = document.format != null,
            priority = if (shown) ToolbarPriority.SHOWN else priority
        )
    }
    val strings = workspaceStrings
    return listOfNotNull(
        pane(DocumentPane.Query, strings.query, PantrixIcons.Search, ToolbarPriority.QUERY),
        pane(DocumentPane.Convert, strings.convert, PantrixIcons.ArrowForward, ToolbarPriority.CONVERT),
        pane(DocumentPane.Types, typesStrings.types, PantrixIcons.Code, ToolbarPriority.TYPES),
        pane(DocumentPane.Schema, schemaStrings.schema, null, ToolbarPriority.SCHEMA),
        ToolbarItem.Button(
            key = "compare",
            label = strings.compare,
            onClick = { onIntent(WorkspaceIntent.ShowPane(DocumentPane.Compare)) },
            priority = ToolbarPriority.COMPARE
        ).takeIf { wide }
    )
}

/** The "…" menu's own rows: on a tablet sorting the keys, comparing and saving; everywhere sharing and closing. */
@Composable
private fun menuRows(
    state: WorkspaceState,
    document: OpenDocument,
    wide: Boolean,
    onIntent: (WorkspaceIntent) -> Unit,
    commands: DocumentCommands
): List<ToolbarMenuItem> {
    val strings = workspaceStrings
    val tablet = if (wide) {
        emptyList()
    } else {
        listOf(
            ToolbarMenuItem(
                key = "sort_keys",
                label = strings.sortKeys,
                onClick = { onIntent(WorkspaceIntent.Edit(DocumentEdit.SortKeys)) },
                enabled = DocumentEdit.SortKeys in state.edits
            ),
            ToolbarMenuItem(key = "compare", label = strings.compare, onClick = {
                onIntent(WorkspaceIntent.ShowPane(DocumentPane.Compare))
            }),
            ToolbarMenuItem(key = "save", label = strings.saveFile, onClick = { commands.save(document) }, icon = PantrixIcons.Download)
        )
    }
    return tablet + listOf(
        ToolbarMenuItem(key = "share", label = strings.share, onClick = { onIntent(WorkspaceIntent.Share(open = true)) }),
        ToolbarMenuItem(key = "close", label = strings.closeDocument, onClick = {
            onIntent(WorkspaceIntent.Close)
        }, icon = PantrixIcons.Close)
    )
}
