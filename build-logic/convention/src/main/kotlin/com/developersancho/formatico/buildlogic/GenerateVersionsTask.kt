package com.developersancho.formatico.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Writes `FormaticoVersions.kt` in [packageName]: Formatico's version, `formatico.version` of `gradle.properties`, and
 * the version of Pantrix UI it is built with, the catalog's: what Settings' About says, from the one place the build
 * reads them.
 */
@CacheableTask
abstract class GenerateVersionsTask : DefaultTask() {
    /** The package the object is written in. */
    @get:Input
    abstract val packageName: Property<String>

    /** Formatico's version. */
    @get:Input
    abstract val appVersion: Property<String>

    /** Pantrix UI's version. */
    @get:Input
    abstract val pantrixVersion: Property<String>

    /** Where the source is written, as a source directory of Kotlin. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val directory = outputDirectory.get().asFile.resolve(packageName.get().replace('.', '/'))
        directory.mkdirs()
        directory.resolve("FormaticoVersions.kt").writeText(
            """
            |package ${packageName.get()}
            |
            |/** The versions the build was made with: written by the build's `generateVersions`, not by hand. */
            |internal object FormaticoVersions {
            |    /** Formatico's version. */
            |    const val APP = "${appVersion.get()}"
            |
            |    /** The version of Pantrix UI Formatico is built with. */
            |    const val PANTRIX = "${pantrixVersion.get()}"
            |}
            |
            """.trimMargin()
        )
    }
}
