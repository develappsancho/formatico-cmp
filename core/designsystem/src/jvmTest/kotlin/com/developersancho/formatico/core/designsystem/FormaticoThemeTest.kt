package com.developersancho.formatico.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.testing.PantrixTestTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FormaticoThemeTest {
    /** What Formatico's theme is in, inside a test theme that is [dark]: its syntax colours and whether it is dark. */
    private fun seenIn(dark: Boolean, chosen: Boolean? = null): Pair<SyntaxColors?, Boolean?> {
        var syntax: SyntaxColors? = null
        var isDark: Boolean? = null
        runDesktopComposeUiTest {
            setContent {
                PantrixTestTheme(dark = dark) {
                    val content = @Composable {
                        syntax = FormaticoTheme.syntax
                        isDark = PantrixTheme.isDark
                    }
                    if (chosen == null) FormaticoTheme(content = content) else FormaticoTheme(darkTheme = chosen, content = content)
                }
            }
            waitForIdle()
        }
        return syntax to isDark
    }

    @Test
    fun aThemeToldNothingOfTheDarkFollowsTheThemeAroundIt() {
        assertEquals(SyntaxColors.Dark to true, seenIn(dark = true))
        assertEquals(SyntaxColors.Light to false, seenIn(dark = false))
    }

    @Test
    fun theReadersChoiceIsKeptWhateverIsAroundIt() {
        assertEquals(SyntaxColors.Light to false, seenIn(dark = true, chosen = false))
        assertEquals(SyntaxColors.Dark to true, seenIn(dark = false, chosen = true))
    }
}
