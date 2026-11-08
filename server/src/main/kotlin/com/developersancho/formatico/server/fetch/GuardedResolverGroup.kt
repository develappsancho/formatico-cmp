package com.developersancho.formatico.server.fetch

import io.netty.resolver.AddressResolver
import io.netty.resolver.AddressResolverGroup
import io.netty.resolver.InetNameResolver
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.Promise
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException

/** Looks up the addresses of a host's name: the system's resolver, or a test's own. */
fun interface HostLookup {
    /** The addresses [host] has; an [UnknownHostException] where it has none. */
    fun lookup(host: String): List<InetAddress>

    companion object {
        /** The system's resolver, as Java asks it. */
        val System = HostLookup { host -> InetAddress.getAllByName(host).toList() }
    }
}

/**
 * The resolver the fetch's client connects with: it looks a host up with [lookup] and fails it where [guard] refuses
 * any of its addresses, with a [RefusedAddressException]. Netty connects only to the addresses its resolver gives, so
 * the addresses checked are the ones connected to: a host is looked up once, and a name that would answer with a
 * public address first and a private one after (DNS rebinding) is not asked a second time.
 *
 * The lookup blocks, so it runs on [lookups], never on Netty's event loop.
 */
class GuardedResolverGroup(
    private val lookup: HostLookup,
    private val guard: FetchGuard,
    private val lookups: Scheduler = Schedulers.boundedElastic()
) : AddressResolverGroup<InetSocketAddress>() {
    override fun newResolver(executor: EventExecutor): AddressResolver<InetSocketAddress> =
        GuardedNameResolver(executor).asAddressResolver()

    /** The public addresses of [host], every one of which [guard] takes. Blocks while it looks them up. */
    fun addressesOf(host: String): List<InetAddress> {
        val addresses = lookup.lookup(host)
        if (addresses.isEmpty()) throw UnknownHostException(host)
        if (!addresses.all(guard::allows)) throw RefusedAddressException()
        return addresses
    }

    private inner class GuardedNameResolver(executor: EventExecutor) : InetNameResolver(executor) {
        override fun doResolve(inetHost: String, promise: Promise<InetAddress>) {
            settle(promise) { addressesOf(inetHost).first() }
        }

        override fun doResolveAll(inetHost: String, promise: Promise<List<InetAddress>>) {
            settle(promise) { addressesOf(inetHost) }
        }

        // Whatever the lookup throws has to fail the promise: otherwise the connection waits until the fetch times out.
        @Suppress("TooGenericExceptionCaught")
        private fun <T> settle(promise: Promise<T>, addresses: () -> T) {
            lookups.schedule {
                try {
                    promise.trySuccess(addresses())
                } catch (failed: Exception) {
                    promise.tryFailure(failed)
                }
            }
        }
    }
}
