package com.developersancho.formatico.server.fetch

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** What the server's own guard lets the fetch ask: public `http` and `https` addresses, on ports 80 and 443. */
class FetchGuardTest {
    private val guard = FetchGuard.Public

    @Test
    fun takesPublicHttpAndHttpsAddresses() {
        val taken = mapOf(
            "https://example.com/data.json" to "https://example.com/data.json",
            "http://example.com" to "http://example.com",
            "https://example.com:443/a/b?c=d&e=%20f" to "https://example.com:443/a/b?c=d&e=%20f",
            "http://example.com:80/" to "http://example.com:80/",
            "https://example.com:80/" to "https://example.com:80/",
            "http://example.com:443/" to "http://example.com:443/",
            "HTTPS://Example.COM/Data.json" to "https://Example.COM/Data.json",
            "https://example.com/a#section" to "https://example.com/a",
            "  https://example.com/  " to "https://example.com/",
            "https://8.8.8.8/x" to "https://8.8.8.8/x",
            "https://[2606:4700:4700::1111]/x" to "https://[2606:4700:4700::1111]/x"
        )
        for ((raw, target) in taken) assertEquals(URI(target), guard.target(raw), raw)
    }

    @Test
    fun refusesEveryOtherScheme() {
        val refused = listOf(
            "ftp://example.com/data.json",
            "file:///etc/passwd",
            "gopher://example.com/",
            "ws://example.com/",
            "jar:https://example.com/a.jar!/b",
            "javascript:alert(1)",
            "data:text/plain,hello",
            "mailto:someone@example.com",
            "//example.com/data.json",
            "/api/health",
            "example.com/data.json",
            ""
        )
        for (raw in refused) assertNull(guard.target(raw), raw)
    }

    @Test
    fun refusesEveryOtherPort() {
        for (port in listOf(0, 1, 21, 22, 25, 81, 444, 3000, 5432, 6379, 8080, 8443, 9200, 65535)) {
            assertNull(guard.target("http://example.com:$port/"), "port $port")
            assertNull(guard.target("https://example.com:$port/"), "port $port")
        }
    }

    @Test
    fun refusesAUserOrAPassword() {
        assertNull(guard.target("https://user:secret@example.com/"))
        assertNull(guard.target("https://user@example.com/"))
        assertNull(guard.target("https://example.com@127.0.0.1/"))
    }

    @Test
    fun refusesAddressesThatAreNoAddress() {
        val unreadable = listOf(
            "http://",
            "http:///data.json",
            "https://exa mple.com/",
            "https://example.com/a b",
            "http://[::1/",
            "http:example.com",
            "http://@example.com/",
            "http://127.1/",
            "http://0x7f.0.0.1/"
        )
        for (raw in unreadable) {
            assertNull(guard.target(raw), raw)
        }
    }

    @Test
    fun refusesTheServersOwnAndItsNetworksAddressesWrittenInTheLink() {
        val refused = listOf(
            "http://127.0.0.1/",
            "http://127.0.0.1:80/",
            "https://127.10.20.30/",
            "http://0.0.0.0/",
            "http://10.0.0.1/",
            "http://172.16.5.4/",
            "http://192.168.1.1/",
            "http://100.64.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://224.0.0.1/",
            "http://255.255.255.255/",
            "http://[::1]/",
            "http://[::]/",
            "http://[::ffff:127.0.0.1]/",
            "http://[::ffff:a9fe:a9fe]/",
            "http://[::ffff:192.168.0.1]/",
            "http://[fd00::1]/",
            "http://[fe80::1]/",
            "http://[ff02::1]/",
            "http://[64:ff9b::a00:1]/"
        )
        for (raw in refused) assertNull(guard.target(raw), raw)
    }

    @Test
    fun leavesANameToTheResolver() {
        // Names, and the numbers Java reads as addresses and Netty does not, are checked once they are looked up.
        for (raw in listOf("http://localhost/", "http://2130706433/", "http://metadata.google.internal/")) {
            assertEquals(URI(raw), guard.target(raw), raw)
        }
    }

    @Test
    fun followsARedirectToAPublicAddress() {
        val from = URI("https://example.com/a/b.json?x=1")
        assertEquals(to("https://example.com/c.json"), guard.redirect(from, "/c.json"))
        assertEquals(to("https://example.com/a/d.json"), guard.redirect(from, "d.json"))
        assertEquals(to("https://example.com/e.json"), guard.redirect(from, "../e.json"))
        assertEquals(to("https://cdn.example.org/f.json"), guard.redirect(from, "https://cdn.example.org/f.json"))
        assertEquals(to("https://cdn.example.org/g.json"), guard.redirect(from, "//cdn.example.org/g.json"))
        assertEquals(to("https://example.com/h.json"), guard.redirect(URI("https://example.com"), "h.json"))
        assertEquals(to("https://example.com/i.json"), guard.redirect(from, "/i.json#part"))
    }

    @Test
    fun refusesARedirectToAnAddressTheFetchMayNotAsk() {
        val from = URI("https://example.com/a.json")
        val refused = listOf(
            "http://127.0.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://[::1]/",
            "http://[::ffff:10.0.0.1]/",
            "https://example.com:8443/",
            "ftp://example.com/a.json",
            "file:///etc/passwd",
            "https://user:secret@example.com/"
        )
        for (location in refused) assertEquals(FetchGuard.Redirect.Refused, guard.redirect(from, location), location)
    }

    @Test
    fun readsARedirectThatIsNoAddressAsNone() {
        val from = URI("https://example.com/a.json")
        assertEquals(FetchGuard.Redirect.Unreadable, guard.redirect(from, "https://exa mple.com/"))
        assertEquals(FetchGuard.Redirect.Unreadable, guard.redirect(from, "http://[::1/"))
    }

    @Test
    fun checksEveryAddressAConnectionIsAboutToBeMadeTo() {
        val public = InetSocketAddress(InetAddress.getByName("93.184.216.34"), 443)
        val private = InetSocketAddress(InetAddress.getByName("10.0.0.1"), 443)
        assertEquals(listOf(public), guard.checked(listOf(public)))
        assertFailsWith<RefusedAddressException> { guard.checked(listOf(public, private)) }
        assertFailsWith<RefusedAddressException> { guard.checked(listOf(InetSocketAddress(InetAddress.getByName("::1"), 80))) }
        assertFailsWith<RefusedAddressException> { guard.checked(listOf(InetSocketAddress.createUnresolved("example.com", 443))) }
    }

    @Test
    fun letsTheTestsGuardReachAnything() {
        assertEquals(URI("http://127.0.0.1:8080/x"), FetchGuard.Anywhere.target("http://127.0.0.1:8080/x"))
        assertNull(FetchGuard.Anywhere.target("ftp://127.0.0.1/x"))
    }

    private fun to(uri: String) = FetchGuard.Redirect.To(URI(uri))
}
