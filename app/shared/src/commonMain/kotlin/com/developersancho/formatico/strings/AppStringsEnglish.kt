package com.developersancho.formatico.strings

/** [AppStrings] in English. */
internal object AppStringsEnglish : AppStrings {
    override val appName = "Formatico"
    override val newTab = "New tab"
    override val searchOrRunACommand = "Search or run a command"
    override val useDarkTheme = "Use the dark theme"
    override val useLightTheme = "Use the light theme"
    override val settings = "Settings"
    override val untitled = "Untitled"
    override val changesNotSaved = "changes not saved to its file"

    override fun switchDocuments(first: String, second: Int) = "$first, one of $second open documents. Switch documents"

    override fun switchDocumentsFromNewTab(count: Int, first: Int) =
        if (count == 1) {
            "New tab, beside $first open document. Switch documents"
        } else {
            "New tab, beside $first open documents. Switch documents"
        }

    override fun openDocuments(count: Int, first: Int) = if (count == 1) "$first open document" else "$first open documents"

    override val palettePlaceholder = "Type a command or a format"
    override val paletteEmpty = "No command has those words"
    override val paletteNewDocument = "New document"
    override val paletteOpenFile = "Open a file"
    override val paletteOpenAddress = "Open a web address"
    override val palettePaste = "Paste as a new document"
}
