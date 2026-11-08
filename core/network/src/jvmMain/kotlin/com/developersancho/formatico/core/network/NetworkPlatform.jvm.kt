package com.developersancho.formatico.core.network

import com.developersancho.formatico.core.share.ShareLinks
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.java.Java

internal actual fun platformClient(configure: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Java) { configure() }

internal actual val ShareCipherProvider: CryptographyProvider get() = CryptographyProvider.JDK

// The stack on this machine, for a desktop app run against it: -Dformatico.server=http://localhost:4005.
internal actual fun platformServer(): String = System.getProperty("formatico.server") ?: ShareLinks.SITE.trimEnd('/')

internal actual val downloadsItself: Boolean = true

actual fun linkSite(): String = ShareLinks.SITE
