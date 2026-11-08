package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.DataArray
import com.developersancho.formatico.core.format.convert.DataBoolean
import com.developersancho.formatico.core.format.convert.DataMember
import com.developersancho.formatico.core.format.convert.DataNull
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.convert.DataString
import com.developersancho.formatico.core.format.convert.DataValue
import com.developersancho.formatico.core.format.convert.dataOf

/** An object of [members], in their order. */
internal fun objectOf(vararg members: Pair<String, DataValue>): DataObject =
    DataObject(members.map { (name, value) -> DataMember(name, value) })

/** The order of the boards, as a conversion carries it: its numbers as they are written. */
internal val BoardOrderValue: DataValue = objectOf(
    "id" to DataString("ord_8842"),
    "status" to DataString("shipped"),
    "createdAt" to DataString("2026-10-28T14:32:00Z"),
    "total" to DataNumber("129.90"),
    "currency" to DataString("EUR"),
    "paid" to DataBoolean(true),
    "coupon" to DataNull,
    "customer" to objectOf(
        "name" to DataString("Elif Yılmaz"),
        "email" to DataString("elif@example.com"),
        "avatar" to DataString("https://images.example.com/u/elif.png"),
        "website" to DataString("https://elif.dev")
    ),
    "theme" to objectOf("accent" to DataString("#0F766E")),
    "items" to DataArray(
        listOf(
            objectOf(
                "sku" to DataString("TS-001"),
                "name" to DataString("T-shirt"),
                "qty" to DataNumber("2"),
                "price" to DataNumber("39.95")
            ),
            objectOf("sku" to DataString("MG-014"), "name" to DataString("Mug"), "qty" to DataNumber("1"), "price" to DataNumber("50.00"))
        )
    ),
    "shipping" to objectOf(
        "method" to DataString("express"),
        "eta" to DataString("2026-11-04"),
        "address" to objectOf("city" to DataString("İzmir"), "country" to DataString("TR"))
    )
)

/** The types of [value] in [language], the root named [rootName], where there are some. */
internal fun typesOf(value: DataValue, language: TypeLanguage, rootName: String = "Root"): GeneratedTypes =
    checkNotNull(generateTypes(value, language, rootName)) { "No types for $value" }

/** The types of the document [json], read as JSON5, in [language], the root named [rootName]. */
internal fun typesOf(json: String, language: TypeLanguage, rootName: String = "Root"): GeneratedTypes =
    typesOf(dataOf(json), language, rootName)

/** The text of the types of the document [json] in [language], the root named "Root". */
internal fun textOf(json: String, language: TypeLanguage): String = typesOf(json, language).text
