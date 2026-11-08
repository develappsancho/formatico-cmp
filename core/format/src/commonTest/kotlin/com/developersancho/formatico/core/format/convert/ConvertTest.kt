package com.developersancho.formatico.core.format.convert

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.csv.CsvDocument
import com.developersancho.formatico.core.format.ini.IniDocument
import com.developersancho.formatico.core.format.json.JsonDialect
import com.developersancho.formatico.core.format.json.JsonDocument
import com.developersancho.formatico.core.format.properties.PropertiesDocument
import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.format.toml.TomlDocument
import com.developersancho.formatico.core.format.xml.XmlDocument
import com.developersancho.formatico.core.format.yaml.YamlDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConvertTest {
    private val order = dataOf(BoardOrder)

    @Test
    fun theBoardsOrderIsYamlWithNothingLostItsDatesQuotedAndItsNumbersSpelledAsTheyWere() {
        val converted = convert(order, Format.Yaml)
        assertEquals(
            lines(
                "id: ord_8842",
                "status: shipped",
                "createdAt: \"2026-10-28T14:32:00Z\"",
                "total: 129.90",
                "currency: EUR",
                "paid: true",
                "coupon: null",
                "customer:",
                "  name: Elif Yılmaz",
                "  email: elif@example.com",
                "  avatar: https://images.example.com/u/elif.png",
                "  website: https://elif.dev",
                "theme:",
                "  accent: \"#0F766E\"",
                "items:",
                "  - sku: TS-001",
                "    name: T-shirt",
                "    qty: 2",
                "    price: 39.95",
                "  - sku: MG-014",
                "    name: Mug",
                "    qty: 1",
                "    price: 50.00",
                "shipping:",
                "  method: express",
                "  eta: \"2026-11-04\"",
                "  address:",
                "    city: İzmir",
                "    country: TR"
            ),
            converted.text
        )
        assertEquals(emptyList(), converted.losses)
        assertEquals(listOf(Note.Quoted(dates = true, count = 2), Note.Spelling("129.90", "50.00", 2)), converted.notes)
        assertTrue(YamlDocument.parse(converted.text).isValid)
    }

    @Test
    fun theBoardsOrderIsTomlItsNullLeftOutWhereACommentSaysItWas() {
        val converted = convert(order, Format.Toml, ConvertOptions(nullComment = "{name} = null in the JSON: TOML has no null"))
        assertEquals(
            lines(
                "id = \"ord_8842\"",
                "status = \"shipped\"",
                "createdAt = \"2026-10-28T14:32:00Z\"",
                "total = 129.90",
                "currency = \"EUR\"",
                "paid = true",
                "# coupon = null in the JSON: TOML has no null",
                "",
                "[customer]",
                "name = \"Elif Yılmaz\"",
                "email = \"elif@example.com\"",
                "avatar = \"https://images.example.com/u/elif.png\"",
                "website = \"https://elif.dev\"",
                "",
                "[theme]",
                "accent = \"#0F766E\"",
                "",
                "[[items]]",
                "sku = \"TS-001\"",
                "name = \"T-shirt\"",
                "qty = 2",
                "price = 39.95",
                "",
                "[[items]]",
                "sku = \"MG-014\"",
                "name = \"Mug\"",
                "qty = 1",
                "price = 50.00",
                "",
                "[shipping]",
                "method = \"express\"",
                "eta = \"2026-11-04\"",
                "",
                "[shipping.address]",
                "city = \"İzmir\"",
                "country = \"TR\""
            ),
            converted.text
        )
        assertEquals(listOf(Loss.Nulls("coupon", 1, NullHandling.LeaveOut)), converted.losses)
        assertEquals(listOf(6), converted.nullLines)
        assertTrue(TomlDocument.parse(converted.text).isValid)
    }

    @Test
    fun aNullTomlWritesAsAnEmptyStringWhereTheOptionsSaySo() {
        val converted =
            convert(
                dataOf("""{"coupon": null, "tags": ["a", null], "inner": {"x": null}}"""),
                Format.Toml,
                ConvertOptions(nulls = NullHandling.EmptyString)
            )
        assertEquals(lines("coupon = \"\"", "tags = [\"a\", \"\"]", "", "[inner]", "x = \"\""), converted.text)
        assertEquals(listOf(Loss.Nulls("coupon", 3, NullHandling.EmptyString)), converted.losses)
    }

    @Test
    fun tomlWritesATablesOwnKeysFirstAndAnArrayOfOtherValuesOnItsLine() {
        val converted = convert(
            dataOf(
                """{"server": {"port": 8080}, "name": "api", "tags": [1, "two", {"three": 3}], "empty": {}, "deep": {"a": {"b": true}}}"""
            ),
            Format.Toml
        )
        assertEquals(
            lines(
                "name = \"api\"",
                "tags = [1, \"two\", { three = 3 }]",
                "empty = {}",
                "",
                "[server]",
                "port = 8080",
                "",
                "[deep.a]",
                "b = true"
            ),
            converted.text
        )
        assertEquals(listOf(Loss.Moved("name", 3)), converted.losses)
        assertTrue(TomlDocument.parse(converted.text).isValid)
    }

    @Test
    fun tomlPutsADocumentThatIsNoTableUnderAKeyAndQuotesTheKeysItCannotWriteBare() {
        val converted = convert(dataOf("""[{"first name": "Ada"}, {"first name": "Alan"}]"""), Format.Toml)
        assertEquals(lines("[[items]]", "\"first name\" = \"Ada\"", "", "[[items]]", "\"first name\" = \"Alan\""), converted.text)
        assertEquals(listOf(Loss.Wrapped("items")), converted.losses)
    }

    @Test
    fun jsonWritesEveryValueAndRespellsWhatJsonCannotWriteSo() {
        val converted = convert(dataOf("""{"hex": 0x1F, "half": .5, "plus": +1, "big": Infinity, "same": 1.50}"""), Format.Json)
        assertEquals(
            lines("{", "  \"hex\": 31,", "  \"half\": 0.5,", "  \"plus\": 1,", "  \"big\": \"Infinity\",", "  \"same\": 1.50", "}"),
            converted.text
        )
        assertEquals(listOf(Loss.Unwritable("big", 1), Loss.Respelled("hex", "31", 3)), converted.losses)
        assertEquals(listOf(Note.Spelling("1.50", null, 1)), converted.notes)
        assertTrue(JsonDocument.parse(converted.text).isValid)
    }

    @Test
    fun json5KeepsItsOwnNumbersAndJsonKeepsANameGivenTwice() {
        assertEquals(
            lines("{", "    \"hex\": 0x1F,", "    \"inf\": -Infinity", "}"),
            convert(dataOf("""{"hex": 0x1F, "inf": -Infinity}"""), Format.Json5, ConvertOptions(indentation = Indentation.Spaces(4))).text
        )
        val twice = convert(dataOf("""{"a": 1, "a": 2}"""), Format.Json)
        assertEquals(lines("{", "  \"a\": 1,", "  \"a\": 2", "}"), twice.text)
        assertEquals(emptyList(), twice.losses)
        assertTrue(JsonDocument.parse(twice.text, JsonDialect.Json5).isValid)
    }

    @Test
    fun yamlKeepsANameOnceAndQuotesWhatItWouldReadAsAnotherValue() {
        val converted = convert(
            dataOf(
                """{"a": 1, "zip": "007", "flag": "yes", "y": "x", "a": 2, "empty": "", "note": "a: b", "lines": "one\ntwo", "list": [], "map": {}}"""
            ),
            Format.Yaml
        )
        assertEquals(
            lines(
                "zip: \"007\"",
                "flag: \"yes\"",
                "\"y\": x",
                "a: 2",
                "empty: \"\"",
                "note: \"a: b\"",
                "lines: \"one\\ntwo\"",
                "list: []",
                "map: {}"
            ),
            converted.text
        )
        assertEquals(listOf(Loss.Duplicates("a", 1)), converted.losses)
        assertEquals(listOf(Note.Quoted(dates = false, count = 2)), converted.notes)
        assertTrue(YamlDocument.parse(converted.text).isValid)
    }

    @Test
    fun yamlWritesAnArrayInAnArrayAndADocumentThatIsAScalar() {
        assertEquals(
            lines("- - 1", "  - 2", "- name: x", "  tags:", "    - a"),
            convert(dataOf("""[[1, 2], {"name": "x", "tags": ["a"]}]"""), Format.Yaml).text
        )
        assertEquals(lines("\"true\""), convert(DataString("true"), Format.Yaml).text)
    }

    @Test
    fun xmlWritesTheTreesOwnWayOfReadingItBack() {
        val converted = convert(
            dataOf(
                """{"feed": {"@lang": "en", "title": "News", "item": [{"@id": 1, "#text": "First"}, {"@id": 2, "#text": "Second"}], "draft": false}}"""
            ),
            Format.Xml
        )
        assertEquals(
            lines(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<feed lang=\"en\">",
                "  <title>News</title>",
                "  <item id=\"1\">First</item>",
                "  <item id=\"2\">Second</item>",
                "  <draft>false</draft>",
                "</feed>"
            ),
            converted.text
        )
        assertEquals(listOf(Loss.Untyped("feed.item[0].@id", 3)), converted.losses)
        assertTrue(XmlDocument.parse(converted.text).isValid)
    }

    @Test
    fun xmlPutsTheOrderInARootAndSaysWhatItCannotWrite() {
        val converted = convert(order, Format.Xml, ConvertOptions(nullComment = "{name} = null in the JSON: XML has no null"))
        val text = converted.text
        assertTrue(text.startsWith(lines("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "<root>", "  <id>ord_8842</id>")), text)
        assertTrue("  <!-- coupon = null in the JSON: XML has no null -->\n" in text, text)
        assertEquals("  <!-- coupon = null in the JSON: XML has no null -->", text.lines()[converted.nullLines.single()])
        assertTrue("  <items>\n    <sku>TS-001</sku>\n" in text, text)
        assertEquals(
            listOf(Loss.Wrapped("root"), Loss.Nulls("coupon", 1, NullHandling.LeaveOut), Loss.Untyped("total", 6)),
            converted.losses
        )
        assertTrue(XmlDocument.parse(text).isValid)
    }

    @Test
    fun xmlRenamesWhatIsNoNameAndNamesTheItemsOfAnArrayInAnArray() {
        val converted = convert(dataOf("""{"first name": "Ada", "1st": "x", "grid": [[1, 2]], "none": [], "blank": {}}"""), Format.Xml)
        assertEquals(
            lines(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<root>",
                "  <first_name>Ada</first_name>",
                "  <_1st>x</_1st>",
                "  <grid>",
                "    <item>1</item>",
                "    <item>2</item>",
                "  </grid>",
                "  <blank/>",
                "</root>"
            ),
            converted.text
        )
        assertEquals(
            listOf(
                Loss.Wrapped("root"),
                Loss.Untyped("grid[0][0]", 2),
                Loss.Emptied("none", 2),
                Loss.Renamed("first name", "first_name", 2),
                Loss.ItemsNamed("grid[0]", 1)
            ),
            converted.losses
        )
    }

    @Test
    fun csvWritesAnArraysItemsAsRowsAndAValueInsideOneAsAColumnOfItsOwn() {
        val converted = convert(
            dataOf(
                """[{"id": 1, "name": "Ada, Countess", "address": {"city": "London"}, "active": true}, {"id": 0x2, "note": "said \"hi\"", "name": null}]"""
            ),
            Format.Csv
        )
        assertEquals(
            lines(
                "id,name,address.city,active,note",
                "1,\"Ada, Countess\",London,true,",
                "2,,,,\"said \"\"hi\"\"\""
            ),
            converted.text
        )
        assertEquals(
            listOf(
                Loss.Nulls("[1].name", 1, NullHandling.EmptyString),
                Loss.Untyped("[0].active", 1),
                Loss.Flattened("[0].address.city", 1),
                Loss.Respelled("[1].id", "2", 1)
            ),
            converted.losses
        )
        assertTrue(CsvDocument.parse(converted.text).isValid)
    }

    @Test
    fun tsvPartsItsFieldsWithTabsAndAnObjectIsOneRow() {
        val converted = convert(dataOf("""{"a": "x\ty", "b": 2}"""), Format.Tsv)
        assertEquals(lines("a\tb", "\"x\ty\"\t2"), converted.text)
        assertEquals(listOf(Loss.Wrapped("[0]")), converted.losses)
    }

    @Test
    fun propertiesNameEveryValueByItsPath() {
        val converted = convert(
            dataOf("""{"server": {"port": 8080, "hosts": ["a", "b"]}, "greeting": " hello\nworld", "key with=sign": "v", "unset": null}"""),
            Format.Properties,
            ConvertOptions(nullComment = "{name} = null in the JSON")
        )
        assertEquals(
            lines(
                "server.port=8080",
                "server.hosts[0]=a",
                "server.hosts[1]=b",
                "greeting=\\ hello\\nworld",
                "key\\ with\\=sign=v",
                "# unset = null in the JSON"
            ),
            converted.text
        )
        assertEquals(listOf(5), converted.nullLines)
        assertEquals(
            listOf(Loss.Nulls("unset", 1, NullHandling.LeaveOut), Loss.Untyped("server.port", 1), Loss.Flattened("server.port", 3)),
            converted.losses
        )
        assertTrue(PropertiesDocument.parse(converted.text).isValid)
    }

    @Test
    fun iniWritesTheDocumentsOwnKeysFirstAndASectionForEachObject() {
        val converted = convert(
            dataOf(
                """{"database": {"host": "db", "pool": {"size": 5}}, "name": "api", "tags": ["a"], "empty": {}, "padded": " x ", "lines": "a\nb"}"""
            ),
            Format.Ini
        )
        assertEquals(
            lines(
                "name = api",
                "tags[0] = a",
                "padded = \" x \"",
                "lines = a\\nb",
                "",
                "[database]",
                "host = db",
                "pool.size = 5",
                "",
                "[empty]"
            ),
            converted.text
        )
        assertEquals(
            listOf(
                Loss.Untyped("database.pool.size", 1),
                Loss.Flattened("tags[0]", 2),
                Loss.Moved("name", 4),
                Loss.OneLine("lines", 1)
            ),
            converted.losses
        )
        assertTrue(IniDocument.parse(converted.text).isValid)
    }

    @Test
    fun ndjsonWritesAnArrayAValueToALine() {
        assertEquals(lines("{\"a\":1}", "[1,2]", "\"x\""), convert(dataOf("""[{"a": 1}, [1, 2], "x"]"""), Format.Ndjson).text)
        val one = convert(dataOf("""{"a": {"b": null}}"""), Format.Ndjson)
        assertEquals(lines("{\"a\":{\"b\":null}}"), one.text)
        assertEquals(listOf(Loss.Wrapped("[0]")), one.losses)
    }

    @Test
    fun sortingWritesEveryObjectsMembersInTheOrderOfTheirNames() {
        val converted =
            convert(dataOf("""{"b": {"z": 1, "a": 2}, "a": [{"y": 1, "x": 2}]}"""), Format.Json, ConvertOptions(sortKeys = true))
        assertEquals(
            lines(
                "{",
                "  \"a\": [",
                "    {",
                "      \"x\": 2,",
                "      \"y\": 1",
                "    }",
                "  ],",
                "  \"b\": {",
                "    \"a\": 2,",
                "    \"z\": 1",
                "  }",
                "}"
            ),
            converted.text
        )
    }

    @Test
    fun aTomlDateIsAStringElsewhereAndADateInToml() {
        val value = DataObject(listOf(DataMember("eta", DataDate("2026-11-04")), DataMember("at", DataDate("14:32:00"))))
        val yaml = convert(value, Format.Yaml)
        assertEquals(lines("eta: \"2026-11-04\"", "at: \"14:32:00\""), yaml.text)
        assertEquals(listOf(Loss.Dates("eta", 2)), yaml.losses)
        assertEquals(lines("eta = 2026-11-04", "at = 14:32:00"), convert(value, Format.Toml).text)
    }

    @Test
    fun numbersAreWrittenInEachFormatsOwnSpelling() {
        val value = DataObject(
            listOf("1_000", "0o17", "0b101", "+inf", "nan", ".inf", "5.", "007", "12345678901234567890").mapIndexed { index, written ->
                DataMember("n$index", DataNumber(written))
            }
        )
        assertEquals(
            lines(
                "n0 = 1_000",
                "n1 = 0o17",
                "n2 = 0b101",
                "n3 = +inf",
                "n4 = nan",
                "n5 = inf",
                "n6 = 5.0",
                "n7 = 7",
                "n8 = \"12345678901234567890\""
            ),
            convert(value, Format.Toml).text
        )
        assertEquals(
            lines("n0: 1000", "n1: 0o17", "n2: 5", "n3: .inf", "n4: .nan", "n5: .inf", "n6: 5.", "n7: 007", "n8: 12345678901234567890"),
            convert(value, Format.Yaml).text
        )
    }
}
