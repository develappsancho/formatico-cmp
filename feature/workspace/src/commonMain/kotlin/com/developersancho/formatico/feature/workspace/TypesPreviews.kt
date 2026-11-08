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
import com.developersancho.formatico.core.format.types.TypeLanguage

/**
 * The order the boards of Types, Convert and Start's sample hold: [PreviewOrder]'s, its items with a quantity and no
 * date, and its shipping with an address.
 */
internal val PreviewBoardOrder =
    """
    {
      "id": "ord_8842",
      "status": "shipped",
      "createdAt": "2026-10-28T14:32:00Z",
      "total": 129.90,
      "currency": "EUR",
      "paid": true,
      "coupon": null,
      "customer": {
        "name": "Elif Yılmaz",
        "email": "elif@example.com",
        "avatar": "https://images.example.com/u/elif.png",
        "website": "https://elif.dev"
      },
      "theme": {
        "accent": "#0F766E"
      },
      "items": [
        { "sku": "TS-001", "name": "T-shirt", "qty": 2, "price": 39.95 },
        { "sku": "MG-014", "name": "Mug", "qty": 1, "price": 50.00 }
      ],
      "shipping": {
        "method": "express",
        "eta": "2026-11-04",
        "address": {
          "city": "İzmir",
          "country": "TR"
        }
      }
    }
    """.trimIndent() + "\n"

/** [text], named [name] and read as [format], its types written as [choice] says. */
private fun typed(name: String, text: String, format: Format?, choice: TypesChoice = TypesChoice()) =
    previewState(name, text, format, DocumentView(pane = DocumentPane.Types, types = choice))

/** The order's types in Kotlin, the language a document's types are written in first: Types-1440's state. */
internal val OrderInKotlin = typed("order.json", PreviewBoardOrder, Format.Json)

/** The order's types in TypeScript: Types-834's state. */
internal val OrderInTypeScript = typed("order.json", PreviewBoardOrder, Format.Json, TypesChoice(language = TypeLanguage.TypeScript))

/** The order's types in Swift: Types-390's state. */
internal val OrderInSwift = typed("order.json", PreviewBoardOrder, Format.Json, TypesChoice(language = TypeLanguage.Swift))

/** A case of the types: [state], laid out for [layout], in a pane as tall as [height]. */
internal class TypesCase(val state: WorkspaceState, val layout: WorkspaceLayout, val height: Int) {
    /** The board the pane stands in, and the width it has there: half a wide window, or the whole. */
    val board get() = LaidOutState(state, layout).board

    val width: Int
        get() = if (layout == WorkspaceLayout.Wide) board.width / 2 else board.width
}

/**
 * The types' cases: the order's in Kotlin, TypeScript and Swift, as the three boards draw them, in Go and as a JSON
 * Schema; its root type
 * named by the reader; a list of events, NDJSON, whose root is one of its lines; a deployment, YAML; a document with
 * no object, which has no types; an invalid document, which is repaired first; and plain text, which has none.
 */
internal class TypesCases :
    PreviewCases<TypesCase>(
        "kotlin" to TypesCase(OrderInKotlin, WorkspaceLayout.Wide, height = 760),
        "typescript on a tablet" to TypesCase(OrderInTypeScript, WorkspaceLayout.Tablet, height = 1000),
        "swift on a phone" to TypesCase(OrderInSwift, WorkspaceLayout.Phone, height = 844),
        "go" to
            TypesCase(
                typed("order.json", PreviewBoardOrder, Format.Json, TypesChoice(language = TypeLanguage.Go)),
                WorkspaceLayout.Wide,
                height = 760
            ),
        "json schema" to
            TypesCase(
                typed("order.json", PreviewBoardOrder, Format.Json, TypesChoice(language = TypeLanguage.JsonSchema)),
                WorkspaceLayout.Wide,
                height = 760
            ),
        "named root" to TypesCase(
            typed("order.json", PreviewBoardOrder, Format.Json, TypesChoice(root = "Purchase")),
            WorkspaceLayout.Tablet,
            height = 1000
        ),
        "list" to TypesCase(typed("events.ndjson", PreviewEvents, Format.Ndjson), WorkspaceLayout.Wide, height = 760),
        "yaml" to TypesCase(
            typed("deployment.yaml", PreviewDeployment, Format.Yaml, TypesChoice(language = TypeLanguage.Go)),
            WorkspaceLayout.Phone,
            height = 844
        ),
        "no object" to TypesCase(typed("totals.json", "[129.9, 50, 39.95]\n", Format.Json), WorkspaceLayout.Wide, height = 400),
        "invalid" to TypesCase(typed("settings.json", PreviewInvalid, Format.Json), WorkspaceLayout.Wide, height = 400),
        "plain text" to TypesCase(typed("notes.txt", "Things to check before the release.\n", null), WorkspaceLayout.Phone, height = 600)
    )

@Preview
@Composable
internal fun TypesPreview(@PreviewParameter(TypesCases::class) case: PreviewCase<TypesCase>) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { types ->
        val commands = rememberDocumentCommands(remember { Workbench() })
        if (types.layout == WorkspaceLayout.Phone) {
            PhoneTypes(state = types.state, onIntent = {}, commands = commands)
        } else {
            TypesPane(state = types.state, layout = types.layout, onIntent = {}, commands = commands)
        }
    }
}
