package com.developersancho.formatico

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.storage.memoryStorageModule
import com.developersancho.formatico.core.testing.awaitText
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onPantrixNode
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test

/** The language chosen in Settings: the theme's, which every feature's words follow at once, on every platform. */
@OptIn(ExperimentalTestApi::class)
class AppLanguageTest {
    @AfterTest
    fun closeTheApp() {
        stopKoin()
    }

    @Test
    fun aLanguageChosenInSettingsIsTheOneTheAppSpeaksAtOnce() = runComposeUiTest {
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

    @Test
    fun theCommentThatMarksANullLeftOutIsInTheLanguageChosen() = runComposeUiTest {
        setContent {
            PantrixTestTheme(platform = PlatformType.Desktop, window = PantrixWindow(DpSize(1440.dp, 900.dp))) {
                App(systemDark = false, storage = memoryStorageModule)
            }
        }
        awaitText("Paste anything")
        onPantrixNode("IconButton", "settings").performClick()
        onPantrixNode("Select", "language").performClick()
        onPantrixNode("SelectOption", "tr", scope = "Select_language").performClick()
        awaitText("Dil")

        onPantrixNode("IconButton", "new_tab").performClick()
        onPantrixNode("ListItem", "sample_order").performClick()
        awaitText("Geçerli JSON")
        onPantrixNode("Button", "launcher").performClick()
        onPantrixNode("TextField", "query", scope = "CommandPalette_commands").performTextInput("toml")
        onPantrixNode("Command", "convert_toml", scope = "CommandPalette_commands").performClick()

        // The view model writes the comment's words into the document, in the language Settings has.
        awaitText("# coupon = null (JSON); TOML biçiminde null yok", substring = true)
    }
}
