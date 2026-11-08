package com.developersancho.formatico.buildlogic

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

/**
 * Detekt with Pantrix UI's rules (`com.pantrixui:detekt-rules`) and Kotlinter, in every module.
 *
 * Detekt reads the whole of `src` but the tests, so that a multiplatform module's `commonMain`, `androidMain`,
 * `iosMain`… are all looked at by the plain `detekt` task. Kotlinter checks the form (`lintKotlin`, `formatKotlin`).
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")
            pluginManager.apply("org.jmailen.kotlinter")

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                parallel = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                source.setFrom(file("src"))
            }
            dependencies.add("detektPlugins", libs.library("pantrix-detektRules"))

            // Kotlinter reads every Kotlin source set, the code Compose's resources generate under build/ among them.
            tasks.withType<LintTask>().configureEach {
                exclude { it.file.invariantSeparatorsPath.contains("/build/") }
            }
            tasks.withType<FormatTask>().configureEach {
                exclude { it.file.invariantSeparatorsPath.contains("/build/") }
            }

            tasks.withType<Detekt>().configureEach {
                exclude("**/*Test/**", "**/build/**")
                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                    xml.required.set(false)
                    txt.required.set(false)
                    md.required.set(false)
                }
            }
        }
    }
}
