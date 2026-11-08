package com.developersancho.formatico.web

import com.sun.net.httpserver.HttpServer
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Serves [root], the web app's distribution, on this machine at a port of its own, as a static host serves it: each
 * file with its type, `index.html` for the root. A Wasm module is `application/wasm`, which the browser compiles as it
 * downloads it.
 */
class DistServer(private val root: File) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        createContext("/") { exchange ->
            val path = exchange.requestURI.path.removePrefix("/").ifEmpty { "index.html" }
            val file = root.resolve(path).normalize()
            if (!file.startsWith(root) || !file.isFile) {
                exchange.sendResponseHeaders(NOT_FOUND, -1)
            } else {
                exchange.responseHeaders.add("Content-Type", typeOf(file.name))
                exchange.sendResponseHeaders(OK, file.length())
                exchange.responseBody.use { body -> file.inputStream().use { it.copyTo(body) } }
            }
            exchange.close()
        }
        start()
    }

    /** Where the app is served: `http://127.0.0.1:<port>/`. */
    val address: String = "http://127.0.0.1:${server.address.port}/"

    override fun close() = server.stop(0)

    private companion object {
        const val OK = 200
        const val NOT_FOUND = 404

        val TYPES = mapOf(
            "html" to "text/html; charset=utf-8",
            "js" to "text/javascript",
            "mjs" to "text/javascript",
            "wasm" to "application/wasm",
            "css" to "text/css",
            "json" to "application/json",
            "webmanifest" to "application/manifest+json",
            "svg" to "image/svg+xml",
            "png" to "image/png",
            "ico" to "image/x-icon",
            "ttf" to "font/ttf",
            "otf" to "font/otf",
            "woff2" to "font/woff2",
            "xml" to "application/xml",
            "txt" to "text/plain; charset=utf-8"
        )

        fun typeOf(name: String): String = TYPES[name.substringAfterLast('.', "")] ?: "application/octet-stream"
    }
}
