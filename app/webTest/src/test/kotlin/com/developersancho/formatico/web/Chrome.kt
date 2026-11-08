package com.developersancho.formatico.web

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * This machine's Chrome, started headless with a profile of its own, which nothing outlives, and driven through its
 * DevTools protocol: one page, what it is told to do, and what it wrote to its console. [flags] are Chrome's own, as
 * `--lang=tr` for a reader of Turkish.
 */
class Chrome(vararg flags: String) : AutoCloseable {
    private val profile: File = Files.createTempDirectory("formatico-chrome").toFile()
    private val process: Process = ProcessBuilder(
        listOf(
            executable(),
            "--headless=new",
            "--remote-debugging-port=0",
            "--user-data-dir=${profile.absolutePath}",
            "--no-first-run",
            "--no-default-browser-check",
            "--disable-extensions",
            "--window-size=1440,900"
        ) + flags + "about:blank"
    ).redirectErrorStream(true).redirectOutput(File(profile, "chrome.log")).start()
    private val http = HttpClient.newHttpClient()
    private val pending = ConcurrentHashMap<Int, CompletableFuture<JsonObject>>()
    private val ids = AtomicInteger()
    private val socket: WebSocket

    /** What the page wrote to its console as an error, and what it threw and did not catch, in order. */
    val errors: MutableList<String> = CopyOnWriteArrayList()

    init {
        val port = awaitPort()
        val pages = http.send(HttpRequest.newBuilder(URI("http://127.0.0.1:$port/json/list")).build(), HttpResponse.BodyHandlers.ofString())
        val page = Json.parseToJsonElement(pages.body()).jsonArray.map { it.jsonObject }.first { it.text("type") == "page" }
        socket = http.newWebSocketBuilder().buildAsync(URI(page.text("webSocketDebuggerUrl")), Listener()).get(STARTUP, TimeUnit.SECONDS)
        send("Runtime.enable")
        send("Page.enable")
    }

    /** Sends [method] with [params] and waits for its answer, failing where the protocol says it failed. */
    fun send(method: String, params: JsonObject = JsonObject(emptyMap())): JsonObject {
        val id = ids.incrementAndGet()
        val answer = CompletableFuture<JsonObject>()
        pending[id] = answer
        socket.sendText(
            buildJsonObject {
                put("id", id)
                put("method", method)
                put("params", params)
            }.toString(),
            true
        )
        val reply = answer.get(ANSWER, TimeUnit.SECONDS)
        reply["error"]?.let { error("$method failed: $it") }
        return reply["result"]?.jsonObject ?: JsonObject(emptyMap())
    }

    /** Runs [script] in every page from now on, before the page's own scripts. */
    fun beforeEachPage(script: String) {
        send("Page.addScriptToEvaluateOnNewDocument", buildJsonObject { put("source", script) })
    }

    /** The value of [expression], evaluated in the page, a promise's once it settles. */
    fun evaluate(expression: String): JsonElement? {
        val result = send(
            "Runtime.evaluate",
            buildJsonObject {
                put("expression", expression)
                put("returnByValue", true)
                put("awaitPromise", true)
            }
        )
        result["exceptionDetails"]?.let { error("The page threw evaluating $expression: $it") }
        return result["result"]?.jsonObject?.get("value")
    }

    /** Waits, for [within] at most, until [done] says so, looking every tenth of a second; [what] is what did not. */
    fun awaitUntil(what: String, within: Duration = 20.seconds, done: () -> Boolean) {
        val started = TimeSource.Monotonic.markNow()
        while (!done()) {
            check(started.elapsedNow() < within) { "$what did not happen in $within" }
            Thread.sleep(POLL_MILLIS)
        }
    }

    override fun close() {
        runCatching { socket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(1, TimeUnit.SECONDS) }
        process.destroy()
        if (!process.waitFor(EXIT, TimeUnit.SECONDS)) process.destroyForcibly()
        profile.deleteRecursively()
    }

    /** Chrome writes the port it took into its profile once it listens. */
    private fun awaitPort(): Int {
        val file = File(profile, "DevToolsActivePort")
        val started = TimeSource.Monotonic.markNow()
        while (!file.isFile || file.readLines().firstOrNull().isNullOrBlank()) {
            check(process.isAlive) { "Chrome stopped: " + File(profile, "chrome.log").readText() }
            check(started.elapsedNow() < STARTUP.seconds) { "Chrome did not listen in $STARTUP seconds" }
            Thread.sleep(POLL_MILLIS)
        }
        return file.readLines().first().trim().toInt()
    }

    private inner class Listener : WebSocket.Listener {
        private val message = StringBuilder()

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            message.append(data)
            if (last) {
                handle(Json.parseToJsonElement(message.toString()).jsonObject)
                message.clear()
            }
            webSocket.request(1)
            return null
        }

        private fun handle(message: JsonObject) {
            message["id"]?.let { pending.remove(it.jsonPrimitive.int)?.complete(message) }
            val params = message["params"]?.jsonObject ?: return
            when (message.text("method")) {
                "Runtime.exceptionThrown" -> errors += params.toString()
                "Runtime.consoleAPICalled" -> if (params.text("type") == "error") errors += params["args"].toString()
            }
        }
    }

    private companion object {
        const val STARTUP = 20L
        const val ANSWER = 30L
        const val EXIT = 5L
        const val POLL_MILLIS = 100L

        fun JsonObject.text(key: String): String? = get(key)?.jsonPrimitive?.content

        /** The Chrome named by `formatico.chrome`, or the one a Mac installs. */
        fun executable(): String = System.getProperty("formatico.chrome")
            ?: "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    }
}
