package com.developersancho.formatico.feature.workspace.strings

/** [WorkspaceStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object WorkspaceStringsEnglish : WorkspaceStrings {
    override val untitled = "Untitled"
    override val plainText = "Plain text"

    override fun formatMenu(first: String) = "Format: $first. Change the format"

    override val format = "Format"
    override val minify = "Minify"
    override val sortKeys = "Sort keys"
    override val query = "Query"
    override val convert = "Convert"
    override val compare = "Compare"
    override val copyDocument = "Copy the document"
    override val saveFile = "Save as a file"
    override val moreActions = "More actions"
    override val moreActionsSortCompareSave = "More actions: sort keys, compare, save as a file"
    override val closeDocument = "Close the document"
    override val viewText = "Text"
    override val viewTree = "Tree"
    override val viewTable = "Table"
    override val find = "Find a key or a value"
    override val findInTree = "Find in the tree"
    override val findInText = "Find in the text"
    override val showSummaryItem = "Show the summary"
    override val changeFormat = "Change the format"

    override fun match(first: String) = "match $first"

    override val openTo = "Open to"

    override fun openToLevel(first: Int) = "Open the tree to level $first"

    override val openAll = "All"
    override val openAllLevels = "Open the whole tree"
    override val closeSearch = "Close the search"
    override val plainTitle = "Plain text has no tree"
    override val plainBody = "Choose the format it is written in, and it is read as one."
    override val emptyTitle = "The document is empty"
    override val emptyBody = "There is nothing in it to read yet."
    override val noValueTitle = "No value could be read"
    override val noValueBody = "The document’s problems stand in the way of its tree."

    override fun textOf(first: String) = "The text of $first"

    override fun lineColumn(first: Int, second: Int) = "Ln $first, Col $second"

    override val statusReading = "Reading…"

    override fun statusValid(first: String) = "Valid $first"

    override fun statusProblems(count: Int, first: Int) = if (count == 1) "$first problem" else "$first problems"

    override fun lines(count: Int, first: Int) = if (count == 1) "$first line" else "$first lines"

    override fun separated(first: String, second: String) = "$first · $second"

    override val encoding = "UTF-8"
    override val indentTabs = "Tabs"

    override fun indentSpaces(count: Int, first: Int) = if (count == 1) "$first space" else "$first spaces"

    override val formatted = "Formatted"
    override val minified = "Minified"

    override fun minifiedComments(count: Int, first: Int) =
        if (count == 1) {
            "Minified, and its comment removed"
        } else {
            "Minified, and its $first comments removed"
        }

    override val keysSorted = "Keys sorted"
    override val undo = "Undo"
    override val alreadyFormatted = "It is formatted already"
    override val alreadyMinified = "It is minified already"
    override val alreadySorted = "Its keys are in order already"
    override val editImpossible = "Formatico changes only a valid JSON, JSONC, JSON5 or YAML document"
    override val aliasBeforeAnchor = "The keys are left as they are: sorting them would put an alias before its anchor"
    override val copied = "Document copied"
    override val copyFailed = "The document couldn’t be copied"
    override val valueCopied = "Value copied"
    override val valueCopyFailed = "The value couldn’t be copied"
    override val saveFailed = "The file couldn’t be saved"
    override val documentLabel = "Document"
    override val valueLabel = "Value"
    override val showInText = "Show in text"
    override val copyValue = "Copy the value"
    override val saveCsv = "Save as CSV…"
    override val tableOut = "Save or copy the table"
    override val copyCsv = "Copy as CSV"
    override val copyMarkdown = "Copy as a Markdown table"
    override val tableLabel = "Table"
    override val tableCopied = "Table copied"
    override val tableCopyFailed = "The table couldn’t be copied"
    override val copyPath = "Copy path"

    override fun copyPathOf(first: String) = "Copy the path of $first as JSONPath, JSON Pointer or jq"

    override fun copyPathAs(first: String) = "Copy the path of $first as"

    override val pathLabel = "Path"
    override val pathCopied = "Path copied"
    override val pathCopyFailed = "The path couldn’t be copied"
    override val kindDate = "Date"
    override val kindColour = "Colour"
    override val kindLink = "Link"
    override val kindPicture = "Picture"
    override val kindString = "String"
    override val kindNumber = "Number"
    override val kindBoolean = "Boolean"
    override val kindNull = "Null"
    override val kindObject = "Object"
    override val kindArray = "Array"
    override val kindAlias = "Alias"

    override fun colourOf(first: String) = "Colour $first"

    override val pictureAt = "Picture at this address"
    override val openLink = "Open the link"

    override fun aliasDetail(first: String) = "Points to the anchor &$first"

    override fun aliasMissing(first: String) = "No anchor &$first comes before it"

    override val goToAnchor = "Go to the anchor"

    override fun keys(count: Int, first: Int) = if (count == 1) "$first key" else "$first keys"

    override fun items(count: Int, first: Int) = if (count == 1) "$first item" else "$first items"

    override fun characters(count: Int, first: Int) = if (count == 1) "$first character" else "$first characters"

    override fun momentUtc(first: String, second: String) = "$first, $second UTC"

    override fun momentLocal(first: String) = "$first in your time zone"

    override val today = "today"
    override val yesterday = "yesterday"
    override val tomorrow = "tomorrow"

    override fun daysAgo(count: Int, first: Int) = if (count == 1) "$first day ago" else "$first days ago"

    override fun daysAhead(count: Int, first: Int) = if (count == 1) "in $first day" else "in $first days"

    override fun rows(count: Int, first: Int) = if (count == 1) "$first row" else "$first rows"

    override fun rowsOf(count: Int, first: Int, second: Int) = if (count == 1) "$first of $second row" else "$first of $second rows"

    override fun tableColumns(count: Int, first: Int) = if (count == 1) "$first column" else "$first columns"

    override val filterRows = "Filter rows"
    override val columns = "Columns"

    override fun columnsAll(first: Int) = "Columns: all $first shown. Choose columns"

    override fun columnsSome(first: Int, second: Int) = "Columns: $first of $second shown. Choose columns"

    override val valueColumn = "value"
    override val index = "Index in the array"

    override fun missingKey(first: String) = "No $first key in this object"

    override fun openValue(first: String) = "$first. Open"

    override fun numbers(count: Int, first: String) = if (count == 1) "$first number" else "$first numbers"

    override fun values(count: Int, first: String) = if (count == 1) "$first value" else "$first values"

    override val statMin = "min"
    override val statMax = "max"
    override val statAverage = "average"
    override val statDifferent = "different"
    override val statEmpty = "empty"

    override fun sortedBy(first: String, second: String) = "Sorted by $first, $second"

    override val newestFirst = "newest first"
    override val oldestFirst = "oldest first"
    override val largestFirst = "largest first"
    override val smallestFirst = "smallest first"
    override val aToZ = "A to Z"
    override val zToA = "Z to A"
    override val noTableTitle = "No list to show as a table"
    override val noTableBody = "The table shows an array, and this document has none."

    override fun noRows(first: String) = "No row holds “$first”"

    override val repair = "Repair"
    override val repairAll = "Repair all"
    override val readAsJson5 = "Read as JSON5"

    override fun invalidTitle(first: String) = "This is not valid $first"

    override fun problemWhere(first: Int, second: Int) = "Line $first, column $second"

    override val nextProblem = "Next problem"
    override val previousProblem = "Previous problem"

    override fun viewProblems(first: Int) = "Problems ($first)"

    override val viewProblemsShort = "Problems"

    override fun invalidBody(count: Int, first: Int) =
        if (count == 1) {
            "Formatico read what it could: the tree and the table show it. Repair fixes the problem."
        } else {
            "Formatico read what it could: the tree and the table show it. Repair fixes all $first problems at once."
        }

    override fun json5Allows(count: Int, first: Int, second: Int) = "JSON5 allows $first of the $second."

    override fun notValidCount(count: Int, first: Int, second: String) =
        if (count == 1) {
            "Not valid $second: $first problem"
        } else {
            "Not valid $second: $first problems"
        }

    override fun problemOf(count: Int, first: Int, second: Int, third: Int, fourth: Int) =
        "Problem $first of $second · line $third, column $fourth"

    override val problemError = "Error"
    override val problemWarning = "Warning"
    override val viewSummary = "Summary"

    override fun showSummary(first: String) = "$first. Show the summary of the document"

    override fun summaryShown(first: String) = "$first. The summary is shown"

    override val share = "Share"
}
