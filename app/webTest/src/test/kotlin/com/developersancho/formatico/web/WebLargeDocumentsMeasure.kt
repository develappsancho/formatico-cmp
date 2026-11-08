package com.developersancho.formatico.web

import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * The web app on a large document, in its JavaScript build, the one the loader falls back to where a browser has no
 * WebAssembly garbage collection: Start's drop zone given the file through the page's file chooser, how long until the
 * document is shown read, its status said, and how much of the JavaScript heap the page holds on the way and once it is
 * shown. It runs only when asked for, on the samples `LargeDocumentsMeasure.samples` writes:
 * `./gradlew :app:webTest:test --tests '*WebLargeDocumentsMeasure*' -Pformatico.measure -Pformatico.measure.samples=<folder>`.
 * The table is written to `build/measure/web-large-documents.md`.
 */
class WebLargeDocumentsMeasure {
    private val folder = System.getProperty("formatico.measure.samples")?.let(::File)
    private val rows =
        mutableListOf(
            "| Build | File | MB | Shown in s | JS heap at most MB | JS heap once shown MB |",
            "| :--- | :--- | ---: | ---: | ---: | ---: |"
        )

    @Test
    fun largeDocumentsInTheBrowser() {
        if (System.getProperty("formatico.measure") == null) return
        val samples = requireNotNull(folder) { "-Pformatico.measure.samples names the samples' folder" }
        val files = samples.listFiles().orEmpty().filter { it.name.endsWith("-10MB.json") || it.name.endsWith("-10MB.csv") }.sorted()
        check(files.isNotEmpty()) { "No samples of 10 MB in $samples: LargeDocumentsMeasure.samples writes them" }
        val dist = File(checkNotNull(System.getProperty("formatico.web.dist")) { "formatico.web.dist is not set" })
        val headers = File(checkNotNull(System.getProperty("formatico.web.headers")) { "formatico.web.headers is not set" })
        File("build/measure").mkdirs()
        DistServer(dist, nginxHeadersOf(headers)).use { server ->
            for (file in files) {
                measure(server, file, javaScript = true)
                measure(server, file, javaScript = false)
                File("build/measure/web-large-documents.md").writeText(rows.joinToString("\n", postfix = "\n"))
            }
        }
        println(rows.joinToString("\n"))
    }

    private fun measure(server: DistServer, file: File, javaScript: Boolean) = Chrome().use { chrome ->
        // As a browser that does not take a module with a struct type and a `try` is answered: the loader's fallback.
        if (javaScript) chrome.beforeEachPage("WebAssembly.validate = () => false;")
        val page = FormaticoPage(chrome, server.address)
        page.open()
        page.awaitText("Paste anything")
        check(if (javaScript) page.ranJavaScript else page.ranWasm) { "The page ran the other build" }
        chrome.chooseFiles()
        val valid = if (file.name.startsWith("csv-")) "Valid CSV" else "Valid JSON"
        val before = heap(chrome, collect = true)
        var most = before
        val started = TimeSource.Monotonic.markNow()
        page.press("FileDrop_file")
        chrome.pick(file)
        chrome.awaitUntil("${file.name} was shown", within = 5.minutes) {
            most = maxOf(most, heap(chrome))
            page.shows(valid)
        }
        val shown = started.elapsedNow()
        val after = heap(chrome, collect = true)
        val build = if (javaScript) "JavaScript" else "Wasm"
        rows += "| $build | ${file.name} | ${file.length() / MB} | ${"%.1f".format(shown.inWholeMilliseconds / 1000.0)} | " +
            "${most / MB} | ${after / MB} |"
    }

    /**
     * The JavaScript heap the page uses, once what nothing holds is collected where [collect] says so. A Wasm build's
     * objects are in it too, as its garbage collection is the browser's; Skia's own memory is not.
     */
    private fun heap(chrome: Chrome, collect: Boolean = false): Long {
        if (collect) chrome.send("HeapProfiler.collectGarbage")
        return chrome.send("Runtime.getHeapUsage")["usedSize"]?.jsonPrimitive?.double?.toLong() ?: 0L
    }

    private companion object {
        const val MB = 1_000_000L
    }
}
