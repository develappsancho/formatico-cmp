package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.Loss
import com.developersancho.formatico.core.format.convert.NullHandling
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.convert_loss_aliases_many
import formatico.feature.workspace.generated.resources.convert_loss_aliases_one
import formatico.feature.workspace.generated.resources.convert_loss_aliases_title
import formatico.feature.workspace.generated.resources.convert_loss_comments
import formatico.feature.workspace.generated.resources.convert_loss_comments_title
import formatico.feature.workspace.generated.resources.convert_loss_dates_many
import formatico.feature.workspace.generated.resources.convert_loss_dates_one
import formatico.feature.workspace.generated.resources.convert_loss_dates_title
import formatico.feature.workspace.generated.resources.convert_loss_duplicates_many
import formatico.feature.workspace.generated.resources.convert_loss_duplicates_one
import formatico.feature.workspace.generated.resources.convert_loss_duplicates_title
import formatico.feature.workspace.generated.resources.convert_loss_emptied_many
import formatico.feature.workspace.generated.resources.convert_loss_emptied_one
import formatico.feature.workspace.generated.resources.convert_loss_emptied_title
import formatico.feature.workspace.generated.resources.convert_loss_flattened_many
import formatico.feature.workspace.generated.resources.convert_loss_flattened_one
import formatico.feature.workspace.generated.resources.convert_loss_flattened_title
import formatico.feature.workspace.generated.resources.convert_loss_in_array
import formatico.feature.workspace.generated.resources.convert_loss_items_many
import formatico.feature.workspace.generated.resources.convert_loss_items_one
import formatico.feature.workspace.generated.resources.convert_loss_items_title
import formatico.feature.workspace.generated.resources.convert_loss_line_title
import formatico.feature.workspace.generated.resources.convert_loss_moved_many
import formatico.feature.workspace.generated.resources.convert_loss_moved_one
import formatico.feature.workspace.generated.resources.convert_loss_moved_title
import formatico.feature.workspace.generated.resources.convert_loss_nulls_empty_many
import formatico.feature.workspace.generated.resources.convert_loss_nulls_empty_one
import formatico.feature.workspace.generated.resources.convert_loss_nulls_left_many
import formatico.feature.workspace.generated.resources.convert_loss_nulls_left_one
import formatico.feature.workspace.generated.resources.convert_loss_nulls_title
import formatico.feature.workspace.generated.resources.convert_loss_one_line_many
import formatico.feature.workspace.generated.resources.convert_loss_one_line_one
import formatico.feature.workspace.generated.resources.convert_loss_one_line_title
import formatico.feature.workspace.generated.resources.convert_loss_renamed_many
import formatico.feature.workspace.generated.resources.convert_loss_renamed_one
import formatico.feature.workspace.generated.resources.convert_loss_renamed_title
import formatico.feature.workspace.generated.resources.convert_loss_respelled_many
import formatico.feature.workspace.generated.resources.convert_loss_respelled_one
import formatico.feature.workspace.generated.resources.convert_loss_respelled_title
import formatico.feature.workspace.generated.resources.convert_loss_row_title
import formatico.feature.workspace.generated.resources.convert_loss_untyped_many
import formatico.feature.workspace.generated.resources.convert_loss_untyped_one
import formatico.feature.workspace.generated.resources.convert_loss_untyped_title
import formatico.feature.workspace.generated.resources.convert_loss_unwritable_many
import formatico.feature.workspace.generated.resources.convert_loss_unwritable_one
import formatico.feature.workspace.generated.resources.convert_loss_unwritable_title
import formatico.feature.workspace.generated.resources.convert_loss_wrapped
import formatico.feature.workspace.generated.resources.convert_loss_wrapped_title
import formatico.feature.workspace.generated.resources.convert_tag_aliases
import formatico.feature.workspace.generated.resources.convert_tag_comments
import formatico.feature.workspace.generated.resources.convert_tag_dates
import formatico.feature.workspace.generated.resources.convert_tag_duplicates
import formatico.feature.workspace.generated.resources.convert_tag_emptied
import formatico.feature.workspace.generated.resources.convert_tag_flattened
import formatico.feature.workspace.generated.resources.convert_tag_items
import formatico.feature.workspace.generated.resources.convert_tag_line
import formatico.feature.workspace.generated.resources.convert_tag_moved
import formatico.feature.workspace.generated.resources.convert_tag_nulls_empty
import formatico.feature.workspace.generated.resources.convert_tag_nulls_left
import formatico.feature.workspace.generated.resources.convert_tag_one_line
import formatico.feature.workspace.generated.resources.convert_tag_renamed
import formatico.feature.workspace.generated.resources.convert_tag_respelled
import formatico.feature.workspace.generated.resources.convert_tag_row
import formatico.feature.workspace.generated.resources.convert_tag_untyped
import formatico.feature.workspace.generated.resources.convert_tag_unwritable
import formatico.feature.workspace.generated.resources.convert_tag_wrapped
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The loss's key among the alerts' tags, the same in every language. */
internal val Loss.key: String
    get() = when (this) {
        is Loss.Nulls -> "nulls"
        is Loss.Comments -> "comments"
        is Loss.Aliases -> "aliases"
        is Loss.Untyped -> "untyped"
        is Loss.Flattened -> "flattened"
        is Loss.Wrapped -> "wrapped"
        is Loss.Renamed -> "renamed"
        is Loss.Duplicates -> "duplicates"
        is Loss.Respelled -> "respelled"
        is Loss.Unwritable -> "unwritable"
        is Loss.Dates -> "dates"
        is Loss.Moved -> "moved"
        is Loss.Emptied -> "emptied"
        is Loss.OneLine -> "one_line"
        is Loss.ItemsNamed -> "items_named"
    }

/** What an alert says of a loss, [title] and [message], and the status bar's tag of it, [tag]. */
internal data class LossWords(val title: String, val message: String, val tag: String)

/** The words of [loss], found converting a document of [from] to [to]. */
@Composable
internal fun lossWords(loss: Loss, from: Format, to: Format): LossWords {
    val source = from.displayName
    val target = to.displayName
    return when (loss) {
        is Loss.Nulls -> nullWords(loss, source, target)

        is Loss.Comments -> LossWords(
            title = stringResource(Res.string.convert_loss_comments_title),
            message = pluralStringResource(Res.plurals.convert_loss_comments, loss.count, source, loss.count, target),
            tag = tag(Res.plurals.convert_tag_comments, loss.count)
        )

        is Loss.Aliases -> counted(
            Res.string.convert_loss_aliases_title,
            one = stringResource(Res.string.convert_loss_aliases_one, loss.first),
            many = { pluralStringResource(Res.plurals.convert_loss_aliases_many, it, loss.first, it) },
            tag = Res.plurals.convert_tag_aliases,
            count = loss.count
        )

        is Loss.Untyped -> counted(
            stringResource(Res.string.convert_loss_untyped_title, target),
            one = stringResource(Res.string.convert_loss_untyped_one, loss.first),
            many = { pluralStringResource(Res.plurals.convert_loss_untyped_many, it, loss.first, it) },
            tag = Res.plurals.convert_tag_untyped,
            count = loss.count
        )

        is Loss.Flattened -> counted(
            stringResource(Res.string.convert_loss_flattened_title, target),
            one = stringResource(Res.string.convert_loss_flattened_one, loss.first),
            many = { pluralStringResource(Res.plurals.convert_loss_flattened_many, it, loss.first, it) },
            tag = Res.plurals.convert_tag_flattened,
            count = loss.count
        )

        is Loss.Wrapped -> wrappedWords(loss, to, source, target)

        else -> structureWords(loss, source, target)
    }
}

/** The words of the losses that are about how the new document is put together, past the first few. */
@Composable
private fun structureWords(loss: Loss, source: String, target: String): LossWords = when (loss) {
    is Loss.Renamed -> counted(
        Res.string.convert_loss_renamed_title,
        one = stringResource(Res.string.convert_loss_renamed_one, loss.first, target, loss.written),
        many = { pluralStringResource(Res.plurals.convert_loss_renamed_many, it, loss.first, it, target, loss.written) },
        tag = Res.plurals.convert_tag_renamed,
        count = loss.count
    )

    is Loss.Duplicates -> counted(
        Res.string.convert_loss_duplicates_title,
        one = stringResource(Res.string.convert_loss_duplicates_one, loss.first, target),
        many = { pluralStringResource(Res.plurals.convert_loss_duplicates_many, it, loss.first, it, target) },
        tag = Res.plurals.convert_tag_duplicates,
        count = loss.count
    )

    is Loss.Moved -> counted(
        Res.string.convert_loss_moved_title,
        one = stringResource(Res.string.convert_loss_moved_one, target, loss.first),
        many = { pluralStringResource(Res.plurals.convert_loss_moved_many, it, target, loss.first, it) },
        tag = Res.plurals.convert_tag_moved,
        count = loss.count
    )

    is Loss.Emptied -> counted(
        Res.string.convert_loss_emptied_title,
        one = stringResource(Res.string.convert_loss_emptied_one, loss.first, target),
        many = { pluralStringResource(Res.plurals.convert_loss_emptied_many, it, loss.first, it, target) },
        tag = Res.plurals.convert_tag_emptied,
        count = loss.count
    )

    is Loss.ItemsNamed -> counted(
        Res.string.convert_loss_items_title,
        one = stringResource(Res.string.convert_loss_items_one, loss.first),
        many = { pluralStringResource(Res.plurals.convert_loss_items_many, it, loss.first, it) },
        tag = Res.plurals.convert_tag_items,
        count = loss.count
    )

    else -> valueWords(loss, source, target)
}

/** The words of the losses that are about how single values are written. */
@Composable
private fun valueWords(loss: Loss, source: String, target: String): LossWords = when (loss) {
    is Loss.Respelled -> counted(
        Res.string.convert_loss_respelled_title,
        one = stringResource(Res.string.convert_loss_respelled_one, loss.first, loss.written, target),
        many = { pluralStringResource(Res.plurals.convert_loss_respelled_many, it, loss.first, it, target, loss.written) },
        tag = Res.plurals.convert_tag_respelled,
        count = loss.count
    )

    is Loss.Unwritable -> counted(
        Res.string.convert_loss_unwritable_title,
        one = stringResource(Res.string.convert_loss_unwritable_one, loss.first, target),
        many = { pluralStringResource(Res.plurals.convert_loss_unwritable_many, it, loss.first, it, target) },
        tag = Res.plurals.convert_tag_unwritable,
        count = loss.count
    )

    is Loss.Dates -> counted(
        Res.string.convert_loss_dates_title,
        one = stringResource(Res.string.convert_loss_dates_one, loss.first, source, target),
        many = { pluralStringResource(Res.plurals.convert_loss_dates_many, it, loss.first, it, target) },
        tag = Res.plurals.convert_tag_dates,
        count = loss.count
    )

    is Loss.OneLine -> counted(
        Res.string.convert_loss_one_line_title,
        one = stringResource(Res.string.convert_loss_one_line_one, loss.first, target),
        many = { pluralStringResource(Res.plurals.convert_loss_one_line_many, it, loss.first, it, target) },
        tag = Res.plurals.convert_tag_one_line,
        count = loss.count
    )

    // The kinds above are every kind there is but those [lossWords] says itself.
    else -> LossWords(title = "", message = "", tag = "")
}

@Composable
private fun nullWords(loss: Loss.Nulls, source: String, target: String): LossWords {
    val left = loss.handling == NullHandling.LeaveOut
    return counted(
        stringResource(Res.string.convert_loss_nulls_title, target),
        one = stringResource(
            if (left) Res.string.convert_loss_nulls_left_one else Res.string.convert_loss_nulls_empty_one,
            loss.first,
            source
        ),
        many = {
            val words = if (left) Res.plurals.convert_loss_nulls_left_many else Res.plurals.convert_loss_nulls_empty_many
            pluralStringResource(words, it, loss.first, it, source)
        },
        tag = if (left) Res.plurals.convert_tag_nulls_left else Res.plurals.convert_tag_nulls_empty,
        count = loss.count
    )
}

/**
 * The words of the document put under a name of its own: under a key, or, in a format that is a list of rows or of
 * lines, in an array.
 */
@Composable
private fun wrappedWords(loss: Loss.Wrapped, to: Format, source: String, target: String): LossWords = when (to) {
    Format.Csv, Format.Tsv, Format.Ndjson -> LossWords(
        title = stringResource(if (to == Format.Ndjson) Res.string.convert_loss_line_title else Res.string.convert_loss_row_title),
        message = stringResource(Res.string.convert_loss_in_array, target, source),
        tag = stringResource(if (to == Format.Ndjson) Res.string.convert_tag_line else Res.string.convert_tag_row)
    )

    else -> LossWords(
        title = stringResource(Res.string.convert_loss_wrapped_title, loss.name),
        message = stringResource(Res.string.convert_loss_wrapped, target, source, loss.name),
        tag = stringResource(Res.string.convert_tag_wrapped, loss.name)
    )
}

/** The words of a loss about [count] values: [one]'s message for one, and [many]'s, given how many more, for more. */
@Composable
private fun counted(
    title: StringResource,
    one: String,
    many: @Composable (more: Int) -> String,
    tag: PluralStringResource,
    count: Int
): LossWords = counted(stringResource(title), one, many, tag, count)

@Composable
private fun counted(
    title: String,
    one: String,
    many: @Composable (more: Int) -> String,
    tag: PluralStringResource,
    count: Int
): LossWords = LossWords(title = title, message = if (count == 1) one else many(count - 1), tag = tag(tag, count))

@Composable
private fun tag(words: PluralStringResource, count: Int): String = pluralStringResource(words, count, count)
