package com.developersancho.formatico.server.fetch

import io.netty.util.NetUtil
import io.netty.util.concurrent.GlobalEventExecutor
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The resolver the fetch connects with: a name that answers with an address the server may not reach is refused. */
class GuardedResolverGroupTest {
    private val names = mapOf(
        "public.test" to listOf(address("93.184.216.34")),
        "dual.test" to listOf(address("93.184.216.34"), address("2606:2800:220:1:248:1893:25c8:1946")),
        "loopback.test" to listOf(address("127.0.0.1")),
        "private.test" to listOf(address("10.0.0.5")),
        "home.test" to listOf(address("192.168.1.20")),
        "metadata.test" to listOf(address("169.254.169.254")),
        "cgnat.test" to listOf(address("100.64.1.1")),
        "mixed.test" to listOf(address("93.184.216.34"), address("127.0.0.1")),
        "ipv6-loopback.test" to listOf(address("::1")),
        "ula.test" to listOf(address("fd00::5")),
        "link-local.test" to listOf(address("fe80::1")),
        "mapped.test" to listOf(address("::ffff:192.168.1.1")),
        "nothing.test" to emptyList()
    )
    private val lookup = HostLookup { host -> names[host] ?: throw UnknownHostException(host) }
    private val group = GuardedResolverGroup(lookup, FetchGuard.Public)

    @AfterTest
    fun close() {
        group.close()
    }

    @Test
    fun givesThePublicAddressesOfAName() {
        assertEquals(names.getValue("public.test"), group.addressesOf("public.test"))
        assertEquals(names.getValue("dual.test"), group.addressesOf("dual.test"))
    }

    @Test
    fun refusesANameThatAnswersWithAnAddressTheServerMayNotReach() {
        val refused = listOf(
            "loopback.test",
            "private.test",
            "home.test",
            "metadata.test",
            "cgnat.test",
            "mixed.test",
            "ipv6-loopback.test",
            "ula.test",
            "link-local.test",
            "mapped.test"
        )
        for (host in refused) assertFailsWith<RefusedAddressException>(host) { group.addressesOf(host) }
    }

    @Test
    fun failsANameWithNoAddress() {
        assertFailsWith<UnknownHostException> { group.addressesOf("nothing.test") }
        assertFailsWith<UnknownHostException> { group.addressesOf("unknown.test") }
    }

    @Test
    fun refusesWhatJavaReadsAsTheServersOwnAddress() {
        // Netty does not read these as addresses and asks the resolver, where Java reads them as 127.0.0.1 and ::1.
        val system = GuardedResolverGroup(HostLookup.System, FetchGuard.Public)
        // Netty reads no IPv4 address written in IPv6 after another prefix than ::ffff: either; Java does.
        for (host in listOf("2130706433", "localhost", "[64:ff9b::127.0.0.1]", "[64:ff9b::169.254.169.254]", "[::ffff:10.0.0.1]")) {
            assertFailsWith<RefusedAddressException>(host) { system.addressesOf(host) }
        }
        system.close()
    }

    @Test
    fun resolvesForNettyOnlyWhatItChecked() {
        val resolver = group.getResolver(GlobalEventExecutor.INSTANCE)
        val resolved = resolver.resolveAll(InetSocketAddress.createUnresolved("dual.test", 443)).await(5, TimeUnit.SECONDS)
        assertTrue(resolved)
        val addresses = resolver.resolveAll(InetSocketAddress.createUnresolved("dual.test", 443)).syncUninterruptibly().now
        assertEquals(names.getValue("dual.test").map { InetSocketAddress(it, 443) }, addresses)
        assertEquals(
            InetSocketAddress(address("93.184.216.34"), 80),
            resolver.resolve(InetSocketAddress.createUnresolved("public.test", 80)).syncUninterruptibly().now
        )
    }

    @Test
    fun failsNettysResolutionOfANameItRefuses() {
        val resolver = group.getResolver(GlobalEventExecutor.INSTANCE)
        for (host in listOf("loopback.test", "metadata.test", "mixed.test", "mapped.test")) {
            val all = resolver.resolveAll(InetSocketAddress.createUnresolved(host, 80)).awaitUninterruptibly()
            assertIs<RefusedAddressException>(all.cause(), host)
            val one = resolver.resolve(InetSocketAddress.createUnresolved(host, 80)).awaitUninterruptibly()
            assertIs<RefusedAddressException>(one.cause(), host)
        }
        assertIs<UnknownHostException>(
            resolver.resolve(InetSocketAddress.createUnresolved("unknown.test", 80)).awaitUninterruptibly().cause()
        )
    }

    @Test
    fun failsTheResolutionWhateverTheLookupThrows() {
        val failing = GuardedResolverGroup(HostLookup { error("broken") }, FetchGuard.Public)
        val resolution = failing.getResolver(GlobalEventExecutor.INSTANCE)
            .resolve(InetSocketAddress.createUnresolved("public.test", 80))
            .awaitUninterruptibly()
        assertIs<IllegalStateException>(resolution.cause())
        failing.close()
    }

    private fun address(literal: String): InetAddress {
        val bytes = requireNotNull(NetUtil.createByteArrayFromIpAddressString(literal))
        return if (bytes.size == 16) Inet6Address.getByAddress(null, bytes, -1) else InetAddress.getByAddress(bytes)
    }
}
