package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.Loss
import com.developersancho.formatico.core.format.convert.Note
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_back
import formatico.feature.workspace.generated.resources.convert_back_with
import formatico.feature.workspace.generated.resources.convert_losses
import formatico.feature.workspace.generated.resources.convert_note_quoted_dates
import formatico.feature.workspace.generated.resources.convert_note_quoted_values
import formatico.feature.workspace.generated.resources.convert_note_spelling_more
import formatico.feature.workspace.generated.resources.convert_note_spelling_one
import formatico.feature.workspace.generated.resources.convert_note_spelling_two
import formatico.feature.workspace.generated.resources.convert_notes_and
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The status bar's tag of [losses]: the one loss's own, or how many there are. */
@Composable
internal fun lossesTag(losses: List<Loss>, from: Format, to: Format): String = when (losses.size) {
    1 -> lossWords(losses.single(), from, to).tag
    else -> pluralStringResource(Res.plurals.convert_losses, losses.size, losses.size)
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
    if (notes.isEmpty()) return stringResource(Res.string.convert_back, source)
    // A conversion does two things at most for it: quotes strings, and keeps the spelling of numbers.
    val said = if (notes.size == 1) notes.single() else stringResource(Res.string.convert_notes_and, notes[0], notes[1])
    return stringResource(Res.string.convert_back_with, said, target, source)
}

@Composable
private fun noteWords(note: Note, target: String): String = when (note) {
    is Note.Quoted -> stringResource(
        if (note.dates) Res.string.convert_note_quoted_dates else Res.string.convert_note_quoted_values,
        target
    )

    is Note.Spelling -> {
        val second = note.second
        when {
            note.count == 1 -> stringResource(Res.string.convert_note_spelling_one, note.first)
            note.count == 2 && second != null -> stringResource(Res.string.convert_note_spelling_two, note.first, second)
            else -> pluralStringResource(Res.plurals.convert_note_spelling_more, note.count - 1, note.first, note.count - 1)
        }
    }
}
