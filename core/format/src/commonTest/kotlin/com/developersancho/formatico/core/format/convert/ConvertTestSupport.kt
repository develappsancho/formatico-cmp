package com.developersancho.formatico.core.format.convert

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

/** The order of the Convert boards. */
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

/** [lines] as a text, each ended by a line break. */
internal fun lines(vararg lines: String): String = lines.joinToString("") { it + "\n" }
