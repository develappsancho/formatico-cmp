package com.developersancho.formatico.server

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The server's settings, under `formatico.` in `application.properties` or in the environment, as
 * `FORMATICO_SHARES_DIRECTORY`: where the [shares] are kept, what the [fetch] may reach, and the [allowedOrigins] a
 * browser may call the API from (`FORMATICO_ALLOWEDORIGINS`). The web app's own origin is one of them: nginx serves the
 * app and the API from one origin, but the server, behind it and Traefik, sees neither the scheme nor the port the
 * browser used, and takes each of the app's calls for one from another origin.
 */
@ConfigurationProperties("formatico")
data class FormaticoProperties(
    val shares: Shares = Shares(),
    val fetch: Fetch = Fetch(),
    val allowedOrigins: List<String> = listOf(SITE)
) {
    /** The folder the shares are kept in: [directory]. */
    data class Shares(val directory: String = "/data/shares")

    /**
     * Whether the fetch may reach any address on any port: [allowPrivate], for the tests that fetch from a server on
     * this machine. Never on in the server's own settings.
     */
    data class Fetch(val allowPrivate: Boolean = false)

    private companion object {
        const val SITE = "https://formatico.pantrixui.com"
    }
}
