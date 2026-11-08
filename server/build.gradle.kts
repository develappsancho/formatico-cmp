plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.formatico.quality)
}

// Formatico's server (Faz 16), as core:share's ShareServer sets it out: it keeps the encrypted shares, which it cannot
// read, and fetches a document from a public address for the web app, whose browser may not read another site.
// Spring Boot on WebFlux, in Docker behind nginx: `bootJar` writes build/libs/server.jar.
kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(projects.core.share)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.okhttp.mockwebserver)
    // kotlin-test on JUnit 5, whose kotlin-test-junit5 also brings the JUnit Platform's launcher Gradle 9 needs.
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("server.jar")
}

// The plain jar would sit beside server.jar in build/libs; nothing runs it.
tasks.jar {
    enabled = false
}
