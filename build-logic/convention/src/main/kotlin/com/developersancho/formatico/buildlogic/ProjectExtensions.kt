package com.developersancho.formatico.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** The version catalog of the root build, `gradle/libs.versions.toml`. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

/** The package every module of Formatico lives under: `:core:format` is `com.developersancho.formatico.core.format`. */
internal val Project.formaticoNamespace: String
    get() = "com.developersancho.formatico" + path.replace(':', '.')

/** The Android target the `com.android.kotlin.multiplatform.library` plugin adds to the Kotlin extension. */
internal fun KotlinMultiplatformExtension.androidLibrary(configure: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android", configure)
}
