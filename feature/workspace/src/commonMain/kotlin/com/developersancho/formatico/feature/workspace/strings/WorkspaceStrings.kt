package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the workspace: its toolbar, its views, its tree and its status.
 *
 * In English, [WorkspaceStringsEnglish], and in Turkish, [WorkspaceStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface WorkspaceStrings {
    val untitled: String
    val plainText: String

    /** "Format: {first}. Change the format" */
    fun formatMenu(first: String): String

    val format: String
    val minify: String
    val sortKeys: String
    val query: String
    val convert: String
    val compare: String
    val copyDocument: String
    val saveFile: String
    val moreActions: String
    val moreActionsSortCompareSave: String
    val closeDocument: String
    val viewText: String
    val viewTree: String
    val viewTable: String
    val find: String
    val findInTree: String
    val findInText: String
    val showSummaryItem: String

    /** What pressing the phone's status does: it opens the formats the document can be read as. */
    val changeFormat: String

    /** "match {first}" */
    fun match(first: String): String

    val openTo: String

    /** "Open the tree to level {first}" */
    fun openToLevel(first: Int): String

    val openAll: String
    val openAllLevels: String
    val closeSearch: String
    val plainTitle: String
    val plainBody: String
    val emptyTitle: String
    val emptyBody: String
    val noValueTitle: String
    val noValueBody: String

    /** "The text of {first}" */
    fun textOf(first: String): String

    /** "Ln {first}, Col {second}" */
    fun lineColumn(first: Int, second: Int): String

    val statusReading: String

    /** "Valid {first}" */
    fun statusValid(first: String): String

    /** "{first} problems", the form [count] chooses */
    fun statusProblems(count: Int, first: Int): String

    /** "{first} lines", the form [count] chooses */
    fun lines(count: Int, first: Int): String

    /** "{first} · {second}" */
    fun separated(first: String, second: String): String

    val encoding: String
    val indentTabs: String

    /** "{first} spaces", the form [count] chooses */
    fun indentSpaces(count: Int, first: Int): String

    val formatted: String
    val minified: String

    /** "Minified, and its {first} comments removed", the form [count] chooses */
    fun minifiedComments(count: Int, first: Int): String

    val keysSorted: String
    val undo: String
    val alreadyFormatted: String
    val alreadyMinified: String
    val alreadySorted: String
    val editImpossible: String
    val aliasBeforeAnchor: String
    val copied: String
    val copyFailed: String
    val valueCopied: String
    val valueCopyFailed: String
    val saveFailed: String
    val documentLabel: String
    val valueLabel: String
    val showInText: String
    val copyValue: String
    val saveCsv: String
    val tableOut: String
    val copyCsv: String
    val copyMarkdown: String
    val tableLabel: String
    val tableCopied: String
    val tableCopyFailed: String
    val copyPath: String

    /** "Copy the path of {first} as JSONPath, JSON Pointer or jq" */
    fun copyPathOf(first: String): String

    /** "Copy the path of {first} as" */
    fun copyPathAs(first: String): String

    val pathLabel: String
    val pathCopied: String
    val pathCopyFailed: String
    val kindDate: String
    val kindColour: String
    val kindLink: String
    val kindPicture: String
    val kindString: String
    val kindNumber: String
    val kindBoolean: String
    val kindNull: String
    val kindObject: String
    val kindArray: String
    val kindAlias: String

    /** "Colour {first}" */
    fun colourOf(first: String): String

    val pictureAt: String
    val openLink: String

    /** "Points to the anchor &{first}" */
    fun aliasDetail(first: String): String

    /** "No anchor &{first} comes before it" */
    fun aliasMissing(first: String): String

    val goToAnchor: String

    /** "{first} keys", the form [count] chooses */
    fun keys(count: Int, first: Int): String

    /** "{first} items", the form [count] chooses */
    fun items(count: Int, first: Int): String

    /** "{first} characters", the form [count] chooses */
    fun characters(count: Int, first: Int): String

    /** "{first}, {second} UTC" */
    fun momentUtc(first: String, second: String): String

    /** "{first} in your time zone" */
    fun momentLocal(first: String): String

    val today: String
    val yesterday: String
    val tomorrow: String

    /** "{first} days ago", the form [count] chooses */
    fun daysAgo(count: Int, first: Int): String

    /** "in {first} days", the form [count] chooses */
    fun daysAhead(count: Int, first: Int): String

    /** "{first} rows", the form [count] chooses */
    fun rows(count: Int, first: Int): String

    /** "{first} of {second} rows", the form [count] chooses */
    fun rowsOf(count: Int, first: Int, second: Int): String

    /** "{first} columns", the form [count] chooses */
    fun tableColumns(count: Int, first: Int): String

    val filterRows: String
    val columns: String

    /** "Columns: all {first} shown. Choose columns" */
    fun columnsAll(first: Int): String

    /** "Columns: {first} of {second} shown. Choose columns" */
    fun columnsSome(first: Int, second: Int): String

    val valueColumn: String
    val index: String

    /** "No {first} key in this object" */
    fun missingKey(first: String): String

    /** "{first}. Open" */
    fun openValue(first: String): String

    /** "{first} numbers", the form [count] chooses */
    fun numbers(count: Int, first: String): String

    /** "{first} values", the form [count] chooses */
    fun values(count: Int, first: String): String

    val statMin: String
    val statMax: String
    val statAverage: String
    val statDifferent: String
    val statEmpty: String

    /** "Sorted by {first}, {second}" */
    fun sortedBy(first: String, second: String): String

    val newestFirst: String
    val oldestFirst: String
    val largestFirst: String
    val smallestFirst: String
    val aToZ: String
    val zToA: String
    val noTableTitle: String
    val noTableBody: String

    /** "No row holds “{first}”" */
    fun noRows(first: String): String

    val repair: String
    val repairAll: String
    val readAsJson5: String

    /** "This is not valid {first}" */
    fun invalidTitle(first: String): String

    /** "Line {first}, column {second}" */
    fun problemWhere(first: Int, second: Int): String

    val nextProblem: String
    val previousProblem: String

    /** "Problems ({first})" */
    fun viewProblems(first: Int): String

    val viewProblemsShort: String

    /**
     * "Formatico read what it could: the tree and the table show it. Repair fixes all {first} problems at once.",
     * the form [count] chooses
     */
    fun invalidBody(count: Int, first: Int): String

    /** "JSON5 allows {first} of the {second}.", the form [count] chooses */
    fun json5Allows(count: Int, first: Int, second: Int): String

    /** "Not valid {second}: {first} problems", the form [count] chooses */
    fun notValidCount(count: Int, first: Int, second: String): String

    /** "Problem {first} of {second} · line {third}, column {fourth}", the form [count] chooses */
    fun problemOf(count: Int, first: Int, second: Int, third: Int, fourth: Int): String

    val problemError: String
    val problemWarning: String
    val viewSummary: String

    /** "{first}. Show the summary of the document" */
    fun showSummary(first: String): String

    /** "{first}. The summary is shown" */
    fun summaryShown(first: String): String

    val share: String
}

/** [WorkspaceStrings] in the theme's language. */
internal val workspaceStrings: WorkspaceStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(WorkspaceStringsEnglish, WorkspaceStringsTurkish)

/** [WorkspaceStrings] in the language whose tag is [language], away from a screen. */
internal fun workspaceStringsIn(language: String): WorkspaceStrings = inLanguage(language, WorkspaceStringsEnglish, WorkspaceStringsTurkish)
