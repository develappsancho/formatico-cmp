package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What opens a schema beside a document: a file the reader picks, or the document at a web address, each in a tab of
 * its own that is not chosen; [pickFile] and [openAddress] hand [onOpened] its id. What could not be opened, the app
 * says as it says it elsewhere.
 */
@Stable
interface SchemaOpener {
    /** Opens the platform's picker, and the file chosen in it. */
    fun pickFile(onOpened: (Long) -> Unit)

    /** Opens the document at [address], as Open a web address does. */
    fun openAddress(address: String, onOpened: (Long) -> Unit)
}

/** The app's [SchemaOpener]; none in a preview or a test that gives none, where Choose a file opens nothing. */
val LocalSchemaOpener = staticCompositionLocalOf<SchemaOpener?> { null }
