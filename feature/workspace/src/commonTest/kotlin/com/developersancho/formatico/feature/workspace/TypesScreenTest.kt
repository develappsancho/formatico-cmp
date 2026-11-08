package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.types.TypeLanguage
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
class TypesScreenTest {
    private val desktop = PantrixWindow(DpSize(1440.dp, 900.dp))
    private val tablet = PantrixWindow(DpSize(834.dp, 1194.dp))
    private val phone = PantrixWindow(DpSize(390.dp, 844.dp))
    private val clock = TestClock(Instant.parse("2026-11-02T09:41:00Z"))

    @Test
    fun aWideWindowWritesTheTypesBesideTheTextAndSaysWhatCouldNotBeTold() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = desktop, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderInKotlin, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("6 types from order.json")
        awaitText(
            "Kotlin data classes for kotlinx.serialization. coupon is null in every value, so its type cannot be told: it is " +
                "written as JsonElement?. createdAt and eta stay strings."
        )
        // The text stands beside them, under its format, its name and its lines.
        onPantrixNode("Tag", "source_format").assert(hasText("JSON"))
        onPantrixNode("Button", "types_language").assert(hasContentDescription("Write the types in Kotlin. Choose another language"))
        onPantrixNode("Button", "save_types").assert(hasText("Save as Order.kt"))
        onPantrixNode("IconButton", "copy_types").assert(hasContentDescription("Copy the Kotlin"))
        onPantrixNode("TextField", "root_type").assert(hasContentDescription("The root type’s name"))
        awaitText(" B → Kotlin: 6 types, ", substring = true)
        onPantrixNode("Tag", "status").assert(hasText("Valid JSON"))

        onPantrixNode("Button", "types_language").performClick()
        awaitNode(hasText("TypeScript"), "The menu of languages")
        onPantrixNode("MenuItem", "typescript", scope = "Menu_languages").performClick()
        onPantrixNode("TextField", "root_type").performTextReplacement("Purchase")
        // Pressed again, the toolbar's Types gives the tree back.
        onPantrixNode("Action", "types", scope = "Toolbar_document").performClick()
        assertEquals(
            listOf(
                WorkspaceIntent.Types(TypesChoice(language = TypeLanguage.TypeScript)),
                WorkspaceIntent.Types(TypesChoice(root = "Purchase")),
                WorkspaceIntent.ShowPane(DocumentPane.Tree)
            ),
            intents
        )
        assertNoFindings(findings)
    }

    @Test
    fun theTypesAreCopiedAndSavedUnderTheRootsName() = runComposeUiTest {
        val clipboard = TestClipboard()
        val saver = TestFileSaver()
        setContent {
            PantrixTestTheme(window = desktop, clock = clock, clipboard = clipboard) {
                CompositionLocalProvider(LocalFileSaver provides saver) {
                    WorkspaceScreen(state = OrderInKotlin, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
                }
            }
        }

        awaitText("6 types from order.json")
        val kotlin = OrderInKotlin.types.written?.text.orEmpty()
        onPantrixNode("IconButton", "copy_types").performClick()
        awaitUntil("The types are copied") { clipboard.entries.isNotEmpty() }
        assertEquals(kotlin, clipboard.text())

        onPantrixNode("Button", "save_types").performClick()
        val file = saver.files.single()
        assertEquals("Order.kt", file.name)
        assertEquals(kotlin, file.readBytes().decodeToString())
    }

    @Test
    fun aTabletWritesTheTypesInTheWholeRoom() = runComposeUiTest {
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = tablet, clock = clock) {
                WorkspaceScreen(state = OrderInTypeScript, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("From JSON, in")
        awaitText("TypeScript interfaces. coupon is null in every value", substring = true)
        // A tablet has no room for the text beside them.
        onPantrixNode("Tag", "source_format").assertDoesNotExist()
        onPantrixNode("Button", "save_types").assert(hasText("Save as order.ts"))
        onPantrixNode("IconButton", "copy_types").assert(hasContentDescription("Copy the TypeScript"))
        awaitText(" B → TypeScript: 6 types, ", substring = true)
        // The diagnostics are not asked about: the test draws in less than the tablet's window (PANTRIX_FINDINGS, "Test
        // temasına verilen pencere, testin çizdiği boy değil").
    }

    @Test
    fun aPhoneWritesTheTypesOnAScreenOfItsOwnThatItsBackLeaves() = runComposeUiTest {
        val findings = PantrixFindings()
        val intents = mutableListOf<WorkspaceIntent>()
        setContent {
            PantrixTestTheme(window = phone, findings = findings, clock = clock) {
                WorkspaceScreen(state = OrderInSwift, onIntent = { intents += it }, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Types of order.json")
        // A phone leaves out what the types are written for: its notes alone.
        awaitText(
            "coupon is null in every value, so its type cannot be told: it is written as String?, marked as a guess.",
            substring = true
        )
        onPantrixNode("TextField", "root_type").assert(hasText("Root type", substring = true))
        onPantrixNode("Button", "save_types").assert(hasText("Save as Order.swift"))
        onPantrixNode("Segment", "tree", scope = "SegmentButton_view").assertDoesNotExist()

        onPantrixNode("IconButton", "back").performClick()
        assertEquals(WorkspaceIntent.ShowPane(DocumentPane.Tree), intents.last())
        assertNoFindings(findings)
    }

    @Test
    fun aPhonesMenuOpensTheTypes() = runComposeUiTest {
        var typing = false
        setContent {
            PantrixTestTheme(window = phone, clock = clock) {
                DocumentMenu(
                    size = ButtonSize.Large,
                    onTypes = { typing = true },
                    canSort = true,
                    onSortKeys = {},
                    onClose = {}
                )
            }
        }

        onPantrixNode("IconButton", "more").performClick()
        // The menu's row stands past the test's foot: it is pressed as a screen reader presses it (PANTRIX_FINDINGS,
        // "Test temasına verilen pencere, testin çizdiği boy değil").
        onPantrixNode("MenuItem", "types", scope = "Menu_document").performSemanticsAction(SemanticsActions.OnClick)
        awaitUntil("The types are opened") { typing }
    }

    @Test
    fun whatHasNoTypesSaysWhy() = runComposeUiTest {
        val state = typedState("totals.json", "[129.9, 50, 39.95]\n")
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                TypesPane(state = state, layout = WorkspaceLayout.Wide, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("No types to write")
        awaitText("The JSON holds no object: a single value, or a list of values, has no types to write.")
    }

    @Test
    fun anInvalidDocumentIsRepairedBeforeItsTypesAreWritten() = runComposeUiTest {
        val state = typedState("settings.json", PreviewInvalid)
        setContent {
            PantrixTestTheme(window = desktop, clock = clock) {
                TypesPane(state = state, layout = WorkspaceLayout.Wide, onIntent = {}, commands = rememberDocumentCommands(Workbench()))
            }
        }

        awaitText("Repair", substring = true)
        awaitText("to write its types.", substring = true)
    }

    private fun typedState(name: String, text: String) =
        previewState(name, text, Format.Json, DocumentView(pane = DocumentPane.Types))
}
