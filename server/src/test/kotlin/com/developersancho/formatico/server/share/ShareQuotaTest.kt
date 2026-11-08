package com.developersancho.formatico.server.share

import com.developersancho.formatico.server.MutableClock
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** How many shares an address leaves a day. */
class ShareQuotaTest {
    private val clock = MutableClock(Instant.parse("2026-11-07T23:59:00Z"))

    @Test
    fun takesAnAddresssSharesUpToTheLimitAndNoMore() {
        val quota = ShareQuota(perDay = 3, clock = clock)
        repeat(3) { assertTrue(quota.take("203.0.113.7")) }
        assertFalse(quota.take("203.0.113.7"))
        assertTrue(quota.take("203.0.113.8"))
        assertEquals(3, quota.limit)
    }

    @Test
    fun startsAgainAtMidnightInUtc() {
        val quota = ShareQuota(perDay = 1, clock = clock)
        assertTrue(quota.take("203.0.113.7"))
        assertFalse(quota.take("203.0.113.7"))
        assertEquals(Instant.parse("2026-11-08T00:00:00Z"), quota.nextDay())
        clock.advance(Duration.ofMinutes(1))
        assertTrue(quota.take("203.0.113.7"))
        assertEquals(Instant.parse("2026-11-09T00:00:00Z"), quota.nextDay())
    }

    @Test
    fun takesBackOnlyWhatItGave() {
        val quota = ShareQuota(perDay = 2, clock = clock)
        quota.giveBack("203.0.113.7")
        assertTrue(quota.take("203.0.113.7"))
        assertTrue(quota.take("203.0.113.7"))
        quota.giveBack("203.0.113.7")
        assertTrue(quota.take("203.0.113.7"))
        assertFalse(quota.take("203.0.113.7"))
    }

    @Test
    fun countsNoMoreAddressesADayThanItKeepsRoomFor() {
        val quota = ShareQuota(perDay = 5, clock = clock, maxAddresses = 2)
        assertTrue(quota.take("203.0.113.1"))
        assertTrue(quota.take("203.0.113.2"))
        assertFalse(quota.take("203.0.113.3"))
        // An address counted already still leaves its shares.
        assertTrue(quota.take("203.0.113.1"))
        clock.advance(Duration.ofMinutes(1))
        assertTrue(quota.take("203.0.113.3"))
    }
}
