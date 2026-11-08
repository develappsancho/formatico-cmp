package com.developersancho.formatico.core.network

import dev.whyoleg.cryptography.CryptographyProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/** An HTTP client on the platform's own engine: OkHttp on Android, `NSURLSession` on iOS, Java's, or `fetch`. */
internal expect fun platformClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient

/** The platform's own cryptography, which encrypts the shares: the JDK's, CryptoKit, or the browser's WebCrypto. */
internal expect val ShareCipherProvider: CryptographyProvider

/**
 * The address of Formatico's server, which answers beside the web app: on the web, the page's own, so that the stack
 * on this machine answers a page it serves; elsewhere, Formatico's site.
 */
internal expect fun platformServer(): String

/**
 * Whether the platform downloads a document from a web address itself. A browser reads another site only where that
 * site lets it (CORS), so on the web Formatico's server fetches it.
 */
internal expect val downloadsItself: Boolean
