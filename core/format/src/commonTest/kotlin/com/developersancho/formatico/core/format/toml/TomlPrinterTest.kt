package com.developersancho.formatico.core.format.toml

import com.developersancho.formatico.core.format.text.Indentation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TomlPrinterTest {
    private val messy = toml(
        "# A messy one",
        "   title    =   \"Formatico\"   # the name",
        "",
        "",
        "",
        "  [owner]",
        "name=\"Tom\"",
        "  dob = 1979-05-27T07:32:00-08:00",
        "# Before the database",
        "[ database ]",
        "ports = [ 8000,8001 ,  8002 ]",
        "data = [ [ \"delta\", \"phi\" ], [ 3.14 ] ]",
        "temp_targets = { cpu = 79.5,case=72.0 }",
        "hosts = [",
        "    \"alpha\",",
        "  \"omega\",",
        "]",
        "mixed = [",
        "  1, 2,",
        "  3",
        "]",
        "commented = [",
        "  1, # one",
        "    2,",
        "]",
        "  site . \"google.com\" = true",
        "notes = '''",
        "  kept as it is",
        "'''",
        "",
        "# The end"
    )

    private val cargo = toml(
        "[package]",
        "name = \"formatico-cli\"",
        "version = \"0.3.1\"",
        "edition = \"2021\"",
        "authors = [\"Developer Sancho <dev@example.com>\"]",
        "description = $TRIPLE_QUOTE",
        "A command line for Formatico: format, validate and convert \\",
        "structured text.$TRIPLE_QUOTE",
        "license = \"MIT OR Apache-2.0\"",
        "rust-version = \"1.74\"",
        "",
        "[[bin]]",
        "name = \"formatico\"",
        "path = \"src/main.rs\"",
        "",
        "[dependencies]",
        "serde_json = \"1.0\"",
        "clap = { version = \"4.5\", features = [\"derive\", \"env\"] }",
        "serde = { version = \"1.0.197\", features = [\"derive\"] }",
        "toml_edit = { version = \"0.22\", optional = true }",
        "tokio = { version = \"1\", default-features = false, features = [",
        "    \"rt-multi-thread\",",
        "    \"macros\",",
        "] }",
        "",
        "[dev-dependencies]",
        "pretty_assertions = \"1.4\"",
        "",
        "[features]",
        "default = [\"toml\"]",
        "toml = [\"dep:toml_edit\"]",
        "",
        "[profile.release]",
        "lto = true",
        "codegen-units = 1",
        "opt-level = 3",
        "",
        "[target.'cfg(windows)'.dependencies]",
        "windows-sys = { version = \"0.52\", features = [\"Win32_Foundation\"] }"
    )

    @Test
    fun aMessyDocumentIsWrittenOutTidily() {
        val expected = toml(
            "# A messy one",
            "title = \"Formatico\" # the name",
            "",
            "[owner]",
            "name = \"Tom\"",
            "dob = 1979-05-27T07:32:00-08:00",
            "",
            "# Before the database",
            "[database]",
            "ports = [8000, 8001, 8002]",
            "data = [[\"delta\", \"phi\"], [3.14]]",
            "temp_targets = { cpu = 79.5, case = 72.0 }",
            "hosts = [",
            "  \"alpha\",",
            "  \"omega\",",
            "]",
            "mixed = [",
            "  1,",
            "  2,",
            "  3",
            "]",
            "commented = [",
            "  1, # one",
            "    2,",
            "]",
            "site.\"google.com\" = true",
            "notes = '''",
            "  kept as it is",
            "'''",
            "",
            "# The end"
        )
        val formatted = TomlDocument.parse(messy).formatted()

        assertEquals(expected, formatted)
        assertEquals(formatted, TomlDocument.parse(formatted).formatted())
        assertEquals(valuesOf(messy), valuesOf(formatted))
    }

    @Test
    fun theIndentationIsOneLevelOfWhatIsAskedFor() {
        val document = TomlDocument.parse(toml("a = [", "1,", "[2,", "3]", "]"))

        assertEquals(toml("a = [", "\t1,", "\t[", "\t\t2,", "\t\t3", "\t]", "]"), document.formatted(Indentation.Tabs))
        assertEquals(
            toml("a = [", "    1,", "    [", "        2,", "        3", "    ]", "]"),
            document.formatted(Indentation.Spaces(4))
        )
    }

    @Test
    fun aBlankLineGoesBeforeEveryHeaderAndCommentsAboveAHeaderGoWithIt() {
        val text = toml("", "", "[a]", "x = 1", "# about b", "# more", "[b]", "", "", "y = 2", "[[c]]", "[[c]]", "", "")

        assertEquals(
            toml("[a]", "x = 1", "", "# about b", "# more", "[b]", "", "y = 2", "", "[[c]]", "", "[[c]]"),
            TomlDocument.parse(text).formatted()
        )
    }

    @Test
    fun sortingTheKeysTakesTheirCommentsAlongAndKeepsTheHeadersInOrder() {
        val text = toml(
            "zebra = 1",
            "apple.b = 2",
            "apple.a = 3",
            "",
            "[package]",
            "version = \"0.1.0\"",
            "# The crate's name",
            "name = \"formatico\"",
            "edition = \"2021\" # the latest",
            "# left at the end",
            "",
            "# Above the dependencies",
            "[dependencies]",
            "serde = { version = \"1\", features = [\"derive\"] }",
            "anyhow = \"1\""
        )
        val expected = toml(
            "apple.a = 3",
            "apple.b = 2",
            "zebra = 1",
            "",
            "[package]",
            "edition = \"2021\" # the latest",
            "# The crate's name",
            "name = \"formatico\"",
            "version = \"0.1.0\"",
            "# left at the end",
            "",
            "# Above the dependencies",
            "[dependencies]",
            "anyhow = \"1\"",
            "serde = { features = [\"derive\"], version = \"1\" }"
        )
        val sorted = TomlDocument.parse(text).formatted(sortKeys = true)

        assertEquals(expected, sorted)
        assertEquals(valuesOf(text, sorted = true), valuesOf(sorted, sorted = true))
    }

    @Test
    fun minifyingLeavesNoCommentNoBlankLineAndNoSpace() {
        val expected = toml(
            "title=\"Formatico\"",
            "[owner]",
            "name=\"Tom\"",
            "dob=1979-05-27T07:32:00-08:00",
            "[database]",
            "ports=[8000,8001,8002]",
            "data=[[\"delta\",\"phi\"],[3.14]]",
            "temp_targets={cpu=79.5,case=72.0}",
            "hosts=[\"alpha\",\"omega\"]",
            "mixed=[1,2,3]",
            "commented=[1,2]",
            "site.\"google.com\"=true",
            "notes='''",
            "  kept as it is",
            "'''"
        )
        val minified = TomlDocument.parse(messy).minified()

        assertEquals(expected, minified)
        assertEquals(valuesOf(messy), valuesOf(minified))
    }

    @Test
    fun theLineEndingsAreTheDocuments() {
        val document = TomlDocument.parse("a = 1\r\n[b]\r\nc = [\r\n  1,\r\n]\r\nd = \"\"\"x\r\ny\"\"\"\r\n")

        assertEquals("a = 1\r\n\r\n[b]\r\nc = [\r\n  1,\r\n]\r\nd = \"\"\"x\r\ny\"\"\"\r\n", document.formatted())
        assertEquals("a=1\r\n[b]\r\nc=[1]\r\nd=\"\"\"x\r\ny\"\"\"\r\n", document.minified())
        assertEquals("a = 1", TomlDocument.parse("a=1").formatted())
        assertEquals("a=1", TomlDocument.parse("a = 1").minified())
    }

    @Test
    fun aCargoManifestKeepsItsValuesWhicheverWayItIsWrittenOut() {
        val document = TomlDocument.parse(cargo)

        assertEquals(
            listOf("package", "bin", "dependencies", "dev-dependencies", "features", "profile", "target"),
            document.root.entries.map { it.key }
        )
        assertEquals(
            "A command line for Formatico: format, validate and convert structured text.",
            (document.root.at("package", "description") as TomlString).value
        )
        assertEquals("cfg(windows)", (document.root.at("target") as TomlTable).entries.single().key)
        val tokio = document.root.at("dependencies", "tokio") as TomlTable
        val features = (tokio.at("features") as TomlArray).items.map { (it as TomlString).value }
        assertEquals(listOf("rt-multi-thread", "macros"), features)

        val formatted = document.formatted()
        assertEquals(
            cargo.replace("    \"rt-multi-thread\",\n    \"macros\",", "  \"rt-multi-thread\",\n  \"macros\","),
            formatted
        )
        assertEquals(valuesOf(cargo), valuesOf(formatted))
        assertEquals(valuesOf(cargo), valuesOf(document.minified()))
        assertEquals(valuesOf(cargo, sorted = true), valuesOf(document.formatted(sortKeys = true), sorted = true))
    }

    @Test
    fun aDocumentThatIsNotValidIsNotWrittenOut() {
        assertFailsWith<IllegalArgumentException> { TomlDocument.parse("a = 1\na = 2").formatted() }
        assertFailsWith<IllegalArgumentException> { TomlDocument.parse("a = [").minified() }
    }

    @Test
    fun anEmptyDocumentIsWrittenOutEmpty() {
        assertEquals("", TomlDocument.parse("\n\n").formatted())
        assertEquals("", TomlDocument.parse("").minified())
        assertEquals("# only\n", TomlDocument.parse("\n# only\n\n").formatted())
    }
}
