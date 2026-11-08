package com.developersancho.formatico

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.awaitUntil
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.hasPantrixTag
import com.pantrixui.testing.onPantrixNode
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * The whole app on a large document, in the desktop's window: Start's drop zone given the file, and how long until the
 * document is shown read, its status said, and how much of the heap the app holds on the way and once it is shown.
 * It runs only when asked for, on the samples `LargeDocumentsMeasure.samples` writes:
 * `./gradlew :app:shared:jvmTest --tests '*AppLargeDocumentsMeasure*' -Pformatico.measure
 * -Pformatico.measure.samples=<folder>`. The test's heap is 4 GB, what the desktop app takes of a machine of 8
 * (`-XX:MaxRAMPercentage=50`). The table is written to `build/measure/app-large-documents.md`.
 */
@OptIn(ExperimentalTestApi::class)
class AppLargeDocumentsMeasure {
    private val folder = System.getProperty("formatico.measure.samples")?.let(::File)
    private val rows =
        mutableListOf("| File | MB | Shown in s | Heap at most MB | Heap once shown MB |", "| :--- | ---: | ---: | ---: | ---: |")

    @Test
    fun largeDocumentsInTheDesktopsWindow() {
        if (System.getProperty("formatico.measure") == null) return
        val samples = requireNotNull(folder) { "-Pformatico.measure.samples names the samples' folder" }
        val files = samples.listFiles().orEmpty().filter { it.name.startsWith("json-") || it.name.startsWith("csv-") }.sorted()
        check(files.isNotEmpty()) { "No samples in $samples: LargeDocumentsMeasure.samples writes them" }
        File("build/measure").mkdirs()
        for (file in files) {
            try {
                measure(file)
            } finally {
                // Each run starts the app's own Koin; the next one starts it again.
                stopKoin()
            }
            File("build/measure/app-large-documents.md").writeText(rows.joinToString("\n", postfix = "\n"))
        }
        println(rows.joinToString("\n"))
    }

    private fun measure(file: File) = runDesktopComposeUiTest(width = Board.Desktop.width, height = Board.Desktop.height) {
        val picker = BoardFiles()
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = Board.Desktop.window, filePicker = picker) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText(BoardWords.English.start)
        // The file's bytes as the picker hands them: reading them from the disk is not the app's.
        picker.putNext(file.name, file.readText())
        val before = used()
        var most = before
        val valid = if (file.name.startsWith("csv-")) "Valid CSV" else "Valid JSON"
        val started = TimeSource.Monotonic.markNow()
        onPantrixNode("FileDrop", "file").performSemanticsAction(SemanticsActions.OnClick)
        awaitUntil("${file.name} was shown", within = 5.minutes) {
            most = maxOf(most, heap())
            onAllNodes(hasPantrixTag("Tag", "status") and hasText(valid)).fetchSemanticsNodes().isNotEmpty()
        }
        awaitIdle()
        val shown = started.elapsedNow()
        val after = used()
        val megabytes = file.length() / MB
        rows += "| ${file.name} | $megabytes | ${"%.1f".format(shown.inWholeMilliseconds / 1000.0)} | ${most / MB} | ${after / MB} |"
    }

    private fun heap(): Long = Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }

    /** How much of the heap is held once what nothing holds is collected. */
    private fun used(): Long {
        repeat(GC_ROUNDS) {
            System.gc()
            Thread.sleep(GC_PAUSE)
        }
        return heap()
    }

    private companion object {
        // As Formatico counts a file's size: 50 MB is 50,000,000 bytes.
        const val MB = 1_000_000L
        const val GC_ROUNDS = 3
        const val GC_PAUSE = 50L
    }
}
