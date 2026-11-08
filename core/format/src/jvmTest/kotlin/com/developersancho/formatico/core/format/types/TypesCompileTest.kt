package com.developersancho.formatico.core.format.types

import com.developersancho.formatico.core.format.convert.BoardOrder
import com.developersancho.formatico.core.format.convert.dataOf
import org.junit.Assume
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The types Formatico writes, compiled by each language's own compiler and tried on the document they were written of:
 * Go's decodes it with no field left over and writes it back as it was, TypeScript's takes it as it is, Swift's
 * decodes it and writes it again. Each compiler is the one given, as `-Pformatico.go`, `-Pformatico.tsc` and
 * `-Pformatico.swiftc`; a test whose compiler is not given is skipped. Kotlin's is compiled in feature:workspace's
 * tests, with kotlinx.serialization, on every target.
 */
class TypesCompileTest {
    private val samples = listOf(
        Sample(name = "order", text = BoardOrder, root = "Order", list = false),
        Sample(name = "events", text = Events, root = "Event", list = true)
    )
    private val out = File("build/generated-types")

    @Test
    fun goCompilesTheTypesAndTheyReadAndWriteTheDocumentsAsTheyAre() {
        val go = tool("go")
        samples.forEach { sample ->
            val dir = folder("go", sample)
            val types = written(sample, TypeLanguage.Go)
            val name = types.lineSequence().first().removePrefix("package ").trim()
            File(dir, "go.mod").writeText("module check\n\ngo 1.22\n")
            File(dir, "types.go").writeText(types)
            File(dir, "document.json").writeText(sample.text)
            File(dir, "document_test.go").writeText(goTest(name, if (sample.list) "[]${sample.root}" else sample.root))
            val env = mapOf(
                "GOCACHE" to File(out, "gocache").absolutePath,
                "GOPATH" to File(out, "gopath").absolutePath,
                "GOTOOLCHAIN" to "local",
                "GOPROXY" to "off",
                "GOFLAGS" to "-mod=mod"
            )
            run(dir, env, go, "vet", "./...")
            run(dir, env, go, "test", "./...")
        }
    }

    @Test
    fun typeScriptCompilesTheTypesAndTheyTakeTheDocumentsAsTheyAre() {
        val tsc = tool("tsc")
        samples.forEach { sample ->
            val dir = folder("ts", sample)
            File(dir, "types.ts").writeText(written(sample, TypeLanguage.TypeScript))
            val type = if (sample.list) "${sample.root}[]" else sample.root
            File(dir, "check.ts").writeText(
                "import type { ${sample.root} } from \"./types\";\n\nexport const document: $type = ${sample.text.trim()};\n"
            )
            run(dir, emptyMap(), tsc, *TscOptions.toTypedArray(), "check.ts")
        }
    }

    @Test
    fun swiftCompilesTheTypesAndTheyReadAndWriteTheDocuments() {
        val swiftc = tool("swiftc")
        samples.forEach { sample ->
            val dir = folder("swift", sample)
            File(dir, "types.swift").writeText(written(sample, TypeLanguage.Swift))
            File(dir, "main.swift").writeText(swiftMain(sample.text, if (sample.list) "[${sample.root}]" else sample.root))
            run(dir, emptyMap(), swiftc, "-o", "check", "types.swift", "main.swift")
            run(dir, emptyMap(), File(dir, "check").absolutePath)
        }
    }

    /** The compiler named `formatico.<name>`; a test whose compiler is not given is skipped. */
    private fun tool(name: String): String {
        val path = System.getProperty("formatico.$name")
        Assume.assumeTrue("No $name is given: -Pformatico.$name=<path>", path != null)
        return checkNotNull(path)
    }

    private fun folder(language: String, sample: Sample): File =
        File(out, "$language/${sample.name}").apply {
            deleteRecursively()
            mkdirs()
        }

    private fun written(sample: Sample, language: TypeLanguage): String =
        checkNotNull(generateTypes(dataOf(sample.text), language, sample.root)) { "${sample.name} has no types" }.text

    /** Runs [command] in [dir], with [env], and fails with what it wrote where it does not end well. */
    private fun run(dir: File, env: Map<String, String>, vararg command: String) {
        val process = ProcessBuilder(*command).directory(dir).redirectErrorStream(true).apply { environment().putAll(env) }.start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) { "${command.joinToString(" ")} did not end" }
        assertEquals(0, process.exitValue(), "${command.joinToString(" ")} in $dir:\n$output")
    }

    private class Sample(val name: String, val text: String, val root: String, val list: Boolean)

    private companion object {
        const val TIMEOUT_MINUTES = 5L

        /** Strict, as a project that takes the types would be: what tsc checks the document against them with. */
        val TscOptions = listOf("--noEmit", "--strict", "--target", "es2022", "--module", "esnext", "--moduleResolution", "bundler")

        /**
         * A list of events, whose items differ: a name in snake case and names that are keywords, a date, an array empty
         * in every value, a member some items have and others not, one whose values are of two kinds, an array of
         * arrays, a member only ever null and a name that starts with a digit.
         */
        val Events =
            """
            [
              {
                "id": 1, "type": "open", "user_id": "u-1", "class": "a", "default": true, "at": "2026-11-07T10:00:00Z",
                "tags": [], "meta": { "source": "web", "size": 12 }, "value": 1, "matrix": [[1, 2], [3]], "note": null
              },
              {
                "id": 2, "type": "save", "user_id": "u-2", "class": "b", "default": false, "at": "2026-11-07T10:05:00Z",
                "tags": [], "meta": { "source": "app" }, "value": "two", "matrix": [], "extra": 2.5, "note": null, "2fa": true
              }
            ]
            """.trimIndent() + "\n"

        fun goTest(pkg: String, type: String) = """
            |package $pkg
            |
            |import (
            |	"bytes"
            |	"encoding/json"
            |	"os"
            |	"reflect"
            |	"testing"
            |)
            |
            |func TestTheDocumentIsReadAndWrittenAsItIs(t *testing.T) {
            |	data, err := os.ReadFile("document.json")
            |	if err != nil {
            |		t.Fatal(err)
            |	}
            |	decoder := json.NewDecoder(bytes.NewReader(data))
            |	decoder.DisallowUnknownFields()
            |	var value $type
            |	if err := decoder.Decode(&value); err != nil {
            |		t.Fatal(err)
            |	}
            |	written, err := json.Marshal(value)
            |	if err != nil {
            |		t.Fatal(err)
            |	}
            |	var before, after any
            |	if err := json.Unmarshal(data, &before); err != nil {
            |		t.Fatal(err)
            |	}
            |	if err := json.Unmarshal(written, &after); err != nil {
            |		t.Fatal(err)
            |	}
            |	if !reflect.DeepEqual(before, after) {
            |		t.Fatalf("written back otherwise:\n%s", written)
            |	}
            |}
            |
            """.trimMargin()

        fun swiftMain(document: String, type: String) = "import Foundation\n\nlet text = \"\"\"\n" + document.trimEnd() +
            "\n\"\"\"\nlet read = try JSONDecoder().decode($type.self, from: Data(text.utf8))\n" +
            "let written = try JSONEncoder().encode(read)\n_ = try JSONDecoder().decode($type.self, from: written)\nprint(\"ok\")\n"
    }
}
