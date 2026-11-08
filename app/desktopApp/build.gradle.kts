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
        // A 50 MB document read, its tree and its editor take some 2.5 GB (Faz 14's measure): half the machine's
        // memory rather than the JVM's quarter, which is 2 GB on a machine of 8.
        jvmArgs += listOf("-XX:MaxRAMPercentage=50")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            // The Java runtime the app is packed with holds only the modules named: Compose's own few, and these.
            // `jdeps` over the app's jars names the first four (deploy.sh desktop checks it again on each build):
            // java.net.http for Ktor's client, java.management for KSafe and Ktor, jdk.unsupported and
            // java.instrument for kotlinx.coroutines. jdeps sees no use of the last two: jdk.accessibility is
            // Windows' bridge to its screen readers, jdk.charsets the encodings beyond UTF-8 a file may come in.
            // 1.0.0 went out without them and stopped at its start on java.net.http (2026-11-12).
            modules(
                "java.net.http", "java.management", "jdk.unsupported", "java.instrument", "jdk.accessibility",
                "jdk.charsets"
            )
            packageName = "Formatico"
            packageVersion = providers.gradleProperty("formatico.version").get()
            // The icons are drawn by tools/icons/generate.py from the canvas's icon board.
            macOS {
                bundleID = "com.developersancho.formatico"
                iconFile.set(project.file("icons/Formatico.icns"))
            }
            // The MSI is built on a Windows machine (deploy/windows.sh). Its upgrade code stays the same for good: it
            // is how Windows knows a new version for the same app, and replaces the one installed.
            windows {
                iconFile.set(project.file("icons/Formatico.ico"))
                upgradeUuid = "c65b3e56-5b76-486f-80a2-fbb258a3293c"
                menuGroup = "Formatico"
                menu = true
                shortcut = true
                dirChooser = true
                // For the user alone, in their own folder: no administrator is asked, which an unsigned installer
                // would have to be granted.
                perUserInstall = true
            }
            linux {
                iconFile.set(project.file("icons/Formatico.png"))
            }
        }
    }
}
