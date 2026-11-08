package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitText
import com.developersancho.formatico.core.testing.inEnglish
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * The language chosen in Settings, on the desktop, where it is the JVM's: the test gives the JVM its language back as it
 * ends, as every test that reads the app's words in English needs.
 */
@OptIn(ExperimentalTestApi::class)
class AppLanguageTest {
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    @Test
    fun aLanguageChosenInSettingsIsTheOneTheAppSpeaksAtOnce() = inEnglish {
        runComposeUiTest {
            setContent {
                PantrixTestTheme(platform = PlatformType.Desktop, window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                    App(systemDark = false, storage = memoryStorageModule)
                }
            }
            awaitText("Paste anything")
            onPantrixNode("IconButton", "settings").performClick()
            awaitText("Language")

            onPantrixNode("Select", "language").performClick()
            onPantrixNode("SelectOption", "tr", scope = "Select_language").performClick()

            // The app's own words, and Settings is still where the reader was.
            awaitText("Dil")
            awaitText("Belgelerin geçmişini tut")
            onPantrixNode("Tab", "settings", scope = "Tabs_documents").assertExists()
        }
    }
}
