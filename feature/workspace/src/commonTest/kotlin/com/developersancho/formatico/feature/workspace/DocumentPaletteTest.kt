package com.developersancho.formatico.feature.workspace

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DocumentPaletteTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val order = OpenDocument(id = 1, name = "order.json", text = PreviewCompareA, format = Format.Json)

    /**
     * The palette's commands for each of [documents], drawn once in [window] on [platform], once their words are read:
     * a test that runs on every target draws in its own `runComposeUiTest`, and the web reads the words later.
     */
    private suspend fun ComposeUiTest.commandsOf(
        vararg documents: OpenDocument,
        window: PantrixWindow = desktop,
        platform: PlatformType = PlatformType.Desktop
    ): List<List<PantrixCommand>> {
        val commands = MutableList(documents.size) { emptyList<PantrixCommand>() }
        setContent {
            PantrixTestTheme(platform = platform, window = window) {
                documents.forEachIndexed { index, document -> commands[index] = documentPaletteCommands(document) }
            }
        }
        awaitUntil("the commands' words are read") { commands.all { list -> list.isNotEmpty() && list.all { it.label.isNotEmpty() } } }
        return commands
    }

    /** The document's commands Settings lists the keys of, drawn in [window] on [platform], once their words are read. */
    private suspend fun ComposeUiTest.shortcutsOf(
        window: PantrixWindow = desktop,
        platform: PlatformType = PlatformType.Desktop
    ): List<PantrixCommand> {
        var commands: List<PantrixCommand>? = null
        setContent { PantrixTestTheme(platform = platform, window = window) { commands = documentShortcutCommands() } }
        awaitUntil("the shortcuts' words are read") { commands?.all { it.label.isNotEmpty() } == true }
        return commands.orEmpty()
    }

    @Test
    fun settingsListsTheKeysOfAJsonDocumentsCommands() = runComposeUiTest {
        val commands = shortcutsOf()

        assertEquals(listOf("format", "minify", "copy", "save", "tree", "table", "graph", "query"), commands.map { it.key })
        assertEquals(listOf("Document", "View"), commands.mapNotNull { it.group }.distinct())
        assertEquals("Query with JSONPath", commands.last().label)
        assertEquals(DocumentShortcuts.Query, commands.last().shortcut)
    }

    @Test
    fun aBrowserListsNoKeysItKeepsForItself() = runComposeUiTest {
        assertEquals(listOf("format", "minify", "copy", "save", "query"), shortcutsOf(platform = PlatformType.Web).map { it.key })
    }

    @Test
    fun aPhoneListsNoKeys() = runComposeUiTest {
        assertEquals(emptyList(), shortcutsOf(window = phone))
    }

    @Test
    fun aWideWindowListsTheBoardsCommandsUnderTheirGroupsWithTheirKeys() = runComposeUiTest {
        val commands = commandsOf(order).single()

        assertEquals(
            listOf("format", "minify", "sort", "copy", "save", "compare", "tree", "table", "graph", "query") +
                listOf("convert_yaml", "convert_toml", "convert_xml", "convert_csv") +
                listOf("types_kotlin", "types_typescript", "types_swift", "types_go", "types_jsonschema", "schema"),
            commands.map { it.key }
        )
        assertEquals(listOf("Document", "View", "Convert", "Types", "Schema"), commands.mapNotNull { it.group }.distinct())
        val byKey = commands.associateBy { it.key }
        assertEquals("Format document", byKey.getValue("format").label)
        assertEquals(DocumentShortcuts.Format, byKey.getValue("format").shortcut)
        assertEquals(DocumentShortcuts.Tree, byKey.getValue("tree").shortcut)
        assertEquals("Query with JSONPath", byKey.getValue("query").label)
        assertEquals("Convert to YAML", byKey.getValue("convert_yaml").label)
        assertEquals("Generate TypeScript types", byKey.getValue("types_typescript").label)
        assertEquals("Generate a JSON Schema", byKey.getValue("types_jsonschema").label)
        assertEquals("Validate against a schema", byKey.getValue("schema").label)
        assertTrue("beautify" in byKey.getValue("format").keywords)
        assertTrue(commands.all { it.enabled })
    }

    @Test
    fun aPhoneListsFewerCommandsAndNoKeys() = runComposeUiTest {
        val commands = commandsOf(order, window = phone).single()

        assertEquals(
            listOf("format", "minify", "copy", "save", "query", "convert_yaml", "convert_toml", "convert_xml", "convert_csv") +
                listOf("types_kotlin", "types_typescript", "types_swift", "types_go", "types_jsonschema", "schema"),
            commands.map { it.key }
        )
        assertTrue(commands.all { it.shortcut == null })
    }

    @Test
    fun aBrowserKeepsCommandAndANumberForItsOwnTabs() = runComposeUiTest {
        val byKey = commandsOf(order, platform = PlatformType.Web).single().associateBy { it.key }

        assertNull(byKey.getValue("tree").shortcut)
        assertNull(byKey.getValue("table").shortcut)
        assertEquals(DocumentShortcuts.Query, byKey.getValue("query").shortcut)
    }

    @Test
    fun whatADocumentCannotTakeIsListedFaint() = runComposeUiTest {
        val (csv, text, xml) = commandsOf(
            OpenDocument(id = 2, name = "a.csv", text = "a,b\n1,2\n", format = Format.Csv),
            OpenDocument(id = 3, name = "notes.txt", text = "Notes\n", format = null),
            OpenDocument(id = 4, name = "a.xml", text = "<a/>", format = Format.Xml)
        ).map { commands -> commands.associateBy { it.key } }
        assertTrue(csv.getValue("format").enabled)
        assertFalse(csv.getValue("minify").enabled)
        assertFalse(csv.getValue("sort").enabled)
        assertTrue("convert_json" in csv && "convert_csv" !in csv)

        assertFalse(text.getValue("format").enabled)
        assertFalse(text.getValue("query").enabled)
        assertFalse(text.getValue("tree").enabled)
        assertFalse(text.getValue("convert_yaml").enabled)
        assertFalse(text.getValue("types_kotlin").enabled)
        assertFalse(text.getValue("schema").enabled)
        assertTrue(text.getValue("copy").enabled && text.getValue("compare").enabled)

        assertEquals("Query with XPath", xml.getValue("query").label)
    }

    @Test
    fun runningACommandShowsItsViewOrEditsTheDocument() = runComposeUiTest {
        val workbench = Workbench()
        val id = workbench.open("order.json", """{"b":1,"a":2}""", Format.Json)
        lateinit var palette: DocumentPalette
        setContent { PantrixTestTheme(window = desktop) { palette = rememberDocumentPalette(workbench) } }
        waitForIdle()
        val view = { workbench.state.value.documents.single().view }

        assertTrue(palette.run("tree", id))
        assertEquals(DocumentPane.Tree, view().pane)
        assertTrue(palette.run("convert_yaml", id))
        assertEquals(DocumentPane.Convert, view().pane)
        assertEquals(Format.Yaml, view().convert.to)
        assertTrue(palette.run("types_go", id))
        assertEquals(DocumentPane.Types, view().pane)
        assertEquals(TypeLanguage.Go, view().types.language)
        assertTrue(palette.run("graph", id))
        assertEquals(DocumentPane.Graph, view().pane)
        assertTrue(palette.run("query", id))
        assertEquals(DocumentPane.Query, view().pane)
        assertTrue(palette.run("schema", id))
        assertEquals(DocumentPane.Schema, view().pane)
        assertFalse(palette.run("settings", id))

        assertTrue(palette.run("format", id))
        awaitUntil("the document is formatted") { workbench.state.value.documents.single().text.contains('\n') }
    }
}
