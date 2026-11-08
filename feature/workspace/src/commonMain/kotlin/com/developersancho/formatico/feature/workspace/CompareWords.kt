package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.diff.Change
import com.developersancho.formatico.core.format.json.jsonPath
import com.pantrixui.components.data.codeblock.CodeTone
import com.pantrixui.components.data.tag.TagTone
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.compare_change_added
import formatico.feature.workspace.generated.resources.compare_change_changed
import formatico.feature.workspace.generated.resources.compare_change_removed
import formatico.feature.workspace.generated.resources.compare_line
import formatico.feature.workspace.generated.resources.compare_lines
import formatico.feature.workspace.generated.resources.compare_moved
import formatico.feature.workspace.generated.resources.compare_moved_pair
import formatico.feature.workspace.generated.resources.compare_not_counted_both
import formatico.feature.workspace.generated.resources.compare_respelled_after
import formatico.feature.workspace.generated.resources.compare_respelled_alone
import formatico.feature.workspace.generated.resources.compare_respelled_more
import formatico.feature.workspace.generated.resources.compare_what
import formatico.feature.workspace.generated.resources.workspace_untitled
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The mark a change is drawn with, beside its row and its line's number: `~`, `+` or `−`. */
internal val Change.mark: String
    get() = when (this) {
        Change.Changed -> "~"
        Change.Added -> "+"
        Change.Removed -> "−"
    }

/** The tone of a change's mark and of its lines: a warning's, a success's or a danger's. */
internal val Change.tagTone: TagTone
    get() = when (this) {
        Change.Changed -> TagTone.Warning
        Change.Added -> TagTone.Success
        Change.Removed -> TagTone.Danger
    }

/** The tone of a change's lines in the text. */
internal val Change.codeTone: CodeTone
    get() = when (this) {
        Change.Changed -> CodeTone.Warning
        Change.Added -> CodeTone.Success
        Change.Removed -> CodeTone.Danger
    }

/** What a screen reader calls a change. */
@Composable
internal fun changeName(change: Change): String = stringResource(
    when (change) {
        Change.Changed -> Res.string.compare_change_changed
        Change.Added -> Res.string.compare_change_added
        Change.Removed -> Res.string.compare_change_removed
    }
)

/** Where [difference] is: its value's path, or the lines it runs over, in A where it has any and in B otherwise. */
@Composable
internal fun differenceWhere(difference: ShownDifference): String {
    difference.path?.let { return jsonPath(it) }
    val lines = difference.aLines.takeUnless { it.isEmpty() } ?: difference.bLines
    val first = lines.first + 1
    val last = lines.last + 1
    return if (last <= first) stringResource(Res.string.compare_line, first) else stringResource(Res.string.compare_lines, first, last)
}

/** What [difference] is: A's value against B's, or the one that is there. */
@Composable
internal fun differenceWhat(difference: ShownDifference): String {
    val before = difference.before
    val after = difference.after
    return when {
        before != null && after != null -> stringResource(Res.string.compare_what, before, after)
        else -> before ?: after.orEmpty()
    }
}

/** The name of a document compared: its file's, or "Untitled". */
@Composable
internal fun comparedName(document: ComparedDocument): String = document.name ?: stringResource(Res.string.workspace_untitled)

/**
 * What a comparison by meaning did not count as a difference, as a sentence or two: the keys that moved and the numbers
 * written another way; null where there was neither.
 */
@Composable
internal fun notCountedMessage(compared: CompareOutcome.Compared): String? {
    val moved = compared.moved?.let { moved ->
        val other = moved.other
        if (other != null && moved.count == 1) {
            stringResource(Res.string.compare_moved_pair, moved.first, other)
        } else {
            pluralStringResource(Res.plurals.compare_moved, moved.count - 1, moved.first, moved.count - 1)
        }
    }
    val respelled = compared.respelled?.let { numbers ->
        when {
            moved == null -> stringResource(Res.string.compare_respelled_alone, numbers.first, numbers.second)

            numbers.count == 1 -> stringResource(Res.string.compare_respelled_after, numbers.first, numbers.second)

            else -> pluralStringResource(
                Res.plurals.compare_respelled_more,
                numbers.count - 1,
                numbers.first,
                numbers.second,
                numbers.count - 1
            )
        }
    }
    return when {
        moved != null && respelled != null -> stringResource(Res.string.compare_not_counted_both, moved, respelled)
        else -> moved ?: respelled
    }
}
