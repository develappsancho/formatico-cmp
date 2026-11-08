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
@Suppress("TooManyFunctions") // A sentence that holds values is a function of them.
internal interface ShareStrings {
    /** "Share {first}" */
    fun title(first: String): String

    val ways: String
    val inLink: String
    val encrypted: String
    val inLinkBody: String
    val encryptedBody: String
    val encryptedBodyPhone: String
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
    val formattedEncrypted: String
    val keepFor: String

    /** "{count} days", "1 day" */
    fun days(count: Int): String

    val trust: String
    val unreachableTitle: String
    val unreachableBody: String
    val unreachableBodyEncryptedOnly: String
    val busyTitle: String
    val busyBody: String
    val busyBodyEncryptedOnly: String
    val dailyLimitTitle: String

    /** "Formatico’s server keeps up to {count} a day from one network." */
    fun dailyLimitBody(count: Int): String

    val dailyLimitLater: String
    val dailyLimitLaterEncryptedOnly: String

    val deleteFailed: String
    val cancel: String
    val copy: String
    val send: String
    val create: String
    val creating: String
    val tryAgain: String
    val done: String
    val delete: String

    /** "Kept, encrypted, until {first}. {second} characters: short enough for every browser and app." */
    fun kept(first: String, second: String): String

    /** "Kept, encrypted, until {first}" */
    fun keptPhone(first: String): String

    /**
     * "At {second}, {first} is too large to go in a link, which holds about 32 KB. It can go as an encrypted link
     * instead."
     */
    fun encryptedOnlyBody(first: String, second: String): String

    /** "{first} is too large to share" */
    fun tooLargeTitle(first: String): String

    /**
     * "A link holds about 32 KB and an encrypted link up to 10 MB, and this document is {first}. Save it as a file
     * and send the file instead."
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
