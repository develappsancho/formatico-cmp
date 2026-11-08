package com.developersancho.formatico.feature.start

import com.pantrixui.foundation.date.PantrixDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** How long ago a document was edited, as Recent says it. */
internal sealed interface Recency {
    /** Less than a minute ago. */
    data object JustNow : Recency

    /** [minutes] minutes ago, under an hour. */
    data class MinutesAgo(val minutes: Int) : Recency

    /** Earlier today, at [hour]:[minute] on the reader's clock. */
    data class Today(val hour: Int, val minute: Int) : Recency

    /** Yesterday on the reader's clock. */
    data object Yesterday : Recency

    /** Before yesterday, on [date]. */
    data class OnDate(val date: PantrixDate, val thisYear: Boolean) : Recency
}

/** How long before [now] the moment [then] is, on a clock [utcOffset] ahead of UTC at each moment. */
internal fun recency(then: Instant, now: Instant, utcOffset: (Instant) -> Duration): Recency {
    val elapsed = now - then
    if (elapsed < 1.minutes) return Recency.JustNow
    if (elapsed < 1.hours) return Recency.MinutesAgo(elapsed.inWholeMinutes.toInt())
    val offset = utcOffset(then)
    val day = PantrixDate.of(then, offset)
    val today = PantrixDate.of(now, utcOffset(now))
    return when (today.epochDays - day.epochDays) {
        0L -> {
            val secondOfDay = (then + offset).epochSeconds.mod(SECONDS_IN_A_DAY)
            Recency.Today(
                hour = (secondOfDay / SECONDS_IN_AN_HOUR).toInt(),
                minute = (secondOfDay % SECONDS_IN_AN_HOUR / SECONDS_IN_A_MINUTE).toInt()
            )
        }

        1L -> {
            Recency.Yesterday
        }

        else -> {
            Recency.OnDate(date = day, thisYear = day.year == today.year)
        }
    }
}

private const val SECONDS_IN_A_DAY = 86_400L
private const val SECONDS_IN_AN_HOUR = 3_600L
private const val SECONDS_IN_A_MINUTE = 60L
