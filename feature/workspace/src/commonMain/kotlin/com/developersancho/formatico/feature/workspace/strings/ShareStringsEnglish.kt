package com.developersancho.formatico.feature.workspace.strings

/** [ShareStrings] in English. */
internal object ShareStringsEnglish : ShareStrings {
    override fun title(first: String) = "Share $first"

    override val body =
        "The link holds the document itself, compressed. It is never sent to a server: whoever has the link can read the document."
    override val bodyPhone =
        "The link holds the document itself, compressed. It is never sent to a server: whoever has the link can read it."
    override val link = "Link"

    override fun fitsShort(first: String, second: String, third: String) =
        "$first of $second in $third characters: short enough for every browser and app."

    override fun fitsShortPhone(first: String, second: String, third: String) = "$first of $second in $third characters"

    override fun fitsLong(first: String, second: String, third: String) =
        "$first of $second in $third characters: browsers open it, but a chat or mail app may cut a link this long."

    override val plainText = "text"
    override val formatted = "Format the document in the link"
    override val cancel = "Cancel"
    override val copy = "Copy the link"
    override val send = "Send with another app"

    override fun tooLargeTitle(first: String) = "$first is too large for a link"

    override fun tooLargeBody(first: String) =
        "A link holds about 32 KB of text, and this document is $first. Save it as a file and send the file instead."

    override val linkLabel = "Link"
    override val linkCopied = "Link copied"
    override val linkCopyFailed = "The link couldn’t be copied"
}
