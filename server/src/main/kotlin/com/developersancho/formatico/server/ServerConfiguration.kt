package com.developersancho.formatico.server

import com.developersancho.formatico.core.share.ShareServer
import com.developersancho.formatico.server.fetch.DocumentFetcher
import com.developersancho.formatico.server.fetch.FetchGuard
import com.developersancho.formatico.server.fetch.FetchHandler
import com.developersancho.formatico.server.share.ShareCleanup
import com.developersancho.formatico.server.share.ShareHandler
import com.developersancho.formatico.server.share.ShareStore
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.coRouter
import java.nio.file.Path
import java.time.Clock

/** The server's parts, its routes, and the browsers that may call it. */
@Configuration(proxyBeanMethods = false)
class ServerConfiguration(private val properties: FormaticoProperties) {
    /** What time it is: the system's, in UTC, unless a test moves it. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /** The shares, in `formatico.shares.directory`. */
    @Bean
    fun shareStore(clock: Clock): ShareStore = ShareStore(Path.of(properties.shares.directory), clock)

    /** The hourly deletion of the shares that have expired. */
    @Bean
    fun shareCleanup(store: ShareStore): ShareCleanup = ShareCleanup(store)

    /** The fetch, which reaches only public addresses unless `formatico.fetch.allow-private` is on. */
    @Bean
    fun documentFetcher(): DocumentFetcher {
        val allowPrivate = properties.fetch.allowPrivate
        if (allowPrivate) logger.warn("formatico.fetch.allow-private is on: the fetch reaches every address. Tests only.")
        return DocumentFetcher(if (allowPrivate) FetchGuard.Anywhere else FetchGuard.Public)
    }

    /** The API's routes, as [ShareServer] sets them out. */
    @Bean
    fun routes(store: ShareStore, fetcher: DocumentFetcher): RouterFunction<ServerResponse> {
        val shares = ShareHandler(store)
        val fetch = FetchHandler(fetcher)
        return coRouter {
            GET(ShareServer.HEALTH) { ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).bodyValueAndAwait("ok") }
            POST(ShareServer.SHARES, shares::keep)
            GET("${ShareServer.SHARES}/{${ShareHandler.ID}}", shares::read)
            DELETE("${ShareServer.SHARES}/{${ShareHandler.ID}}", shares::delete)
            GET(ShareServer.FETCH, fetch::fetch)
        }
    }

    /**
     * The browsers that may call the API: `formatico.allowed-origins`. The web app's own calls come through here too,
     * as the server sees another origin than the browser's behind nginx; the apps, which are no browser, send none.
     */
    @Bean
    fun corsFilter(): CorsWebFilter {
        val cors = CorsConfiguration().apply {
            allowedOrigins = properties.allowedOrigins
            allowedMethods = listOf(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name())
            allowedHeaders = listOf(HttpHeaders.CONTENT_TYPE, ShareServer.DELETE_TOKEN)
        }
        return CorsWebFilter(UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", cors) })
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ServerConfiguration::class.java)
    }
}
