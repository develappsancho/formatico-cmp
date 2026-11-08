package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.ShareLinks
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.cryptokit.CryptoKit
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

internal actual fun platformClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Darwin) { configure() }

internal actual val ShareCipherProvider: CryptographyProvider get() = CryptographyProvider.CryptoKit

internal actual fun platformServer(): String = ShareLinks.SITE.trimEnd('/')

internal actual val downloadsItself: Boolean = true
