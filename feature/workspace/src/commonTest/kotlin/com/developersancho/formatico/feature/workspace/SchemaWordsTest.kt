package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.schema.JsonType
import com.developersancho.formatico.core.schema.SchemaDraft
import com.developersancho.formatico.core.schema.SchemaFault
import com.developersancho.formatico.core.schema.SchemaProblem
import com.developersancho.formatico.core.schema.SchemaRule
import com.developersancho.formatico.feature.workspace.strings.SchemaStrings
import com.developersancho.formatico.feature.workspace.strings.SchemaStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.SchemaStringsTurkish
import kotlin.test.Test
import kotlin.test.assertEquals

class SchemaWordsTest {
    private val coupon = listOf(PathStep.Key("coupon"))

    private fun SchemaStrings.of(rule: SchemaRule, at: List<PathStep> = coupon, type: JsonType = JsonType.Null, value: String? = "null") =
        sentenceOf(SchemaProblem(at, rule, type, value))

    @Test
    fun eachRuleIsASentenceInEnglish() {
        with(SchemaStringsEnglish) {
            assertEquals("coupon must be a string, and is null.", of(SchemaRule.Type(listOf(JsonType.String))))
            assertEquals(
                "coupon must be a string, an integer, or null, and is true.",
                of(SchemaRule.Type(listOf(JsonType.String, JsonType.Integer, JsonType.Null)), value = "true")
            )
            assertEquals(
                "customer has no phone, which the schema requires.",
                of(SchemaRule.Required(listOf("phone")), listOf(PathStep.Key("customer")))
            )
            assertEquals(
                "customer must have phone, fax, and mobile, and has none of them.",
                of(SchemaRule.Required(listOf("phone", "fax", "mobile")), listOf(PathStep.Key("customer")))
            )
            assertEquals(
                "payment has card, so it must have expiry too.",
                of(SchemaRule.DependentRequired("card", listOf("expiry")), listOf(PathStep.Key("payment")))
            )
            assertEquals("coupon must be one of \"A\" or \"B\", and is null.", of(SchemaRule.Enum(listOf("\"A\"", "\"B\""))))
            assertEquals("coupon must be \"A\", and is null.", of(SchemaRule.Const("\"A\"")))
            assertEquals(
                "qty must be at least 1, and is 0.",
                of(SchemaRule.Bound("1", lower = true, exclusive = false), listOf(PathStep.Key("qty")), JsonType.Integer, "0")
            )
            assertEquals(
                "price must be more than 0, and is 0.",
                of(SchemaRule.Bound("0", lower = true, exclusive = true), listOf(PathStep.Key("price")), JsonType.Integer, "0")
            )
            assertEquals(
                "qty must be at most 9, and is 10.",
                of(SchemaRule.Bound("9", lower = false, exclusive = false), listOf(PathStep.Key("qty")), JsonType.Integer, "10")
            )
            assertEquals(
                "qty must be less than 9, and is 10.",
                of(SchemaRule.Bound("9", lower = false, exclusive = true), listOf(PathStep.Key("qty")), JsonType.Integer, "10")
            )
            assertEquals(
                "qty must be a multiple of 5, and is 7.",
                of(SchemaRule.MultipleOf("5"), listOf(PathStep.Key("qty")), JsonType.Integer, "7")
            )
            assertEquals("coupon must be at least 3 characters long, and is null.", of(SchemaRule.Length(3, lower = true)))
            assertEquals("coupon must be at most 3 characters long, and is null.", of(SchemaRule.Length(3, lower = false)))
            assertEquals("coupon must match ^[A-Z]+$, and is null.", of(SchemaRule.Pattern("^[A-Z]+$")))
            assertEquals("coupon must be written as email, and is null.", of(SchemaRule.Format("email")))
            assertEquals(
                "items must hold at least 1 item.",
                of(SchemaRule.ItemCount(1, lower = true), listOf(PathStep.Key("items")), JsonType.Array, null)
            )
            assertEquals(
                "items must hold at most 3 items.",
                of(SchemaRule.ItemCount(3, lower = false), listOf(PathStep.Key("items")), JsonType.Array, null)
            )
            assertEquals("The items of tags must differ from each other.", of(SchemaRule.UniqueItems, listOf(PathStep.Key("tags"))))
            assertEquals("tags must hold an item the schema describes.", of(SchemaRule.Contains, listOf(PathStep.Key("tags"))))
            assertEquals("theme must have at least 1 key.", of(SchemaRule.KeyCount(1, lower = true), listOf(PathStep.Key("theme"))))
            assertEquals("theme must have at most 2 keys.", of(SchemaRule.KeyCount(2, lower = false), listOf(PathStep.Key("theme"))))
            assertEquals("extra is not allowed here.", of(SchemaRule.NotAllowed, listOf(PathStep.Key("extra"))))
            assertEquals("coupon must not be what the schema rules out.", of(SchemaRule.Not))
            assertEquals(
                "items[1] matches none of the 2 schemas its anyOf allows.",
                of(SchemaRule.NoChoice("anyOf", 2), listOf(PathStep.Key("items"), PathStep.Index(1)))
            )
            assertEquals("coupon matches more than one of the schemas its oneOf allows, and must match one.", of(SchemaRule.ManyChoices))
            assertEquals("coupon does not meet the schema’s dependentSchemas.", of(SchemaRule.Other("dependentSchemas")))
            assertEquals(
                "The document must be an object, and is an array.",
                of(SchemaRule.Type(listOf(JsonType.Object)), emptyList(), JsonType.Array, null)
            )
        }
    }

    @Test
    fun eachRuleIsASentenceInTurkish() {
        with(SchemaStringsTurkish) {
            assertEquals("coupon bir dizgi olmalı, ama null.", of(SchemaRule.Type(listOf(JsonType.String))))
            assertEquals(
                "customer içinde phone yok; şema bunu istiyor.",
                of(SchemaRule.Required(listOf("phone")), listOf(PathStep.Key("customer")))
            )
            assertEquals(
                "customer içinde phone ve fax yok; şema bunları istiyor.",
                of(SchemaRule.Required(listOf("phone", "fax")), listOf(PathStep.Key("customer")))
            )
            assertEquals(
                "price 0 değerinden büyük olmalı, ama 0.",
                of(SchemaRule.Bound("0", lower = true, exclusive = true), listOf(PathStep.Key("price")), JsonType.Integer, "0")
            )
            assertEquals("coupon şunlardan biri olmalı: \"A\" veya \"B\"; ama null.", of(SchemaRule.Enum(listOf("\"A\"", "\"B\""))))
            assertEquals(
                "Belge bir nesne olmalı, ama bir dizi.",
                of(SchemaRule.Type(listOf(JsonType.Object)), emptyList(), JsonType.Array, null)
            )
            assertEquals("extra burada olamaz.", of(SchemaRule.NotAllowed, listOf(PathStep.Key("extra"))))
        }
    }

    @Test
    fun anEnumNamesItsFirstSixValues() {
        val values = (1..8).map { "\"$it\"" }
        assertEquals(
            "coupon must be one of \"1\", \"2\", \"3\", \"4\", \"5\", \"6\", or …, and is null.",
            SchemaStringsEnglish.of(SchemaRule.Enum(values))
        )
    }

    @Test
    fun aSchemaThatCannotBeUsedSaysWhy() {
        with(SchemaStringsEnglish) {
            assertEquals(
                "Its \$ref on line 40, #/\$defs/adress, points at nothing. Correct it in its own tab, and order.json is validated again.",
                faultOf(
                    SchemaCheck.Unusable("order.schema.json", SchemaFault.UnresolvedReference("#/\$defs/adress", emptyList()), 40),
                    "order.json"
                )
            )
            assertEquals(
                "What it holds on line 5, type, is not what JSON Schema 2020-12 allows there. " +
                    "Correct it in its own tab, and order.json is validated again.",
                faultOf(
                    SchemaCheck.Unusable(
                        "order.schema.json",
                        SchemaFault.NotASchema(SchemaDraft.Draft202012, listOf(PathStep.Key("type")), 1),
                        5
                    ),
                    "order.json"
                )
            )
            assertEquals(
                "Its \$schema names https://example.com/my-draft, which is no draft Formatico knows: " +
                    "2020-12, 2019-09, draft-07, draft-06 or draft-04.",
                faultOf(
                    SchemaCheck.Unusable("my.schema.json", SchemaFault.UnknownDraft("https://example.com/my-draft"), null),
                    "order.json"
                )
            )
            assertEquals(
                "The validator could not read it: circled",
                faultOf(SchemaCheck.Unusable("a", SchemaFault.Unreadable("circled"), null), "b")
            )
        }
    }
}
