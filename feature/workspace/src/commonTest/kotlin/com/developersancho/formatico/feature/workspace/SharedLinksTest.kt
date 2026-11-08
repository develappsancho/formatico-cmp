package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.storage.MemorySmallStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class SharedLinksTest {
    private val week = TestEncryptedLink.copy(expiresAt = Instant.parse("2026-11-13T18:30:00Z"))

    private class FixedClock(var now: Instant) : Clock {
        override fun now(): Instant = now
    }

    @Test
    fun aLinkIsFoundByTheTextItWasMadeOfUntilItExpires() = runTest {
        val clock = FixedClock(Instant.parse("2026-11-06T18:30:00Z"))
        val links = SharedLinks(MemorySmallStore(), clock)
        links.keep("""{"a":1}""", week)

        assertEquals(week, links.find("""{"a":1}"""))
        assertNull(links.find("""{"a":2}"""))

        clock.now = Instant.parse("2026-11-13T18:30:01Z")
        assertNull(links.find("""{"a":1}"""))
    }

    @Test
    fun aLinkForgottenIsNotFoundAndANewOneTakesItsTextsPlace() = runTest {
        val links = SharedLinks(MemorySmallStore(), FixedClock(Instant.parse("2026-11-06T18:30:00Z")))
        val other = week.copy(id = "Ld4wR8vXq2", link = "https://formatico.pantrixui.com/#s=Ld4wR8vXq2&k=x")
        links.keep("text", week)
        links.keep("text", other)
        assertEquals(other, links.find("text"))

        links.forget(other)
        assertNull(links.find("text"))
    }
}
