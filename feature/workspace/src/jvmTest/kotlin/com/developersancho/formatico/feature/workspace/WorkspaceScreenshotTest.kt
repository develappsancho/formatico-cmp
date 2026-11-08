package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.captureOpenMenu
import com.developersancho.formatico.core.testing.captureOverlay
import com.developersancho.formatico.core.testing.capturePreview
import kotlin.test.Test

/**
 * The workspace's previews, every case: the screen at every board, a component at its own. `recordRoborazziJvm`
 * writes each under `screenshots/`, named by what it shows, its case and its board's width
 * (`StatusBar/invalid-dark-1440.png`), and `verifyRoborazziJvm` compares against them.
 */
class WorkspaceScreenshotTest {
    @Test
    fun share() = ShareCases().values.forEach { case ->
        Board.entries.forEach { board -> captureOverlay("Share", case, board) { SharePreview(case) } }
    }

    @Test
    fun workspaceScreen() = WorkspaceScreenCases().values.forEach { case ->
        Board.entries.forEach { board -> capturePreview("WorkspaceScreen", case, board) { WorkspaceScreenPreview(case) } }
    }

    @Test
    fun statusBar() = StatusBarCases().values.forEach { case ->
        capturePreview("StatusBar", case, case.value.board) { StatusBarPreview(case) }
    }

    @Test
    fun pathTrail() = PathTrailCases().values.forEach { case ->
        capturePreview("PathTrail", case, case.value.board) { PathTrailPreview(case) }
    }

    @Test
    fun tableRecord() = TableRecordCases().values.forEach { case ->
        capturePreview("TableRecord", case, Board.Phone) { TableRecordPreview(case) }
    }

    @Test
    fun columnSummary() = ColumnSummaryCases().values.forEach { case ->
        capturePreview("ColumnSummary", case, Board.Desktop) { ColumnSummaryPreview(case) }
    }

    @Test
    fun sortSummary() = SortSummaryCases().values.forEach { case ->
        capturePreview("SortSummary", case, Board.Desktop) { SortSummaryPreview(case) }
    }

    @Test
    fun treePaneMessage() = TreePaneMessageCases().values.forEach { case ->
        capturePreview("TreePaneMessage", case, Board.Desktop) { TreePaneMessagePreview(case) }
    }

    @Test
    fun editorComing() = EditorComingCases().values.forEach { case ->
        capturePreview("EditorComing", case, Board.Desktop) { EditorComingPreview(case) }
    }

    @Test
    fun viewChoice() = ViewChoiceCases().values.forEach { case ->
        capturePreview("ViewChoice", case, case.value.board) { ViewChoicePreview(case) }
    }

    @Test
    fun documentTree() = DocumentTreeCases().values.forEach { case ->
        capturePreview("DocumentTree", case, case.value.board) { DocumentTreePreview(case) }
    }

    @Test
    fun query() = QueryCases().values.forEach { case ->
        capturePreview("Query", case, case.value.board) { QueryPreview(case) }
    }

    @Test
    fun convert() = ConvertCases().values.forEach { case ->
        capturePreview("Convert", case, case.value.board) { ConvertPreview(case) }
    }

    @Test
    fun types() = TypesCases().values.forEach { case ->
        capturePreview("Types", case, case.value.board) { TypesPreview(case) }
    }

    @Test
    fun graph() = GraphCases().values.forEach { case ->
        capturePreview("Graph", case, case.value.board) { GraphPreview(case) }
    }

    @Test
    fun schema() = SchemaCases().values.forEach { case ->
        capturePreview("Schema", case, case.value.board) { SchemaPreview(case) }
    }

    @Test
    fun compare() = CompareCases().values.forEach { case ->
        capturePreview("Compare", case, case.value.board) { ComparePreview(case) }
    }

    @Test
    fun treeSearchBar() = TreeSearchBarCases().values.forEach { case ->
        capturePreview("TreeSearchBar", case, case.value.board) { TreeSearchBarPreview(case) }
    }

    @Test
    fun openMenus() = WorkspaceScreenCases().values.forEach { case ->
        when (case.name) {
            "json" -> {
                captureOpenMenu("FormatMenu", case, Board.Desktop, opener = "format_menu") { WorkspaceScreenPreview(case) }
                captureOpenMenu("DocumentMenu", case, Board.Desktop, opener = "more") { WorkspaceScreenPreview(case) }
                captureOpenMenu("CopyPathMenu", case, Board.Desktop, opener = "copy_path") { WorkspaceScreenPreview(case) }
                captureOpenMenu("CopyPathMenu", case, Board.Tablet, opener = "copy_path") { WorkspaceScreenPreview(case) }
            }

            "yaml" -> {
                captureOpenMenu("CopyPathMenu", case, Board.Phone, opener = "copy_path") { WorkspaceScreenPreview(case) }
            }

            "convert" -> {
                captureOpenMenu("TargetMenu", case, Board.Desktop, opener = "convert_to") { WorkspaceScreenPreview(case) }
            }

            "schema" -> {
                captureOpenMenu("SchemaMenu", case, Board.Desktop, opener = "schema_against") { WorkspaceScreenPreview(case) }
            }

            "compare" -> {
                captureOpenMenu("PartnerMenu", case, Board.Desktop, opener = "partner") { WorkspaceScreenPreview(case) }
            }

            "json table" -> {
                captureOpenMenu("ColumnsChooser", case, Board.Desktop, opener = "columns") { WorkspaceScreenPreview(case) }
                captureOpenMenu("TableOutMenu", case, Board.Desktop, opener = "table_out") { WorkspaceScreenPreview(case) }
            }
        }
    }
}
