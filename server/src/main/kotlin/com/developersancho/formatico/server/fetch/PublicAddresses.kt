package com.developersancho.formatico.server.fetch

import io.netty.util.NetUtil
import java.net.InetAddress

/**
 * Which addresses are public: those of the internet at large, which the fetch may reach. The others are the server's
 * own and its network's, which a fetch must never reach: loopback, the private ranges, link-local (the cloud's
 * metadata at 169.254.169.254 among them), carrier-grade NAT, "this network", multicast, broadcast and the reserved,
 * documentation and benchmarking ranges, in IPv4 and in IPv6.
 */
object PublicAddresses {
    private const val IPV4_BYTES = 4
    private const val IPV6_BYTES = 16

    private val NotPublicIpv4 = listOf(
        "0.0.0.0/8", // "this network"
        "10.0.0.0/8", // private
        "100.64.0.0/10", // carrier-grade NAT
        "127.0.0.0/8", // loopback
        "169.254.0.0/16", // link-local, the cloud's metadata among it
        "172.16.0.0/12", // private
        "192.0.0.0/24", // the IETF's protocol assignments
        "192.0.2.0/24", // documentation
        "192.88.99.0/24", // 6to4 relays, retired
        "192.168.0.0/16", // private
        "198.18.0.0/15", // benchmarking
        "198.51.100.0/24", // documentation
        "203.0.113.0/24", // documentation
        "224.0.0.0/4", // multicast
        "240.0.0.0/4" // reserved, and the broadcast address 255.255.255.255
    ).map(Block::of)

    /** IPv6's global unicast addresses: no other IPv6 address is public. */
    private val GlobalUnicast = Block.of("2000::/3")

    private val NotPublicGlobalIpv6 = listOf(
        "2001::/23", // the IETF's protocol assignments, Teredo among them
        "2001:db8::/32", // documentation
        "2002::/16", // 6to4, which reaches any IPv4 address
        "3fff::/20" // documentation
    ).map(Block::of)

    /** IPv4 addresses written in IPv6, `::ffff:a.b.c.d`: as public as the IPv4 address they hold. */
    private val Ipv4Mapped = Block.of("::ffff:0:0/96")

    /** NAT64's well-known prefix, `64:ff9b::a.b.c.d`: as public as the IPv4 address it translates to. */
    private val Nat64 = Block.of("64:ff9b::/96")

    /** Whether [address] is public. */
    fun isPublic(address: InetAddress): Boolean = isPublic(address.address)

    /** Whether the address of [bytes], 4 for IPv4 or 16 for IPv6, is public. */
    fun isPublic(bytes: ByteArray): Boolean = when (bytes.size) {
        IPV4_BYTES -> NotPublicIpv4.none { it.contains(bytes) }
        IPV6_BYTES -> isPublicIpv6(bytes)
        else -> false
    }

    private fun isPublicIpv6(bytes: ByteArray): Boolean = when {
        Ipv4Mapped.contains(bytes) || Nat64.contains(bytes) -> isPublic(bytes.copyOfRange(IPV6_BYTES - IPV4_BYTES, IPV6_BYTES))
        !GlobalUnicast.contains(bytes) -> false
        else -> NotPublicGlobalIpv6.none { it.contains(bytes) }
    }

    /** A range of addresses: those whose first [bits] are the [prefix]'s. */
    private class Block(private val prefix: ByteArray, private val bits: Int) {
        fun contains(address: ByteArray): Boolean {
            if (address.size != prefix.size) return false
            val whole = bits / Byte.SIZE_BITS
            for (index in 0 until whole) {
                if (address[index] != prefix[index]) return false
            }
            val rest = bits % Byte.SIZE_BITS
            if (rest == 0) return true
            val mask = (BYTE_MASK shl (Byte.SIZE_BITS - rest)) and BYTE_MASK
            return (address[whole].toInt() and mask) == (prefix[whole].toInt() and mask)
        }

        companion object {
            private const val BYTE_MASK = 0xFF

            /** The range [cidr] writes, as `10.0.0.0/8` or `fc00::/7`. */
            fun of(cidr: String): Block {
                val (address, bits) = cidr.split('/')
                val prefix = requireNotNull(NetUtil.createByteArrayFromIpAddressString(address)) { "Not an address: $address" }
                return Block(prefix, bits.toInt())
            }
        }
    }
}
