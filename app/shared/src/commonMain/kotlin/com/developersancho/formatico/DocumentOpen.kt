package com.developersancho.formatico

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.settings.SettingsStore
import com.developersancho.formatico.core.settings.UnknownFormat
import com.developersancho.formatico.feature.start.DocumentOpening
import com.developersancho.formatico.feature.start.OpenedDocument
import com.developersancho.formatico.feature.workspace.DocumentDrop
import com.developersancho.formatico.feature.workspace.SchemaOpener
import com.developersancho.formatico.feature.workspace.Workbench
import com.pantrixui.foundation.files.PantrixFile

/**
 * Opens what Start and the command palette hand on in a tab of [workbench]: a text whose format could not be told as
 * [settings] say, read as JSON, left as text, or [asking] which format first.
 */
@Stable
internal class DocumentOpen(private val workbench: Workbench, private val settings: SettingsStore) {
    /** The document whose format is being asked for, or null while none is. */
    var asking: OpenedDocument? by mutableStateOf(null)
        private set

    /** Opens [document], or asks its format first where Settings says so. */
    fun open(document: OpenedDocument) {
        val untold = document.format == null && document.text.isNotBlank()
        when {
            !untold -> show(document, document.format)
            settings.preferences.value.unknownFormat == UnknownFormat.Ask -> asking = document
            settings.preferences.value.unknownFormat == UnknownFormat.Json -> show(document, Format.Json)
            else -> show(document, null)
        }
    }

    /** Opens the document asked about in [format], or as plain text for null. */
    fun answer(format: Format?) {
        asking?.let { show(it, format) }
        asking = null
    }

    /** Opens nothing: the question was dismissed. */
    fun dismiss() {
        asking = null
    }

    private fun show(document: OpenedDocument, format: Format?) {
        workbench.open(document.name, document.text, format, document.historyId)
    }
}

/**
 * Opens a schema beside the document it validates, as [opening] opens a file or a web address: in a tab of its own that
 * is not chosen, read as JSON where its format cannot be told, since a schema is JSON.
 */
internal class AppSchemaOpener(private val opening: DocumentOpening, private val workbench: Workbench) : SchemaOpener {
    override fun pickFile(onOpened: (Long) -> Unit) = opening.pickFile { onOpened(beside(it)) }

    override fun openAddress(address: String, onOpened: (Long) -> Unit) = opening.openAddress(address) { onOpened(beside(it)) }

    private fun beside(document: OpenedDocument): Long =
        workbench.open(document.name, document.text, document.format ?: Format.Json, document.historyId, choose = false)
}

/**
 * Opens a file dropped on an open document as [opening] opens one: in a new tab, chosen, Settings left; what could not be
 * opened is said in a snackbar, as Start's zone says it.
 */
internal class AppDocumentDrop(private val opening: DocumentOpening) : DocumentDrop {
    override val maxFileSize: Long get() = opening.maxFileSize

    override fun open(files: List<PantrixFile>) = opening.openDropped(files)
}
