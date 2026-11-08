package com.developersancho.formatico.feature.settings

import formatico.feature.settings.generated.resources.Res

/** A licence a library is used under, by its name and the file of this module's resources that holds its text. */
enum class OpenSourceLicence(val title: String, internal val file: String) {
    Apache("Apache License 2.0", "files/licenses/apache-2.0.txt"),

    /** navigation3-browser's: the MIT License with its author's notice. */
    MitNavigation("MIT License", "files/licenses/navigation3-browser.txt"),

    /** The MIT License with json-schema-validator's notice. */
    MitSchemaValidator("MIT License", "files/licenses/json-schema-validator.txt"),

    /** The MIT License with karacteristics' notice. */
    MitKaracteristics("MIT License", "files/licenses/karacteristics.txt"),

    /** The MIT License with doistx-normalize's notice. */
    MitNormalize("MIT License", "files/licenses/doistx-normalize.txt"),

    /** The MIT License with kotlin-codepoints' notice. */
    MitCodepoints("MIT License", "files/licenses/kotlin-codepoints.txt"),

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
    SchemaValidator("json-schema-validator", OpenSourceLicence.MitSchemaValidator),
    Karacteristics("karacteristics", OpenSourceLicence.MitKaracteristics),
    UriKmp("uri-kmp", OpenSourceLicence.Apache),
    Normalize("doistx-normalize", OpenSourceLicence.MitNormalize),
    Codepoints("kotlin-codepoints", OpenSourceLicence.MitCodepoints),
    NavigationBrowser("navigation3-browser", OpenSourceLicence.MitNavigation, inTheWebApp = true),
    Geist("Geist and Geist Mono", OpenSourceLicence.Ofl)
}

/** The text of [licence], as its file holds it. */
internal suspend fun licenceText(licence: OpenSourceLicence): String = Res.readBytes(licence.file).decodeToString()
