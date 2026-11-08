package com.developersancho.formatico.feature.workspace.editor

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.pantrixui.components.data.codeblock.CodeTokenKind
import com.pantrixui.components.data.codeblock.CodeTokenKind.Comment
import com.pantrixui.components.data.codeblock.CodeTokenKind.Keyword
import com.pantrixui.components.data.codeblock.CodeTokenKind.Number
import com.pantrixui.components.data.codeblock.CodeTokenKind.Property
import com.pantrixui.components.data.codeblock.CodeTokenKind.Punctuation
import com.pantrixui.components.data.codeblock.CodeTokenKind.StringLiteral
import com.pantrixui.components.data.codeblock.CodeTokenKind.Tag
import com.pantrixui.components.data.codeeditor.CodeHighlighter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HighlightersTest {
    /** Each line of [lines] read as [highlighter] reads them, one after another: its runs as `text:kind`. */
    private fun <S : Any> CodeHighlighter<S>.read(vararg lines: String): List<List<String>> {
        var state = initialState
        return lines.map { line ->
            val read = highlight(line, state)
            state = read.state
            read.tokens.map { "${line.substring(it.start, it.end)}:${it.kind.short()}" }
        }
    }

    private fun CodeTokenKind.short() = when (this) {
        Property -> "key"
        StringLiteral -> "string"
        Number -> "number"
        Keyword -> "word"
        Punctuation -> "sign"
        Comment -> "comment"
        Tag -> "tag"
        else -> name
    }

    @Test
    fun jsonKeysAreTheStringsBeforeAColon() {
        assertEquals(
            listOf(
                listOf("{:sign"),
                listOf("\"id\":key", "::sign", "\"ord_8842\":string", ",:sign"),
                listOf("\"total\":key", "::sign", "-129.90:number", ",:sign", "\"paid\":key", "::sign", "true:word"),
                listOf("}:sign")
            ),
            JsonHighlighter.read("{", "  \"id\": \"ord_8842\",", "  \"total\": -129.90, \"paid\": true", "}")
        )
    }

    @Test
    fun jsonCommentsRunToTheLinesEndOrOverSeveralLines() {
        assertEquals(
            listOf(
                listOf("// a note:comment"),
                listOf("/* one:comment"),
                listOf("two */:comment", "null:word"),
                listOf("unquoted:key", "::sign", "Infinity:number")
            ),
            JsonHighlighter.read("// a note", "/* one", "two */ null", "unquoted: Infinity")
        )
    }

    @Test
    fun yamlKeysValuesAnchorsAndBlockScalars() {
        assertEquals(
            listOf(
                listOf("# A service:comment"),
                listOf("labels:key", "::sign", "&labels:Annotation"),
                listOf("-:sign", "name:key", "::sign", "web:string"),
                listOf("replicas:key", "::sign", "3:number", "# three:comment"),
                listOf("script:key", "::sign", "|:sign"),
                listOf("    echo: one:string"),
                listOf("next:key", "::sign", "~:word")
            ),
            YamlHighlighter.read(
                "# A service",
                "labels: &labels",
                "- name: web",
                "replicas: 3 # three",
                "script: |",
                "    echo: one",
                "next: ~"
            )
        )
    }

    @Test
    fun csvsHeaderIsItsKeysAndAQuotedFieldGoesOnOverLines() {
        assertEquals(
            listOf(
                listOf("code:key", ",:sign", "name:key"),
                listOf("004:number", ",:sign", "\"Åland:string"),
                listOf("Islands\":string")
            ),
            TableHighlighter(',').read("code,name", "004,\"Åland", "Islands\"")
        )
    }

    @Test
    fun settingsAndXml() {
        assertEquals(
            listOf(listOf("[package]:tag"), listOf("name :key", "=:sign", "\"formatico\":string"), listOf("# done:comment")),
            SettingsHighlighter(comments = "#", separators = "=").read("[package]", "name = \"formatico\"", "# done")
        )
        assertEquals(
            listOf(
                listOf("<:sign", "entry:tag", "id:key", "=:sign", "\"1\":string", ">:sign", "</:sign", "entry:tag", ">:sign"),
                listOf("<!-- a:comment"),
                listOf("note -->:comment")
            ),
            XmlHighlighter.read("<entry id=\"1\">Formatico</entry>", "<!-- a", "note -->")
        )
    }

    @Test
    fun tomlCarriesItsStringsAndArraysOverLines() {
        assertEquals(
            listOf(
                listOf("[[bin]]:tag", "# the tool:comment"),
                listOf("description :key", "=:sign", "\"\"\"Format:string"),
                listOf("and convert\"\"\":string", "# done:comment"),
                listOf("deps :key", "=:sign", "[:sign"),
                listOf(
                    "{:sign",
                    "name:key",
                    "=:sign",
                    "\"serde\":string",
                    ",:sign",
                    "optional:key",
                    "=:sign",
                    "true:word",
                    "}:sign",
                    ",:sign"
                ),
                listOf("1979-05-27T07:32:00Z:number", ",:sign", "0x1F:number", "]:sign"),
                listOf("key :key", "=:sign", "1_000:number")
            ),
            TomlHighlighter.read(
                "[[bin]] # the tool",
                "description = \"\"\"Format",
                "and convert\"\"\" # done",
                "deps = [",
                "  { name = \"serde\", optional = true },",
                "  1979-05-27T07:32:00Z, 0x1F]",
                "key = 1_000"
            )
        )
    }

    @Test
    fun propertiesCarryAValueOnOverLinesItsBackslashesEnd() {
        assertEquals(
            listOf(
                listOf("! note:comment"),
                listOf("path\\ with\\ spaces:key", "=:sign", "one, \\:string"),
                listOf("two:string"),
                listOf("next:key", "::sign", "3:string")
            ),
            PropertiesHighlighter.read("! note", "path\\ with\\ spaces = one, \\", "    two", "next: 3")
        )
    }

    @Test
    fun xmlCarriesCdataAndValuesOverLines() {
        assertEquals(
            listOf(
                listOf("<:sign", "a:tag", "title:key", "=:sign", "\"one:string"),
                listOf("two\":string", ">:sign", "<![CDATA[:sign"),
                listOf("]]>:sign", "</:sign", "a:tag", ">:sign")
            ),
            XmlHighlighter.read("<a title=\"one", "two\"><![CDATA[if (a < b)", "{ }]]></a>")
        )
    }

    /** The highlighter of the code Types writes in [language]. */
    private fun typesHighlighter(language: TypeLanguage) = editorLanguage(language).highlighter as TypesHighlighter

    @Test
    fun kotlinTypesHaveKeywordsTypesAndAnnotations() {
        assertEquals(
            listOf(
                listOf("import:word", ".:sign", ".:sign", "Serializable:Type"),
                listOf("@Serializable:Annotation"),
                listOf("data:word", "class:word", "Order:Type", "(:sign"),
                listOf(
                    "@SerialName:Annotation",
                    "(:sign",
                    "\"first_name\":string",
                    "):sign",
                    "val:word",
                    "::sign",
                    "String:Type",
                    ",:sign"
                ),
                listOf("val:word", "::sign", "List:Type", "<:sign", "Item:Type", ">:sign", "?:sign", "=:sign", "null:word"),
                listOf("):sign")
            ),
            typesHighlighter(TypeLanguage.Kotlin).read(
                "import kotlinx.serialization.Serializable",
                "@Serializable",
                "data class Order(",
                "    @SerialName(\"first_name\") val firstName: String,",
                "    val `class`: List<Item>? = null",
                ")"
            )
        )
    }

    @Test
    fun typeScriptsOwnTypesAreTypes() {
        assertEquals(
            listOf(
                listOf("export:word", "interface:word", "Order:Type", "{:sign"),
                listOf("?:sign", "::sign", "string:Type", "|:sign", "null:word", ";:sign"),
                listOf("\"first-name\":string", "::sign", "Item:Type", "[:sign", "]:sign", ";:sign")
            ),
            typesHighlighter(
                TypeLanguage.TypeScript
            ).read("export interface Order {", "  nickname?: string | null;", "  \"first-name\": Item[];")
        )
    }

    @Test
    fun swiftCommentsAndGoTagsAreRead() {
        assertEquals(
            listOf(listOf("let:word", "::sign", "String:Type", "?:sign", "// Only ever null here: its type is a guess:comment")),
            typesHighlighter(TypeLanguage.Swift).read("    let coupon: String? // Only ever null here: its type is a guess")
        )
        assertEquals(
            listOf(
                listOf("type:word", "Order:Type", "struct:word", "{:sign"),
                listOf("string:Type", "`json:\"id\"`:string"),
                listOf("[:sign", "]:sign", "Item:Type", "`json:\"items,omitempty\"`:string")
            ),
            typesHighlighter(
                TypeLanguage.Go
            ).read("type Order struct {", "\tID    string `json:\"id\"`", "\tItems []Item `json:\"items,omitempty\"`")
        )
    }

    @Test
    fun aBlockCommentGoesOnOverLines() {
        assertEquals(
            listOf(listOf("/* one:comment"), listOf("two */:comment", "val:word")),
            typesHighlighter(TypeLanguage.Kotlin).read("/* one", "two */ val a")
        )
    }

    @Test
    fun plainTextHasNoHighlighterAndEachFormatItsOwn() {
        assertNull(editorLanguage(null).highlighter)
        assertEquals("//", editorLanguage(Format.Jsonc).syntax.lineComment)
        assertNull(editorLanguage(Format.Json).syntax.lineComment)
        assertEquals("#", editorLanguage(Format.Yaml).syntax.lineComment)
        assertEquals("\t", editorIndent(com.developersancho.formatico.core.format.text.Indentation.Tabs))
    }
}
