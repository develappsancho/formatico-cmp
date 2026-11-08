plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.roborazzi) apply false
}

// The build logic is a build of its own: the root's lintKotlin, formatKotlin and detekt look at it as well.
listOf("lintKotlin", "formatKotlin", "detekt").forEach { name ->
    tasks.register(name) { dependsOn(gradle.includedBuild("build-logic").task(":convention:$name")) }
}
