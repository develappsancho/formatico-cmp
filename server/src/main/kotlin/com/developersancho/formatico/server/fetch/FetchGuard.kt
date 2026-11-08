package com.developersancho.formatico.server.fetch

import io.netty.util.NetUtil
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.URI
import java.net.URISyntaxException

/**
 * What the fetch may reach: an `http` or `https` address, with no user or password in it, on a port [allowsPort]
 * takes, whose host is only ever connected to at an address [allowsAddress] takes.
 *
 * [Public] is the server's own guard: public addresses, on ports 80 and 443. [Anywhere] reaches every address and
 * port, for the tests that fetch from a server on this machine (`formatico.fetch.allow-private`).
 */
class FetchGuard(private val allowsAddress: (InetAddress) -> Boolean, private val allowsPort: (Int) -> Boolean) {
    /** [raw] as the address the fetch asks, without its fragment, which is no server's; null where it may not. */
    fun target(raw: String): URI? = parsed(raw)?.let(::allowed)

    /**
     * Where the redirect [location] that [from] answered with leads, as [target] takes it, or why it does not:
     * [Redirect.Refused] for an address the fetch may not ask, [Redirect.Unreadable] for a location that is none.
     */
    fun redirect(from: URI, location: String): Redirect {
        val reference = parsed(location) ?: return Redirect.Unreadable
        // URI.resolve puts no slash between a host and a relative path when the base's path is empty.
        val base = if (from.rawPath.isNullOrEmpty()) URI("${from.scheme}://${from.rawAuthority}/") else from
        val next = base.resolve(reference)
        return allowed(next)?.let(Redirect::To) ?: Redirect.Refused
    }

    /** Whether the fetch may connect to [address]. */
    fun allows(address: InetAddress): Boolean = allowsAddress(address)

    /**
     * [addresses], the ones a connection is about to be made to, when [allows] takes every one; otherwise a
     * [RefusedAddressException].
     */
    fun checked(addresses: List<SocketAddress>): List<SocketAddress> {
        for (address in addresses) {
            val inet = (address as? InetSocketAddress)?.address
            if (inet == null || !allows(inet)) throw RefusedAddressException()
        }
        return addresses
    }

    private fun allowed(uri: URI): URI? {
        val scheme = uri.scheme?.lowercase() ?: return null
        val host = uri.host ?: return null
        val port = if (uri.port == NO_PORT) DEFAULT_PORTS[scheme] else uri.port
        val allowed = scheme in DEFAULT_PORTS && uri.rawUserInfo == null && port != null && allowsPort(port) && allowsLiteral(host)
        if (!allowed) return null
        val text = uri.toString()
        return URI(scheme + text.substring(uri.scheme.length).substringBefore('#'))
    }

    /**
     * Whether the [host] an address names is allowed where it is an address itself, as `127.0.0.1` or `[::1]`: Netty
     * connects to such a host as it is, without asking a resolver. A name is checked by [GuardedResolverGroup].
     */
    private fun allowsLiteral(host: String): Boolean {
        val literal = NetUtil.createByteArrayFromIpAddressString(host) ?: return true
        return allows(InetAddress.getByAddress(literal))
    }

    private fun parsed(raw: String): URI? = try {
        URI(raw.trim())
    } catch (_: URISyntaxException) {
        null
    }

    /** Where a redirect leads. */
    sealed interface Redirect {
        /** To [uri], which the fetch may ask. */
        data class To(val uri: URI) : Redirect

        /** To an address the fetch may not ask. */
        data object Refused : Redirect

        /** Nowhere: the location is no address. */
        data object Unreadable : Redirect
    }

    companion object {
        private const val NO_PORT = -1
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
        private val DEFAULT_PORTS = mapOf("http" to HTTP_PORT, "https" to HTTPS_PORT)

        /** The server's own guard: public addresses, on ports 80 and 443. */
        val Public = FetchGuard(PublicAddresses::isPublic) { it == HTTP_PORT || it == HTTPS_PORT }

        /** Every address and port: for the tests that fetch from a server on this machine, never for the server. */
        val Anywhere = FetchGuard({ true }, { true })
    }
}

/** An address the fetch was about to reach, and may not. */
class RefusedAddressException : IOException("The address is not one the fetch may reach")
