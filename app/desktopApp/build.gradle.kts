import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.formatico.quality)
}

dependencies {
    implementation(projects.app.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
}

compose.desktop {
    application {
        mainClass = "com.developersancho.formatico.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Formatico"
            packageVersion = providers.gradleProperty("formatico.version").get()
            // The icons are drawn by tools/icons/generate.py from the canvas's icon board.
            macOS {
                bundleID = "com.developersancho.formatico"
                iconFile.set(project.file("icons/Formatico.icns"))
            }
            windows {
                iconFile.set(project.file("icons/Formatico.ico"))
            }
            linux {
                iconFile.set(project.file("icons/Formatico.png"))
            }
        }
    }
}
