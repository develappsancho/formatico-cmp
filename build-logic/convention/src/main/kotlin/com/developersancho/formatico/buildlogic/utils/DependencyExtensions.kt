package com.developersancho.formatico.buildlogic.utils

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler

internal fun DependencyHandler.implementation(dependency: Any) {
    add("implementation", dependency)
}

internal fun DependencyHandler.androidRuntimeClasspath(dependency: Any) {
    add("androidRuntimeClasspath", dependency)
}

internal fun DependencyHandler.ksp(dependency: Any) {
    add("ksp", dependency)
}

internal fun DependencyHandler.compileOnly(dependency: Any) {
    add("compileOnly", dependency)
}

internal fun DependencyHandler.coreLibraryDesugaring(dependency: Any) {
    add("coreLibraryDesugaring", dependency)
}

internal fun DependencyHandler.project(
    path: String,
    configuration: String? = null
): Dependency {
    return project(
        mapOf(
            "path" to path,
            "configuration" to configuration
        ).filterValues { it != null }
    )
}
