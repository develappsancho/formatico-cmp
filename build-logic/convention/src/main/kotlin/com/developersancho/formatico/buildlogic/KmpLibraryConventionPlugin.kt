package com.developersancho.formatico.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

/**
 * A Kotlin Multiplatform library of Formatico, for every target the app runs on: Android, iOS, the desktop (JVM) and
 * the web (Wasm, and JavaScript for the browsers without Wasm GC). Pure Kotlin: a module that draws applies
 * `formatico.kmp.compose`, which builds on this one.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply(QualityConventionPlugin::class.java)

            extensions.configure<KotlinMultiplatformExtension> {
                androidLibrary {
                    namespace = formaticoNamespace
                    compileSdk = libs.version("android-compileSdk").toInt()
                    minSdk = libs.version("android-minSdk").toInt()
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_11)
                    }
                }
                iosArm64()
                iosSimulatorArm64()
                jvm()
                js {
                    // The JS build is the Wasm build's code: its tests compile, and run in the browser on Wasm.
                    browser {
                        testTask { enabled = false }
                    }
                }
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser {
                        testTask {
                            useKarma {
                                useChromeHeadless()
                                useConfigDirectory(rootDir.resolve("config/karma"))
                            }
                        }
                    }
                }

                sourceSets.commonTest.dependencies {
                    implementation(libs.library("kotlin-test"))
                }
            }

            // Klibs keep their sources' paths relative to the repository, not the building machine's own.
            tasks.withType<Kotlin2JsCompile>().configureEach {
                compilerOptions.freeCompilerArgs.add("-Xklib-relative-path-base=${rootDir.absolutePath}")
            }
        }
    }
}
