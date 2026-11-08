package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases

/** What the Workspace boards' search for "elif" marks: its first match, the customer's name, the current one. */
private val ElifMarks = TreeMarks(sought = "elif", language = "en", current = "$.customer.name")

/** The tree of [state] in the tree's pane of [layout], with what a search [marks]. */
internal class TreeCase(val state: WorkspaceState, val layout: WorkspaceLayout, val marks: TreeMarks? = null) {
    val board: Board get() = LaidOutState(state, layout).board

    /** The pane's width: on the desktop the tree's share of the window beside the text, as its board draws it. */
    val width: Int get() = if (layout == WorkspaceLayout.Wide) WIDE_PANE else board.width

    /** As tall as the boards' rows, which a lazy tree is given room for. */
    val height: Int
        get() = when (layout) {
            WorkspaceLayout.Wide -> 560
            WorkspaceLayout.Tablet -> 640
            WorkspaceLayout.Phone -> 520
        }

    private companion object {
        /** 1440's share of 1.15 to 1 for the text and the tree. */
        const val WIDE_PANE = 670
    }
}

/**
 * The tree's cases: the order with a picture chosen and with nothing chosen, searched, a deployment's anchor and the
 * alias chosen; on a tablet and on a phone.
 */
internal class DocumentTreeCases :
    PreviewCases<TreeCase>(
        "json" to TreeCase(Order, WorkspaceLayout.Wide),
        "nothing chosen" to TreeCase(OrderUnchosen, WorkspaceLayout.Wide),
        "searched" to TreeCase(OrderSearched, WorkspaceLayout.Wide, ElifMarks),
        "yaml" to TreeCase(DeploymentAlias, WorkspaceLayout.Wide),
        "json on a tablet" to TreeCase(Order, WorkspaceLayout.Tablet),
        "searched on a phone" to TreeCase(OrderSearched, WorkspaceLayout.Phone, ElifMarks),
        "yaml on a phone" to TreeCase(DeploymentAlias, WorkspaceLayout.Phone)
    )

@Preview
@Composable
internal fun DocumentTreePreview(@PreviewParameter(DocumentTreeCases::class) case: PreviewCase<TreeCase>) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { tree ->
        DocumentTree(state = tree.state, layout = tree.layout, onIntent = {}, marks = tree.marks)
    }
}
