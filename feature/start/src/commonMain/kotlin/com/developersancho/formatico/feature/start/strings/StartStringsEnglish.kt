package com.developersancho.formatico.feature.start.strings

/** [StartStrings] in English. */
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal object StartStringsEnglish : StartStrings {
    override val title = "Paste anything"
    override val lead =
        "JSON, XML, YAML, TOML, CSV or properties: Formatico tells which it is, checks it and lays it out. Your text never " +
            "leaves this device."
    override val leadShort = "JSON, XML, YAML, TOML, CSV or properties. Your text never leaves this device."
    override val paste = "Paste from clipboard"

    override fun pasteHint(first: String) = "or press $first anywhere on this screen"

    override val newEmptyDocument = "New empty document"
    override val newDocument = "New document"
    override val openFile = "Open a file"
    override val dropAFile = "Drop a file here"
    override val chooseAFile = "Choose a file"
    override val clipboardEmpty = "No text was read from the clipboard"

    override fun pasteTooLarge(first: String) = "The pasted text is larger than $first"

    override val linkBroken = "This link holds no document Formatico can read. It may have been cut short."
    override val clipboardUnreadable = "This browser does not let Formatico read the clipboard. Paste with the keyboard, or open a file."

    override fun fileDropSupporting(first: String) =
        "JSON, JSONC, JSON5, NDJSON, XML, YAML, TOML, CSV, TSV, properties and INI, up to $first"

    override fun fileTooLarge(first: String, second: String) = "$first is larger than $second"

    override fun fileWrongType(first: String) = "$first is not a kind of text Formatico reads"

    override fun fileUnreadable(first: String) = "$first could not be read"

    override val recent = "Recent"
    override val clearHistory = "Clear history"
    override val recentEmptyTitle = "Nothing opened yet"
    override val recentEmptyText = "What you paste, open or drop is listed here, and stays on this device."
    override val untitled = "Untitled"
    override val plainText = "Text"

    override fun recentMeta(first: String, second: String, third: String) = "$first · $second · $third"

    override val editedJustNow = "edited just now"

    override fun editedMinutesAgo(count: Int, first: Int) = if (count == 1) "edited $first minute ago" else "edited $first minutes ago"

    override fun editedTodayAt(first: String) = "edited today at $first"

    override val editedYesterday = "edited yesterday"

    override fun editedOn(first: String) = "edited on $first"

    override val openedJustNow = "opened just now"

    override fun openedMinutesAgo(count: Int, first: Int) = if (count == 1) "opened $first minute ago" else "opened $first minutes ago"

    override fun openedTodayAt(first: String) = "opened today at $first"

    override val openedYesterday = "opened yesterday"

    override fun openedOn(first: String) = "opened on $first"

    override val justNow = "just now"

    override fun minutesAgo(count: Int, first: Int) = if (count == 1) "$first minute ago" else "$first minutes ago"

    override val today = "today"
    override val yesterday = "yesterday"
    override val samples = "Samples"
    override val samplesHint = "Open one to try Formatico"
    override val allSamples = "All samples"
    override val sampleOrder = "An order"
    override val sampleOrderDetail = "Dates, colours, links and a picture"
    override val sampleOrderDetailShort = "Dates, colours, links"
    override val sampleDeployment = "A Kubernetes deployment"
    override val sampleDeploymentShort = "A deployment"
    override val sampleDeploymentDetail = "Nested lists and anchors"
    override val sampleDeploymentDetailShort = "Nested lists"
    override val sampleManifest = "A Rust manifest"
    override val sampleManifestDetail = "Tables and arrays of tables"
    override val sampleCountries = "Countries"
    override val sampleCountriesDetail = "249 rows, 6 columns"
    override val privacy =
        "History stays on this device, and Formatico has no account. A document goes to its server only when you share an encrypted link."
    override val privacyWeb =
        "History stays on this device, and Formatico has no account. A document goes through its server only when you share an " +
            "encrypted link or, on the web, open a web address."
    override val unknownTitle = "What format is this?"
    override val unknownPasted = "Formatico could not tell the format of the text you pasted. Choose how to read it."

    override fun unknownFile(first: String) = "Formatico could not tell the format of $first. Choose how to read it."

    override val unknownReadAs = "Read it as"
    override val unknownRule = "Settings, under Formats, can make one of these the rule."
    override val unknownLeave = "Leave it as text"
    override val unknownOpen = "Open"
    override val openAddress = "Open a web address"
    override val addressMessage = "Formatico opens the document at a public http or https address."
    override val addressLabel = "Address"
    override val addressPlaceholder = "https://example.com/orders/8842.json"
    override val addressHelpWeb = "On the web it comes through Formatico’s server, which keeps nothing. Up to 10 MB."

    override fun addressHelp(first: String) = "Formatico downloads it itself. Up to $first."

    override val addressCancel = "Cancel"
    override val addressOpen = "Open"
    override val addressOpening = "Opening"
    override val addressNotAllowed = "Formatico opens only public http and https addresses."

    override fun addressNotThere(first: String) = "The address answered $first: there is no document there."

    override fun addressAnswered(first: String) = "The address answered $first, and gave no document."

    override fun addressTooLarge(first: String, second: String) = "The document is $first, and Formatico opens up to $second here."

    override fun addressTooLargeUntold(first: String) = "The document is larger than Formatico opens here, $first."

    override val addressNotText = "What is at this address is not text, and Formatico opens only text."
    override val addressUnreachable = "The address could not be reached. Check it, or try again."
    override val addressServerUnreachable = "Formatico’s server, which fetches it on the web, could not be reached. Try again."
    override val sharedOpeningTitle = "Opening a shared document"
    override val sharedOpeningMessage = "It is fetched from Formatico’s server and decrypted on this device."
    override val sharedOpeningMessagePhone = "It is fetched from Formatico’s server and decrypted on this phone."
    override val sharedOpeningCancel = "Cancel"
    override val sharedLinkGone = "This shared link has expired, or whoever shared it deleted it."
    override val sharedLinkUnreachable = "Formatico’s server could not be reached, so the shared document could not be opened."
    override val sharedLinkTryAgain = "Try again"
}
