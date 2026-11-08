package com.developersancho.formatico.feature.workspace.editor

import com.developersancho.formatico.core.format.Format
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
    fun plainTextHasNoHighlighterAndEachFormatItsOwn() {
        assertNull(editorLanguage(null).highlighter)
        assertEquals("//", editorLanguage(Format.Jsonc).syntax.lineComment)
        assertNull(editorLanguage(Format.Json).syntax.lineComment)
        assertEquals("#", editorLanguage(Format.Yaml).syntax.lineComment)
        assertEquals("\t", editorIndent(com.developersancho.formatico.core.format.text.Indentation.Tabs))
    }
}
