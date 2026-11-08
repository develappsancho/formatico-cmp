package com.developersancho.formatico.feature.settings

import formatico.feature.settings.generated.resources.Res

/** A licence a library is used under, by its name and the file of this module's resources that holds its text. */
enum class OpenSourceLicence(val title: String, internal val file: String) {
    Apache("Apache License 2.0", "files/licenses/apache-2.0.txt"),

    /** navigation3-browser's: the MIT License with its author's notice. */
    MitNavigation("MIT License", "files/licenses/navigation3-browser.txt"),

    Ofl("SIL Open Font License 1.1", "files/licenses/geist-ofl.txt")
}

/**
 * A library Formatico is built on, as the licences page lists it: its [title], its [licence], and whether it ships
 * [inTheWebApp] alone.
 */
enum class OpenSourceLibrary(val title: String, val licence: OpenSourceLicence, val inTheWebApp: Boolean = false) {
    Compose("Compose Multiplatform", OpenSourceLicence.Apache),
    Kotlin("Kotlin, kotlinx.coroutines and kotlinx.serialization", OpenSourceLicence.Apache),
    AndroidX("AndroidX Navigation 3 and Lifecycle", OpenSourceLicence.Apache),
    Pantrix("Pantrix UI", OpenSourceLicence.Apache),
    Koin("Koin", OpenSourceLicence.Apache),
    KSafe("KSafe", OpenSourceLicence.Apache),
    Cryptography("cryptography-kotlin", OpenSourceLicence.Apache),
    SnakeYaml("snakeyaml-engine-kmp", OpenSourceLicence.Apache),
    NavigationBrowser("navigation3-browser", OpenSourceLicence.MitNavigation, inTheWebApp = true),
    Geist("Geist and Geist Mono", OpenSourceLicence.Ofl)
}

/** The text of [licence], as its file holds it. */
internal suspend fun licenceText(licence: OpenSourceLicence): String = Res.readBytes(licence.file).decodeToString()
