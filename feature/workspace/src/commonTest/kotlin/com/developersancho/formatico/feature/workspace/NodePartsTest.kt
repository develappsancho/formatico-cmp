package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.json.PathStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NodePartsTest {
    private val orders = PathStep.Key("orders")
    private val first = PathStep.Index(0)
    private val id = PathStep.Key("id")

    @Test
    fun aPathIsItsParentsStepsAndOneMore() {
        val parent = NodePath.Root + orders + first
        val path = parent + id

        assertEquals(0, NodePath.Root.size)
        assertTrue(NodePath.Root.isEmpty())
        assertEquals(listOf(orders, first, id), path.toList())
        assertEquals(id, path.last())
        assertEquals(first, path[1])
        // It is a list like any other: equal to one of the same steps, and the parent is left as it was.
        assertEquals(listOf<PathStep>(orders, first, id), path)
        assertEquals(listOf<PathStep>(orders, first).hashCode(), parent.hashCode())
        assertEquals(2, parent.size)
        assertFailsWith<IndexOutOfBoundsException> { path[3] }
    }

    @Test
    fun aDocumentsNamesAndStepsAreKeptOnce() {
        val parts = SharedParts()
        val name = parts.name(buildString { append("sta").append("tus") })

        assertSame(name, parts.name(buildString { append("status") }))
        assertSame(parts.key("status"), parts.key(buildString { append("status") }))
        assertSame(name, parts.key("status").name)
        assertEquals("12", parts.place(12))
        assertSame(parts.place(3), parts.place(3))
        val read = buildString { append("Elif") }
        assertSame(read, parts.sameOrBoth(buildString { append("Elif") }, read))
        assertEquals("\"Elif\"", parts.sameOrBoth("\"Elif\"", read))
    }

    @Test
    fun aReadDocumentsNodesShareTheirParentsPathsAndTheirNames() {
        val text = """{"orders": [{"id": 1, "status": "open"}, {"id": 2, "status": "paid"}]}"""
        val root = (readDocument(text, Format.Json) as Reading.Read).root!!
        val (one, two) = root.children.single().children

        assertSame(one.children[0].name, two.children[0].name)
        assertEquals(listOf(orders, first, id), one.children[0].path)
        val csv = (readDocument("id,name\n1,Elif\n2,\"Ada\"\n", Format.Csv) as Reading.Read).root!!
        // A field in no quotes is its value as it is written; a quoted one is written with its quotes.
        val elif = csv.children[0].children[1]
        assertSame(elif.value, elif.text)
        assertEquals("\"Ada\"", csv.children[1].children[1].text)
        assertEquals("Ada", csv.children[1].children[1].value)
    }
}
