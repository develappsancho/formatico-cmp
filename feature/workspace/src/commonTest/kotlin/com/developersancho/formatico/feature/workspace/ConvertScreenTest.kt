package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.NullHandling
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.files.LocalFileSaver
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.TestClock
import com.pantrixui.testing.TestFileSaver
import com.pantrixui.testing.assertNoFindings
import com.pantrixui.testing.onPantrixNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class ConvertScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    @Test
    fun aWideWindowConvertsBesideTheTextAndSaysNothingIsLost() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderToYaml, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Nothing is lost", forgetting = findings)
        awaitText(
            "Strings YAML would read as dates are written in quotes, and 129.90 and 3 more numbers keep their spelling, " +
                "so the YAML converts back to this JSON as it is."
        )
        // The text stands beside it, under its format, its name and its lines.
        onPantrixNode("Tag", "source_format").assert(hasText("JSON"))
        awaitText("order.json · 21 lines")
        // The button's name says the format: its words are said by its description alone.
        onPantrixNode("Button", "convert_to").assert(hasContentDescription("Convert to YAML. Choose another format"))
        onPantrixNode("Segment", "2", scope = "SegmentButton_indent").assertIsSelected()
        onPantrixNode("Button", "save_converted").assert(hasText("Save as order.yaml"))
        onPantrixNode("IconButton", "copy_converted").assert(hasContentDescription("Copy the YAML"))
        awaitText(" B → YAML ", substring = true)
        onPantrixNode("Tag", "status").assert(hasText("Valid JSON"))

        onPantrixNode("Segment", "4", scope = "SegmentButton_indent").performClick()
        onPantrixNode("SwitchButton", "convert_sort_keys").performClick()
        onPantrixNode("Button", "convert_to").performClick()
        awaitNode(hasText("TOML"), "The menu of formats")
        onPantrixNode("MenuItem", "toml", scope = "Menu_targets").performClick()
        // Pressed again, the toolbar's Convert gives the tree back.
        onPantrixNode("Button", "convert").performClick()
        assertEquals(
            listOf(
                WorkspaceIntent.Convert(ConvertChoice(indent = 4)),
                WorkspaceIntent.Convert(ConvertChoice(sortKeys = true)),
                WorkspaceIntent.Convert(ConvertChoice(to = Format.Toml)),
                WorkspaceIntent.ShowPane(DocumentPane.Tree)
            ),
            intents
        )
        assertNoFindings(findings)
    }

    @Test
    fun theConvertedDocumentIsCopiedSavedAndOpenedInATabOfItsOwn() = runComposeUiTest {
        val clipboard = TestClipboard()
        val saver = TestFileSaver()
        val workbench = Workbench()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock, clipboard = clipboard) {
                CompositionLocalProvider(LocalFileSaver provides saver) {
                    WorkspaceScreen(state = OrderToYaml, onIntent = {}, commands = rememberDocumentCommands(workbench))
                }
            }
        }

        awaitText("Nothing is lost")
        val yaml = OrderToYaml.conversion.converted?.text.orEmpty()
        onPantrixNode("IconButton", "copy_converted").performClick()
        awaitUntil("The YAML is copied") { clipboard.entries.isNotEmpty() }
        assertEquals(yaml, clipboard.text())

        onPantrixNode("Button", "save_converted").performClick()
        val file = saver.files.single()
        assertEquals("order.yaml", file.name)
        assertEquals(yaml, file.readBytes().decodeToString())

        onPantrixNode("Button", "open_converted").performClick()
        val opened = workbench.state.value.selected
        assertEquals("order.yaml", opened?.name)
        assertEquals(Format.Yaml, opened?.format)
        assertEquals(yaml, opened?.text)
    }

    @Test
    fun aTabletConvertsInTheWholeRoomAndSaysWhatTomlLeavesOut() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = OrderToToml, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("TOML has no null")
        awaitText(
            "coupon and 1 more value are null in the JSON. They are left out, and comments mark where they were: " +
                "converting back will not bring them."
        )
        awaitText("From JSON to")
        // A tablet has no room for the text beside it.
        onPantrixNode("Tag", "source_format").assertDoesNotExist()
        onPantrixNode("Tag", "status").assert(hasText("2 values left out"))
        onPantrixNode("Button", "save_converted").assert(hasText("Save as order.toml"))
        onPantrixNode("Segment", "leave", scope = "SegmentButton_nulls").assertIsSelected()

        onPantrixNode("Segment", "empty", scope = "SegmentButton_nulls").performClick()
        assertEquals(WorkspaceIntent.Convert(ConvertChoice(to = Format.Toml, nulls = NullHandling.EmptyString)), intents.last())
        // The diagnostics are not asked about: the test draws in less than the tablet's window (PANTRIX_FINDINGS, "Test
        // temasına verilen pencere, testin çizdiği boy değil").
    }

    @Test
    fun aPhoneConvertsOnAScreenOfItsOwnThatItsBackLeaves() = runComposeUiTest {
        val findings = PantrixFindings()
        val clipboard = TestClipboard()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock, clipboard = clipboard) {
                WorkspaceScreen(state = OrderToYaml, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Convert order.json", forgetting = findings)
        awaitText("It converts back to this JSON as it is.")
        // A phone has no view's choice over it, nor the text beside it.
        onPantrixNode("Segment", "tree", scope = "SegmentButton_view").assertDoesNotExist()
        onPantrixNode("IconButton", "save_converted").assert(hasContentDescription("Save as order.yaml"))
        onPantrixNode("IconButton", "copy_converted").performClick()
        awaitUntil("The YAML is copied") { clipboard.entries.isNotEmpty() }

        onPantrixNode("IconButton", "back").performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Tree), intents.last())
        assertNoFindings(findings)
    }

    @Test
    fun aPhonesMenuOpensTheConversion() = runComposeUiTest {
        var converting = false
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                DocumentMenu(
                    items = DocumentMenuItems.FindSummarySortCompareClose,
                    size = ButtonSize.Large,
                    onConvert = { converting = true },
                    canSort = true,
                    onSortKeys = {},
                    onSave = {},
                    onClose = {}
                )
            }
        }

        onPantrixNode("IconButton", "more").performClick()
        // The test draws in less than the phone's window, and the menu's row stands past its foot: it is pressed as a
        // screen reader presses it (PANTRIX_FINDINGS, "Test temasına verilen pencere, testin çizdiği boy değil").
        onPantrixNode("MenuItem", "convert", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
        awaitUntil("The conversion is opened") { converting }
    }

    @Test
    fun anInvalidDocumentIsRepairedBeforeItIsConverted() = runComposeUiTest {
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                WorkspaceScreen(
                    state = previewState("settings.json", PreviewInvalid, Format.Json, DocumentView(pane = DocumentPane.Convert)),
                    onIntent = {},
                    commands = rememberDocumentCommands(Workbench())
                )
            }
        }

        awaitText("The JSON has", substring = true)
        awaitText("Repair them to convert the document.", substring = true)
        onPantrixNode("Button", "convert_to").assertDoesNotExist()
    }

    @Test
    fun theToolbarConvertsADocumentInAFormatAndNotPlainText() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        var plain by mutableStateOf(false)
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                val state = if (plain) previewState("notes.txt", "Notes\n", null) else OrderUnchosen
                WorkspaceScreen(state = state, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        onPantrixNode("Button", "convert").assertIsEnabled().performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Convert), intents.last())
        plain = true
        waitForIdle()
        onPantrixNode("Button", "convert").assertIsNotEnabled()
        assertEquals(1, intents.size)
    }
}
