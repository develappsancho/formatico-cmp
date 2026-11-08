package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.developersancho.formatico.feature.workspace.strings.schemaStrings
import com.developersancho.formatico.feature.workspace.strings.typesStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.foundation.icons.PantrixIcons

/**
 * What is made of the document, each a view of its own: Query, Convert, Types and Schema. Each, pressed again, gives
 * the view the document opens with back. Types stands in words alone, as Compare and Schema do, as the boards draw it.
 */
@Composable
internal fun MadeButtons(document: OpenDocument, size: ButtonSize, onIntent: (WorkspaceIntent) -> Unit) {
    PaneButton(document, DocumentPane.Query, workspaceStrings.query, PantrixIcons.Search, size, onIntent, "query")
    PaneButton(document, DocumentPane.Convert, workspaceStrings.convert, PantrixIcons.ArrowForward, size, onIntent, "convert")
    PaneButton(document, DocumentPane.Types, typesStrings.types, null, size, onIntent, "types")
    PaneButton(document, DocumentPane.Schema, schemaStrings.schema, null, size, onIntent, "schema")
}

/** The button of [pane], soft while it is shown, which shows it, or gives the view the document opens with back. */
@Composable
private fun PaneButton(
    document: OpenDocument,
    pane: DocumentPane,
    text: String,
    icon: ImageVector?,
    size: ButtonSize,
    onIntent: (WorkspaceIntent) -> Unit,
    tagKey: String
) {
    val shown = document.view.pane == pane
    PantrixButton(
        text = text,
        onClick = { onIntent(WorkspaceIntent.ShowPane(if (shown) openingPane(document.format) else pane)) },
        variant = if (shown) ButtonVariant.Soft else ButtonVariant.Text,
        size = size,
        enabled = document.format != null,
        leadingIcon = icon?.let { { PantrixIcon(imageVector = it, contentDescription = null) } },
        tagKey = tagKey
    )
}
