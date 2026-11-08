package com.developersancho.formatico

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.testing.awaitNode
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.diagnostics.PantrixCheck
import com.pantrixui.foundation.diagnostics.PantrixFinding
import com.pantrixui.foundation.diagnostics.PantrixFindings
import com.pantrixui.foundation.files.FilePickRequest
import com.pantrixui.foundation.files.FilePicker
import com.pantrixui.foundation.files.PantrixFile
import com.pantrixui.testing.TestClipboard
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import kotlin.test.assertTrue

/**
 * A board of the canvas, as the app is brought to it: Formatico's eleven screens, the YAML document's workspace, and
 * Start's two answers to what is pasted (a text of no format told, a link cut short). [fileName] is its screenshots'.
 */
internal enum class AppBoard(val fileName: String) {
    Start("Start"),
    Workspace("Workspace"),
    Yaml("Yaml"),
    Table("Table"),
    Invalid("Invalid"),
    Query("Query"),
    Convert("Convert"),
    Types("Types"),
    Compare("Compare"),
    Summary("Summary"),
    Palette("Palette"),
    Settings("Settings"),
    Ask("Ask"),
    Share("Share"),
    BrokenLink("BrokenLink");

    /** Whether the board shows a window of its own over the app, which is the last root drawn. */
    val overlay: Boolean get() = this == Palette || this == Ask || this == Share

    /** The clipboard the board's app is given: a link cut short where the board pastes one, else a text of no format. */
    fun clipboard(): TestClipboard = TestClipboard().apply { putText(if (this@AppBoard == BrokenLink) BROKEN_LINK else UNTOLD) }
}

/** The words a tour waits for, in the language the app is drawn in. */
internal class BoardWords(
    val start: String,
    val reading: String,
    val nothingLost: String,
    val copyTheDocument: String,
    val indentation: String,
    val whatFormat: String,
    val characters: String,
    val linkBroken: String,
    val secondProblemAt: String,
    val values: String,
    val types: String
) {
    companion object {
        val English = BoardWords(
            start = "Paste anything",
            reading = "Reading…",
            nothingLost = "Nothing is lost",
            copyTheDocument = "Copy the document",
            indentation = "Indentation",
            whatFormat = "What format is this?",
            characters = " characters",
            linkBroken = "This link holds no document",
            secondProblemAt = "Line 10, column 5",
            values = "Values",
            types = "6 types from order.json"
        )

        val Turkish = BoardWords(
            start = "Ne varsa yapıştır",
            reading = "Okunuyor…",
            nothingLost = "Hiçbir şey kaybolmuyor",
            copyTheDocument = "Belgeyi kopyala",
            indentation = "Girinti",
            whatFormat = "Bu hangi biçim?",
            characters = " karakter",
            linkBroken = "Bu bağlantıda Formatico’nun okuyabileceği bir belge yok",
            secondProblemAt = "Satır 10, sütun 5",
            values = "Değerler",
            types = "order.json belgesinden 6 tip"
        )
    }
}

/** The picker a tour opens its boards' documents with: each pick is answered with the file put next. */
internal class BoardFiles : FilePicker {
    private var next: PantrixFile? = null

    /** What the next pick answers with. */
    fun putNext(name: String, text: String) {
        next = PantrixFile(name, text.encodeToByteArray())
    }

    override fun pick(request: FilePickRequest, onPicked: (List<PantrixFile>) -> Unit) {
        next?.let { onPicked(listOf(it)) }
        next = null
    }
}

/**
 * Brings the app, drawn on [window] with [files] as its picker, from Start to what [board] shows, waiting for each step
 * in [words]. Every control is pressed as a screen reader presses it: a pointer left where it was would hover over
 * what stands there once the screen has changed, and a test that draws the board larger than its surface reaches past
 * its foot.
 */
@OptIn(ExperimentalTestApi::class)
internal suspend fun ComposeUiTest.tour(
    board: AppBoard,
    window: Board,
    words: BoardWords,
    files: BoardFiles
) = Trip(this, window, words, files).to(board)

/**
 * Asserts that the theme's diagnostics found nothing on the board, as Pantrix's `assertNoFindings` does, once the
 * clock has passed what they wait for: past the half second of a squeezed target and the second and a half of a
 * repeated tag. What Pantrix's own components find, and Formatico cannot give them, is left out while it is a finding.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.assertNoFindingsOnBoard(findings: PantrixFindings) {
    mainClock.advanceTimeBy(FINDINGS_WAIT)
    waitForIdle()
    val found = findings.all.filterNot(::isPantrixs)
    assertTrue(
        found.isEmpty(),
        "The diagnostics found ${found.size}:\n" +
            found.joinToString("\n") { "- ${it.check} ${it.component} (${it.testTag}): ${it.message}" }
    )
}

/**
 * The palette's field has no name, and `PantrixCommandPalette` takes none to give it, only its placeholder
 * (PANTRIX_FINDINGS: "Komut paletinin arama alanının adı yok").
 */
private fun isPantrixs(finding: PantrixFinding): Boolean =
    finding.check == PantrixCheck.MissingName && finding.testTag == "CommandPalette_commands.TextField_query"

/** One way from Start to a board: the test, the board's window, the words waited for and the picker. */
@OptIn(ExperimentalTestApi::class)
private class Trip(
    private val test: ComposeUiTest,
    private val window: Board,
    private val words: BoardWords,
    private val files: BoardFiles
) {
    private val phone = window == Board.Phone

    suspend fun to(board: AppBoard) {
        test.awaitText(words.start)
        // A tablet and a phone are used by touch: a touch in the bar's empty corner tells the screen so, as a device's
        // first touch does, and no control shows the focus ring a keyboard would.
        if (window != Board.Desktop) test.onRoot().performTouchInput { click(Offset(1f, 1f)) }
        when (board) {
            AppBoard.Start -> Unit

            AppBoard.Workspace -> openSample("order")

            AppBoard.Yaml -> openSample("deployment")

            AppBoard.Table -> {
                openSample("countries")
                // The table is laid out away from the main thread once the document is read: its first row is waited for.
                awaitShown("Afghanistan")
            }

            AppBoard.Invalid -> invalid()

            AppBoard.Query -> query()

            AppBoard.Convert -> {
                openSample("order")
                // A phone converts from its bar's "more" menu, a wider window from its toolbar.
                if (phone) documentMenu("convert") else press("Button", "convert")
                awaitShown(words.nothingLost)
            }

            AppBoard.Types -> types()

            AppBoard.Compare -> compare()

            AppBoard.Summary -> {
                openSample("order")
                // The status bar's size opens it; a phone, which has no status bar, opens it from its "more" menu.
                if (phone) documentMenu("summary") else press("Button", "size")
                awaitShown(words.values)
            }

            AppBoard.Palette -> {
                openSample("order")
                press(if (window == Board.Desktop) "Button" else "IconButton", "launcher")
                awaitShown(words.copyTheDocument)
            }

            AppBoard.Settings -> {
                openSample("order")
                // A phone's bar over a document has no settings: its Start has.
                if (phone) newTab()
                press("IconButton", "settings")
                awaitShown(words.indentation)
            }

            AppBoard.Ask -> {
                press("Button", "paste")
                awaitShown(words.whatFormat)
            }

            AppBoard.Share -> {
                openSample("order")
                documentMenu("share")
                // The link is made a moment after the share opens: its length is said under it then.
                awaitShown(words.characters)
            }

            AppBoard.BrokenLink -> {
                press("Button", "paste")
                awaitShown(words.linkBroken)
            }
        }
    }

    /** The settings, beside the order, at their second problem: the line the board's caret is on. */
    private suspend fun invalid() {
        openFile("settings.json", INVALID_SETTINGS)
        newTab()
        openSample("order")
        chooseTab("settings.json")
        awaitRead()
        // The list of problems goes to one, and a phone shows the text then, its strip saying which problem it is.
        // The row's own press goes to its problem; its button, keyed after it, repairs it.
        press("Segment", "problems", scope = "SegmentButton_view")
        val second = hasClickAction() and hasText(words.secondProblemAt, substring = true)
        test.awaitNode(second, "the second problem")
        test.onNode(second).performSemanticsAction(SemanticsActions.OnClick)
    }

    /**
     * The order, beside the feed, queried for its items' names after three other queries. The recent queries are the
     * ones used, the newest first, and the one asked is not among them. A query reaches the document a frame after it
     * is typed: its first match is waited for before it is used.
     */
    private suspend fun query() {
        openSample("order")
        newTab()
        openFile("feed.xml", FEED)
        chooseTab("order.json")
        awaitRead()
        if (phone) documentMenu("query") else press("Button", "query")
        val field = test.onPantrixNode("TextField", "query")
        listOf(
            "$.items[?(@.qty > 1)]" to "$.items[0]",
            "$.customer.email" to "$.customer.email",
            "$..price" to "$.items[0].price",
            "$.items[*].name" to "$.items[0].name"
        ).forEachIndexed { index, (query, first) ->
            field.performTextClearance()
            field.performTextInput(query)
            test.awaitUntil("$query was asked") {
                test.onAllNodes(hasKeyEnding("match0") and hasText(first, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
            if (index < USED_QUERIES) field.performImeAction()
        }
        test.awaitUntil("the recent queries were offered") {
            test.onAllNodes(hasPantrixTag("Chip", "recent${USED_QUERIES - 1}")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The two orders, compared by meaning, the first difference shown. */
    private suspend fun compare() {
        openFile("order.json", ORDER_A)
        newTab()
        openFile("order-v2.json", ORDER_B)
        chooseTab("order.json")
        awaitRead()
        // Only a wide window's toolbar has Compare; a narrower one has it in the document's "more" menu.
        if (window == Board.Desktop) press("Button", "compare") else documentMenu("compare")
        // A wide window heads the differences with their count; each width lists them, the first chosen.
        test.awaitUntil("the differences were listed") {
            test.onAllNodes(hasKeyEnding("difference0")).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Opens Start's sample [key], which a phone lists once its two first are passed, and waits until it is read. */
    private suspend fun openSample(key: String) {
        // The link's words take the click, inside its tag: a press would leave a pointer over what comes there next.
        if (phone && test.onAllNodes(hasPantrixTag("Link", "all_samples")).fetchSemanticsNodes().isNotEmpty()) {
            test.onNode(hasClickAction() and hasAnyAncestor(hasPantrixTag("Link", "all_samples")), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
        }
        test.onPantrixNode("ListItem", "sample_$key").performScrollTo()
        press("ListItem", "sample_$key")
        awaitRead()
    }

    /**
     * Opens the file [name], which holds [text], as Start's "Open a file" does on a phone and its drop zone, pressed,
     * on a wider window, and waits until it is read.
     */
    private suspend fun openFile(name: String, text: String) {
        files.putNext(name, text)
        if (phone) press("Button", "open_file") else press("FileDrop", "file")
        awaitRead()
    }

    /** Goes to a new tab, Start: from a wider window's tabs, or a phone's list of its documents. */
    private suspend fun newTab() {
        if (phone) {
            press("Button", "switch_documents")
            press("MenuItem", "new", scope = "Menu_documents")
        } else {
            press("IconButton", "new_tab")
        }
        test.awaitText(words.start)
    }

    /** Chooses the document [name]'s tab, or a phone's row of it in its list of documents. */
    private suspend fun chooseTab(name: String) {
        if (phone) press("Button", "switch_documents")
        val component = if (phone) "MenuItem_document_" else "Tab_document_"
        val tab = SemanticsMatcher("the $component of $name") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.contains(component) == true
        } and hasText(name)
        test.awaitNode(tab, "the tab of $name")
        test.onNode(tab).performSemanticsAction(SemanticsActions.OnClick)
    }

    /**
     * The order's types, as the Types boards draw them: in Kotlin on a desktop, from the toolbar; in TypeScript on a
     * tablet, from the toolbar; in Swift on a phone, from its bar's "more" menu. Each is waited for by its file's name.
     */
    private suspend fun types() {
        openSample("order")
        if (phone) documentMenu("types") else press("Button", "types")
        awaitShown(words.types)
        val (language, file) = when (window) {
            Board.Desktop -> null to "Order.kt"
            Board.Tablet -> "typescript" to "order.ts"
            Board.Phone -> "swift" to "Order.swift"
        }
        if (language != null) {
            press("Button", "types_language")
            // The row is touched, as a tablet's and a phone's reader touches it: pressed as a screen reader presses it,
            // the menu gives the focus back to its button with a keyboard's ring.
            test.awaitNode(hasPantrixTag("MenuItem", language, "Menu_languages"), "MenuItem $language")
            test.onPantrixNode("MenuItem", language, scope = "Menu_languages").performTouchInput { click() }
        }
        awaitShown(file)
    }

    /** Presses the document's "more" menu's row [key]: a phone's in its bar, a wider window's in its toolbar. */
    private suspend fun documentMenu(key: String) {
        press("IconButton", "more")
        press("MenuItem", key, scope = "Menu_document")
    }

    /**
     * Waits until a document is shown in Start's place and read: the text shows at once, its tree and its status once
     * it is read. Start says nothing is being read either, so its going is waited for first.
     */
    private suspend fun awaitRead() {
        test.awaitUntil("the document was shown") { test.onAllNodes(hasText(words.start)).fetchSemanticsNodes().isEmpty() }
        test.awaitUntil("the document was read") { test.onAllNodes(hasText(words.reading)).fetchSemanticsNodes().isEmpty() }
    }

    /** Waits until a text that holds [text] is shown, once or more: a count can be a heading and a field's word both. */
    private suspend fun awaitShown(text: String) {
        test.awaitUntil("\"$text\" was shown") { test.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    /**
     * Presses the [component] whose automation key is [key], in [scope] where it has one, as a screen reader does, once
     * it is shown: a bar that changes as a document opens has its controls a frame later.
     */
    private suspend fun press(component: String, key: String, scope: String? = null) {
        test.awaitNode(hasPantrixTag(component, key, scope), "$component $key")
        test.onPantrixNode(component, key, scope = scope).performSemanticsAction(SemanticsActions.OnClick)
    }
}

/** A node whose automation key is [key], in whatever component and scope. */
private fun hasKeyEnding(key: String) = SemanticsMatcher("has the automation key $key") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)?.endsWith("_$key") == true
}

/** How many of the Query board's queries are used, and so recent, before the one it shows. */
private const val USED_QUERIES = 3

/** Longer than the diagnostics wait for a repeated tag to last. */
private const val FINDINGS_WAIT = 2_000L

/** The Share board's link, whose checksum is not its text's: a link Start says it cannot read. */
private const val BROKEN_LINK = "https://formatico.pantrixui.com/#d=eJyrVspMUbJSUEpKzSmyUiopTUoqUqoFAGG1CBs"

/** A text Formatico cannot tell the format of, as the Start boards' question shows it. */
private const val UNTOLD = "2026-11-05 21:04:11 INFO  Server started on port 8080\n" +
    "2026-11-05 21:04:12 WARN  Slow query took 812 ms\n2026-11-05 21:04:15 INFO  Shutting down"

/** The Invalid boards' settings: a comment, a missing comma, a key in no quotes and a trailing comma. */
private val INVALID_SETTINGS =
    """
    {
      "name": "formatico-web",
      "version": "1.4.0",
      "port": 8080,
      "debug": true,
      // turn off before the release
      "hosts": [
        "formatico.pantrixui.com",
        "localhost"
        "127.0.0.1"
      ],
      timeout: 30,
      "retries": 3,
      "features": {
        "share": false,
        "ai": false,
      },
      "theme": "teal"
    }
    """.trimIndent() + "\n"

/** The Query boards' second tab: Formatico's notes as an RSS feed. */
private val FEED =
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <rss version="2.0">
      <channel>
        <title>Formatico notes</title>
        <link>https://formatico.pantrixui.com/notes</link>
        <language>en</language>
        <item>
          <title>Tabs that remember</title>
          <pubDate>Mon, 26 Oct 2026 09:00:00 GMT</pubDate>
          <category>release</category>
        </item>
      </channel>
    </rss>
    """.trimIndent() + "\n"

/** The order A of the Compare boards. */
private val ORDER_A =
    """
    {
      "id": "ord_8842",
      "status": "shipped",
      "createdAt": "2026-10-28T14:32:00Z",
      "total": 129.90,
      "coupon": null,
      "items": [
        { "sku": "TS-001", "qty": 2 },
        { "sku": "MG-014", "qty": 1 }
      ]
    }
    """.trimIndent() + "\n"

/** The order B of the Compare boards: delivered, with the second item's quantity changed. */
private val ORDER_B =
    """
    {
      "status": "delivered",
      "id": "ord_8842",
      "createdAt": "2026-10-28T14:32:00Z",
      "deliveredAt": "2026-11-01T10:05:00Z",
      "total": 129.9,
      "items": [
        { "sku": "TS-001", "qty": 2 },
        { "sku": "MG-014", "qty": 2 }
      ]
    }
    """.trimIndent() + "\n"
