package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.ShareLinks
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.webcrypto.WebCrypto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js
import kotlinx.browser.window

internal actual fun platformClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Js) { configure() }

internal actual val ShareCipherProvider: CryptographyProvider get() = CryptographyProvider.WebCrypto

internal actual fun platformServer(): String =
    window.location.origin.takeIf { it.startsWith("http") } ?: ShareLinks.SITE.trimEnd('/')

internal actual val downloadsItself: Boolean = false

actual fun linkSite(): String = window.location.origin.takeIf { it.startsWith("http") }?.let { "$it/" } ?: ShareLinks.SITE
