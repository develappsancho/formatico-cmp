package com.developersancho.formatico.core.storage

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeConfig
import java.io.File

internal actual fun deviceStores(): DeviceStores {
    val place = desktopPlace(
        installed = System.getProperty(INSTALLED_APP) != null,
        system = System.getProperty("os.name"),
        home = System.getProperty("user.home"),
        environment = System::getenv
    )
    val ksafe = KSafe(
        fileName = STORE_NAME,
        baseDir = File(place.directory, "ksafe"),
        config = KSafeConfig(appNamespace = place.namespace)
    )
    return DeviceStores(
        small = KSafeSmallStore(ksafe),
        texts = encryptedTexts(ksafe, FileTextStore(File(place.directory, "texts")))
    )
}

internal actual suspend fun KSafe.awaitReady() = Unit

/** What jpackage's launcher, the installed app's, tells the app it was started from; a run from the code has none. */
internal const val INSTALLED_APP = "jpackage.app-path"

/** The name KSafe keeps the installed app's keys under in the system's vault. */
internal const val APP_NAMESPACE = "com.developersancho.formatico"

/** The name a run from the code keeps its keys under: Gradle's `run`, or the IDE's. */
internal const val DEVELOPMENT_NAMESPACE = "$APP_NAMESPACE.development"

/**
 * Where the desktop keeps Formatico's data: [directory], the system's place for an app's own data, which no backup of
 * documents takes; and [namespace], the name its keys go under in the system's vault (macOS' login keychain, Windows'
 * DPAPI, Linux's Secret Service).
 */
internal data class DesktopPlace(val directory: File, val namespace: String)

/**
 * The [DesktopPlace] of the app as [installed], or of a run from the code, which keeps its own: its history and its
 * keys never meet the installed app's.
 *
 * The keychain lets the app that made an item read it unasked, and asks the login password of any other. Until 1.0.2
 * every run kept its keys under KSafe's shared name (`eu.anifantakis.ksafe.shared`), and the items a run from the code
 * had made asked the installed app for the password at each start (2026-11-12). KSafe reads a key it lacks from that
 * shared name once and keeps it under the new one: an older install's history stays, at the cost of one question.
 */
internal fun desktopPlace(
    installed: Boolean,
    system: String,
    home: String,
    environment: (String) -> String?
): DesktopPlace {
    val os = system.lowercase()
    val name = if (installed) "Formatico" else "Formatico Development"
    val directory = when {
        "mac" in os -> File(home, "Library/Application Support/$name")
        "win" in os -> File(environment("APPDATA") ?: home, name)
        else -> File(environment("XDG_DATA_HOME") ?: "$home/.local/share", name.lowercase().replace(' ', '-'))
    }
    return DesktopPlace(directory, if (installed) APP_NAMESPACE else DEVELOPMENT_NAMESPACE)
}
