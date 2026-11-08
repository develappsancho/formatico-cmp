package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TomlDocumentTest {
    /** The range of the first [written] in [text], or of the first after [after]. */
    private fun rangeOf(text: String, written: String, after: String = ""): TextRange {
        val start = text.indexOf(written, startIndex = if (after.isEmpty()) 0 else text.indexOf(after) + after.length)
        check(start >= 0) { "<$written> is not in the text" }
        return TextRange(start, start + written.length)
    }

    @Test
    fun everyKindOfStringIsReadWithItsTextAndItsValue() {
        val text = toml(
            """basic = "tab\there \"quoted\" \u00E9 \U0001F600 \\ \b\f\r\n"""",
            """literal = 'C:\Users\nodejs\templates'""",
            """empty = """"",
            """multi = $TRIPLE_QUOTE""",
            "Roses are red",
            """Violets are blue$TRIPLE_QUOTE""",
            """folded = $TRIPLE_QUOTE\""",
            """    The quick brown \   """,
            "",
            """    fox.$TRIPLE_QUOTE""",
            """quotes = $TRIPLE_QUOTE"quoted" and "" two""$TRIPLE_QUOTE""",
            "raw = '''",
            "The first line break is trimmed.",
            "   Other whitespace is kept: \\n.",
            "'''",
            "literalQuotes = ''''That,' she said.''''"
        )
        val document = TomlDocument.parse(text)
        val root = document.root

        assertTrue(document.isValid, document.problems.toString())
        val expected = mapOf(
            "basic" to "tab\there \"quoted\" \u00E9 \uD83D\uDE00 \\ \b\u000C\r\n",
            "literal" to "C:\\Users\\nodejs\\templates",
            "empty" to "",
            "multi" to "Roses are red\nViolets are blue",
            "folded" to "The quick brown fox.",
            "quotes" to "\"quoted\" and \"\" two\"\"",
            "raw" to "The first line break is trimmed.\n   Other whitespace is kept: \\n.\n",
            "literalQuotes" to "'That,' she said.'"
        )
        expected.forEach { (key, value) ->
            val string = root.at(key) as TomlString
            assertEquals(value, string.value, key)
            assertEquals(string.raw, document.textOf(string), key)
        }
        assertEquals(rangeOf(text, "'C:\\Users\\nodejs\\templates'"), root.at("literal").range)
        assertTrue((root.at("multi") as TomlString).raw.startsWith(TRIPLE_QUOTE + "\nRoses"))
    }

    @Test
    fun numbersAreKeptAsTheyAreWritten() {
        val integers = listOf("1_000", "0xFF", "0xdead_beef", "0o17", "0b1101", "+3", "-0", "9223372036854775807", "-17")
        val floats = listOf("3.14", "-1e-3", "6.02E+23", "1_000.5", "5e+22", "inf", "+inf", "-nan", "0.0", "-0.0", "1e06")
        val text = toml(
            *integers.mapIndexed { index, raw -> "i$index = $raw" }.toTypedArray(),
            *floats.mapIndexed { index, raw -> "f$index = $raw" }.toTypedArray()
        )
        val document = TomlDocument.parse(text)

        assertTrue(document.isValid, document.problems.toString())
        integers.forEachIndexed { index, raw ->
            val integer = document.root.at("i$index") as TomlInteger
            assertEquals(raw, integer.raw)
            assertEquals(rangeOf(text, raw, after = "i$index = ").start, integer.range.start)
            assertEquals(raw, document.textOf(integer))
        }
        floats.forEachIndexed { index, raw ->
            val float = document.root.at("f$index") as TomlFloat
            assertEquals(raw, float.raw)
            assertEquals(raw, document.textOf(float))
        }
    }

    @Test
    fun booleansDatesAndTimes() {
        val text = toml(
            "yes = true",
            "no = false",
            "odt1 = 1979-05-27T07:32:00Z",
            "odt2 = 1979-05-27T00:32:00.999999-07:00",
            "odt3 = 1979-05-27 07:32:00+05:30",
            "ldt = 1979-05-27t07:32:00.5",
            "ld = 1979-05-27",
            "leap = 2024-02-29",
            "lt = 00:32:00.999999",
            "second = 23:59:60"
        )
        val document = TomlDocument.parse(text)
        val root = document.root

        assertTrue(document.isValid, document.problems.toString())
        assertEquals(true, (root.at("yes") as TomlBoolean).value)
        assertEquals(false, (root.at("no") as TomlBoolean).value)
        assertEquals(rangeOf(text, "false"), root.at("no").range)
        val kinds = mapOf(
            "odt1" to TomlDateTimeKind.OffsetDateTime,
            "odt2" to TomlDateTimeKind.OffsetDateTime,
            "odt3" to TomlDateTimeKind.OffsetDateTime,
            "ldt" to TomlDateTimeKind.LocalDateTime,
            "ld" to TomlDateTimeKind.LocalDate,
            "leap" to TomlDateTimeKind.LocalDate,
            "lt" to TomlDateTimeKind.LocalTime,
            "second" to TomlDateTimeKind.LocalTime
        )
        kinds.forEach { (key, kind) ->
            val date = root.at(key) as TomlDateTime
            assertEquals(kind, date.kind, key)
            assertEquals(date.raw, document.textOf(date), key)
        }
        assertEquals("1979-05-27 07:32:00+05:30", (root.at("odt3") as TomlDateTime).raw)
    }

    @Test
    fun arraysAndInlineTablesHoldValuesOfEveryKind() {
        val text = toml(
            "nested = [[1, 2], [\"a\", 'b'], { c = true }, 1979-05-27]",
            "empty = []",
            "point = { x = 1, y.z = 2 }",
            "none = {}",
            "lines = [",
            "  1, # one",
            "",
            "  2,",
            "]"
        )
        val document = TomlDocument.parse(text)
        val root = document.root

        assertTrue(document.isValid, document.problems.toString())
        val nested = root.at("nested") as TomlArray
        assertFalse(nested.ofTables)
        assertEquals(4, nested.items.size)
        assertEquals(rangeOf(text, "[[1, 2], [\"a\", 'b'], { c = true }, 1979-05-27]"), nested.range)
        assertEquals(rangeOf(text, "[\"a\", 'b']"), nested.items[1].range)
        assertEquals(TomlTableKind.Inline, (nested.items[2] as TomlTable).kind)
        assertEquals(emptyList(), (root.at("empty") as TomlArray).items)

        val point = root.at("point") as TomlTable
        assertEquals(TomlTableKind.Inline, point.kind)
        assertEquals(rangeOf(text, "{ x = 1, y.z = 2 }"), point.range)
        assertEquals(TomlTableKind.Implicit, (point.at("y") as TomlTable).kind)
        assertEquals(rangeOf(text, "y.z = 2"), point.at("y").range)
        assertEquals(rangeOf(text, "2", after = "y.z = "), point.at("y", "z").range)
        assertEquals(TomlTableKind.Inline, (root.at("none") as TomlTable).kind)

        val lines = root.at("lines") as TomlArray
        assertEquals(listOf("1", "2"), lines.items.map { (it as TomlInteger).raw })
        assertEquals(listOf(rangeOf(text, "# one")), document.comments)
    }

    @Test
    fun tablesDottedKeysAndArraysOfTablesRunOverWhatWentIntoThem() {
        val text = toml(
            "title = \"x\"",
            "owner.name = \"Tom\"",
            "owner . \"date of birth\" = 1979-05-27",
            "",
            "[servers.alpha]",
            "ip = \"10.0.0.1\"",
            "",
            "[servers.beta]",
            "ip = \"10.0.0.2\" # last",
            "",
            "[[products]]",
            "name = \"Hammer\"",
            "",
            "[[products]]",
            "",
            "[[ products ]]",
            "name = \"Nail\""
        )
        val document = TomlDocument.parse(text)
        val root = document.root

        assertTrue(document.isValid, document.problems.toString())
        assertEquals(TomlTableKind.Root, root.kind)
        assertEquals(TextRange(0, text.length), root.range)
        assertEquals(listOf("title", "owner", "servers", "products"), root.entries.map { it.key })

        val owner = root.entry("owner")
        assertEquals(rangeOf(text, "owner"), owner.keyRange)
        val ownerTable = owner.value as TomlTable
        assertEquals(TomlTableKind.Implicit, ownerTable.kind)
        assertEquals(TextRange(text.indexOf("owner.name"), rangeOf(text, "1979-05-27").end), ownerTable.range)
        assertEquals(listOf("name", "date of birth"), ownerTable.entries.map { it.key })
        assertEquals(rangeOf(text, "\"date of birth\""), ownerTable.entry("date of birth").keyRange)

        val servers = root.entry("servers")
        assertNull(servers.keyRange)
        val serversTable = servers.value as TomlTable
        assertEquals(TomlTableKind.Implicit, serversTable.kind)
        assertEquals(TextRange(text.indexOf("[servers.alpha]"), rangeOf(text, "\"10.0.0.2\"").end), serversTable.range)
        val alpha = serversTable.entry("alpha")
        assertEquals(rangeOf(text, "alpha"), alpha.keyRange)
        assertEquals(TomlTableKind.Header, (alpha.value as TomlTable).kind)
        assertEquals(TextRange(text.indexOf("[servers.alpha]"), rangeOf(text, "\"10.0.0.1\"").end), alpha.value.range)

        val products = root.at("products") as TomlArray
        assertTrue(products.ofTables)
        assertEquals(3, products.items.size)
        assertTrue(products.items.all { (it as TomlTable).kind == TomlTableKind.ArrayItem })
        assertEquals(TextRange(text.indexOf("[[products]]"), rangeOf(text, "\"Nail\"").end), products.range)
        assertEquals(rangeOf(text, "[[products]]", after = "Hammer"), products.items[1].range)
        assertEquals(rangeOf(text, "products"), root.entry("products").keyRange)
        assertEquals("Nail", ((products.items[2] as TomlTable).at("name") as TomlString).value)
    }

    @Test
    fun aTableAboveAHeaderMayBeDefinedAfterItAndDottedKeysMayHaveTablesUnderThem() {
        val text = toml(
            "[a.b]",
            "c = 1",
            "[a]",
            "d = 2",
            "",
            "[fruit]",
            "apple.color = \"red\"",
            "apple.taste.sweet = true",
            "",
            "[fruit.apple.texture]",
            "smooth = true",
            "",
            "[[fruit.varieties]]",
            "name = \"red delicious\""
        )
        val document = TomlDocument.parse(text)
        val root = document.root

        assertTrue(document.isValid, document.problems.toString())
        val a = root.entry("a")
        val aTable = a.value as TomlTable
        assertEquals(TomlTableKind.Header, aTable.kind)
        assertEquals(rangeOf(text, "a", after = "c = 1\n["), a.keyRange)
        assertEquals(TextRange(text.indexOf("[a]"), rangeOf(text, "2", after = "d = ").end), aTable.range)
        assertEquals(listOf("b", "d"), aTable.entries.map { it.key })

        val apple = root.at("fruit", "apple") as TomlTable
        assertEquals(TomlTableKind.Implicit, apple.kind)
        assertEquals(listOf("color", "taste", "texture"), apple.entries.map { it.key })
        assertEquals(TextRange(text.indexOf("apple.color"), rangeOf(text, "true", after = "smooth = ").end), apple.range)
        assertTrue(root.at("fruit", "varieties") is TomlArray)
    }

    @Test
    fun aHeaderUnderAnArrayOfTablesIsInItsLastItem() {
        val text = toml(
            "[[fruits]]",
            "name = \"apple\"",
            "[fruits.physical]",
            "color = \"red\"",
            "[[fruits.varieties]]",
            "name = \"red delicious\"",
            "[[fruits.varieties]]",
            "name = \"granny smith\"",
            "[[fruits]]",
            "name = \"banana\""
        )
        val document = TomlDocument.parse(text)
        val fruits = document.root.at("fruits") as TomlArray

        assertTrue(document.isValid, document.problems.toString())
        assertEquals(2, fruits.items.size)
        val apple = fruits.items.first() as TomlTable
        assertEquals(listOf("name", "physical", "varieties"), apple.entries.map { it.key })
        assertEquals(2, (apple.at("varieties") as TomlArray).items.size)
        // An item runs over its own key/values; the tables other headers put in it are written after it.
        assertEquals(TextRange(0, rangeOf(text, "\"apple\"").end), apple.range)
        assertEquals(listOf("name"), (fruits.items.last() as TomlTable).entries.map { it.key })
    }

    @Test
    fun printingGivesTheTextBackCharacterForCharacter() {
        val valid = toml(
            "\uFEFF# Every construct",
            "a = 1 # after",
            "\"quoted key\".'literal' = $TRIPLE_QUOTE",
            "two",
            "lines$TRIPLE_QUOTE",
            "[ table . sub ]",
            "array = [ 1,",
            "  # inside",
            "  2, ]",
            "inline = { x = 1, y = [ 'a' ] }",
            "[[ items ]]",
            "date = 1979-05-27 07:32:00Z"
        ).replace("\n", "\r\n")
        val broken = toml("a = [1, 2", "b = \"open", "[c", "= 3", "d = { e = 1,", "  f = 2 }")

        assertTrue(TomlDocument.parse(valid).isValid, TomlDocument.parse(valid).problems.toString())
        assertEquals(valid, TomlDocument.parse(valid).print())
        assertFalse(TomlDocument.parse(broken).isValid)
        assertEquals(broken, TomlDocument.parse(broken).print())
    }

    @Test
    fun anEmptyDocumentIsAnEmptyRoot() {
        listOf("", "\n\n", "# only a comment\n").forEach { text ->
            val document = TomlDocument.parse(text)
            assertTrue(document.isValid)
            assertEquals(emptyList(), document.root.entries)
            assertEquals(TextRange(0, text.length), document.root.range)
        }
    }
}
