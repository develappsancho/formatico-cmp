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
            // A document is shared in a link that carries it, as core:share makes one, or as an encrypted link
            // Formatico's server keeps, through core:network.
            implementation(projects.core.share)
            implementation(projects.core.network)
            // A phone's Convert is a screen of its own, which the system's back leaves.
            implementation(projects.core.navigation)
        }
    }
}

// The large documents' measure (Faz 14) runs only when asked for, `-Pformatico.measure`, with the heap a document of
// 50 MB needs; `-Pformatico.measure.sizes` and `-Pformatico.measure.formats` narrow it.
tasks.withType<Test>().matching { it.name == "jvmTest" }.configureEach {
    val measure = providers.gradleProperty("formatico.measure")
    if (measure.isPresent) {
        systemProperty("formatico.measure", "true")
        providers.gradleProperty("formatico.measure.sizes").orNull?.let { systemProperty("formatico.measure.sizes", it) }
        providers.gradleProperty("formatico.measure.formats").orNull?.let { systemProperty("formatico.measure.formats", it) }
        maxHeapSize = "8g"
        // A measure is what it measures, every time: Gradle keeps no result of one.
        outputs.upToDateWhen { false }
    }
}
