plugins {
    alias(libs.plugins.formatico.feature)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // What it opens is in a format of core:format's, which the app hands on.
            api(projects.core.format)
            // The history is kept on this device, in core:storage's stores.
            implementation(projects.core.storage)
            // Whether a history is kept, and what a text of no format told becomes, are Settings'.
            implementation(projects.core.settings)
            // A link of Formatico's, pasted or opened, carries a document of its own, or names an encrypted share; a web
            // address holds one too, both fetched through core:network.
            implementation(projects.core.share)
            implementation(projects.core.network)
        }
    }
}
