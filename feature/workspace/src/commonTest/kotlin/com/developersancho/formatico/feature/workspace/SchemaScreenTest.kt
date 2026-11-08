package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.components.data.codeeditor.CodeEditorState
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class SchemaScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    /** A schema opener that remembers what it was asked, and opens the tab [opened] where it is asked to. */
    private class RecordingOpener(private val opened: Long) : SchemaOpener {
        val asked = mutableListOf<String>()

        override fun pickFile(onOpened: (Long) -> Unit) {
            asked += "file"
            onOpened(opened)
        }

        override fun openAddress(address: String, onOpened: (Long) -> Unit) {
            asked += address
            onOpened(opened)
        }
    }

    @Test
    fun aWideWindowListsTheProblemsBesideTheTextAndCountsThemInTheStatusBar() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderAgainstSchema, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("2 problems against order.schema.json")
        awaitText("order.json is valid JSON, but not what the schema asks for.")
        awaitText("coupon must be a string, and is null.")
        awaitText("Line 8, column 13 · $.coupon")
        awaitText("customer has no phone, which the schema requires.")
        awaitText("Line 9, column 3 · $.customer")
        // The text stands beside them, under its format, its name and its lines.
        onPantrixNode("Tag", "source_format").assert(hasText("JSON"))
        onPantrixNode("Tag", "status").assert(hasText("2 schema problems"))
        onPantrixNode("Button", "schema_against").assert(hasContentDescription("Validate against order.schema.json. Choose another schema"))

        onPantrixNode("Button", "schema_against").performClick()
        awaitNode(hasText("No schema"), "The menu of schemas")
        onPantrixNode("MenuItem", "no_schema", scope = "Menu_schemas").performClick()
        // Pressed again, the toolbar's Schema gives the tree back.
        onPantrixNode("Action", "schema", scope = "Toolbar_document").performClick()
        assertEquals(listOf(WorkspaceIntent.Schema(SchemaChoice()), WorkspaceIntent.ShowPane(DocumentPane.Tree)), intents)
        assertNoFindings(findings)
    }

    @Test
    fun aTabletValidatesInTheWholeRoomAndAProblemPressedShowsTheText() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = OrderAgainstSchema, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Validate order.json against")
        // A tablet has no room for the text beside them.
        onPantrixNode("Tag", "source_format").assertDoesNotExist()
        onNodeWithText("coupon must be a string, and is null.").performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Text), intents.last())
        // The diagnostics are not asked about: the test draws in less than the tablet's window (PANTRIX_FINDINGS, "Test
        // temasına verilen pencere, testin çizdiği boy değil").
    }

    @Test
    fun aDocumentTheSchemaAllowsIsValidAgainstIt() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = ValidOrderAgainstSchema, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Valid against order.schema.json")
        awaitText("Every value of order.json is what the schema asks for (JSON Schema 2020-12).")
        onPantrixNode("Tag", "status").assert(hasText("Valid against the schema"))
    }

    @Test
    fun aPhoneValidatesOnAScreenOfItsOwnAndChoosesAnOpenSchema() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderWithNoSchema, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Validate order.json")
        awaitText("Choose a schema")
        awaitText("One of the open JSON Schema documents, or a file. order.json names none in a \$schema of its own.")
        onPantrixNode("Button", "schema_against").assert(hasContentDescription("Validate against a schema. Choose one")).performClick()
        awaitNode(hasText("order.schema.json"), "The menu of schemas")
        onPantrixNode("MenuItem", "schema_2", scope = "Menu_schemas").performClick()
        onPantrixNode("IconButton", "back").performClick()
        assertEquals(listOf(WorkspaceIntent.Schema(SchemaChoice(with = 2)), WorkspaceIntent.ShowPane(DocumentPane.Tree)), intents)
        assertNoFindings(findings)
    }

    @Test
    fun aFileChosenIsTheSchema() = runComposeUiTest {
        val opener = RecordingOpener(opened = 7)
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                CompositionLocalProvider(LocalSchemaOpener provides opener) {
                    WorkspaceScreen(state = OrderWithNoSchema, onIntent = {
                        intents += it
                    }, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        onPantrixNode("Button", "choose_schema_file").assert(hasText("Choose a file")).performClick()
        assertEquals(listOf("file"), opener.asked)
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Schema(SchemaChoice(with = 7))), intents)
    }

    @Test
    fun theSchemaADocumentNamesIsOpenedFromItsAddress() = runComposeUiTest {
        val address = "https://example.com/schemas/order.json"
        val named = previewState(
            "order.json",
            "{\n  \"\$schema\": \"$address\",\n  \"id\": \"ord_8842\"\n}\n",
            Format.Json,
            DocumentView(pane = DocumentPane.Schema)
        )
        val opener = RecordingOpener(opened = 9)
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                CompositionLocalProvider(LocalSchemaOpener provides opener) {
                    WorkspaceScreen(state = named, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitText("order.json names its schema in its \$schema: $address. Open it from the menu, or choose another.")
        onPantrixNode("Button", "schema_against").performClick()
        awaitNode(hasText("Open $address"), "The menu of schemas")
        onPantrixNode("MenuItem", "open_schema_address", scope = "Menu_schemas").performClick()
        assertEquals(listOf(address), opener.asked)
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Schema(SchemaChoice(with = 9))), intents)
    }

    @Test
    fun aSchemaThatCannotBeUsedSaysWhereItIsWrong() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                SchemaPane(
                    state = OrderAgainstBrokenSchema,
                    editor = CodeEditorState(PreviewBoardOrder),
                    layout = WorkspaceLayout.Wide,
                    onIntent = {}
                )
            }
        }

        awaitText("order.schema.json cannot be used as a schema")
        awaitText(
            "Its \$ref on line 40, #/\$defs/adress, points at nothing. Correct it in its own tab, and order.json is validated again."
        )
    }

    @Test
    fun anInvalidDocumentIsRepairedBeforeItIsValidated() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                SchemaPane(
                    state = previewState("settings.json", PreviewInvalid, Format.Json, DocumentView(pane = DocumentPane.Schema)),
                    editor = CodeEditorState(""),
                    layout = WorkspaceLayout.Wide,
                    onIntent = {}
                )
            }
        }

        awaitText("to validate the document against a schema.", substring = true)
    }

    @Test
    fun aCsvIsNotValidated() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                SchemaPane(
                    state = previewState("orders.csv", "id,total\nord_8842,129.90\n", Format.Csv, DocumentView(pane = DocumentPane.Schema)),
                    editor = CodeEditorState(""),
                    layout = WorkspaceLayout.Wide,
                    onIntent = {}
                )
            }
        }

        awaitText("No schema for CSV")
        awaitText("A JSON Schema validates JSON, YAML and TOML: the values of a CSV document are all text.")
    }
}
