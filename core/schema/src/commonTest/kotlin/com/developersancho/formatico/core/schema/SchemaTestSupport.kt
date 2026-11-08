package com.developersancho.formatico.core.schema

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataBoolean
import com.developersancho.formatico.core.format.convert.DataMember
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.json.JsonArray
import com.developersancho.formatico.core.format.json.JsonBoolean
import com.developersancho.formatico.core.format.json.JsonDialect
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.json.JsonNull
import com.developersancho.formatico.core.format.json.JsonNumber
import com.developersancho.formatico.core.format.json.JsonObject
import com.developersancho.formatico.core.format.json.JsonString
import com.developersancho.formatico.core.format.json.JsonValue

/** The value of [json], read as JSON5 so that a test can write JSON5's numbers, as a conversion carries it. */
internal fun dataOf(json: String): DataValue {
    val document = JsonDocument.parse(json, JsonDialect.Json5)
    check(document.isValid) { "The test's JSON is invalid: ${document.problems}" }
    return checkNotNull(document.root).data()
}

private fun JsonValue.data(): DataValue = when (this) {
    is JsonObject -> DataObject(members.map { DataMember(it.key.name, checkNotNull(it.value).data()) })
    is JsonArray -> DataArray(items.map { it.data() })
    is JsonString -> DataString(value)
    is JsonNumber -> DataNumber(raw)
    is JsonBoolean -> DataBoolean(value)
    is JsonNull -> DataNull
}

/** [json] validated against the schema [schema], beside the [others] open. */
internal fun outcomeOf(json: String, schema: String, others: List<String> = emptyList()): SchemaOutcome =
    validateAgainst(dataOf(json), dataOf(schema), others.map(::dataOf))

/** The problems of [json] against [schema], which it must fail. */
internal fun problemsOf(json: String, schema: String): List<SchemaProblem> {
    val outcome = outcomeOf(json, schema)
    check(outcome is SchemaOutcome.Failed) { "Expected problems, and the outcome is $outcome" }
    return outcome.problems
}

/** The order of the boards. */
internal val BoardOrder =
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
    """.trimIndent()

/** The schema of the Schema boards, order.schema.json: the order fails it twice, its coupon and its customer's phone. */
internal val BoardSchema =
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
    """.trimIndent()
