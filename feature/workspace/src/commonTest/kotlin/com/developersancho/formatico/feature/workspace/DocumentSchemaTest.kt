package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.PathStep
import com.developersancho.formatico.core.format.text.TextRange
import com.developersancho.formatico.core.schema.JsonType
import com.developersancho.formatico.core.schema.SchemaDraft
import com.developersancho.formatico.core.schema.SchemaFault
import com.developersancho.formatico.core.schema.SchemaRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DocumentSchemaTest {
    private val schema = assertNotNull(schemaDocumentOf(PreviewSchemaDocument, readDocument(PreviewOrderSchema, Format.Json)))

    private fun checkOf(
        text: String, format: Format?,
        choice: SchemaChoice = SchemaChoice(
            with = 2
        ),
        schemas: List<SchemaDocument> = listOf(schema)
    ) =
        schemaCheckOf(OpenDocument(id = 1, name = "order.json", text = text, format = format), readDocument(text, format), choice, schemas)

    @Test
    fun theOrdersProblemsArePlacedInItsTextTheCustomerByItsKey() {
        val check = assertIs<SchemaCheck.Failed>(checkOf(PreviewBoardOrder, Format.Json).check)
        assertEquals("order.schema.json", check.schema)
        assertEquals(SchemaDraft.Draft202012, check.draft)
        assertEquals(2, check.total)
        val (coupon, customer) = check.issues
        assertEquals(listOf(PathStep.Key("coupon")), coupon.problem.at)
        assertEquals("$.coupon", coupon.key)
        assertEquals(8 to 13, coupon.line to coupon.column)
        assertEquals("null", PreviewBoardOrder.substring(checkNotNull(coupon.range).start, coupon.range.end))
        assertEquals(SchemaRule.Required(listOf("phone")), customer.problem.rule)
        assertEquals(9 to 3, customer.line to customer.column)
        assertEquals("\"customer\"", PreviewBoardOrder.substring(checkNotNull(customer.range).start, customer.range.end))
    }

    @Test
    fun aValueTheSchemaAllowsIsValidAndAChoiceOfNoneSaysSo() {
        assertEquals(SchemaCheck.Valid("order.schema.json", SchemaDraft.Draft202012), checkOf(PreviewValidOrder, Format.Json).check)
        assertEquals(SchemaCheck.NoSchema(named = null), checkOf(PreviewBoardOrder, Format.Json, SchemaChoice()).check)
    }

    @Test
    fun theSchemaADocumentsSchemaNamesIsChosenByItsIdAndOneNotOpenIsNamed() {
        val withId = PreviewOrderSchema.replaceFirst("{\n", "{\n  \"\$id\": \"https://example.com/schemas/order.json\",\n")
        val identified = assertNotNull(schemaDocumentOf(PreviewSchemaDocument.copy(text = withId), readDocument(withId, Format.Json)))
        val naming = PreviewBoardOrder.replaceFirst("{\n", "{\n  \"\$schema\": \"https://example.com/schemas/order.json#\",\n")
        val validation = checkOf(naming, Format.Json, SchemaChoice(), listOf(identified))
        assertEquals(2L, validation.schema?.id)
        assertIs<SchemaCheck.Failed>(validation.check)
        assertEquals(
            SchemaCheck.NoSchema(named = "https://example.com/schemas/order.json"),
            checkOf(naming, Format.Json, SchemaChoice(), emptyList()).check
        )
    }

    @Test
    fun eachLineOfNdjsonIsValidatedOnItsOwn() {
        val lines = "{\"id\": \"ord_1\", \"status\": \"paid\", \"total\": 1, \"currency\": \"EUR\", \"customer\": {}, \"items\": [{}]}\n" +
            "{\"id\": \"ord_2\", \"status\": \"lost\", \"total\": 2, \"currency\": \"EUR\", \"customer\": {}, \"items\": [{}]}\n"
        val check = assertIs<SchemaCheck.Failed>(checkOf(lines, Format.Ndjson).check)
        val paths = check.issues.map { it.problem.at.take(2) }
        assertEquals(PathStep.Index(1), paths.first { it.last() == PathStep.Key("status") }.first())
        assertEquals(2, check.issues.first { it.problem.at.last() == PathStep.Key("status") }.line)
    }

    @Test
    fun yamlIsValidatedAndMarkedByItsKeys() {
        val yaml = "id: ord_8842\nstatus: lost\ntotal: -1\ncurrency: EUR\ncustomer:\n  name: Elif\nitems: []\n"
        val check = assertIs<SchemaCheck.Failed>(checkOf(yaml, Format.Yaml).check)
        val rules = check.issues.associate { it.problem.at to it.problem.rule }
        assertEquals(SchemaRule.Bound("0", lower = true, exclusive = false), rules[listOf(PathStep.Key("total"))])
        assertEquals(SchemaRule.ItemCount(1, lower = true), rules[listOf(PathStep.Key("items"))])
        val customer = check.issues.first { it.problem.at == listOf(PathStep.Key("customer")) }
        assertEquals("customer", yaml.substring(checkNotNull(customer.range).start, customer.range.end))
        assertEquals(JsonType.Object, customer.problem.type)
    }

    @Test
    fun aSchemaThatCannotBeUsedSaysTheLineInIt() {
        val broken = PreviewOrderSchema.replace("#/\$defs/address", "#/\$defs/adress")
        val document = assertNotNull(schemaDocumentOf(PreviewSchemaDocument.copy(text = broken), readDocument(broken, Format.Json)))
        val check = assertIs<SchemaCheck.Unusable>(checkOf(PreviewBoardOrder, Format.Json, schemas = listOf(document)).check)
        assertIs<SchemaFault.UnresolvedReference>(check.fault)
        assertEquals(40, check.line)
    }

    @Test
    fun whatHasNoTreeOfValuesIsNotValidated() {
        assertEquals(SchemaCheck.Invalid(Format.Json, 4), checkOf(PreviewInvalid, Format.Json).check)
        assertEquals(SchemaCheck.Unsupported(Format.Csv), checkOf("id,total\nord_8842,129.90\n", Format.Csv).check)
        assertEquals(SchemaCheck.Unread, checkOf("Things to check.\n", null).check)
    }

    @Test
    fun onlyAJsonSchemaIsOfferedAsOne() {
        assertNull(
            schemaDocumentOf(
                OpenDocument(id = 3, name = "order.json", text = PreviewBoardOrder, format = Format.Json),
                readDocument(PreviewBoardOrder, Format.Json)
            )
        )
        val yaml = "type: object\nrequired: [id]\nproperties:\n  id:\n    type: string\n"
        assertNotNull(
            schemaDocumentOf(
                OpenDocument(id = 4, name = "order.schema.yaml", text = yaml, format = Format.Yaml),
                readDocument(yaml, Format.Yaml)
            )
        )
    }

    @Test
    fun aKeyIsMarkedHoweverItIsWritten() {
        val text = "{\"a\": {\"b\": 1}}"
        val root = assertNotNull((readDocument(text, Format.Json) as Reading.Read).root)
        // An object by its key, a single value itself, and the document, which has no key, by its first character.
        assertEquals(TextRange(1, 4), markOf(assertNotNull(nodeAtPath(root, listOf(PathStep.Key("a")))), text))
        assertEquals(TextRange(12, 13), markOf(assertNotNull(nodeAtPath(root, listOf(PathStep.Key("a"), PathStep.Key("b")))), text))
        assertEquals(TextRange(0, 1), markOf(root, text))
        val toml = "[server]\nport = 8080\n"
        val tomlRoot = assertNotNull((readDocument(toml, Format.Toml) as Reading.Read).root)
        val port = assertNotNull(nodeAtPath(tomlRoot, listOf(PathStep.Key("server"), PathStep.Key("port"))))
        assertEquals("8080", toml.substring(markOf(port, toml).start, markOf(port, toml).end))
    }
}
