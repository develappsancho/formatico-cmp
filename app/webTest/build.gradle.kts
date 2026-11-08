plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.formatico.quality)
}

// The web app as a browser runs it: the distribution `wasmJsBrowserDistribution` builds, the Wasm build and the
// JavaScript one its loader falls back to, served from this machine and driven in Chrome through its DevTools.
dependencies {
    testImplementation(libs.kotlin.testJunit)
    testImplementation(libs.kotlinx.serialization.json)
}

val distribution = project(":app:webApp").layout.buildDirectory.dir("dist/wasmJs/productionExecutable")

// The headers nginx gives every answer on the host, its Content-Security-Policy among them: the tests serve the app
// with them, so that a page they pass is one the policy lets run.
val headers = rootProject.layout.projectDirectory.file("deploy/web/headers.conf")

tasks.test {
    // The distribution, and the JavaScript build its loader falls back to, which is copied into it.
    dependsOn(":app:webApp:wasmJsBrowserDistribution", ":app:webApp:copyJsApp")
    inputs.dir(distribution)
    inputs.file(headers)
    systemProperty("formatico.web.dist", distribution.get().asFile.absolutePath)
    systemProperty("formatico.web.headers", headers.asFile.absolutePath)
    // The large documents' measure (Faz 21) runs only when asked for, `-Pformatico.measure`, on the samples
    // `-Pformatico.measure.samples` names.
    if (providers.gradleProperty("formatico.measure").isPresent) {
        systemProperty("formatico.measure", "true")
        providers.gradleProperty("formatico.measure.samples").orNull?.let { systemProperty("formatico.measure.samples", it) }
        outputs.upToDateWhen { false }
    }
    // The Chrome this machine has, unless another is named: -Pformatico.chrome=<path>.
    providers.gradleProperty("formatico.chrome").orNull?.let { systemProperty("formatico.chrome", it) }
}
