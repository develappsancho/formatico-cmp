package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.NullHandling

/** [text], named [name] and read as [format], converted as [choice] says. */
private fun converted(name: String, text: String, format: Format?, choice: ConvertChoice = ConvertChoice()) =
    previewState(name, text, format, DocumentView(pane = DocumentPane.Convert, convert = choice))

/** The order converted to YAML, the first format a JSON document is converted to: Convert-1440's and -390's state. */
internal val OrderToYaml = converted("order.json", PreviewOrder, Format.Json)

/** The order converted to TOML, its nulls left out: Convert-834's state. */
internal val OrderToToml = converted("order.json", PreviewOrder, Format.Json, ConvertChoice(to = Format.Toml))

/** A case of the conversion: [state], laid out for [layout], in a pane as tall as [height]. */
internal class ConvertCase(val state: WorkspaceState, val layout: WorkspaceLayout, val height: Int) {
    /** The board the pane stands in, and the width it has there: half a wide window, or the whole. */
    val board get() = LaidOutState(state, layout).board

    val width: Int
        get() = if (layout == WorkspaceLayout.Wide) board.width / 2 else board.width
}

/**
 * The conversion's cases: the order to YAML, nothing lost, at each width, and to TOML on a tablet and a phone, its
 * nulls left out or written as empty strings; to XML, with many losses, to CSV, and to JSON from a YAML document with
 * comments and aliases; an invalid document, which is repaired first; and plain text, which has nothing to convert.
 */
internal class ConvertCases :
    PreviewCases<ConvertCase>(
        "yaml" to ConvertCase(OrderToYaml, WorkspaceLayout.Wide, height = 760),
        "toml on a tablet" to ConvertCase(OrderToToml, WorkspaceLayout.Tablet, height = 1000),
        "yaml on a phone" to ConvertCase(OrderToYaml, WorkspaceLayout.Phone, height = 844),
        "toml on a phone" to ConvertCase(
            converted("order.json", PreviewOrder, Format.Json, ConvertChoice(to = Format.Toml, nulls = NullHandling.EmptyString)),
            WorkspaceLayout.Phone,
            height = 844
        ),
        "xml" to
            ConvertCase(
                converted("order.json", PreviewOrder, Format.Json, ConvertChoice(to = Format.Xml, indent = 4)),
                WorkspaceLayout.Wide,
                height = 760
            ),
        "csv" to
            ConvertCase(
                converted("countries.csv", PreviewCountries, Format.Csv, ConvertChoice(to = Format.Json, sortKeys = true)),
                WorkspaceLayout.Tablet,
                height = 800
            ),
        "yaml to json" to ConvertCase(converted("deployment.yaml", PreviewDeployment, Format.Yaml), WorkspaceLayout.Wide, height = 760),
        "invalid" to ConvertCase(converted("settings.json", PreviewInvalid, Format.Json), WorkspaceLayout.Wide, height = 400),
        "plain text" to
            ConvertCase(converted("notes.txt", "Things to check before the release.\n", null), WorkspaceLayout.Phone, height = 600)
    )

@Preview
@Composable
internal fun ConvertPreview(@PreviewParameter(ConvertCases::class) case: PreviewCase<ConvertCase>) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { convert ->
        val commands = rememberDocumentCommands(remember { Workbench() })
        if (convert.layout == WorkspaceLayout.Phone) {
            PhoneConvert(state = convert.state, onIntent = {}, commands = commands)
        } else {
            ConvertPane(state = convert.state, layout = convert.layout, onIntent = {}, commands = commands)
        }
    }
}
