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
 * the view the document opens with back. Types has the code's icon in a [wide] window (the owner's decision, Faz 21);
 * a tablet's toolbar has no room for it beside its "more" button, and Types stands there in words alone until the
 * toolbar moves what does not fit into a menu (PANTRIX_FINDINGS, "Sığmayan eylemleri menüsüne alan bir araç çubuğu
 * yok"). Schema stands in words alone, as Compare does.
 */
@Composable
internal fun MadeButtons(document: OpenDocument, size: ButtonSize, wide: Boolean, onIntent: (WorkspaceIntent) -> Unit) {
    PaneButton(document, DocumentPane.Query, workspaceStrings.query, PantrixIcons.Search, size, onIntent, "query")
    PaneButton(document, DocumentPane.Convert, workspaceStrings.convert, PantrixIcons.ArrowForward, size, onIntent, "convert")
    PaneButton(document, DocumentPane.Types, typesStrings.types, PantrixIcons.Code.takeIf { wide }, size, onIntent, "types")
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
