package com.developersancho.formatico.buildlogic

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

            extensions.configure<KotlinMultiplatformExtension> {
                js {
                    browser {
                        commonWebpackConfig { outputFileName = jsScript }
                        testTask { enabled = false }
                    }
                    binaries.executable()
                }
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser {
                        commonWebpackConfig { outputFileName = wasmScript }
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
