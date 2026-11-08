package com.developersancho.formatico.server.fetch

import io.netty.util.NetUtil
import java.net.Inet6Address
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Which addresses the fetch may reach: the internet's, never the server's own or its network's. */
class PublicAddressesTest {
    private val notPublicIpv4 = listOf(
        "0.0.0.0",
        "0.1.2.3",
        "0.255.255.255",
        "10.0.0.1",
        "10.255.255.255",
        "100.64.0.1",
        "100.127.255.254",
        "127.0.0.1",
        "127.1.2.3",
        "127.255.255.255",
        "169.254.169.254",
        "169.254.0.1",
        "169.254.255.255",
        "172.16.0.1",
        "172.20.10.5",
        "172.31.255.255",
        "192.0.0.1",
        "192.0.0.170",
        "192.0.2.1",
        "192.88.99.1",
        "192.168.0.1",
        "192.168.255.255",
        "198.18.0.1",
        "198.19.255.255",
        "198.51.100.7",
        "203.0.113.9",
        "224.0.0.1",
        "232.1.1.1",
        "239.255.255.255",
        "240.0.0.1",
        "250.1.2.3",
        "255.255.255.255"
    )

    private val publicIpv4 = listOf(
        "1.1.1.1",
        "8.8.8.8",
        "9.255.255.255",
        "11.0.0.1",
        "93.184.216.34",
        "100.63.255.255",
        "100.128.0.0",
        "126.255.255.255",
        "128.0.0.1",
        "169.253.255.255",
        "169.255.0.1",
        "172.15.255.255",
        "172.32.0.0",
        "192.0.1.1",
        "192.167.255.255",
        "192.169.0.0",
        "198.17.255.255",
        "198.20.0.0",
        "223.255.255.255"
    )

    private val notPublicIpv6 = listOf(
        "::",
        "::1",
        "::ffff:127.0.0.1",
        "::ffff:10.0.0.1",
        "::ffff:169.254.169.254",
        "::ffff:192.168.1.1",
        "::ffff:0.0.0.0",
        "::127.0.0.1",
        "::10.0.0.1",
        "64:ff9b::7f00:1",
        "64:ff9b::a01:203",
        "64:ff9b::a9fe:a9fe",
        "64:ff9b:1::1",
        "100::1",
        "fc00::1",
        "fd12:3456:789a::1",
        "fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
        "fe80::1",
        "febf::1",
        "fec0::1",
        "ff02::1",
        "ff05::2",
        "ff0e::1",
        "2001::1",
        "2001:0:4136:e378:8000:63bf:3fff:fdd2",
        "2001:2::1",
        "2001:1ff::1",
        "2001:db8::1",
        "2001:db8:ffff::1",
        "2002:c0a8:101::1",
        "2002:7f00:1::1",
        "3fff::1",
        "5f00::1",
        "4000::1",
        "e000::1"
    )

    private val publicIpv6 = listOf(
        "2606:4700:4700::1111",
        "2001:4860:4860::8888",
        "2a00:1450:4001:81c::200e",
        "2400:cb00::1",
        "2001:200::1",
        "2003::1",
        "3ffe::1",
        "::ffff:8.8.8.8",
        "::ffff:93.184.216.34",
        "64:ff9b::808:808"
    )

    @Test
    fun refusesTheServersOwnAndItsNetworksIpv4Addresses() {
        for (address in notPublicIpv4) assertFalse(PublicAddresses.isPublic(literal(address)), address)
    }

    @Test
    fun takesTheInternetsIpv4Addresses() {
        for (address in publicIpv4) assertTrue(PublicAddresses.isPublic(literal(address)), address)
    }

    @Test
    fun refusesTheServersOwnAndItsNetworksIpv6Addresses() {
        for (address in notPublicIpv6) assertFalse(PublicAddresses.isPublic(literal(address)), address)
    }

    @Test
    fun takesTheInternetsIpv6Addresses() {
        for (address in publicIpv6) assertTrue(PublicAddresses.isPublic(literal(address)), address)
    }

    @Test
    fun readsAnIpv4AddressWrittenInIpv6AsTheIpv4AddressItHolds() {
        // Netty connects to an address written in a link as IPv6 bytes: ::ffff:127.0.0.1 stays an Inet6Address.
        val mapped = Inet6Address.getByAddress(null, bytes("::ffff:127.0.0.1"), -1)
        assertFalse(PublicAddresses.isPublic(mapped))
        assertTrue(PublicAddresses.isPublic(Inet6Address.getByAddress(null, bytes("::ffff:8.8.8.8"), -1)))
    }

    @Test
    fun readsJavasOwnAddressesAsNettysBytes() {
        for (address in notPublicIpv4 + notPublicIpv6) assertFalse(PublicAddresses.isPublic(InetAddress.getByName(address)), address)
        for (address in publicIpv4 + publicIpv6) assertTrue(PublicAddresses.isPublic(InetAddress.getByName(address)), address)
    }

    @Test
    fun refusesBytesThatAreNoAddress() {
        assertFalse(PublicAddresses.isPublic(ByteArray(0)))
        assertFalse(PublicAddresses.isPublic(byteArrayOf(8, 8, 8)))
        assertFalse(PublicAddresses.isPublic(ByteArray(8) { 8 }))
    }

    private fun bytes(literal: String): ByteArray = requireNotNull(NetUtil.createByteArrayFromIpAddressString(literal)) { literal }

    /** The address as Netty reads it from a link, which is the address it connects to. */
    private fun literal(literal: String): InetAddress {
        val bytes = bytes(literal)
        return if (bytes.size == 16) Inet6Address.getByAddress(null, bytes, -1) else InetAddress.getByAddress(bytes)
    }
}
