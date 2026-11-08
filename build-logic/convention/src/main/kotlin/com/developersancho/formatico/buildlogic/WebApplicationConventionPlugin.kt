package com.developersancho.formatico.buildlogic

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

/**
 * The web application: one app built twice, as Wasm and as JavaScript, in one folder, and a loader that picks the
 * build the browser can run (Pantrix UI's guide "Old Browsers and a Wasm That Will Not Start, on the Web").
 *
 * The scripts are `<module>-wasm.js` and `<module>-js.js`; the page loads `<module>.js`, the loader, from the module's
 * resources.
 */
class WebApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply(QualityConventionPlugin::class.java)

            val jsScript = "${project.name}-js.js"
            val wasmScript = "${project.name}-wasm.js"
            // Formatico's own webpack plugins: strip-file-paths.js keeps the building machine's paths out of the scripts.
            val webpackConfig = rootDir.resolve("config/webpack")
            val machinePaths = listOf(rootDir.absolutePath, System.getProperty("user.home"))

            extensions.configure<KotlinMultiplatformExtension> {
                js {
                    browser {
                        commonWebpackConfig {
                            outputFileName = jsScript
                            configDirectory = webpackConfig
                        }
                        testTask { enabled = false }
                    }
                    binaries.executable()
                }
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser {
                        commonWebpackConfig {
                            outputFileName = wasmScript
                            configDirectory = webpackConfig
                        }
                    }
                    binaries.executable()
                }
            }

            tasks.withType<Kotlin2JsCompile>().configureEach {
                compilerOptions.freeCompilerArgs.add("-Xklib-relative-path-base=${rootDir.absolutePath}")
            }

            // The JS script and its Wasm (Skia's, the same file in both builds) go beside the Wasm build.
            val copyJs = tasks.register<Copy>("copyJsApp") {
                mustRunAfter("jsBrowserDistribution")
                from(layout.buildDirectory.dir("dist/js/productionExecutable")) {
                    include(jsScript, "*.wasm")
                    exclude("skiko.wasm")
                }
                into(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
                // What goes out with the app names no folder of the machine that built it.
                doLast { destinationDir.assertNamesNone(machinePaths) }
            }
            // The JS distribution is a dependency, the copy only a finalizer: a finalizer that waited on Kotlin's JS
            // tasks, which have finalizers of their own, would stop Gradle with "Unable to make progress".
            tasks.named { it == "wasmJsBrowserDistribution" }.configureEach {
                dependsOn("jsBrowserDistribution")
                finalizedBy(copyJs)
            }
        }
    }
}

/** Fails where a file in this folder holds one of [paths]: the place a build was made, or its user's home. */
private fun File.assertNamesNone(paths: List<String>) {
    val found = walkTopDown().filter { it.isFile }.flatMap { file ->
        val content = file.readText(Charsets.ISO_8859_1)
        paths.filter { it in content }.map { "${file.relativeTo(this)}: $it" }
    }.toList()
    if (found.isNotEmpty()) {
        throw GradleException("The web build names the machine that built it:\n" + found.joinToString("\n"))
    }
}
