package com.developersancho.formatico.feature.workspace.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.developersancho.formatico.core.designsystem.inLanguage
import com.developersancho.formatico.core.designsystem.inTheLanguage

/**
 * The words of the share.
 *
 * In English, [ShareStringsEnglish], and in Turkish, [ShareStringsTurkish]. A sentence that holds values is a
 * function of them, in their order; one whose form follows a number takes it first, as `count`.
 */
internal interface ShareStrings {
    /** "Share {first}" */
    fun title(first: String): String

    val body: String
    val bodyPhone: String
    val link: String

    /** "{first} of {second} in {third} characters: short enough for every browser and app." */
    fun fitsShort(first: String, second: String, third: String): String

    /** "{first} of {second} in {third} characters" */
    fun fitsShortPhone(first: String, second: String, third: String): String

    /**
     * "{first} of {second} in {third} characters: browsers open it, but a chat or mail app may cut a link this
     * long."
     */
    fun fitsLong(first: String, second: String, third: String): String

    val plainText: String
    val formatted: String
    val cancel: String
    val copy: String
    val send: String

    /** "{first} is too large for a link" */
    fun tooLargeTitle(first: String): String

    /**
     * "A link holds about 32 KB of text, and this document is {first}. Save it as a file and send the file instead."
     */
    fun tooLargeBody(first: String): String

    val linkLabel: String
    val linkCopied: String
    val linkCopyFailed: String
}

/** [ShareStrings] in the theme's language. */
internal val shareStrings: ShareStrings
    @Composable
    @ReadOnlyComposable
    get() = inTheLanguage(ShareStringsEnglish, ShareStringsTurkish)

/** [ShareStrings] in the language whose tag is [language], away from a screen. */
internal fun shareStringsIn(language: String): ShareStrings = inLanguage(language, ShareStringsEnglish, ShareStringsTurkish)
