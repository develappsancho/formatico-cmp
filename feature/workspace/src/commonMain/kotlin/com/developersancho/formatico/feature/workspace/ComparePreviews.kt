package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.designsystem.FormaticoPreview
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.developersancho.formatico.core.designsystem.PreviewCases
import com.developersancho.formatico.core.format.Format

/** The order A of the Compare boards. */
internal val PreviewCompareA =
    """
    {
      "id": "ord_8842",
      "status": "shipped",
      "createdAt": "2026-10-28T14:32:00Z",
      "total": 129.90,
      "coupon": null,
      "items": [
        { "sku": "TS-001", "qty": 2 },
        { "sku": "MG-014", "qty": 1 }
      ]
    }
    """.trimIndent() + "\n"

/** The order B of the Compare boards: delivered, with the second item's quantity changed. */
internal val PreviewCompareB =
    """
    {
      "status": "delivered",
      "id": "ord_8842",
      "createdAt": "2026-10-28T14:32:00Z",
      "deliveredAt": "2026-11-01T10:05:00Z",
      "total": 129.9,
      "items": [
        { "sku": "TS-001", "qty": 2 },
        { "sku": "MG-014", "qty": 2 }
      ]
    }
    """.trimIndent() + "\n"

/** B of the boards, open in the tab after A's. */
private val OrderV2 = OpenDocument(id = 2, name = "order-v2.json", text = PreviewCompareB, format = Format.Json)

/** A compared with [others], as [choice] says. */
private fun compared(
    choice: CompareChoice = CompareChoice(),
    others: List<OpenDocument> = listOf(
        OrderV2
    ),
    text: String = PreviewCompareA
) =
    previewState("order.json", text, Format.Json, DocumentView(pane = DocumentPane.Compare, compare = choice), others)

/** The two orders compared by meaning, the first difference shown: the Compare boards' state. */
internal val OrdersCompared = compared()

/** The two orders compared by text. */
internal val OrdersComparedAsText = compared(CompareChoice(by = CompareBy.Text))

/** A case of the comparison: [state], laid out for [layout]. */
internal class CompareCase(val state: WorkspaceState, val layout: WorkspaceLayout) {
    val board get() = LaidOutState(state, layout).board
}

/**
 * The comparison's cases: the boards' two orders by meaning at each width, the last difference shown on a phone, and by
 * text; the order against itself in YAML, which differs in nothing; one open document alone; and B with errors, and B
 * in plain text, which have nothing to compare by meaning.
 */
internal class CompareCases :
    PreviewCases<CompareCase>(
        "meaning" to CompareCase(OrdersCompared, WorkspaceLayout.Wide),
        "meaning on a tablet" to CompareCase(OrdersCompared, WorkspaceLayout.Tablet),
        "meaning on a phone" to CompareCase(OrdersCompared, WorkspaceLayout.Phone),
        "last on a phone" to CompareCase(compared(CompareChoice(current = 3)), WorkspaceLayout.Phone),
        "text" to CompareCase(OrdersComparedAsText, WorkspaceLayout.Wide),
        "the same" to CompareCase(
            compared(
                others = listOf(
                    OpenDocument(id = 2, name = "order.yaml", text = convertedYaml(), format = Format.Yaml)
                )
            ),
            WorkspaceLayout.Tablet
        ),
        "alone" to CompareCase(compared(others = emptyList()), WorkspaceLayout.Wide),
        "invalid" to CompareCase(
            compared(others = listOf(OpenDocument(id = 2, name = "settings.json", text = PreviewInvalid, format = Format.Json))),
            WorkspaceLayout.Tablet
        ),
        "plain text" to CompareCase(
            compared(others = listOf(OpenDocument(id = 2, name = "notes.txt", text = "Things to check.\n", format = null))),
            WorkspaceLayout.Phone
        )
    )

/** The boards' order A converted to YAML: the same values, written another way. */
private fun convertedYaml(): String =
    (
        previewState(
            "order.json",
            PreviewCompareA,
            Format.Json,
            DocumentView(pane = DocumentPane.Convert)
        ).conversion.converted
    )?.text.orEmpty()

@Preview
@Composable
internal fun ComparePreview(@PreviewParameter(CompareCases::class) case: PreviewCase<CompareCase>) {
    FormaticoPreview(case, Modifier.width(case.value.board.width.dp).height(case.value.board.height.dp)) { compare ->
        if (compare.layout == WorkspaceLayout.Phone) {
            PhoneCompare(state = compare.state, onIntent = {}, more = {})
        } else {
            CompareScreen(state = compare.state, layout = compare.layout, onIntent = {})
        }
    }
}
