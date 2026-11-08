package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkbenchTest {
    private val workbench = Workbench(worker = UnconfinedTestDispatcher())

    @Test
    fun aDocumentOpensInATabOfItsOwnAndIsChosen() {
        val first = workbench.open("order.json", "{}", Format.Json)
        val second = workbench.open(null, "a: 1", Format.Yaml)

        val state = workbench.state.value
        assertEquals(listOf(first, second), state.documents.map { it.id })
        assertEquals(second, state.selectedId)
    }

    @Test
    fun theSameDocumentIsChosenRatherThanOpenedTwice() {
        val first = workbench.open("order.json", "{}", Format.Json)
        workbench.select(null)

        assertEquals(first, workbench.open("order.json", "{}", Format.Json))
        assertEquals(1, workbench.state.value.documents.size)
        assertEquals(first, workbench.state.value.selectedId)
    }

    @Test
    fun closingTheChosenTabChoosesTheNextOrElseThePrevious() {
        val a = workbench.open("a.json", "1", Format.Json)
        val b = workbench.open("b.json", "2", Format.Json)
        val c = workbench.open("c.json", "3", Format.Json)

        workbench.select(b)
        workbench.close(b)
        assertEquals(c, workbench.state.value.selectedId)

        workbench.close(c)
        assertEquals(a, workbench.state.value.selectedId)

        workbench.close(a)
        assertNull(workbench.state.value.selectedId)
        assertTrue(workbench.state.value.documents.isEmpty())
    }

    @Test
    fun anEditChangesTheTextAndCanBeUndone() = runTest {
        val id = workbench.open("order.json", """{"b":1,"a":[true]}""", Format.Json)

        val result = assertIs<EditResult.Changed>(workbench.edit(id, DocumentEdit.SortKeys))
        val sorted = workbench.state.value.selected!!
        assertEquals("{\n  \"a\": [\n    true\n  ],\n  \"b\": 1\n}", sorted.text)
        assertTrue(sorted.edited)

        workbench.restore(result.previous)
        val restored = workbench.state.value.selected!!
        assertEquals("""{"b":1,"a":[true]}""", restored.text)
        assertFalse(restored.edited)
    }

    @Test
    fun anEditThatChangesNothingSaysSo() = runTest {
        val id = workbench.open(null, """{"a":1}""", Format.Json)

        assertEquals(EditResult.Unchanged, workbench.edit(id, DocumentEdit.Minify))
        assertFalse(workbench.state.value.selected!!.edited)
    }

    @Test
    fun aDocumentWithErrorsOrInAFormatNotReadYetIsNotEdited() = runTest {
        val broken = workbench.open("broken.json", """{"a": }""", Format.Json)
        val toml = workbench.open("Cargo.toml", "a = 1", Format.Toml)
        val yaml = workbench.open("broken.yaml", "a: [1\n", Format.Yaml)

        assertEquals(EditResult.NotPossible, workbench.edit(broken, DocumentEdit.Format))
        assertEquals(EditResult.NotPossible, workbench.edit(toml, DocumentEdit.Format))
        assertEquals(EditResult.NotPossible, workbench.edit(yaml, DocumentEdit.Format))
    }

    @Test
    fun yamlIsFormattedMinifiedAndSortedWithItsIndentation() = runTest {
        val id = workbench.open("a.yaml", "b:\n    d: 1 # one\n    c: [2, 3]\na: x\n", Format.Yaml)

        assertIs<EditResult.Changed>(workbench.edit(id, DocumentEdit.SortKeys))
        assertEquals("a: x\nb:\n    c: [2, 3]\n    d: 1 # one\n", workbench.state.value.selected!!.text)

        assertIs<EditResult.Changed>(workbench.edit(id, DocumentEdit.Minify))
        assertEquals("{a: x,b: {c: [2,3],d: 1}}\n", workbench.state.value.selected!!.text)

        // A flow collection on one line stays as it is written.
        assertEquals(EditResult.Unchanged, workbench.edit(id, DocumentEdit.Format))
    }

    @Test
    fun aCsvOpensOnItsTableAndIsFormattedAlone() = runTest {
        val id = workbench.open("a.csv", "\"a\",\"b\"\n\"1\",\"x, y\"\n", Format.Csv)

        assertEquals(DocumentPane.Table, workbench.state.value.selected!!.view.pane)
        assertIs<EditResult.Changed>(workbench.edit(id, DocumentEdit.Format))
        assertEquals("a,b\n1,\"x, y\"\n", workbench.state.value.selected!!.text)
        assertEquals(EditResult.NotPossible, workbench.edit(id, DocumentEdit.Minify))
        assertEquals(EditResult.NotPossible, workbench.edit(id, DocumentEdit.SortKeys))
    }

    @Test
    fun yamlKeysAreNotSortedWhereAnAliasWouldComeBeforeItsAnchor() = runTest {
        val text = "b: &x 1\na: *x\n"
        val id = workbench.open("a.yaml", text, Format.Yaml)

        assertEquals(EditResult.AliasBeforeAnchor, workbench.edit(id, DocumentEdit.SortKeys))
        assertEquals(text, workbench.state.value.selected!!.text)
    }

    @Test
    fun formattingKeepsTheDocumentsIndentation() = runTest {
        val id = workbench.open(null, "{\n    \"a\": [1, 2]\n}", Format.Json)

        workbench.edit(id, DocumentEdit.Format)

        assertEquals("{\n    \"a\": [\n        1,\n        2\n    ]\n}", workbench.state.value.selected!!.text)
    }

    @Test
    fun changingTheFormatForgetsHowTheTreeWasLeft() {
        val id = workbench.open(null, "{}", Format.Json)
        workbench.changeView(id) { it.copy(expanded = setOf("$"), selected = "$") }

        workbench.changeFormat(id, Format.Json5)

        val document = workbench.state.value.selected!!
        assertEquals(Format.Json5, document.format)
        assertEquals(DocumentView(), document.view)
    }
}
