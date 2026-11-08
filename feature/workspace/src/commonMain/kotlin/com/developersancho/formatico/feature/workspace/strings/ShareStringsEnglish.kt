package com.developersancho.formatico.feature.workspace.strings

/** [ShareStrings] in English. */
internal object ShareStringsEnglish : ShareStrings {
    override fun title(first: String) = "Share $first"

    override val ways = "How to share it"
    override val inLink = "In the link"
    override val encrypted = "As an encrypted link"
    override val inLinkBody =
        "The link holds the document itself, compressed. It is never sent to a server: whoever has the link can read it."
    override val encryptedBody =
        "A short link to the document, encrypted on this device and kept on Formatico’s server until it expires."
    override val encryptedBodyPhone =
        "A short link to the document, encrypted on this phone and kept on Formatico’s server until it expires."
    override val link = "Link"

    override fun fitsShort(first: String, second: String, third: String) =
        "$first of $second in $third characters: short enough for every browser and app."

    override fun fitsShortPhone(first: String, second: String, third: String) = "$first of $second in $third characters"

    override fun fitsLong(first: String, second: String, third: String) =
        "$first of $second in $third characters: browsers open it, but a chat or mail app may cut a link this long."

    override val plainText = "text"
    override val formatted = "Format the document in the link"
    override val formattedEncrypted = "Format the document before it is encrypted"
    override val keepFor = "Keep it for"

    override fun days(count: Int) = if (count == 1) "1 day" else "$count days"

    override val trust =
        "Formatico’s server keeps only the encrypted document and cannot read it: the key is in the link alone. Whoever has " +
            "the link can read the document."
    override val unreachableTitle = "Formatico’s server could not be reached"
    override val unreachableBody = "The document was not sent. Try again, or share it in the link."
    override val unreachableBodyEncryptedOnly = "The document was not sent. Try again."
    override val deleteFailed = "The link was not deleted. Try again."
    override val cancel = "Cancel"
    override val copy = "Copy the link"
    override val send = "Send with another app"
    override val create = "Create the link"
    override val creating = "Creating the link"
    override val tryAgain = "Try again"
    override val done = "Done"
    override val delete = "Delete the link"

    override fun kept(first: String, second: String) =
        "Kept, encrypted, until $first. $second characters: short enough for every browser and app."

    override fun keptPhone(first: String) = "Kept, encrypted, until $first"

    override fun encryptedOnlyBody(first: String, second: String) =
        "At $second, $first is too large to go in a link, which holds about 32 KB. It can go as an encrypted link instead."

    override fun tooLargeTitle(first: String) = "$first is too large to share"

    override fun tooLargeBody(first: String) =
        "A link holds about 32 KB and an encrypted link up to 10 MB, and this document is $first. Save it as a file and send " +
            "the file instead."

    override val linkLabel = "Link"
    override val linkCopied = "Link copied"
    override val linkCopyFailed = "The link couldn’t be copied"
}
