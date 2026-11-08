plugins {
    alias(libs.plugins.formatico.feature)
}

// The download menu (Faz 22): Formatico's app for macOS, Windows and Android, from its latest release on GitHub, which
// Start and Settings offer on the desktop and the web. Not a screen of its own: a button and the menu it opens.
kotlin {
    sourceSets {
        commonMain.dependencies {
            // The latest release is read from GitHub's API through core:network.
            implementation(projects.core.network)
        }
    }
}
