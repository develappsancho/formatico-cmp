package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.developersancho.formatico.core.format.json.jsonPath
import com.developersancho.formatico.feature.workspace.strings.WorkspaceStrings
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.date.PantrixDate
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.l10n.PantrixStrings
import com.pantrixui.foundation.theme.PantrixTheme
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

/**
 * The words the tree's rows end in, for a string that looks like something more: a date in the reader's language, or
 * what kind of thing it is. Read once, for every row.
 */
internal data class PreviewWords(
    private val strings: PantrixStrings,
    private val words: WorkspaceStrings,
    private val colour: String,
    private val link: String,
    private val picture: String
) {
    fun of(preview: ValuePreview): String = when (preview) {
        is ValuePreview.Moment -> {
            val reading = preview.instant.on(Duration.ZERO)
            val date = reading.date
            words.momentUtc(strings.date(date.day, date.month, date.year), strings.clock(reading.hour, reading.minute))
        }

        is ValuePreview.Day -> {
            strings.date(preview.date.day, preview.date.month, preview.date.year)
        }

        is ValuePreview.Colour -> {
            colour
        }

        is ValuePreview.Link -> {
            link
        }

        is ValuePreview.Picture -> {
            picture
        }
    }
}

@Composable
internal fun previewWords(): PreviewWords {
    val strings = PantrixTheme.strings
    val words = workspaceStrings
    val colour = workspaceStrings.kindColour
    val link = workspaceStrings.kindLink
    val picture = workspaceStrings.kindPicture
    return remember(strings, words, colour, link, picture) { PreviewWords(strings, words, colour, link, picture) }
}

/**
 * A value read out for its card: what kind of thing it is, with an icon for a date, the value itself, and what more
 * there is to say of it.
 */
internal class ValueReading(val kind: String, val icon: ImageVector?, val headline: String, val detail: String?)

@Composable
internal fun readValue(node: ValueNode): ValueReading {
    val strings = PantrixTheme.strings
    // A date is written with its month's name, as the boards' card does; its day of the week is said under it.
    return when (val preview = node.preview) {
        is ValuePreview.Moment -> {
            val clock = LocalPantrixClock.current
            val utc = preview.instant.on(Duration.ZERO)
            val local = preview.instant.on(clock.utcOffset(preview.instant))
            val headline = workspaceStrings.momentUtc(
                strings.fullDate(utc.date.day, utc.date.month, utc.date.year),
                strings.clock(utc.hour, utc.minute)
            )
            val detail = workspaceStrings.separated(
                dayDetail(local.date),
                workspaceStrings.momentLocal(strings.clock(local.hour, local.minute))
            )
            ValueReading(workspaceStrings.kindDate, PantrixIcons.Calendar, headline, detail)
        }

        is ValuePreview.Day -> {
            ValueReading(
                kind = workspaceStrings.kindDate,
                icon = PantrixIcons.Calendar,
                headline = strings.fullDate(preview.date.day, preview.date.month, preview.date.year),
                detail = dayDetail(preview.date)
            )
        }

        is ValuePreview.Colour -> {
            ValueReading(
                workspaceStrings.kindColour,
                null,
                node.value.orEmpty(),
                null
            )
        }

        is ValuePreview.Link -> {
            ValueReading(workspaceStrings.kindLink, null, preview.url, null)
        }

        is ValuePreview.Picture -> {
            ValueReading(
                workspaceStrings.kindPicture,
                null,
                preview.url,
                null
            )
        }

        null -> {
            plainReading(node)
        }
    }
}

@Composable
private fun plainReading(node: ValueNode): ValueReading {
    val count = node.children.size
    return when (node.kind) {
        ValueKind.Object -> {
            ValueReading(
                kind = workspaceStrings.kindObject,
                icon = null,
                headline = workspaceStrings.keys(count, count),
                detail = null
            )
        }

        ValueKind.Array -> {
            ValueReading(
                kind = workspaceStrings.kindArray,
                icon = null,
                headline = workspaceStrings.items(count, count),
                detail = null
            )
        }

        ValueKind.String -> {
            val value = node.value.orEmpty()
            ValueReading(
                kind = workspaceStrings.kindString,
                icon = null,
                headline = value,
                detail = workspaceStrings.characters(value.length, value.length)
            )
        }

        ValueKind.Number -> {
            ValueReading(workspaceStrings.kindNumber, null, node.text, null)
        }

        ValueKind.Boolean -> {
            ValueReading(workspaceStrings.kindBoolean, null, node.text, null)
        }

        ValueKind.Null -> {
            ValueReading(workspaceStrings.kindNull, null, node.text, null)
        }

        ValueKind.Alias -> {
            val anchor = node.text.removePrefix("*")
            val target = node.target
            ValueReading(
                kind = workspaceStrings.kindAlias,
                icon = null,
                headline = target?.let { jsonPath(it.path) } ?: node.text,
                detail = if (target != null) workspaceStrings.aliasDetail(anchor) else workspaceStrings.aliasMissing(anchor)
            )
        }
    }
}

/** `A Wednesday, 5 days ago`: [date]'s day of the week, and how far it is from the reader's today. */
@Composable
private fun dayDetail(date: PantrixDate): String {
    val clock = LocalPantrixClock.current
    val now = clock.clock.now()
    val today = PantrixDate.of(now, clock.utcOffset(now))
    val days = (today.epochDays - date.epochDays).toInt()
    val distance = when {
        days == 0 -> workspaceStrings.today
        days == 1 -> workspaceStrings.yesterday
        days == -1 -> workspaceStrings.tomorrow
        days > 1 -> workspaceStrings.daysAgo(days, days)
        else -> workspaceStrings.daysAhead(-days, -days)
    }
    return workspaceStrings.dayOfTheWeek(PantrixTheme.strings.weekday(date.dayOfWeek), distance)
}
