package com.developersancho.formatico.feature.start

import com.pantrixui.foundation.date.PantrixDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RecencyTest {
    private val istanbul: (Instant) -> Duration = { 3.hours }
    private val newYork: (Instant) -> Duration = { (-5).hours }

    // 09:41 in Istanbul, 01:41 in New York.
    private val now = Instant.parse("2026-11-03T06:41:00Z")

    @Test
    fun underAMinuteIsJustNow() {
        assertEquals(Recency.JustNow, recency(now - 30.seconds, now, istanbul))
    }

    @Test
    fun underAnHourIsCountedInMinutes() {
        assertEquals(Recency.MinutesAgo(2), recency(now - 2.minutes, now, istanbul))
        assertEquals(Recency.MinutesAgo(59), recency(now - 59.minutes, now, istanbul))
    }

    @Test
    fun earlierTheSameDayIsItsTimeOnTheReadersClock() {
        assertEquals(Recency.Today(hour = 7, minute = 5), recency(Instant.parse("2026-11-03T04:05:00Z"), now, istanbul))
    }

    @Test
    fun theDayIsTheReadersOwn() {
        val then = Instant.parse("2026-11-03T04:05:00Z")
        // 07:05 today in Istanbul is 23:05 yesterday in New York.
        assertEquals(Recency.Today(hour = 7, minute = 5), recency(then, now, istanbul))
        assertEquals(Recency.Yesterday, recency(then, now, newYork))
    }

    @Test
    fun beforeYesterdayIsADate() {
        assertEquals(
            Recency.OnDate(PantrixDate(2026, 10, 29), thisYear = true),
            recency(Instant.parse("2026-10-29T12:00:00Z"), now, istanbul)
        )
        assertEquals(
            Recency.OnDate(PantrixDate(2025, 12, 31), thisYear = false),
            recency(Instant.parse("2025-12-31T12:00:00Z"), now, istanbul)
        )
    }
}
