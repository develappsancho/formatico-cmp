package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases

/** A wide window's tree pane at its narrowest, where the search bar has the least room. */
private const val NARROW_PANE = 340

/**
 * A search in the tree for [query], [count] values found and the one at [current] chosen, in a bar laid out for
 * [layout], as wide as its board or as a pane [width] wide.
 */
internal class SearchCase(val query: String, val count: Int, val current: Int, val layout: WorkspaceLayout, val width: Int? = null) {
    val board: Board get() = LaidOutState(WorkspaceState(), layout).board
}

/**
 * The search bar's cases: a search that finds four values, one that finds none, the same in a wide window's narrow tree
 * pane, one not typed yet, a tablet's and a phone's.
 */
internal class TreeSearchBarCases :
    PreviewCases<SearchCase>(
        "found" to SearchCase("elif", count = 4, current = 0, WorkspaceLayout.Wide),
        "no matches" to SearchCase("elif yilmaz", count = 0, current = 0, WorkspaceLayout.Wide),
        "no matches in a narrow pane" to SearchCase("elif yilmaz", count = 0, current = 0, WorkspaceLayout.Wide, width = NARROW_PANE),
        "not typed" to SearchCase("", count = 0, current = 0, WorkspaceLayout.Wide),
        "found on a tablet" to SearchCase("elif", count = 4, current = 2, WorkspaceLayout.Tablet),
        "found on a phone" to SearchCase("elif", count = 4, current = 0, WorkspaceLayout.Phone)
    )

@Preview
@Composable
internal fun TreeSearchBarPreview(@PreviewParameter(TreeSearchBarCases::class) case: PreviewCase<SearchCase>) {
    FormaticoPreview(case, Modifier.width((case.value.width ?: case.value.board.width).dp)) { search ->
        TreeSearchBar(
            query = rememberTextFieldState(search.query),
            position = searchPosition(search.query, search.current, search.count),
            layout = search.layout,
            onStep = {},
            onClose = {}
        )
    }
}
