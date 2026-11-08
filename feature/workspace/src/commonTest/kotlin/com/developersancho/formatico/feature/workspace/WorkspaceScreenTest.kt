package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.files.LocalFileSaver
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.TestFileSaver
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
@IgnoreInBrowser
class WorkspaceScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    private val order = """
        {
          "id": "ord_8842",
          "createdAt": "2026-10-28T14:32:00Z",
          "customer": {"name": "Elif"}
        }
    """.trimIndent()

    private val deployment = """
        metadata:
          labels: &labels
            app: formatico
        spec:
          selector:
            matchLabels: *labels
    """.trimIndent()

    /** A workspace's state as its view model makes it, for [text] read as [format]. */
    private fun stateOf(
        text: String,
        format: Format?,
        name: String = "order.json",
        view: DocumentView = DocumentView()
    ): WorkspaceState {
        val reading = readDocument(text, format)
        val root = (reading as? Reading.Read)?.root
        return WorkspaceState(
            document = OpenDocument(id = 1, name = name, text = text, format = format, view = view),
            reading = reading,
            stats = TextStats.of(text),
            expanded = view.expanded ?: defaultExpanded(root),
            trail = trail(root, view.selected),
            table = tableOf(root, view.selected)
        )
    }

    /** Waits for [text]: in a browser the screen's words are fetched after its first frame. */
    private fun ComposeUiTest.awaitText(text: String) = waitUntilExactlyOneExists(hasText(text), timeoutMillis = 10_000)

    @Test
    fun aWideWindowShowsTheTreeBesideTheEditorsPlace() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Text editing is on its way")
        onNodeWithText("Valid JSON").assertExists()
        onNodeWithText("5 lines · ", substring = true).assertExists()
        onNodeWithText("UTF-8 · LF · 2 spaces").assertExists()
        onPantrixNode("Node", "id", scope = "TreeView_document").assertExists()
        onPantrixNode("Button", "sort_keys").assertIsEnabled()
        onPantrixNode("Button", "format").performClick()
        onPantrixNode("Node", "customer.name", scope = "TreeView_document").performClick()

        assertEquals(
            listOf(WorkspaceIntent.Edit(DocumentEdit.Format), WorkspaceIntent.Select("$.customer.name")),
            intents
        )
        assertNoFindings(findings)
    }

    @Test
    fun aFormatNotReadYetSaysSoAndCannotBeChanged() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = stateOf("a = 1\n", Format.Toml, name = "Cargo.toml"),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("TOML isn’t read yet")
        onNodeWithText("TOML, not checked").assertExists()
        onPantrixNode("Button", "format").assertIsNotEnabled()
        onPantrixNode("Button", "minify").assertIsNotEnabled()
        onPantrixNode("TreeView", "document").assertDoesNotExist()
    }

    @Test
    fun aTabletShowsTheTrailToTheChosenValueBesideTheView() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = tablet, findings = findings, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json, view = DocumentView(selected = "$.customer.name")),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Valid JSON")
        onNodeWithText("$.customer.name").assertExists()
        onPantrixNode("Button", "sort_keys").assertDoesNotExist()
        onPantrixNode("Item", "customer", scope = "Breadcrumb_path").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Select("$.customer")), intents)
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneReadsTheChosenDateOutAndSwitchesToTheText() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json, view = DocumentView(selected = "$.createdAt")),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Copy the value")
        onPantrixNode("Tag", "kind").assertExists()
        onNodeWithText("Date").assertExists()
        onPantrixNode("Button", "show_in_text").assertIsNotEnabled()
        onPantrixNode("Segment", "text", scope = "SegmentButton_view").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.ShowPane(DocumentPane.Text)), intents)
        assertNoFindings(findings)
    }

    @Test
    fun aYamlDocumentShowsItsTreeAndCanBeChanged() = runComposeUiTest {
        val findings = PantrixFindings()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(
                        deployment,
                        Format.Yaml,
                        name = "deployment.yaml",
                        view = DocumentView(expanded = setOf("$", "$.spec", "$.spec.selector"))
                    ),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Valid YAML")
        onNodeWithText("matchLabels: *labels").assertExists()
        onPantrixNode("Button", "format").assertIsEnabled()
        onPantrixNode("Button", "sort_keys").assertIsEnabled()
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneGoesFromAnAliasToItsAnchor() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(
                        deployment,
                        Format.Yaml,
                        name = "deployment.yaml",
                        view = DocumentView(selected = "$.spec.selector.matchLabels")
                    ),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Points to the anchor &labels")
        onPantrixNode("Button", "go_to_anchor").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Select("$.metadata.labels")), intents)
    }

    @Test
    fun aPhoneShowsTheEditorsPlaceForTheText() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json, view = DocumentView(pane = DocumentPane.Text)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Text editing is on its way")
        onPantrixNode("TreeView", "document").assertDoesNotExist()
        onPantrixNode("Card", "selected_value").assertDoesNotExist()
    }

    private val orders = """
        {"orders": [
          {"id": "a", "total": 129.90, "coupon": "AUTUMN"},
          {"id": "b", "total": 4.50, "coupon": null},
          {"id": "c", "total": 1250.00}
        ]}
    """.trimIndent()

    @Test
    fun aWideTableOrdersFiltersSumsUpAndSaves() = runComposeUiTest {
        val findings = PantrixFindings()
        val saver = TestFileSaver()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                CompositionLocalProvider(LocalFileSaver provides saver) {
                    WorkspaceScreen(
                        state = stateOf(
                            orders,
                            Format.Json,
                            name = "orders.json",
                            view = DocumentView(selected = "$.orders[1].total", pane = DocumentPane.Table)
                        ),
                        onIntent = { intents += it },
                        commands = rememberDocumentCommands(Workbench())
                    )
                }
            }
        }

        awaitText("3 rows · 3 columns")
        onNodeWithText("3 numbers").assertExists()
        onNodeWithText("1,250.00").assertExists()
        onPantrixNode("Header", "total", scope = "DataTable_table").performClick()
        onNodeWithText("AUTUMN").performClick()
        onPantrixNode("TextField", "filter").performTextInput("autumn")
        awaitText("1 of 3 rows · 3 columns")
        onPantrixNode("IconButton", "save_csv").performClick()

        assertEquals(
            listOf(
                WorkspaceIntent.SortTable(TableSort("total", descending = false)),
                WorkspaceIntent.Select("$.orders[0].coupon")
            ),
            intents
        )
        val file = saver.files.single()
        assertEquals("orders.csv", file.name)
        assertEquals("id,total,coupon\na,129.90,AUTUMN\n", file.readBytes().decodeToString())
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneShowsACsvAsItsRecords() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(
                        "code,name,numeric\nAD,Andorra,020\nAL,Albania,008\n",
                        Format.Csv,
                        name = "countries.csv",
                        view = DocumentView(pane = DocumentPane.Table)
                    ),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("2 rows")
        onNodeWithText("numeric: 008").assertExists()
        onNodeWithText("Albania").performClick()
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Select("$[1]")), intents)
        onPantrixNode("Button", "minify").assertDoesNotExist()
    }

    @Test
    fun aTabletKeepsTheTablesToolsBesideItsViews() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Ios, window = tablet, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(
                        orders,
                        Format.Json,
                        name = "orders.json",
                        view = DocumentView(pane = DocumentPane.Table)
                    ),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("3 rows")
        onPantrixNode("Breadcrumb", "path").assertExists()
        onPantrixNode("TextField", "filter").assertWidthIsAtLeast(120.dp)
        onPantrixNode("IconButton", "save_csv").assertWidthIsAtLeast(32.dp)
    }

    @Test
    fun aDocumentWithNoArraySaysItHasNoTable() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(
                    state = stateOf("{\"a\": 1}", Format.Json, view = DocumentView(pane = DocumentPane.Table)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("No list to show as a table")
    }
}
