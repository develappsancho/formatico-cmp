package com.developersancho.formatico.feature.settings.strings

/** [SettingsStrings] in English. */
internal object SettingsStringsEnglish : SettingsStrings {
    override val title = "Settings"

    override fun backTo(first: String) = "Back to $first"

    override val general = "General"
    override val editor = "Editor"
    override val formats = "Formats"
    override val history = "History"
    override val shortcuts = "Keyboard shortcuts"
    override val about = "About Formatico"
    override val theme = "Theme"
    override val themeBody = "Light or dark, or as the system is."
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val themeSystem = "As the system"
    override val language = "Language"
    override val languageBody = "What Formatico says. Your documents are left as they are."
    override val languageSystem = "As the system"
    override val indentation = "Indentation"
    override val indentationBody = "What Format writes a level in with."
    override val indentTwo = "2 spaces"
    override val indentFour = "4 spaces"
    override val indentTabs = "Tabs"
    override val wrapLines = "Wrap long lines"
    override val wrapLinesBody = "A line too long for the window goes on under itself."
    override val sortKeys = "Sort keys when formatting"
    override val sortKeysBody = "Off, Format keeps the order the keys are written in."
    override val unknown = "When the format cannot be told"
    override val unknownBody = "Plain text with no braces, tags, keys or columns."
    override val unknownAsk = "Ask me"
    override val unknownJson = "Read it as JSON"
    override val unknownText = "Leave it as text"
    override val keepHistory = "Keep a history of documents"
    override val keepHistoryShort = "Keep a history"

    override fun historyKept(count: Int, first: Int, second: String) =
        if (count == 1) {
            "On this device only: $first document, $second."
        } else {
            "On this device only: $first documents, $second."
        }

    override fun historyKeptPhone(count: Int, first: Int, second: String) =
        if (count == 1) {
            "On this phone only: $first document, $second."
        } else {
            "On this phone only: $first documents, $second."
        }

    override val historyEmpty = "On this device only: nothing yet."
    override val historyEmptyPhone = "On this phone only: nothing yet."
    override val historyOff = "Nothing is kept."
    override val keepFor = "Keep each document for"
    override val keepForBody = "Older ones are taken out of the history by themselves."
    override val keep7 = "7 days"
    override val keep30 = "30 days"
    override val keepAlways = "Until I clear it"
    override val clear = "Clear history"
    override val clearBody = "Takes every document out of the history. Open tabs stay."
    override val clearBodyPhone = "Open documents stay."
    override val clearShort = "Clear"
    override val shortcutsBody =
        "Each runs the command of the same name in the command palette. On Windows and Linux, Ctrl takes the place of ⌘."

    override fun version(first: String) = "Formatico $first"

    override fun builtWith(first: String) = "Built with Pantrix UI $first. Set in Geist and Geist Mono."

    override val licences = "Open-source licences"
    override val getTheApp = "Get the app"
    override val getTheAppBody = "Formatico for macOS, Windows and Android, from GitHub."
    override val licencesBody = "Formatico is built on these, each used under its licence. Press one to read it."
    override val licencesBodyPhone = "Formatico is built on these, each used under its licence. Tap one to read it."

    override fun inTheWebApp(first: String) = "$first · in the web app"
}
