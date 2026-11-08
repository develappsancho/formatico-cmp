package com.developersancho.formatico

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.feature.workspace.DocumentMoreMenu
import com.developersancho.formatico.feature.workspace.OpenDocument
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkbenchState
import com.pantrixui.foundation.adaptive.PantrixWindowFrame

/** The bar as a case shows it: over [workbench]'s documents and its chosen tab, on [board]. */
internal class AppTopBarCase(val workbench: WorkbenchState, val board: Board)

private val Open = listOf(
    OpenDocument(id = 1, name = "order.json", text = "{}", format = Format.Json),
    OpenDocument(id = 2, name = "deployment.yaml", text = "a: 1", format = Format.Yaml, edited = true),
    OpenDocument(id = 3, name = null, text = "", format = null)
)

private val Many = Open + listOf("countries.csv", "Cargo.toml", "settings.properties", "feed.xml", "logs.ndjson")
    .mapIndexed { index, name -> OpenDocument(id = 4L + index, name = name, text = "", format = Format.fromFileName(name)) }

/**
 * The bar's cases on each board: nothing open, documents open while a new tab is shown, a document shown, and on the
 * desktop more tabs than its row holds.
 */
internal class AppTopBarCases :
    PreviewCases<AppTopBarCase>(
        "nothing open" to AppTopBarCase(WorkbenchState(), Board.Desktop),
        "nothing open on a tablet" to AppTopBarCase(WorkbenchState(), Board.Tablet),
        "nothing open on a phone" to AppTopBarCase(WorkbenchState(), Board.Phone),
        "new tab" to AppTopBarCase(WorkbenchState(Open), Board.Desktop),
        "new tab on a tablet" to AppTopBarCase(WorkbenchState(Open), Board.Tablet),
        "new tab on a phone" to AppTopBarCase(WorkbenchState(Open), Board.Phone),
        "document" to AppTopBarCase(WorkbenchState(Open, selectedId = 2), Board.Desktop),
        "document on a tablet" to AppTopBarCase(WorkbenchState(Open, selectedId = 2), Board.Tablet),
        "document on a phone" to AppTopBarCase(WorkbenchState(Open, selectedId = 2), Board.Phone),
        "many tabs" to AppTopBarCase(WorkbenchState(Many, selectedId = 8), Board.Desktop)
    )

@Preview
@Composable
internal fun AppTopBarPreview(@PreviewParameter(AppTopBarCases::class) case: PreviewCase<AppTopBarCase>) {
    val workbench = remember { Workbench() }
    FormaticoPreview(case, Modifier.width(case.value.board.width.dp)) {
        // The bar lays itself out by the window: its board's, whatever room the preview has.
        PantrixWindowFrame(window = it.board.window) {
            AppTopBar(
                insets = PaddingValues(),
                workbench = it.workbench,
                onSelectTab = {},
                darkTheme = case.dark,
                onToggleTheme = {},
                onOpenPalette = {},
                documentMenu = { id -> DocumentMoreMenu(documentId = id, workbench = workbench) }
            )
        }
    }
}
