package com.developersancho.formatico.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Fails where the app's words in English (`values/strings.xml`) and in Turkish (`values-tr/strings.xml`) part: a word
 * one language has and the other has not, a placeholder (`%1$s`) one writes and the other does not, a plural without
 * the `other` its language needs, or a Turkish word still the English one. A word the same in both languages, a
 * format's name or a symbol, is named in [same], with why.
 */
@CacheableTask
abstract class TranslationsTask : DefaultTask() {
    /** Every `strings.xml` of the module's Compose resources, in English and in Turkish. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** The words written the same in both languages, by name, one to a line; `#` starts a note. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val same: RegularFileProperty

    /** Written once the words have passed, so that the task is up to date until one changes. */
    @get:OutputFile
    abstract val passed: RegularFileProperty

    @TaskAction
    fun check() {
        val allowed = same.orNull?.asFile?.takeIf(File::exists)?.readLines().orEmpty()
            .map { it.substringBefore('#').trim() }
            .filter(String::isNotEmpty)
            .toSet()
        val english = sources.files.filter { it.parentFile.name == "values" }.sortedBy { it.path }
        val found = english.flatMap { file ->
            val turkish = File(file.parentFile.parentFile, "values-tr/${file.name}")
            if (!turkish.exists()) listOf("$file has no Turkish beside it, $turkish") else compare(file, turkish, allowed)
        }
        if (found.isNotEmpty()) throw GradleException("The words in English and in Turkish part:\n" + found.joinToString("\n"))
        passed.get().asFile.writeText("${english.size} files of words\n")
    }

    private fun compare(englishFile: File, turkishFile: File, allowed: Set<String>): List<String> {
        val english = words(englishFile)
        val turkish = words(turkishFile)
        val found = mutableListOf<String>()
        (english.keys - turkish.keys).sorted().forEach { found += "$turkishFile: \"$it\" is not in Turkish" }
        (turkish.keys - english.keys).sorted().forEach { found += "$englishFile: \"$it\" is not in English" }
        english.keys.intersect(turkish.keys).sorted().forEach { name ->
            val en = english.getValue(name)
            val tr = turkish.getValue(name)
            val enHolders = en.values.flatMap(::placeholders).toSet()
            val trHolders = tr.values.flatMap(::placeholders).toSet()
            if (enHolders != trHolders) found += "$turkishFile: \"$name\" holds ${trHolders.sorted()}, the English ${enHolders.sorted()}"
            if (en.size > 1 || tr.size > 1) {
                if ("other" !in tr) found += "$turkishFile: the plural \"$name\" has no \"other\""
                if ("other" !in en) found += "$englishFile: the plural \"$name\" has no \"other\""
            }
            if (name !in allowed && en == tr && en.values.any(::hasWords)) {
                found += "$turkishFile: \"$name\" is the English \"${en.values.first()}\"; translate it, or name it in the list of words the same in both"
            }
        }
        allowed.filter { it in english && english[it] != turkish[it] }.sorted().forEach {
            found += "\"$it\" is named the same in both languages, but is not: take it off the list"
        }
        return found
    }

    /** The file's words by name: a string as one text under `""`, a plural as its texts by quantity. */
    private fun words(file: File): Map<String, Map<String, String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        val words = linkedMapOf<String, Map<String, String>>()
        val children = root.childNodes
        for (index in 0 until children.length) {
            val element = children.item(index) as? Element ?: continue
            val name = element.getAttribute("name")
            when (element.tagName) {
                "string" -> words[name] = mapOf("" to element.textContent)
                "plurals" -> {
                    val items = element.getElementsByTagName("item")
                    words[name] = (0 until items.length).associate { item ->
                        val quantity = items.item(item) as Element
                        quantity.getAttribute("quantity") to quantity.textContent
                    }
                }
            }
        }
        return words
    }

    private companion object {
        /** A placeholder: `%1$s`, `%2$d`, `%s`. */
        val PLACEHOLDER = Regex("%(\\d+\\$)?[sdf]")

        /** A word of three letters or more, which a format's name or a symbol is not. */
        val WORD = Regex("\\p{Ll}{3,}")

        fun placeholders(text: String): List<String> = PLACEHOLDER.findAll(text).map { it.value }.toList()

        fun hasWords(text: String): Boolean = WORD.containsMatchIn(PLACEHOLDER.replace(text, ""))
    }
}
