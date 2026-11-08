plugins {
    alias(libs.plugins.formatico.feature)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The documents it holds are in a format of core:format's, which the app hands on.
            api(projects.core.format)
            // The open documents are kept on this device, in core:storage's stores.
            implementation(projects.core.storage)
            // Format writes as Settings chose.
            implementation(projects.core.settings)
            // A phone's Convert is a screen of its own, which the system's back leaves: navigation3's own version.
            implementation(libs.navigationevent.compose)
        }
    }
}
