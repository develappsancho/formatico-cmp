package com.developersancho.formatico.core.storage

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPlaceTest {
    private val home = "/Users/elif"
    private val noEnvironment: (String) -> String? = { null }

    @Test
    fun theInstalledAppKeepsItsDataInApplicationSupportAndItsKeysUnderItsOwnName() {
        assertEquals(
            DesktopPlace(File(home, "Library/Application Support/Formatico"), "com.developersancho.formatico"),
            desktopPlace(installed = true, system = "Mac OS X", home = home, environment = noEnvironment)
        )
    }

    @Test
    fun aRunFromTheCodeKeepsApartFromTheInstalledApp() {
        val installed = desktopPlace(installed = true, system = "Mac OS X", home = home, environment = noEnvironment)
        val development = desktopPlace(installed = false, system = "Mac OS X", home = home, environment = noEnvironment)

        assertEquals(
            DesktopPlace(
                File(home, "Library/Application Support/Formatico Development"),
                "com.developersancho.formatico.development"
            ),
            development
        )
        assert(installed.directory != development.directory && installed.namespace != development.namespace)
    }

    @Test
    fun windowsKeepsTheDataInTheRoamingAppData() {
        val environment = mapOf("APPDATA" to "C:\\Users\\elif\\AppData\\Roaming")

        assertEquals(
            File("C:\\Users\\elif\\AppData\\Roaming", "Formatico"),
            desktopPlace(installed = true, system = "Windows 11", home = home, environment = environment::get).directory
        )
        assertEquals(
            File(home, "Formatico Development"),
            desktopPlace(installed = false, system = "Windows 11", home = home, environment = noEnvironment).directory
        )
    }

    @Test
    fun linuxKeepsTheDataWhereXdgSaysOrInTheHomesShare() {
        assertEquals(
            File("$home/.local/share", "formatico"),
            desktopPlace(installed = true, system = "Linux", home = home, environment = noEnvironment).directory
        )
        val environment = mapOf("XDG_DATA_HOME" to "/data")

        assertEquals(
            File("/data", "formatico-development"),
            desktopPlace(installed = false, system = "Linux", home = home, environment = environment::get).directory
        )
    }
}
