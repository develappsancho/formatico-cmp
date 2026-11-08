import org.gradle.initialization.DependenciesAccessors
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    `kotlin-dsl`
}

group = "com.developersancho.formatico.buildlogic"

dependencies {
    gradle.serviceOf<DependenciesAccessors>().classes.asFiles.forEach {
        compileOnly(files(it.absolutePath))
    }

    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
    compileOnly(libs.kotlinter.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.formatico.kmpLibrary.get().pluginId
            implementationClass = "com.developersancho.formatico.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.formatico.kmpCompose.get().pluginId
            implementationClass = "com.developersancho.formatico.buildlogic.KmpComposeConventionPlugin"
        }
        register("feature") {
            id = libs.plugins.formatico.feature.get().pluginId
            implementationClass = "com.developersancho.formatico.buildlogic.FeatureConventionPlugin"
        }
        register("quality") {
            id = libs.plugins.formatico.quality.get().pluginId
            implementationClass = "com.developersancho.formatico.buildlogic.QualityConventionPlugin"
        }
        register("webApplication") {
            id = libs.plugins.formatico.webApplication.get().pluginId
            implementationClass = "com.developersancho.formatico.buildlogic.WebApplicationConventionPlugin"
        }
    }
}
