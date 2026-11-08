package com.developersancho.formatico.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Formatico's server, the documents at web addresses and the pictures at them, in Koin, on the platform's own HTTP
 * client.
 */
val networkModule: Module = networkModule(pictures = null)

/**
 * [networkModule], its pictures read by [pictures] where it is given: a test's, which has them on the machine it runs on,
 * since a document's picture values would otherwise ask the network for theirs.
 */
fun networkModule(pictures: WebPictures?): Module = module {
    single { formaticoClient() }
    single { FormaticoServer(get(), platformServer()) }
    single<EncryptedShares> { ServerShares(get()) }
    single<WebAddresses> { HttpWebAddresses(get(), get()) }
    single<WebPictures> { pictures ?: HttpWebPictures(get()) }
    single { Online(get(), get()) }
}

/**
 * The platform's HTTP client, as Formatico asks: an answer of any status is the caller's to read, and a request gives
 * up where the network does not answer, a share of 10 MB on a slow line given its time.
 */
internal fun formaticoClient(): HttpClient = platformClient {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT
        socketTimeoutMillis = SOCKET_TIMEOUT
        requestTimeoutMillis = REQUEST_TIMEOUT
    }
}

private const val CONNECT_TIMEOUT = 10_000L
private const val SOCKET_TIMEOUT = 30_000L
private const val REQUEST_TIMEOUT = 120_000L

/** What the app reaches on the network: the [shares] Formatico's server keeps, and the documents at web [addresses]. */
class Online(val shares: EncryptedShares, val addresses: WebAddresses)
