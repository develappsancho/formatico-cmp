package com.developersancho.formatico.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A feature of Formatico: one screen and what it holds, as a `formatico.kmp.compose` library with the MVI bases
 * (`:core:base`), the theme (`:core:designsystem`) and Koin. A feature does not navigate: it says what the user asked
 * for, and the app goes there.
 */
class FeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(KmpComposeConventionPlugin::class.java)

            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.commonMain.dependencies {
                    implementation(project(":core:base"))
                    implementation(project(":core:designsystem"))
                    implementation(libs.library("kotlinx-coroutines-core"))
                    // MviScreen collects with the lifecycle: the catalog's version, above what Koin and
                    // navigation3 ask for.
                    implementation(libs.library("androidx-lifecycle-runtimeCompose"))
                    implementation(libs.library("koin-core"))
                    implementation(libs.library("koin-compose"))
                    implementation(libs.library("koin-compose-viewmodel"))
                }
                sourceSets.commonTest.dependencies {
                    implementation(project(":core:testing"))
                    implementation(libs.library("kotlinx-coroutines-test"))
                }
            }
        }
    }
}
