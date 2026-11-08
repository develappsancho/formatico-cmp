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
import com.pantrixui.components.data.codeeditor.CodeEditorState

/**
 * The schema of the Schema boards, order.schema.json, of draft 2020-12: the order fails it twice, its coupon, which is
 * null and must be a string, and its customer's phone, which it has not.
 */
internal val PreviewOrderSchema =
    """
    {
      "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
      "title": "Order",
      "type": "object",
      "required": ["id", "status", "total", "currency", "customer", "items"],
      "properties": {
        "id": { "type": "string", "pattern": "^ord_[0-9]+${'$'}" },
        "status": { "enum": ["pending", "paid", "shipped", "delivered"] },
        "createdAt": { "type": "string", "format": "date-time" },
        "total": { "type": "number", "minimum": 0 },
        "currency": { "type": "string", "minLength": 3, "maxLength": 3 },
        "paid": { "type": "boolean" },
        "coupon": { "type": "string" },
        "customer": { "${'$'}ref": "#/${'$'}defs/customer" },
        "items": { "type": "array", "minItems": 1, "items": { "${'$'}ref": "#/${'$'}defs/item" } },
        "shipping": { "${'$'}ref": "#/${'$'}defs/shipping" }
      },
      "${'$'}defs": {
        "customer": {
          "type": "object",
          "required": ["name", "email", "phone"],
          "properties": {
            "email": { "type": "string", "format": "email" },
            "website": { "type": "string", "format": "uri" }
          }
        },
        "item": {
          "type": "object",
          "required": ["sku", "qty", "price"],
          "properties": {
            "qty": { "type": "integer", "minimum": 1 },
            "price": { "type": "number", "exclusiveMinimum": 0 }
          }
        },
        "shipping": {
          "type": "object",
          "properties": {
            "method": { "enum": ["standard", "express"] },
            "eta": { "type": "string", "format": "date" },
            "address": { "${'$'}ref": "#/${'$'}defs/address" }
          }
        },
        "address": {
          "type": "object",
          "required": ["city", "country"],
          "properties": { "country": { "type": "string", "pattern": "^[A-Z]{2}${'$'}" } }
        }
      }
    }
    """.trimIndent() + "\n"

/** The order as its schema allows it: its coupon a string, and its customer with a phone. */
internal val PreviewValidOrder = PreviewBoardOrder
    .replace("\"coupon\": null", "\"coupon\": \"AUTUMN\"")
    .replace("\"name\": \"Elif Yılmaz\",", "\"name\": \"Elif Yılmaz\",\n    \"phone\": \"+90 555 010 0100\",")

/** order.schema.json, open in a tab of its own beside the order. */
internal val PreviewSchemaDocument = OpenDocument(id = 2, name = "order.schema.json", text = PreviewOrderSchema, format = Format.Json)

/** [text], named [name] and read as [format], validated against the open [schema] where [chosen] says so. */
private fun validated(name: String, text: String, format: Format?, schema: OpenDocument? = PreviewSchemaDocument, chosen: Boolean = true) =
    previewState(
        name,
        text,
        format,
        DocumentView(pane = DocumentPane.Schema, schema = SchemaChoice(with = schema?.id?.takeIf { chosen })),
        listOfNotNull(schema)
    )

/** The order against order.schema.json: its coupon and its customer's phone, as Schema-1440 draws them. */
internal val OrderAgainstSchema = validated("order.json", PreviewBoardOrder, Format.Json)

/** The order as its schema allows it, valid against it. */
internal val ValidOrderAgainstSchema = validated("order.json", PreviewValidOrder, Format.Json)

/** The order with no schema chosen, order.schema.json open beside it. */
internal val OrderWithNoSchema = validated("order.json", PreviewBoardOrder, Format.Json, chosen = false)

/** The order against a schema whose `$ref` points at nothing: its address's, `#/$defs/adress`. */
internal val OrderAgainstBrokenSchema = validated(
    "order.json",
    PreviewBoardOrder,
    Format.Json,
    PreviewSchemaDocument.copy(text = PreviewOrderSchema.replace("#/\$defs/address", "#/\$defs/adress"))
)

/** Where the Schema boards leave the caret: in the order's coupon, its first problem. */
internal val SchemaCaret = PreviewBoardOrder.indexOf("null")

/** A case of Schema: [state], laid out for [layout], in a pane as tall as [height]. */
internal class SchemaCase(val state: WorkspaceState, val layout: WorkspaceLayout, val height: Int) {
    /** The board the pane stands in, and the width it has there: half a wide window, or the whole. */
    val board get() = LaidOutState(state, layout).board

    val width: Int
        get() = if (layout == WorkspaceLayout.Wide) board.width / 2 else board.width
}

/**
 * Schema's cases: the order's problems against its schema, valid against it, with none chosen, and against a schema
 * that cannot be used, on each board's width; a document whose `$schema` names a schema not open; YAML against the same
 * schema; an invalid document, which is repaired first; a CSV, which a schema does not validate; and plain text.
 */
internal class SchemaCases :
    PreviewCases<SchemaCase>(
        "problems" to SchemaCase(OrderAgainstSchema, WorkspaceLayout.Wide, height = 760),
        "problems on a tablet" to SchemaCase(OrderAgainstSchema, WorkspaceLayout.Tablet, height = 1000),
        "valid on a tablet" to SchemaCase(ValidOrderAgainstSchema, WorkspaceLayout.Tablet, height = 1000),
        "none on a phone" to SchemaCase(OrderWithNoSchema, WorkspaceLayout.Phone, height = 844),
        "problems on a phone" to SchemaCase(OrderAgainstSchema, WorkspaceLayout.Phone, height = 844),
        "unusable" to SchemaCase(OrderAgainstBrokenSchema, WorkspaceLayout.Wide, height = 760),
        "named" to SchemaCase(
            validated(
                "order.json",
                PreviewBoardOrder.replaceFirst("{\n", "{\n  \"\$schema\": \"https://example.com/schemas/order.json\",\n"),
                Format.Json,
                chosen = false
            ),
            WorkspaceLayout.Tablet,
            height = 1000
        ),
        "yaml" to SchemaCase(
            validated(
                "order.yaml",
                "id: ord_8842\nstatus: lost\ntotal: -1\ncurrency: EUR\ncustomer:\n  name: Elif\nitems: []\n",
                Format.Yaml
            ),
            WorkspaceLayout.Wide,
            height = 760
        ),
        "invalid" to SchemaCase(validated("settings.json", PreviewInvalid, Format.Json), WorkspaceLayout.Wide, height = 400),
        "csv" to SchemaCase(validated("orders.csv", "id,total\nord_8842,129.90\n", Format.Csv), WorkspaceLayout.Wide, height = 400),
        "plain text" to
            SchemaCase(validated("notes.txt", "Things to check before the release.\n", null), WorkspaceLayout.Phone, height = 600)
    )

@Preview
@Composable
internal fun SchemaPreview(@PreviewParameter(SchemaCases::class) case: PreviewCase<SchemaCase>) {
    FormaticoPreview(case, Modifier.width(case.value.width.dp).height(case.value.height.dp)) { schema ->
        val document = schema.state.document
        val editor = remember(document?.id) { CodeEditorState(document?.text.orEmpty()) }
        if (schema.layout == WorkspaceLayout.Phone) {
            PhoneSchema(state = schema.state, editor = editor, onIntent = {})
        } else {
            SchemaPane(state = schema.state, editor = editor, layout = schema.layout, onIntent = {})
        }
    }
}
