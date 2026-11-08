package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.files.PantrixFile
import kotlin.time.Instant

/**
 * What the Start screen draws.
 *
 * @property recent the documents opened before, the newest first.
 * @property samples the documents that come with Formatico, to try it on.
 * @property refusal why the last file given was not opened, until another is given.
 */
data class StartState(
    val recent: List<RecentDocument> = emptyList(),
    val samples: List<Sample> = Sample.entries,
    val refusal: FileRefusal? = null
)

/**
 * A document opened before, as Recent lists it.
 *
 * @property id what tells it from the others in the history.
 * @property name its file's name, or null for a text that was pasted.
 * @property format the format it is written in, or null for plain text.
 * @property size how many bytes its text has, in UTF-8.
 * @property editedAt when it was last opened or changed.
 */
data class RecentDocument(val id: Long, val name: String?, val format: Format?, val size: Long, val editedAt: Instant)

/**
 * A document the Start screen hands on, to be shown in a workspace.
 *
 * @property name its file's name, or null for a new document or a text that was pasted.
 * @property text what it says.
 * @property format the format it is written in, or null for plain text and an empty document.
 */
data class OpenedDocument(val name: String?, val text: String, val format: Format?)

/** A document that comes with Formatico, in `files/samples/` of this module's resources. */
enum class Sample(val fileName: String, val format: Format) {
    /** An order: dates, colours, links and a picture. */
    Order("order.json", Format.Json),

    /** A Kubernetes deployment: nested lists and anchors. */
    Deployment("deployment.yaml", Format.Yaml),

    /** A Rust manifest: tables and arrays of tables. */
    Manifest("Cargo.toml", Format.Toml),

    /** The countries of ISO 3166-1: a table of 249 rows and 6 columns. */
    Countries("countries.csv", Format.Csv)
}

/** Why a file given to the Start screen was not opened. */
sealed interface FileRefusal {
    /** The file's name. */
    val fileName: String

    /** The file has more bytes than the platform's most, [maxSize]. */
    data class TooLarge(override val fileName: String, val maxSize: Long) : FileRefusal

    /** The file is of a kind Formatico does not read. */
    data class WrongType(override val fileName: String) : FileRefusal

    /** The platform could not read the file. */
    data class Unreadable(override val fileName: String) : FileRefusal
}

/** What the user did, or asked for, on the Start screen. */
sealed interface StartIntent {
    /** Paste the clipboard's [text], or null where it holds none. */
    data class Paste(val text: String?) : StartIntent

    /** Start a new, empty document. */
    data object NewDocument : StartIntent

    /** Open the first of [files], chosen in the picker or dropped on the screen. */
    data class OpenFiles(val files: List<PantrixFile>) : StartIntent

    /** A file was not taken, for [refusal]. */
    data class RefuseFile(val refusal: FileRefusal) : StartIntent

    /** Open the document of the history whose id is [id]. */
    data class OpenRecent(val id: Long) : StartIntent

    /** Open [sample]. */
    data class OpenSample(val sample: Sample) : StartIntent

    /** Forget every document of the history. */
    data object ClearHistory : StartIntent
}

/** What happens once on the Start screen. */
sealed interface StartEffect {
    /** Show [document] in a workspace. */
    data class OpenDocument(val document: OpenedDocument) : StartEffect

    /** The clipboard held no text to paste. */
    data object NothingToPaste : StartEffect
}
