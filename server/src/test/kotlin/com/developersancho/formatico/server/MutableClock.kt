package com.developersancho.formatico.server

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock a test moves: it says [now] until it is told otherwise. */
class MutableClock(var now: Instant) : Clock() {
    /** Moves the clock [by] forward. */
    fun advance(by: Duration) {
        now = now.plus(by)
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = now
}
