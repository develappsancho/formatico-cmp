package com.developersancho.formatico.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Fails where a Kotlin source holds a character that cannot be seen: a byte order mark, a space that does not break,
 * a joiner, a mark of the writing's direction. Written as itself, Kotlinter's formatter drops a byte order mark and
 * `''` no longer compiles, and a direction mark can make code read otherwise than it runs; in a source each is
 * written as its escape, `'\ufeff'`.
 */
@CacheableTask
abstract class InvisibleCharactersTask : DefaultTask() {
    /** The sources looked at. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** Written once every source has passed, so that the task is up to date until one changes. */
    @get:OutputFile
    abstract val passed: RegularFileProperty

    @TaskAction
    fun check() {
        val found = sources.files.sortedBy { it.path }.flatMap { file ->
            file.readLines().flatMapIndexed { index, line ->
                line.mapIndexedNotNull { column, char ->
                    INVISIBLE[char]?.let { "${file.path}:${index + 1}:${column + 1}: $it, write it as '\\u%04X'".format(char.code) }
                }
            }
        }
        if (found.isNotEmpty()) {
            throw GradleException("Characters that cannot be seen, in Kotlin sources:\n" + found.joinToString("\n"))
        }
        passed.get().asFile.writeText("${sources.files.size} sources\n")
    }

    private companion object {
        val INVISIBLE: Map<Char, String> = buildMap {
            put('\u00a0', "a space that does not break")
            put('\u00ad', "a soft hyphen")
            put('\u200b', "a space of no width")
            put('\u200c', "a joiner of no width")
            put('\u200d', "a joiner of no width")
            for (char in '\u200e'..'\u200f') put(char, "a mark of the writing's direction")
            for (char in '\u2028'..'\u2029') put(char, "a line or paragraph separator")
            for (char in '\u202a'..'\u202e') put(char, "a mark of the writing's direction")
            for (char in '\u2060'..'\u2064') put(char, "a joiner or an operator that cannot be seen")
            for (char in '\u2066'..'\u2069') put(char, "a mark of the writing's direction")
            put('\ufeff', "a byte order mark")
        }
    }
}
