package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.share.ShareOutcome

/**
 * What the share of a document shows.
 *
 * @property name the document's name, or null for one that has none.
 * @property format the format its text is in, or null for plain text.
 * @property formatted whether the link carries it formatted, as Settings has Format write it.
 * @property canFormat whether it can be formatted: plain text and a document with errors cannot.
 * @property outcome the link made of it, or why none is: null while the first is being made.
 */
internal data class DocumentShare(
    val name: String?,
    val format: Format?,
    val formatted: Boolean = false,
    val canFormat: Boolean = false,
    val outcome: ShareOutcome? = null
)
