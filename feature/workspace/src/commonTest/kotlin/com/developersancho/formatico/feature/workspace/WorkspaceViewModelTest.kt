package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val workbench = Workbench(worker = dispatcher)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(id: Long) = WorkspaceViewModel(documentId = id, workbench = workbench, reader = dispatcher)

    @Test
    fun theDocumentIsReadAndOpensWithItsObjects() {
        val id = workbench.open("order.json", """{"a": {"b": 1}, "c": [2]}""", Format.Json)

        val state = viewModel(id).state.value

        val reading = assertIs<Reading.Read>(state.reading)
        assertEquals(0, reading.errors)
        assertEquals(setOf("$", "$.a"), state.expanded)
        assertTrue(state.editable)
        assertEquals(1, state.stats?.lines)
    }

    @Test
    fun theChosenValueAndTheOpenBranchesAreKeptWithTheDocument() {
        val id = workbench.open(null, """{"a": {"b": 1}}""", Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.Select("$.a.b"))
        viewModel.dispatch(WorkspaceIntent.Expand(setOf("$")))

        assertEquals(listOf("$", "$.a", "$.a.b"), viewModel.state.value.trail.map { it.key })
        assertEquals(setOf("$"), viewModel.state.value.expanded)
        // A workspace made again for the same tab finds it as it was left.
        val again = viewModel(id).state.value
        assertEquals("$.a.b", again.selected?.key)
        assertEquals(setOf("$"), again.expanded)
    }

    @Test
    fun anEditIsMadeAndReportedAndTheDocumentReadAgain() = runTest {
        val id = workbench.open(null, """{"b": 1, "a": 2}""", Format.Json)
        val viewModel = viewModel(id)
        val effects = mutableListOf<WorkspaceEffect>()
        backgroundScope.launch(dispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.dispatch(WorkspaceIntent.Edit(DocumentEdit.SortKeys))

        val edited = assertIs<WorkspaceEffect.Edited>(effects.single())
        assertEquals(DocumentEdit.SortKeys, edited.edit)
        assertIs<EditResult.Changed>(edited.result)
        assertEquals(listOf("a", "b"), viewModel.state.value.root?.children?.map { it.name })
    }

    @Test
    fun aFormatNotReadYetIsNotEditable() {
        val id = workbench.open("deployment.yaml", "a: 1", Format.Yaml)

        val state = viewModel(id).state.value

        assertEquals(Reading.Unread(Format.Yaml), state.reading)
        assertFalse(state.editable)
    }

    @Test
    fun changingTheFormatReadsTheDocumentAgain() {
        val id = workbench.open(null, "{a: 1}", Format.Json)
        val viewModel = viewModel(id)
        assertEquals(1, assertIs<Reading.Read>(viewModel.state.value.reading).errors)

        viewModel.dispatch(WorkspaceIntent.ChangeFormat(Format.Json5))

        assertEquals(0, assertIs<Reading.Read>(viewModel.state.value.reading).errors)
    }

    @Test
    fun closingTheDocumentLeavesTheWorkspaceWithNone() {
        val id = workbench.open(null, "{}", Format.Json)
        val viewModel = viewModel(id)

        viewModel.dispatch(WorkspaceIntent.Close)

        assertNull(viewModel.state.value.document)
        assertTrue(workbench.state.value.documents.isEmpty())
    }
}
