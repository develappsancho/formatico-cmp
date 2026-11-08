package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.network.EncryptedLink
import com.developersancho.formatico.core.share.ShareKeep
import com.developersancho.formatico.core.share.ShareLinks
import com.developersancho.formatico.core.share.ShareOutcome

/** How a document is shared: in the link itself, or as an encrypted link Formatico's server keeps. */
internal enum class ShareWay {
    Link,
    Encrypted
}

/** Where the document's encrypted link stands. */
internal sealed interface EncryptedShare {
    /** None is made yet: the reader chooses how long it is kept. */
    data object Choosing : EncryptedShare

    /** It is being made: the document encrypted on this device, and sent. */
    data object Creating : EncryptedShare

    /** Formatico's server could not be reached, and none was made. */
    data object Failed : EncryptedShare

    /** It is made, as [link]; [deleteFailed] where deleting it could not reach the server. */
    data class Created(val link: EncryptedLink, val deleteFailed: Boolean = false) : EncryptedShare
}

/**
 * What the share of a document shows.
 *
 * @property name the document's name, or null for one that has none.
 * @property format the format its text is in, or null for plain text.
 * @property formatted whether the link carries it formatted, as Settings has Format write it.
 * @property canFormat whether it can be formatted: plain text and a document with errors cannot.
 * @property outcome the link made of it, or why none is: null while the first is being made.
 * @property way how the reader chose to share it.
 * @property keep how long its encrypted link is to be kept.
 * @property encrypted where its encrypted link stands.
 */
internal data class DocumentShare(
    val name: String?,
    val format: Format?,
    val formatted: Boolean = false,
    val canFormat: Boolean = false,
    val outcome: ShareOutcome? = null,
    val way: ShareWay = ShareWay.Link,
    val keep: ShareKeep = ShareKeep.Week,
    val encrypted: EncryptedShare = EncryptedShare.Choosing
) {
    /** Whether the document is too large for a link and not for an encrypted one, which is then its only way. */
    val encryptedOnly: Boolean get() = (outcome as? ShareOutcome.TooLarge)?.let { it.bytes <= ShareLinks.MAX_TEXT } == true

    /** Whether the document is too large for an encrypted link as well: it is saved as a file instead. */
    val tooLarge: Boolean get() = (outcome as? ShareOutcome.TooLarge)?.let { it.bytes > ShareLinks.MAX_TEXT } == true

    /** The way the share shows: an encrypted link for a document a link cannot hold. */
    val shownWay: ShareWay get() = if (encryptedOnly) ShareWay.Encrypted else way
}
