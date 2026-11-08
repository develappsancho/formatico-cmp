package com.developersancho.formatico.buildlogic

import com.developersancho.formatico.buildlogic.utils.androidRuntimeClasspath
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A Kotlin Multiplatform library that draws: `formatico.kmp.library` with Compose Multiplatform, its resources and
 * Pantrix UI, and Pantrix's testing module for its tests. Its screenshots are Roborazzi's, drawn on the desktop by `:core:testing`:
 * `recordRoborazziJvm` writes them under the module's `screenshots/`, `verifyRoborazziJvm` compares against them.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpLibraryConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("io.github.takahirom.roborazzi")
            val desktopSkiko = extensions.getByType<ComposeExtension>().dependencies.desktop.currentOs

            extensions.configure<KotlinMultiplatformExtension> {
                androidLibrary {
                    androidResources { enable = true }
                }
                // A UI test in the browser is bundled with Skiko only where the target makes an executable
                // (CMP-4906): the module's tests run as one, the library is published as it was. Pantrix's testing
                // guide does not say so (PANTRIX_FINDINGS, "Test rehberi tarayıcıda UI testinin iki şartını söylemiyor").
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    binaries.executable()
                }

                sourceSets.commonMain.dependencies {
                    implementation(libs.library("compose-runtime"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-components-resources"))
                    implementation(libs.library("compose-uiToolingPreview"))
                    implementation(libs.library("pantrix-components"))
                }
                sourceSets.commonTest.dependencies {
                    implementation(libs.library("pantrix-testing"))
                }
                // runComposeUiTest draws on the desktop: the test JVM needs this machine's Skiko, and a main dispatcher
                // for the view models a screen holds, Swing's as the desktop app's.
                sourceSets.jvmTest.dependencies {
                    implementation(desktopSkiko)
                    implementation(libs.library("kotlinx-coroutinesSwing"))
                }
            }

            // What Android Studio draws a preview with. It goes on the module's own runtime classpath and nowhere else:
            // as a dependency of androidMain it travelled to every app that uses the library, release builds included,
            // and took its preview activity into their manifests.
            dependencies {
                androidRuntimeClasspath(libs.library("compose-uiTooling"))
            }
        }
    }
}
