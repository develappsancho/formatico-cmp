package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.Loss
import com.developersancho.formatico.core.format.convert.NullHandling
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentConversionTest {
    private fun conversion(text: String, format: Format?, choice: ConvertChoice = ConvertChoice(), name: String? = "order.json") =
        conversionOf(OpenDocument(id = 1, name = name, text = text, format = format), readDocument(text, format), choice, "{name} = null")

    private fun converted(text: String, format: Format, choice: ConvertChoice = ConvertChoice(), name: String? = "order.json") =
        assertIs<ConvertOutcome.Converted>(conversion(text, format, choice, name))

    @Test
    fun jsonAndItsKinAreConvertedToYamlFirstAndEveryOtherFormatToJson() {
        assertEquals(Format.Yaml, Format.Json.firstTarget)
        assertEquals(Format.Yaml, Format.Ndjson.firstTarget)
        assertEquals(Format.Json, Format.Toml.firstTarget)
        assertEquals(Format.Json, Format.Csv.firstTarget)
        assertEquals(Format.entries - Format.Xml, Format.Xml.targets)
        // A choice of the document's own format is no choice: the first is taken.
        assertEquals(Format.Yaml, targetOf(Format.Json, ConvertChoice(to = Format.Json)))
        assertEquals(Format.Toml, targetOf(Format.Json, ConvertChoice(to = Format.Toml)))
    }

    @Test
    fun theFormatsWrittenInLevelsAreIndentedAndThoseWithNoNullOfferAChoice() {
        assertEquals(setOf(Format.Json, Format.Jsonc, Format.Json5, Format.Yaml, Format.Xml), Format.entries.filter { it.indents }.toSet())
        assertEquals(setOf(Format.Toml, Format.Xml, Format.Properties, Format.Ini), Format.entries.filter { it.choosesNulls }.toSet())
    }

    @Test
    fun theOrderIsYamlNamedAfterItWithNothingLost() {
        val yaml = converted(PreviewOrder, Format.Json)
        assertEquals(Format.Json, yaml.from)
        assertEquals(Format.Yaml, yaml.to)
        assertEquals("order.yaml", yaml.name)
        assertTrue(yaml.text.startsWith("id: ord_8842\nstatus: shipped\ncreatedAt: \"2026-10-28T14:32:00Z\"\n"), yaml.text)
        assertEquals(emptyList(), yaml.losses)
        assertEquals(yaml.text.encodeToByteArray().size.toLong(), yaml.bytes)
    }

    @Test
    fun theOrdersNullsAreLeftOutOfTomlOnTheLinesItsCommentsStandOn() {
        val toml = converted(PreviewOrder, Format.Json, ConvertChoice(to = Format.Toml))
        assertEquals(listOf<Loss>(Loss.Nulls("coupon", 2, NullHandling.LeaveOut)), toml.losses)
        assertEquals(listOf("# coupon = null", "# shippedOn = null"), toml.nullLines.map { toml.text.lines()[it] })
        val empty = converted(PreviewOrder, Format.Json, ConvertChoice(to = Format.Toml, nulls = NullHandling.EmptyString))
        assertEquals(listOf<Loss>(Loss.Nulls("coupon", 2, NullHandling.EmptyString)), empty.losses)
        assertEquals(emptyList(), empty.nullLines)
    }

    @Test
    fun aDocumentsCommentsAndAliasesAreSaidFirst() {
        val json = converted(PreviewDeployment, Format.Yaml, name = "deployment.yaml")
        assertEquals("deployment.json", json.name)
        assertEquals(Loss.Comments(1), json.losses[0])
        assertEquals(Loss.Aliases("spec.selector.matchLabels", 2), json.losses[1])
        // An alias is written out as the value it points at.
        assertTrue("\"matchLabels\": {\n        \"app\": \"formatico\"" in json.text, json.text)
    }

    @Test
    fun aTomlDateIsADateInTheTreeAndAStringInJson() {
        val json = converted("released = 2026-11-04\nname = \"x\"\n", Format.Toml, name = "Cargo.toml")
        assertEquals("{\n  \"released\": \"2026-11-04\",\n  \"name\": \"x\"\n}\n", json.text)
        assertEquals(listOf<Loss>(Loss.Dates("released", 1)), json.losses)
    }

    @Test
    fun aCsvsNumbersAreNumbersAndItsCodesStrings() {
        val json =
            converted("country,code,population\nAfghanistan,004,41.1\nAlbania,008,2.8\n", Format.Csv, ConvertChoice(to = Format.Json))
        assertTrue("\"code\": \"004\",\n    \"population\": 41.1" in json.text, json.text)
    }

    @Test
    fun anXmlDocumentConvertsToJsonAndBackToTheSameTree() {
        val json = converted(PreviewFeed, Format.Xml, name = "feed.xml")
        val back = converted(json.text, Format.Json, ConvertChoice(to = Format.Xml), name = "feed.json")
        val before = (readDocument(PreviewFeed, Format.Xml) as Reading.Read).root
        val after = (readDocument(back.text, Format.Xml) as Reading.Read).root
        assertEquals(before?.shape(), after?.shape())
    }

    /** What a tree holds, by name, kind and value, without where it is written. */
    private fun ValueNode.shape(): String =
        "$name:$kind:${value ?: if (children.isEmpty()) text else ""}" + children.joinToString(",", "(", ")") { it.shape() }

    @Test
    fun everySampleConvertsToEveryOtherFormatAndIsReadBackWithNoError() {
        val samples = mapOf(
            Format.Json to PreviewOrder,
            Format.Yaml to PreviewDeployment,
            Format.Csv to PreviewCountries,
            Format.Xml to PreviewFeed,
            Format.Toml to PreviewCargo,
            Format.Ndjson to PreviewEvents,
            Format.Properties to PreviewApplication,
            Format.Ini to PreviewSettings
        )
        samples.forEach { (from, text) ->
            from.targets.forEach { to ->
                val outcome = converted(text, from, ConvertChoice(to = to, sortKeys = to == Format.Yaml))
                val reading = assertIs<Reading.Read>(readDocument(outcome.text, to), "$from to $to")
                assertEquals(0, reading.errors, "$from to $to:\n${outcome.text}\n${reading.problems}")
            }
        }
    }

    @Test
    fun anInvalidDocumentIsRepairedFirstAndPlainTextHasNothingToConvert() {
        val invalid = assertIs<ConvertOutcome.Invalid>(conversion(PreviewInvalid, Format.Json, name = "settings.json"))
        assertEquals(Format.Json, invalid.format)
        assertTrue(invalid.errors > 0)
        assertEquals(ConvertOutcome.Unread, conversion("Notes", null))
        assertEquals(ConvertOutcome.Unread, conversion("", Format.Json))
    }

    @Test
    fun aConvertedDocumentIsNamedAfterTheOldOneOrNotAtAll() {
        assertEquals("order.toml", convertedName("order.json", Format.Toml))
        assertEquals("archive.tar.yaml", convertedName("archive.tar.gz", Format.Yaml))
        assertNull(convertedName(null, Format.Yaml))
        assertNull(converted("{}", Format.Json, name = null).name)
        assertFalse(converted("{}", Format.Json).text.isEmpty())
    }

    @Test
    fun aPathIsWrittenAsAFlattenedName() {
        val root = checkNotNull((readDocument("""{"items": [{"sku": "x"}]}""", Format.Json) as Reading.Read).root)
        assertEquals("$", flatPath(root.path))
        assertEquals("items[0].sku", flatPath(root.children[0].children[0].children[0].path))
    }
}
