package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.date.PantrixDate
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.l10n.PantrixStrings
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.workspace.generated.resources.Res
import formatico.feature.workspace.generated.resources.workspace_characters
import formatico.feature.workspace.generated.resources.workspace_days_ago
import formatico.feature.workspace.generated.resources.workspace_days_ahead
import formatico.feature.workspace.generated.resources.workspace_items
import formatico.feature.workspace.generated.resources.workspace_keys
import formatico.feature.workspace.generated.resources.workspace_kind_array
import formatico.feature.workspace.generated.resources.workspace_kind_boolean
import formatico.feature.workspace.generated.resources.workspace_kind_colour
import formatico.feature.workspace.generated.resources.workspace_kind_date
import formatico.feature.workspace.generated.resources.workspace_kind_link
import formatico.feature.workspace.generated.resources.workspace_kind_null
import formatico.feature.workspace.generated.resources.workspace_kind_number
import formatico.feature.workspace.generated.resources.workspace_kind_object
import formatico.feature.workspace.generated.resources.workspace_kind_picture
import formatico.feature.workspace.generated.resources.workspace_kind_string
import formatico.feature.workspace.generated.resources.workspace_moment_local
import formatico.feature.workspace.generated.resources.workspace_moment_utc
import formatico.feature.workspace.generated.resources.workspace_separated
import formatico.feature.workspace.generated.resources.workspace_today
import formatico.feature.workspace.generated.resources.workspace_tomorrow
import formatico.feature.workspace.generated.resources.workspace_yesterday
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration
import kotlin.time.Instant

private const val SECONDS_IN_A_DAY = 86_400L
private const val SECONDS_IN_AN_HOUR = 3_600L
private const val SECONDS_IN_A_MINUTE = 60L

/** A moment's day and time on a clock [offset] ahead of UTC. */
private class ClockReading(val date: PantrixDate, val hour: Int, val minute: Int)

private fun Instant.on(offset: Duration): ClockReading {
    val secondOfDay = (this + offset).epochSeconds.mod(SECONDS_IN_A_DAY)
    return ClockReading(
        date = PantrixDate.of(this, offset),
        hour = (secondOfDay / SECONDS_IN_AN_HOUR).toInt(),
        minute = (secondOfDay % SECONDS_IN_AN_HOUR / SECONDS_IN_A_MINUTE).toInt()
    )
}

/** [pattern] with its `%1$s`, `%2$s` filled by [arguments], as the resources' own formatting fills them. */
private fun fill(pattern: String, vararg arguments: String): String =
    arguments.foldIndexed(pattern) { index, text, argument -> text.replace("%${index + 1}\$s", argument) }

/**
 * The words the tree's rows end in, for a string that looks like something more: a date in the reader's language, or
 * what kind of thing it is. Read once, for every row.
 */
internal data class PreviewWords(
    private val strings: PantrixStrings,
    private val utc: String,
    private val colour: String,
    private val link: String,
    private val picture: String
) {
    fun of(preview: ValuePreview): String = when (preview) {
        is ValuePreview.Moment -> {
            val reading = preview.instant.on(Duration.ZERO)
            val date = reading.date
            fill(utc, strings.date(date.day, date.month, date.year), strings.clock(reading.hour, reading.minute))
        }

        is ValuePreview.Day -> strings.date(preview.date.day, preview.date.month, preview.date.year)

        is ValuePreview.Colour -> colour

        is ValuePreview.Link -> link

        is ValuePreview.Picture -> picture
    }
}

@Composable
internal fun previewWords(): PreviewWords {
    val strings = PantrixTheme.strings
    val utc = stringResource(Res.string.workspace_moment_utc)
    val colour = stringResource(Res.string.workspace_kind_colour)
    val link = stringResource(Res.string.workspace_kind_link)
    val picture = stringResource(Res.string.workspace_kind_picture)
    return remember(strings, utc, colour, link, picture) { PreviewWords(strings, utc, colour, link, picture) }
}

/**
 * A value read out for its card: what kind of thing it is, with an icon for a date, the value itself, and what more
 * there is to say of it.
 */
internal class ValueReading(val kind: String, val icon: ImageVector?, val headline: String, val detail: String?)

@Composable
internal fun readValue(node: ValueNode): ValueReading {
    val strings = PantrixTheme.strings
    return when (val preview = node.preview) {
        is ValuePreview.Moment -> {
            val clock = LocalPantrixClock.current
            val utc = preview.instant.on(Duration.ZERO)
            val local = preview.instant.on(clock.utcOffset(preview.instant))
            val headline = stringResource(
                Res.string.workspace_moment_utc,
                strings.longDate(utc.date.day, utc.date.month, utc.date.year),
                strings.clock(utc.hour, utc.minute)
            )
            val detail = stringResource(
                Res.string.workspace_separated,
                dayDetail(local.date),
                stringResource(Res.string.workspace_moment_local, strings.clock(local.hour, local.minute))
            )
            ValueReading(stringResource(Res.string.workspace_kind_date), PantrixIcons.Calendar, headline, detail)
        }

        is ValuePreview.Day -> ValueReading(
            kind = stringResource(Res.string.workspace_kind_date),
            icon = PantrixIcons.Calendar,
            headline = strings.longDate(preview.date.day, preview.date.month, preview.date.year),
            detail = dayDetail(preview.date)
        )

        is ValuePreview.Colour -> ValueReading(
            stringResource(Res.string.workspace_kind_colour),
            null,
            node.value.orEmpty(),
            null
        )

        is ValuePreview.Link -> ValueReading(stringResource(Res.string.workspace_kind_link), null, preview.url, null)

        is ValuePreview.Picture -> ValueReading(
            stringResource(Res.string.workspace_kind_picture),
            null,
            preview.url,
            null
        )

        null -> plainReading(node)
    }
}

@Composable
private fun plainReading(node: ValueNode): ValueReading {
    val count = node.children.size
    return when (node.kind) {
        ValueKind.Object -> ValueReading(
            kind = stringResource(Res.string.workspace_kind_object),
            icon = null,
            headline = pluralStringResource(Res.plurals.workspace_keys, count, count),
            detail = null
        )

        ValueKind.Array -> ValueReading(
            kind = stringResource(Res.string.workspace_kind_array),
            icon = null,
            headline = pluralStringResource(Res.plurals.workspace_items, count, count),
            detail = null
        )

        ValueKind.String -> {
            val value = node.value.orEmpty()
            ValueReading(
                kind = stringResource(Res.string.workspace_kind_string),
                icon = null,
                headline = value,
                detail = pluralStringResource(Res.plurals.workspace_characters, value.length, value.length)
            )
        }

        ValueKind.Number -> ValueReading(stringResource(Res.string.workspace_kind_number), null, node.text, null)

        ValueKind.Boolean -> ValueReading(stringResource(Res.string.workspace_kind_boolean), null, node.text, null)

        ValueKind.Null -> ValueReading(stringResource(Res.string.workspace_kind_null), null, node.text, null)
    }
}

/** `5 days ago`: how far [date] is from the reader's today. The long date before it says its day of the week. */
@Composable
private fun dayDetail(date: PantrixDate): String {
    val clock = LocalPantrixClock.current
    val now = clock.clock.now()
    val today = PantrixDate.of(now, clock.utcOffset(now))
    val days = (today.epochDays - date.epochDays).toInt()
    return when {
        days == 0 -> stringResource(Res.string.workspace_today)
        days == 1 -> stringResource(Res.string.workspace_yesterday)
        days == -1 -> stringResource(Res.string.workspace_tomorrow)
        days > 1 -> pluralStringResource(Res.plurals.workspace_days_ago, days, days)
        else -> pluralStringResource(Res.plurals.workspace_days_ahead, -days, -days)
    }
}
