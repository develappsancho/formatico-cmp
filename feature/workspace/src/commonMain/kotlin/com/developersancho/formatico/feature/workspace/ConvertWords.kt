package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.developersancho.formatico.core.format.Format
import com.developersancho.formatico.core.format.convert.Loss
import com.developersancho.formatico.core.format.convert.NullHandling
import com.developersancho.formatico.feature.workspace.strings.convertStrings

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
            title = convertStrings.lossCommentsTitle,
            message = convertStrings.lossComments(loss.count, source, loss.count, target),
            tag = convertStrings.tagComments(loss.count, loss.count)
        )

        is Loss.Aliases -> counted(
            convertStrings.lossAliasesTitle,
            one = convertStrings.lossAliasesOne(loss.first),
            many = { convertStrings.lossAliasesMany(it, loss.first, it) },
            tag = convertStrings::tagAliases,
            count = loss.count
        )

        is Loss.Untyped -> counted(
            convertStrings.lossUntypedTitle(target),
            one = convertStrings.lossUntypedOne(loss.first),
            many = { convertStrings.lossUntypedMany(it, loss.first, it) },
            tag = convertStrings::tagUntyped,
            count = loss.count
        )

        is Loss.Flattened -> counted(
            convertStrings.lossFlattenedTitle(target),
            one = convertStrings.lossFlattenedOne(loss.first),
            many = { convertStrings.lossFlattenedMany(it, loss.first, it) },
            tag = convertStrings::tagFlattened,
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
        convertStrings.lossRenamedTitle,
        one = convertStrings.lossRenamedOne(loss.first, target, loss.written),
        many = { convertStrings.lossRenamedMany(it, loss.first, it, target, loss.written) },
        tag = convertStrings::tagRenamed,
        count = loss.count
    )

    is Loss.Duplicates -> counted(
        convertStrings.lossDuplicatesTitle,
        one = convertStrings.lossDuplicatesOne(loss.first, target),
        many = { convertStrings.lossDuplicatesMany(it, loss.first, it, target) },
        tag = convertStrings::tagDuplicates,
        count = loss.count
    )

    is Loss.Moved -> counted(
        convertStrings.lossMovedTitle,
        one = convertStrings.lossMovedOne(target, loss.first),
        many = { convertStrings.lossMovedMany(it, target, loss.first, it) },
        tag = convertStrings::tagMoved,
        count = loss.count
    )

    is Loss.Emptied -> counted(
        convertStrings.lossEmptiedTitle,
        one = convertStrings.lossEmptiedOne(loss.first, target),
        many = { convertStrings.lossEmptiedMany(it, loss.first, it, target) },
        tag = convertStrings::tagEmptied,
        count = loss.count
    )

    is Loss.ItemsNamed -> counted(
        convertStrings.lossItemsTitle,
        one = convertStrings.lossItemsOne(loss.first),
        many = { convertStrings.lossItemsMany(it, loss.first, it) },
        tag = convertStrings::tagItems,
        count = loss.count
    )

    else -> valueWords(loss, source, target)
}

/** The words of the losses that are about how single values are written. */
@Composable
private fun valueWords(loss: Loss, source: String, target: String): LossWords = when (loss) {
    is Loss.Respelled -> counted(
        convertStrings.lossRespelledTitle,
        one = convertStrings.lossRespelledOne(loss.first, loss.written, target),
        many = { convertStrings.lossRespelledMany(it, loss.first, it, target, loss.written) },
        tag = convertStrings::tagRespelled,
        count = loss.count
    )

    is Loss.Unwritable -> counted(
        convertStrings.lossUnwritableTitle,
        one = convertStrings.lossUnwritableOne(loss.first, target),
        many = { convertStrings.lossUnwritableMany(it, loss.first, it, target) },
        tag = convertStrings::tagUnwritable,
        count = loss.count
    )

    is Loss.Dates -> counted(
        convertStrings.lossDatesTitle,
        one = convertStrings.lossDatesOne(loss.first, source, target),
        many = { convertStrings.lossDatesMany(it, loss.first, it, target) },
        tag = convertStrings::tagDates,
        count = loss.count
    )

    is Loss.OneLine -> counted(
        convertStrings.lossOneLineTitle,
        one = convertStrings.lossOneLineOne(loss.first, target),
        many = { convertStrings.lossOneLineMany(it, loss.first, it, target) },
        tag = convertStrings::tagOneLine,
        count = loss.count
    )

    // The kinds above are every kind there is but those [lossWords] says itself.
    else -> LossWords(title = "", message = "", tag = "")
}

@Composable
private fun nullWords(loss: Loss.Nulls, source: String, target: String): LossWords {
    val left = loss.handling == NullHandling.LeaveOut
    return counted(
        convertStrings.lossNullsTitle(target),
        one = if (left) convertStrings.lossNullsLeftOne(loss.first, source) else convertStrings.lossNullsEmptyOne(loss.first, source),
        many = {
            val words = if (left) convertStrings::lossNullsLeftMany else convertStrings::lossNullsEmptyMany
            words(it, loss.first, it, source)
        },
        tag = if (left) convertStrings::tagNullsLeft else convertStrings::tagNullsEmpty,
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
        title = if (to == Format.Ndjson) convertStrings.lossLineTitle else convertStrings.lossRowTitle,
        message = convertStrings.lossInArray(target, source),
        tag = if (to == Format.Ndjson) convertStrings.tagLine else convertStrings.tagRow
    )

    else -> LossWords(
        title = convertStrings.lossWrappedTitle(loss.name),
        message = convertStrings.lossWrapped(target, source, loss.name),
        tag = convertStrings.tagWrapped(loss.name)
    )
}

/** The words of a loss about [count] values: [one]'s message for one, and [many]'s, given how many more, for more. */
@Composable
private fun counted(
    title: String,
    one: String,
    many: @Composable (more: Int) -> String,
    tag: (count: Int, first: Int) -> String,
    count: Int
): LossWords = LossWords(title = title, message = if (count == 1) one else many(count - 1), tag = tag(count, count))
