package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.developersancho.formatico.core.format.csv.csvText
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.components.overlay.popover.PopoverAlignment
import com.pantrixui.foundation.icons.PantrixIcons
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_copy_csv
import formatico.feature.workspace.generated.resources.workspace_copy_markdown
import formatico.feature.workspace.generated.resources.workspace_save_csv
import formatico.feature.workspace.generated.resources.workspace_table_out
import org.jetbrains.compose.resources.stringResource

/** What the table's rows, as they are shown, are saved or copied as. */
internal enum class TableOut { SaveCsv, CopyCsv, CopyMarkdown }

/** The button that saves or copies the table, and its menu: saved as a CSV file, copied as CSV or as a Markdown table. */
@Composable
internal fun TableOutMenu(size: ButtonSize, onOut: (TableOut) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixIconButton(
                imageVector = PantrixIcons.Download,
                contentDescription = stringResource(Res.string.workspace_table_out),
                onClick = { open = true },
                variant = if (open) ButtonVariant.Soft else ButtonVariant.Text,
                size = size,
                tagKey = "table_out"
            )
        },
        alignment = PopoverAlignment.End,
        tagKey = "table_out"
    ) {
        PantrixMenuItem(
            text = stringResource(Res.string.workspace_save_csv),
            onClick = { onOut(TableOut.SaveCsv) },
            icon = PantrixIcons.Download,
            tagKey = "save_csv"
        )
        PantrixMenuItem(
            text = stringResource(Res.string.workspace_copy_csv),
            onClick = { onOut(TableOut.CopyCsv) },
            icon = PantrixIcons.Copy,
            tagKey = "copy_csv"
        )
        PantrixMenuItem(
            text = stringResource(Res.string.workspace_copy_markdown),
            onClick = { onOut(TableOut.CopyMarkdown) },
            icon = PantrixIcons.Copy,
            tagKey = "copy_markdown"
        )
    }
}

/** Saves [rows], the table as it is shown, as [out] says: as [document]'s CSV file, or copied as CSV or Markdown. */
internal fun DocumentCommands.out(out: TableOut, document: OpenDocument, rows: List<List<String>>) = when (out) {
    TableOut.SaveCsv -> saveCsv(document, csvText(rows))
    TableOut.CopyCsv -> copyText(Copied.Table, csvText(rows))
    TableOut.CopyMarkdown -> copyText(Copied.Table, markdownTable(rows))
}
