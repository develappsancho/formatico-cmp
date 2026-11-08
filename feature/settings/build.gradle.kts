import com.developersancho.formatico.buildlogic.GenerateVersionsTask

plugins {
    alias(libs.plugins.formatico.feature)
}

// What About says: Formatico's version and Pantrix UI's, as the build has them.
val generateVersions = tasks.register<GenerateVersionsTask>("generateVersions") {
    packageName.set("com.developersancho.formatico.feature.settings")
    appVersion.set(providers.gradleProperty("formatico.version"))
    pantrixVersion.set(libs.versions.pantrix)
    outputDirectory.set(layout.buildDirectory.dir("generated/versions/kotlin"))
}

kotlin {
    sourceSets {
        commonMain {
            kotlin.srcDir(generateVersions.map { it.outputDirectory })
            dependencies {
                // What Settings changes is kept in core:settings.
                api(projects.core.settings)
                // A phone's Settings is a screen of its own, which the system's back leaves, a page at a time.
                implementation(libs.navigationevent.compose)
            }
        }
    }
}
