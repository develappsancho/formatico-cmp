package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.files.LocalFileSaver
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.TestFileSaver
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class WorkspaceScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    private val order =
        """
        {
          "id": "ord_8842",
          "createdAt": "2026-10-28T14:32:00Z",
          "customer": {"name": "Elif"}
        }
        """.trimIndent()

    private val deployment =
        """
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
    ): WorkspaceState = readState(OpenDocument(id = 1, name = name, text = text, format = format, view = view))

    @Test
    fun aWideWindowShowsTheTreeBesideTheText() = runComposeUiTest {
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

        awaitNode(hasPantrixTag("CodeEditor", "text"), "The editor", forgetting = findings)
        onPantrixNode("CodeEditor", "text").assert(hasText("\"id\": \"ord_8842\"", substring = true))
        awaitText("Valid JSON")
        awaitText("Ln 1, Col 1")
        // How long the document is, which shows its Summary.
        awaitNode(hasPantrixTag("Button", "size") and hasContentDescription("5 lines · ", substring = true), "The size")
        awaitText("UTF-8 · LF · 2 spaces")
        onPantrixNode("Node", "id", scope = "LazyTreeView_document").assertExists()
        // The text and the tree stand as the board splits them: there is no handle between them.
        onPantrixNode("TwoPaneHandle", "panes").assertDoesNotExist()
        onPantrixNode("Button", "sort_keys").assertIsEnabled()
        onPantrixNode("Button", "format").performClick()
        onPantrixNode("Node", "customer.name", scope = "LazyTreeView_document").performClick()

        assertEquals(
            listOf(WorkspaceIntent.Edit(DocumentEdit.Format), WorkspaceIntent.Select("$.customer.name")),
            intents
        )
        assertNoFindings(findings)
    }

    @Test
    fun plainTextSaysItHasNoTreeAndCannotBeChanged() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = stateOf("Things to check before the release.\n", format = null, name = "notes.txt"),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Plain text has no tree")
        awaitText("Plain text")
        onPantrixNode("Button", "format").assertIsNotEnabled()
        onPantrixNode("Button", "minify").assertIsNotEnabled()
        onPantrixNode("LazyTreeView", "document").assertDoesNotExist()
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

        awaitText("Valid JSON", forgetting = findings)
        awaitText("$.customer.name")
        onPantrixNode("Button", "sort_keys").assertDoesNotExist()
        onPantrixNode("Item", "customer", scope = "Breadcrumb_path").performClick()

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.Select("$.customer")), intents)
        assertNoFindings(findings)
    }

    @Test
    fun theChosenValuesPathIsCopiedInTheFormPicked() = runComposeUiTest {
        val findings = PantrixFindings()
        val clipboard = TestClipboard()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock, clipboard = clipboard) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json, view = DocumentView(selected = "$.customer.name")),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("Valid JSON", forgetting = findings)

        onPantrixNode("Button", "copy_path").assert(hasContentDescription("Copy the path of name as JSONPath, JSON Pointer or jq"))
        onPantrixNode("Button", "copy_path").performClick()
        awaitText("Copy the path of name as", forgetting = findings)
        // Each form shows the path it gives.
        onPantrixNode("MenuItem", "jsonpath", scope = "Menu_path_forms").assert(hasText("$.customer.name"))
        onPantrixNode("MenuItem", "jq", scope = "Menu_path_forms").assert(hasText(".customer.name"))
        onPantrixNode("MenuItem", "json_pointer", scope = "Menu_path_forms").performClick()

        awaitUntil("The path is copied") { clipboard.entries.isNotEmpty() }
        assertEquals("/customer/name", clipboard.text())
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneCopiesThePathFromTheChosenValuesCard() = runComposeUiTest {
        val clipboard = TestClipboard()
        setContent {
            PantrixTestTheme(window = phone, clock = clock, clipboard = clipboard) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json, view = DocumentView(pane = DocumentPane.Tree, selected = "$.createdAt")),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("Valid JSON")

        onPantrixNode("IconButton", "copy_path").performClick()
        onPantrixNode("MenuItem", "jsonpath", scope = "Menu_path_forms").performClick()

        awaitUntil("The path is copied") { clipboard.entries.isNotEmpty() }
        assertEquals("$.createdAt", clipboard.text())
    }

    @Test
    fun theTreeOpensToALevelAndIsSearched() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        val customer = "{\"customer\": {\"name\": \"Elif Yılmaz\", \"email\": \"elif@example.com\"}, \"site\": \"elif.dev\"}"
        var view by mutableStateOf(DocumentView())
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(customer, Format.Json, view = view),
                    onIntent = { intent ->
                        intents += intent
                        if (intent is WorkspaceIntent.Search) view = view.copy(searching = intent.open)
                    },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("Valid JSON", forgetting = findings)

        onPantrixNode("Button", "level_1", scope = "ButtonGroup_levels").performClick()
        onPantrixNode("Button", "level_all", scope = "ButtonGroup_levels").performClick()
        assertEquals(
            listOf<WorkspaceIntent>(WorkspaceIntent.Expand(setOf("$")), WorkspaceIntent.Expand(setOf("$", "$.customer"))),
            intents
        )
        intents.clear()

        // A search goes to its first match, and on from it, round from the last to the first.
        onPantrixNode("IconButton", "find").performClick()
        // In a browser the bar's words are read as it first shows.
        awaitNode(
            hasPantrixTag("IconButton", "close_search") and hasContentDescription("Close the search"),
            "The bar",
            forgetting = findings
        )
        onPantrixNode("TextField", "find").performTextInput("elif")
        awaitText("1 of 3")
        onPantrixNode("IconButton", "next_match").performClick()
        onPantrixNode("IconButton", "next_match").performClick()
        onPantrixNode("IconButton", "next_match").performClick()
        awaitText("1 of 3")
        onPantrixNode("IconButton", "previous_match").performClick()
        assertEquals(
            listOf("$.customer.name", "$.customer.email", "$.site", "$.customer.name", "$.site"),
            intents.filterIsInstance<WorkspaceIntent.Select>().map { it.key }
        )

        onPantrixNode("IconButton", "close_search").performClick()
        onPantrixNode("TextField", "find").assertDoesNotExist()
        assertNoFindings(findings)
    }

    @Test
    fun theSizeOpensTheSummaryWhoseRowsAndNotesLeadToTheTreeAndTheTable() = runComposeUiTest {
        val findings = PantrixFindings()
        val clipboard = TestClipboard()
        val intents = mutableListOf<WorkspaceIntent>()
        val text = "{\"createdAt\": \"2026-10-28T14:32:00Z\", \"total\": 129.90, \"items\": [{\"sku\": \"a\"}, {\"sku\": \"b\"}]}"
        var view by mutableStateOf(DocumentView())
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock, clipboard = clipboard) {
                WorkspaceScreen(
                    state = stateOf(text, Format.Json, view = view),
                    onIntent = { intent ->
                        intents += intent
                        if (intent is WorkspaceIntent.ShowPane) view = view.copy(pane = intent.pane)
                    },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("Valid JSON", forgetting = findings)

        onPantrixNode("Button", "size").performClick()
        // In a browser the Summary's words are read as it first shows.
        awaitNode(
            hasPantrixTag("IconButton", "copy_summary") and hasContentDescription("Copy the summary as text"),
            "Copy",
            forgetting = findings
        )
        awaitNode(hasPantrixTag("ListItem", "dates") and hasText("1 date"), "The dates", forgetting = findings)
        awaitNode(
            hasPantrixTag("Button", "size") and hasContentDescription("The summary is shown", substring = true),
            "The size",
            forgetting = findings
        )
        awaitText("Found in the values")
        onPantrixNode("Segment", "summary", scope = "SegmentButton_view").assertExists()
        awaitText("129.90 keeps its spelling here. A tool that reads it as a number writes 129.9.")
        awaitText("$.items holds 2 objects with the same 1 key, so it reads as a table.")

        onPantrixNode("IconButton", "copy_summary").performClick()
        awaitUntil("The summary is copied") { clipboard.entries.isNotEmpty() }
        val copied = clipboard.text().orEmpty()
        assertTrue("Values: 8 (In 3 objects and 1 array)" in copied, copied)
        assertTrue("1 date: $.createdAt" in copied, copied)

        intents.clear()
        onPantrixNode("ListItem", "dates").performScrollTo().performClick()
        assertEquals(listOf(WorkspaceIntent.Select("$.createdAt"), WorkspaceIntent.ShowPane(DocumentPane.Tree)), intents)

        intents.clear()
        view = view.copy(pane = DocumentPane.Summary)
        awaitNode(hasPantrixTag("Button", "show_table"), "Show as a table")
        onPantrixNode("Button", "show_table").performScrollTo().performClick()
        assertEquals(listOf(WorkspaceIntent.Select("$.items"), WorkspaceIntent.ShowPane(DocumentPane.Table)), intents)

        // The size, pressed while the Summary is shown, gives the tree back.
        view = view.copy(pane = DocumentPane.Summary)
        awaitText("Found in the values")
        intents.clear()
        onPantrixNode("Button", "size").performClick()
        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.ShowPane(DocumentPane.Tree)), intents)
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneSearchesTheTreeInTheBarItsMoreMenuOpens() = runComposeUiTest {
        val workbench = Workbench()
        val customer = "{\"customer\": {\"name\": \"Elif Yılmaz\", \"email\": \"elif@example.com\"}, \"site\": \"elif.dev\"}"
        val id = workbench.open("customer.json", customer, Format.Json)
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                val open by workbench.state.collectAsState()
                Column {
                    DocumentMoreMenu(documentId = id, workbench = workbench)
                    WorkspaceScreen(
                        state = readState(open.documents.single()),
                        onIntent = { intent ->
                            when (intent) {
                                is WorkspaceIntent.Search -> workbench.changeView(id) { it.copy(searching = intent.open) }
                                is WorkspaceIntent.Select -> workbench.changeView(id) { it.copy(selected = intent.key) }
                                else -> {}
                            }
                        },
                        commands = rememberDocumentCommands(workbench)
                    )
                }
            }
        }
        awaitText("Valid JSON")

        // The phone's "more" menu opens the search on the tree.
        onPantrixNode("IconButton", "more").performClick()
        onPantrixNode("MenuItem", "find", scope = "Menu_document").performClick()
        onPantrixNode("TextField", "find").performTextInput("elif")
        awaitText("1 of 3")
        // The chosen value's card says which match it is.
        awaitText("match 1 of 3", substring = true)
        assertEquals("$.customer.name", workbench.state.value.documents.single().view.selected)

        onPantrixNode("IconButton", "close_search").performClick()
        onPantrixNode("TextField", "find").assertDoesNotExist()

        // And the Summary.
        onPantrixNode("IconButton", "more").performClick()
        onPantrixNode("MenuItem", "summary", scope = "Menu_document").performClick()
        awaitText("Size")
        assertEquals(DocumentPane.Summary, workbench.state.value.documents.single().view.pane)
    }

    @Test
    fun aTabletsHandleMakesTheTextTallerAndShorter() = runComposeUiTest {
        val findings = PantrixFindings()
        setContent {
            PantrixTestTheme(window = tablet, findings = findings, clock = clock) {
                WorkspaceScreen(state = stateOf(order, Format.Json), onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }
        awaitText("Valid JSON", forgetting = findings)
        val handle = onPantrixNode("TwoPaneHandle", "panes")
        val top = onPantrixNode("TwoPane", "panes").getBoundsInRoot().top
        val split = { handle.getBoundsInRoot().let { (it.top + it.bottom) / 2 } - top }
        // The handle stands in the middle of the strip under the text's 480 and its line.
        val start = split()
        assertTrue(abs((start - 486.5.dp).value) <= 1f, "The handle is at $start")

        // An arrow makes the text a step taller, and Home as short as it may be.
        handle.requestFocus()
        handle.performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(start + 16.dp, split())
        handle.performKeyInput { pressKey(Key.MoveHome) }
        val shortest = split()
        assertTrue(shortest < start, "The handle is at $shortest")

        // A drag makes it taller again.
        handle.performTouchInput { swipeDown(startY = centerY, endY = centerY + 200f) }
        assertTrue(split() > shortest, "The handle is at ${split()}")
        onPantrixNode("LazyTreeView", "document").assertExists()
        assertNoFindings(findings)
    }

    @Test
    fun onAnAndroidTabletATapOnTheViewsTopEdgeUnderTheHandleIsTheirs() = tapUnderTheHandle(PlatformType.Android)

    @Test
    fun onAnIpadATapOnTheViewsTopEdgeUnderTheHandleIsTheirs() = tapUnderTheHandle(PlatformType.Ios)

    /** A tap on the top edge of the tree's views, where the handle's strip, wider than the gap, reaches over them. */
    private fun tapUnderTheHandle(platform: PlatformType) = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(platform = platform, window = tablet, clock = clock) {
                WorkspaceScreen(state = stateOf(order, Format.Json), onIntent = {
                    intents += it
                }, commands = rememberDocumentCommands(Workbench()))
            }
        }
        awaitText("Valid JSON")
        val table = onPantrixNode("Segment", "table", scope = "SegmentButton_view")
        val edge = table.getBoundsInRoot().top + 2.dp
        assertTrue(onPantrixNode("TwoPaneHandle", "panes").getBoundsInRoot().bottom > edge, "The strip reaches over the views on $platform")

        table.performTouchInput { click(Offset(centerX, 2.dp.toPx())) }

        assertEquals(listOf<WorkspaceIntent>(WorkspaceIntent.ShowPane(DocumentPane.Table)), intents, "On $platform")
    }

    @Test
    fun theChosenValueIsBroughtIntoViewInALongTree() = runComposeUiTest {
        val findings = PantrixFindings()
        val long = (0 until 80).joinToString(",\n", prefix = "{\n", postfix = "\n}") { "  \"key$it\": $it" }
        var view by mutableStateOf(DocumentView(selected = "$.key0"))
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = stateOf(long, Format.Json, view = view), onIntent = {
                }, commands = rememberDocumentCommands(Workbench()))
            }
        }
        awaitText("Valid JSON", forgetting = findings)
        onPantrixNode("Node", "key0", scope = "LazyTreeView_document").assertIsDisplayed()

        // Chosen in the text or by a search, a value far down the tree is scrolled to.
        view = view.copy(selected = "$.key79")
        awaitNode(hasPantrixTag("Node", "key79", scope = "LazyTreeView_document"), "The last row")
        onPantrixNode("Node", "key79", scope = "LazyTreeView_document").assertIsDisplayed()
        assertNoFindings(findings)
    }

    @Test
    fun aDocumentOpenedWithAChosenValueInViewShowsItsTreeFromTheTop() = runComposeUiTest {
        val findings = PantrixFindings()
        val long = (0 until 30).joinToString(",\n", prefix = "{\n", postfix = "\n}") { "  \"key$it\": $it" }
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(long, Format.Json, view = DocumentView(selected = "$.key3")),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        // The chosen value shows without scrolling, so the tree stays at its root, as the board draws it.
        awaitNode(hasPantrixTag("Node", "key3", scope = "LazyTreeView_document"), "The chosen row", forgetting = findings)
        onPantrixNode("Node", "key3", scope = "LazyTreeView_document").assertIsDisplayed()
        onPantrixNode("Node", "root", scope = "LazyTreeView_document").assertIsDisplayed()
        assertNoFindings(findings)
    }

    @Test
    fun theTreesRowsEndInWhatTheirStringsLookLike() = runComposeUiTest {
        val findings = PantrixFindings()
        val looks = "{\"createdAt\": \"2026-10-28T14:32:00Z\", \"accent\": \"#0F766E\", " +
            "\"avatar\": \"https://images.example.com/u/elif.png\", \"website\": \"https://elif.dev\"}"
        var window by mutableStateOf(desktop)
        setContent {
            PantrixTestTheme(window = window, findings = findings, clock = clock) {
                WorkspaceScreen(state = stateOf(looks, Format.Json), onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitNode(hasContentDescription("Colour #0F766E"), "The colour's swatch", forgetting = findings)
        awaitNode(hasContentDescription("Picture at this address"), "The picture's place")
        awaitText("14:32 UTC", substring = true)
        // A link ends its row in the button that opens it (TreeLinkTest).
        onPantrixNode("Action", "website", scope = "LazyTreeView_document", useUnmergedTree = true).assertExists()
        // A phone, with less room, shows the date by its icon, which says it.
        window = phone
        awaitNode(hasContentDescription("14:32 UTC", substring = true), "The date's icon", forgetting = findings)
        assertNoFindings(findings)
    }

    @Test
    fun aPhoneReadsTheChosenDateOutAndShowsItInTheText() = runComposeUiTest {
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

        awaitText("Copy the value", forgetting = findings)
        onPantrixNode("Tag", "kind").assertExists()
        awaitText("Date")
        onPantrixNode("Button", "show_in_text").performClick()
        onPantrixNode("Segment", "text", scope = "SegmentButton_view").performClick()

        assertEquals(List<WorkspaceIntent>(2) { WorkspaceIntent.ShowPane(DocumentPane.Text) }, intents)
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

        awaitText("Valid YAML", forgetting = findings)
        awaitText("matchLabels: *labels")
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
    fun aPhoneShowsTheTextInTheEditor() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(order, Format.Json, view = DocumentView(pane = DocumentPane.Text)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitNode(hasPantrixTag("CodeEditor", "text"), "The editor")
        onPantrixNode("LazyTreeView", "document").assertDoesNotExist()
        onPantrixNode("Card", "selected_value").assertDoesNotExist()
    }

    private val orders =
        """
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

        awaitText("3 rows · 3 columns", forgetting = findings)
        awaitText("3 numbers")
        awaitText("1,250.00")
        onPantrixNode("Header", "total", scope = "DataTable_table").performClick()
        onNodeWithText("AUTUMN").performClick()
        onPantrixNode("TextField", "filter").performTextInput("autumn")
        awaitText("1 of 3 rows · 3 columns")
        onPantrixNode("IconButton", "table_out").performClick()
        // In a browser the menu's words are read as it first shows.
        awaitNode(hasText("Copy as a Markdown table"), "The menu", forgetting = findings)
        onPantrixNode("MenuItem", "save_csv", scope = "Menu_table_out").performClick()

        assertEquals(
            listOf(
                WorkspaceIntent.SortTable(TableSort("total", descending = false)),
                WorkspaceIntent.Select("$.orders[0].coupon"),
                WorkspaceIntent.FilterTable("autumn")
            ),
            intents
        )
        val file = saver.files.single()
        assertEquals("orders.csv", file.name)
        assertEquals("id,total,coupon\na,129.90,AUTUMN\n", file.readBytes().decodeToString())
        assertNoFindings(findings)
    }

    @Test
    fun theTableIsCopiedAsCsvOrAsMarkdown() = runComposeUiTest {
        val clipboard = TestClipboard()
        val orders = "{\"orders\": [{\"id\": \"a\", \"note\": \"x | y\"}, {\"id\": \"b\", \"note\": null}]}"
        setContent {
            PantrixTestTheme(window = desktop, clock = clock, clipboard = clipboard) {
                WorkspaceScreen(
                    state = stateOf(orders, Format.Json, view = DocumentView(pane = DocumentPane.Table)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("2 rows · 2 columns")

        onPantrixNode("IconButton", "table_out").performClick()
        onPantrixNode("MenuItem", "copy_markdown", scope = "Menu_table_out").performClick()
        awaitUntil("The table is copied") { clipboard.entries.isNotEmpty() }
        assertEquals("| id | note |\n| --- | --- |\n| a | x \\| y |\n| b |  |\n", clipboard.text())

        onPantrixNode("IconButton", "table_out").performClick()
        onPantrixNode("MenuItem", "copy_csv", scope = "Menu_table_out").performClick()
        awaitUntil("The table is copied again") { clipboard.entries.size == 2 }
        assertEquals("id,note\na,x | y\nb,\n", clipboard.text())
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
        awaitText("numeric: 008")
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
        onPantrixNode("IconButton", "table_out").assertWidthIsAtLeast(32.dp)
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

    @Test
    fun aDocumentBeingReadDoesNotSayItHasNoTable() = runComposeUiTest {
        val read = stateOf("[1, 2]", Format.Json, view = DocumentView(pane = DocumentPane.Table))
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = read.copy(reading = Reading.Pending, table = null), onIntent = {
                }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Reading", substring = true)
        onNodeWithText("No list to show as a table").assertDoesNotExist()
    }

    @Test
    fun aDocumentWithMistakesSaysHowManyAndCannotBeFormatted() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = stateOf("{\n  \"id\": 1\n  \"total\": 129.\n}\n", Format.Json),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("2 problems", substring = true)
        onPantrixNode("Button", "format").assertIsNotEnabled()
        onPantrixNode("Button", "minify").assertIsNotEnabled()
    }

    @Test
    fun aDocumentBeingReadSaysSo() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = WorkspaceState(document = OpenDocument(id = 1, name = "order.json", text = order, format = Format.Json)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("Reading…")
        onPantrixNode("Button", "format").assertIsNotEnabled()
    }

    @Test
    fun anEmptyDocumentSaysThereIsNothingToRead() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(state = stateOf("", null, name = "Untitled"), onIntent = {
                }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("The document is empty")
    }

    @Test
    fun aColumnOfNumbersLinesThemUpAtItsEndAsItsHeader() = runComposeUiTest {
        val items = "[{\"name\": \"Keyboard\", \"price\": 89.90}, {\"name\": \"Mouse\", \"price\": 5}]\n"
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(items, Format.Json, view = DocumentView(pane = DocumentPane.Table)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("89.90")

        // A cell is pressed as a whole, and its text is merged into it: the text's own node is the unmerged one.
        val long = onNode(hasText("89.90"), useUnmergedTree = true).getBoundsInRoot()
        val short = onNode(hasText("5"), useUnmergedTree = true).getBoundsInRoot()
        val name = onNode(hasText("Keyboard"), useUnmergedTree = true).getBoundsInRoot()
        assertEquals(long.right, short.right, "the numbers' ends")
        assertTrue(short.left > long.left, "a shorter number starts further in")
        assertTrue(name.left < long.left, "text stays at its column's start")
    }

    @Test
    fun theColumnsChooserOpensAndHidesAColumn() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        val items = "[{\"name\": \"Keyboard\", \"price\": 89.90}, {\"name\": \"Mouse\", \"price\": 5}]\n"
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = stateOf(items, Format.Json, view = DocumentView(pane = DocumentPane.Table)),
                    onIntent = { intents += it },
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }
        awaitText("89.90")

        onPantrixNode("Button", "columns").performClick()
        onPantrixNode("Checkbox", "column_1").performClick()

        assertEquals(WorkspaceIntent.HideColumns(setOf("price")), intents.last())
    }
}
