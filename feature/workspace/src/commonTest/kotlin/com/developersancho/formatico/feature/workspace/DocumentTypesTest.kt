package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.format.types.TypeNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DocumentTypesTest {
    private fun typesOf(name: String?, text: String, format: Format?, choice: TypesChoice = TypesChoice()): TypesOutcome {
        val document = OpenDocument(id = 1, name = name, text = text, format = format, view = DocumentView(types = choice))
        return typesOf(document, readDocument(text, format), choice)
    }

    @Test
    fun aDocumentsTypesAreNamedAfterItAndSaidOf() {
        val written = assertIs<TypesOutcome.Written>(typesOf("order.json", PreviewBoardOrder, Format.Json))

        assertEquals(Format.Json, written.from)
        assertEquals(TypeLanguage.Kotlin, written.language)
        assertEquals("Order", written.root)
        assertEquals("Order.kt", written.fileName)
        assertEquals(6, written.types)
        assertEquals(written.text.trimEnd('\n').lines().size, written.lines)
        assertEquals(written.text.encodeToByteArray().size.toLong(), written.bytes)
        assertTrue(TypeNote.OnlyNull(listOf("coupon")) in written.notes, "${written.notes}")
    }

    @Test
    fun theReadersNameForTheRootIsMadeATypesName() {
        val choice = TypesChoice(language = TypeLanguage.TypeScript, root = "my order")
        val written = assertIs<TypesOutcome.Written>(typesOf("order.json", PreviewBoardOrder, Format.Json, choice))

        assertEquals("MyOrder", written.root)
        assertTrue(written.text.contains("export interface MyOrder {"), written.text)
        // A name typed and taken away gives the document's back.
        assertEquals("Order", (typesOf("order.json", PreviewBoardOrder, Format.Json, TypesChoice(root = " ")) as TypesOutcome.Written).root)
    }

    @Test
    fun aListsRootIsOneOfItsItems() {
        val written = assertIs<TypesOutcome.Written>(typesOf("events.ndjson", PreviewEvents, Format.Ndjson))

        assertEquals("Events", written.root)
        assertTrue(TypeNote.RootList in written.notes, "${written.notes}")
    }

    @Test
    fun whatHasNoTypesSaysWhy() {
        assertEquals(TypesOutcome.Untyped(Format.Json), typesOf("totals.json", "[1, 2.5]", Format.Json))
        assertEquals(TypesOutcome.Unread, typesOf("notes.txt", "Notes\n", null))
        assertIs<TypesOutcome.Invalid>(typesOf("settings.json", PreviewInvalid, Format.Json))
    }
}
