package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.Loss
import com.developersancho.formatico.core.format.convert.Note
import com.developersancho.formatico.feature.workspace.strings.convertStrings

/** The status bar's tag of [losses]: the one loss's own, or how many there are. */
@Composable
internal fun lossesTag(losses: List<Loss>, from: Format, to: Format): String = when (losses.size) {
    1 -> lossWords(losses.single(), from, to).tag
    else -> convertStrings.losses(losses.size, losses.size)
}

/**
 * What the alert of a conversion that lost nothing says: that the new document converts back to the old as it is,
 * after what was done for it, where [withNotes] says to tell that too.
 */
@Composable
internal fun nothingLostMessage(converted: ConvertOutcome.Converted, withNotes: Boolean): String {
    val source = converted.from.displayName
    val target = converted.to.displayName
    val notes = if (withNotes) converted.notes.map { noteWords(it, target) } else emptyList()
    if (notes.isEmpty()) return convertStrings.back(source)
    // A conversion does two things at most for it: quotes strings, and keeps the spelling of numbers.
    val said = if (notes.size == 1) notes.single() else convertStrings.notesAnd(notes[0], notes[1])
    return convertStrings.backWith(said, target, source)
}

@Composable
private fun noteWords(note: Note, target: String): String = when (note) {
    is Note.Quoted -> if (note.dates) convertStrings.noteQuotedDates(target) else convertStrings.noteQuotedValues(target)

    is Note.Spelling -> {
        val second = note.second
        when {
            note.count == 1 -> convertStrings.noteSpellingOne(note.first)
            note.count == 2 && second != null -> convertStrings.noteSpellingTwo(note.first, second)
            else -> convertStrings.noteSpellingMore(note.count - 1, note.first, note.count - 1)
        }
    }
}
