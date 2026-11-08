package com.developersancho.formatico.core.navigation

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.developersancho.formatico.core.testing.SendsSystemBack
import com.developersancho.formatico.core.testing.TestSystemBack
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.testing.PantrixTestTheme
import com.pantrixui.testing.onEachPlatform
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SystemBackTest {
    private val touch = listOf(PlatformType.Android, PlatformType.Ios)

    @Test
    fun theSystemsBackLeavesScreensOnAndroidAndIosOnly() = runComposeUiTest {
        val leaves = mutableMapOf<PlatformType, Boolean>()
        onEachPlatform({ platform -> PantrixTestTheme(platform = platform) { leaves[platform] = systemBackLeavesScreens } }) {}

        assertEquals(PlatformType.entries.associateWith { it in touch }, leaves.toMap())
    }

    @Test
    fun aScreenIsLeftByTheSystemsBackOnAndroidAndIosAndNotByEscape() = runComposeUiTest {
        val back = TestSystemBack()
        val left = mutableListOf<PlatformType>()
        onEachPlatform({ platform ->
            PantrixTestTheme(platform = platform) {
                SendsSystemBack(back)
                SystemBack { left += platform }
            }
        }) {
            runOnIdle { back.send() }
        }

        assertEquals(touch, left)
    }
}
