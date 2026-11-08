package com.developersancho.formatico.server.share

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * How many shares an address may leave in a day: [perDay], from midnight to midnight in UTC, by [clock]. The counts are
 * in memory alone, never logged or written down, and start again each day; a server started again starts them again.
 *
 * At most [maxAddresses] addresses are counted in a day: past that, a new address leaves none until the next, so that
 * many addresses at once cannot fill the server's memory. The disk has its own limit ([ShareStore]).
 */
class ShareQuota(
    private val perDay: Int,
    private val clock: Clock,
    private val maxAddresses: Int = MAX_ADDRESSES
) {
    private val counts = HashMap<String, Int>()
    private var day: LocalDate = today()

    /** The most shares an address leaves in a day. */
    val limit: Int get() = perDay

    /** Takes one of [address]'s shares of the day: false where it has left [perDay] of them today. */
    @Synchronized
    fun take(address: String): Boolean {
        startTheDay()
        val count = counts[address] ?: 0
        if (count >= perDay || (count == 0 && counts.size >= maxAddresses)) return false
        counts[address] = count + 1
        return true
    }

    /** Gives [address] back a share [take] took for it, which was not kept after all. */
    @Synchronized
    fun giveBack(address: String) {
        startTheDay()
        val count = counts[address] ?: return
        if (count <= 1) counts.remove(address) else counts[address] = count - 1
    }

    /** When the counts start again: the next midnight, in UTC. */
    fun nextDay(): Instant = today().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()

    private fun startTheDay() {
        val today = today()
        if (today != day) {
            day = today
            counts.clear()
        }
    }

    private fun today(): LocalDate = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC)

    private companion object {
        /** A day's addresses at the most: a few megabytes of memory. */
        const val MAX_ADDRESSES = 100_000
    }
}
