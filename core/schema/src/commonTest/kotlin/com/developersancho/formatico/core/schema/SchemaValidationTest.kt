package com.developersancho.formatico.core.schema

import com.developersancho.formatico.core.format.convert.DataMember
import com.developersancho.formatico.core.format.convert.DataNumber
import com.developersancho.formatico.core.format.convert.DataObject
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.generateTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SchemaValidationTest {
    @Test
    fun theBoardsOrderFailsItsSchemaByItsCouponAndItsCustomersPhone() {
        val outcome = outcomeOf(BoardOrder, BoardSchema)
        assertTrue(outcome is SchemaOutcome.Failed, "$outcome")
        assertEquals(SchemaDraft.Draft202012, outcome.draft)
        assertEquals(2, outcome.total)
        assertEquals(
            listOf(
                SchemaProblem(listOf(PathStep.Key("coupon")), SchemaRule.Type(listOf(JsonType.String)), JsonType.Null, "null"),
                SchemaProblem(listOf(PathStep.Key("customer")), SchemaRule.Required(listOf("phone")), JsonType.Object, null)
            ),
            outcome.problems
        )
    }

    @Test
    fun aValueTheSchemaAllowsIsValid() {
        val order = BoardOrder.replace(
            "\"coupon\": null",
            "\"coupon\": \"AUTUMN\""
        ).replace("\"name\": \"Elif Yılmaz\",", "\"name\": \"Elif Yılmaz\", \"phone\": \"+90 555 0100\",")
        assertEquals(SchemaOutcome.Valid(SchemaDraft.Draft202012), outcomeOf(order, BoardSchema))
    }

    @Test
    fun eachKeywordIsReadWithWhatItHolds() {
        assertEquals(
            SchemaRule.Enum(listOf("\"standard\"", "\"express\"")),
            ruleOf("{\"m\": \"post\"}", "{\"properties\": {\"m\": {\"enum\": [\"standard\", \"express\"]}}}")
        )
        assertEquals(SchemaRule.Const("2"), ruleOf("{\"v\": 1}", "{\"properties\": {\"v\": {\"const\": 2}}}"))
        assertEquals(
            SchemaRule.Bound("1", lower = true, exclusive = false),
            ruleOf("{\"q\": 0}", "{\"properties\": {\"q\": {\"minimum\": 1}}}")
        )
        assertEquals(
            SchemaRule.Bound("0", lower = true, exclusive = true),
            ruleOf("{\"p\": 0}", "{\"properties\": {\"p\": {\"exclusiveMinimum\": 0}}}")
        )
        assertEquals(
            SchemaRule.Bound("10", lower = false, exclusive = false),
            ruleOf("{\"q\": 11}", "{\"properties\": {\"q\": {\"maximum\": 10}}}")
        )
        assertEquals(SchemaRule.MultipleOf("5"), ruleOf("{\"q\": 7}", "{\"properties\": {\"q\": {\"multipleOf\": 5}}}"))
        assertEquals(SchemaRule.Length(3, lower = false), ruleOf("{\"c\": \"EURO\"}", "{\"properties\": {\"c\": {\"maxLength\": 3}}}"))
        assertEquals(SchemaRule.Length(3, lower = true), ruleOf("{\"c\": \"EU\"}", "{\"properties\": {\"c\": {\"minLength\": 3}}}"))
        assertEquals(
            SchemaRule.Pattern("^[A-Z]{2}$"),
            ruleOf("{\"c\": \"tr\"}", "{\"properties\": {\"c\": {\"pattern\": \"^[A-Z]{2}$\"}}}")
        )
        assertEquals(SchemaRule.Format("email"), ruleOf("{\"e\": \"nobody\"}", "{\"properties\": {\"e\": {\"format\": \"email\"}}}"))
        assertEquals(SchemaRule.ItemCount(1, lower = true), ruleOf("{\"i\": []}", "{\"properties\": {\"i\": {\"minItems\": 1}}}"))
        assertEquals(SchemaRule.UniqueItems, ruleOf("{\"i\": [1, 1]}", "{\"properties\": {\"i\": {\"uniqueItems\": true}}}"))
        assertEquals(SchemaRule.KeyCount(1, lower = true), ruleOf("{\"o\": {}}", "{\"properties\": {\"o\": {\"minProperties\": 1}}}"))
        assertEquals(SchemaRule.Not, ruleOf("{\"s\": \"x\"}", "{\"properties\": {\"s\": {\"not\": {\"type\": \"string\"}}}}"))
        assertEquals(
            SchemaRule.DependentRequired("card", listOf("expiry")),
            ruleOf("{\"card\": \"4111\"}", "{\"dependentRequired\": {\"card\": [\"expiry\"]}}")
        )
    }

    @Test
    fun aKeyAnObjectDoesNotAllowIsNotAllowedWhereItIs() {
        val problems = problemsOf("{\"id\": 1, \"extra\": true}", "{\"properties\": {\"id\": {}}, \"additionalProperties\": false}")
        assertEquals(listOf(SchemaProblem(listOf(PathStep.Key("extra")), SchemaRule.NotAllowed, JsonType.Boolean, "true")), problems)
    }

    @Test
    fun aDraft4ExclusiveBoundIsReadWithItsMinimum() {
        val schema = "{\"\$schema\": \"http://json-schema.org/draft-04/schema#\", " +
            "\"properties\": {\"p\": {\"minimum\": 0, \"exclusiveMinimum\": true}}}"
        val outcome = outcomeOf("{\"p\": 0}", schema)
        assertTrue(outcome is SchemaOutcome.Failed, "$outcome")
        assertEquals(SchemaDraft.Draft4, outcome.draft)
        assertEquals(SchemaRule.Bound("0", lower = true, exclusive = true), outcome.problems.single().rule)
    }

    @Test
    fun choicesNoneOfWhichMatchIsOneProblemWhereTheyApply() {
        val kinds = problemsOf("{\"c\": true}", "{\"properties\": {\"c\": {\"anyOf\": [{\"type\": \"string\"}, {\"type\": \"number\"}]}}}")
        assertEquals(
            listOf(
                SchemaProblem(
                    listOf(PathStep.Key("c")),
                    SchemaRule.Type(listOf(JsonType.String, JsonType.Number)),
                    JsonType.Boolean,
                    "true"
                )
            ),
            kinds
        )

        val values =
            problemsOf(
                "{\"s\": \"lost\"}",
                "{\"properties\": {\"s\": {\"oneOf\": [{\"const\": \"paid\"}, {\"enum\": [\"shipped\", \"delivered\"]}]}}}"
            )
        assertEquals(SchemaRule.Enum(listOf("\"paid\"", "\"shipped\"", "\"delivered\"")), values.single().rule)

        val shapes = problemsOf(
            "{\"items\": [{\"a\": 1}, {\"b\": 2}]}",
            "{\"properties\": {\"items\": {\"items\": {\"anyOf\": [{\"required\": [\"a\"]}, {\"required\": [\"c\"]}]}}}}"
        )
        assertEquals(listOf(listOf(PathStep.Key("items"), PathStep.Index(1))), shapes.map { it.at })
        assertEquals(SchemaRule.NoChoice("anyOf", choices = 2), shapes.single().rule)

        val many = problemsOf("{\"n\": 4}", "{\"properties\": {\"n\": {\"oneOf\": [{\"minimum\": 1}, {\"maximum\": 10}]}}}")
        assertEquals(SchemaRule.ManyChoices, many.single().rule)
    }

    @Test
    fun aNumberIsAskedOfAsJsonWritesIt() {
        val value = DataObject(listOf(DataMember("port", DataNumber("0x1F"))))
        val outcome = validateAgainst(value, dataOf("{\"properties\": {\"port\": {\"minimum\": 32}}}"))
        assertTrue(outcome is SchemaOutcome.Failed, "$outcome")
        assertEquals("31", outcome.problems.single().value)
    }

    @Test
    fun aRefToAnotherOpenSchemaIsFollowedByItsId() {
        val customer = "{\"\$id\": \"https://example.com/customer.json\", \"type\": \"object\", \"required\": [\"phone\"]}"
        val order = "{\"\$id\": \"https://example.com/order.json\", \"properties\": {\"customer\": {\"\$ref\": \"customer.json\"}}}"
        val outcome =
            outcomeOf(
                "{\"customer\": {}}",
                order,
                others = listOf(customer, "{\"\$id\": \"https://example.com/broken.json\", \"\$ref\": \"#/nowhere\"}")
            )
        assertTrue(outcome is SchemaOutcome.Failed, "$outcome")
        assertEquals(SchemaRule.Required(listOf("phone")), outcome.problems.single().rule)
    }

    @Test
    fun aSchemaThatCannotBeUsedSaysWhy() {
        assertEquals(
            SchemaOutcome.Unusable(SchemaFault.UnknownDraft("https://example.com/my-draft")),
            outcomeOf("{}", "{\"\$schema\": \"https://example.com/my-draft\"}")
        )
        val broken = BoardSchema.replace("#/\$defs/address", "#/\$defs/adress")
        val unresolved = outcomeOf(BoardOrder, broken)
        assertEquals(
            SchemaOutcome.Unusable(
                SchemaFault.UnresolvedReference(
                    "#/\$defs/adress",
                    listOf(
                        PathStep.Key("\$defs"),
                        PathStep.Key("shipping"),
                        PathStep.Key("properties"),
                        PathStep.Key("address"),
                        PathStep.Key("\$ref")
                    )
                )
            ),
            unresolved
        )
        val misspelt = outcomeOf("{}", "{\"properties\": {\"id\": {\"type\": \"strin\"}}}")
        assertEquals(
            SchemaOutcome.Unusable(
                SchemaFault.NotASchema(
                    SchemaDraft.Draft202012,
                    listOf(PathStep.Key("properties"), PathStep.Key("id"), PathStep.Key("type")),
                    places = 1
                )
            ),
            misspelt
        )
    }

    @Test
    fun aSchemaIsToldFromOtherDocuments() {
        assertTrue(looksLikeSchema(dataOf(BoardSchema)))
        assertTrue(looksLikeSchema(dataOf("{\"type\": \"object\", \"required\": [\"id\"]}")))
        assertTrue(!looksLikeSchema(dataOf(BoardOrder)))
        assertTrue(!looksLikeSchema(dataOf("{\"type\": \"module\", \"main\": \"index.js\"}")))
        assertTrue(!looksLikeSchema(dataOf("[1, 2]")))
        assertEquals("https://example.com/order.json", schemaIdOf(dataOf("{\"\$id\": \"https://example.com/order.json#\"}")))
        assertEquals(
            "https://example.com/order.json",
            schemaNamedBy(dataOf("{\"\$schema\": \"https://example.com/order.json\", \"id\": 1}"))
        )
        assertEquals(null, schemaNamedBy(dataOf("{\"\$schema\": \"http://json-schema.org/draft-07/schema#\"}")))
    }

    @Test
    fun everyDraftIsKnownByItsAddress() {
        assertEquals(SchemaDraft.Draft7, SchemaDraft.named("http://json-schema.org/draft-07/schema#"))
        assertEquals(SchemaDraft.Draft7, SchemaDraft.named("https://json-schema.org/draft-07/schema"))
        assertEquals(SchemaDraft.Draft201909, SchemaDraft.named("https://json-schema.org/draft/2019-09/schema"))
        assertEquals(null, SchemaDraft.named("not an address"))
        SchemaDraft.entries.forEach { draft ->
            assertTrue(
                outcomeOf(
                    "{\"a\": 1}",
                    "{\"\$schema\": \"${draft.uri}\", \"properties\": {\"a\": {\"type\": \"integer\"}}}"
                ) is SchemaOutcome.Valid,
                draft.name
            )
        }
    }

    @Test
    fun theSchemaTypesWritesOfADocumentValidatesItAndFailsAnotherShape() {
        val generated = checkNotNull(generateTypes(dataOf(BoardOrder), TypeLanguage.JsonSchema, "Order")).text
        assertEquals(SchemaOutcome.Valid(SchemaDraft.Draft202012), outcomeOf(BoardOrder, generated))
        val problems = problemsOf(BoardOrder.replace("\"qty\": 2", "\"qty\": \"two\""), generated)
        assertEquals(
            listOf(
                SchemaProblem(
                    listOf(PathStep.Key("items"), PathStep.Index(0), PathStep.Key("qty")),
                    SchemaRule.Type(listOf(JsonType.Integer)),
                    JsonType.String,
                    "\"two\""
                )
            ),
            problems
        )
    }

    private fun ruleOf(json: String, schema: String): SchemaRule = problemsOf(json, schema).single().rule
}
