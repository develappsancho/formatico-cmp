package com.developersancho.formatico.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * Whether the system's back leaves a screen where the app runs: Android's back and iOS's swipe from the edge do. On the
 * desktop and the web the system's back is Escape, which Compose sends where nothing on the screen took the key: it
 * closes what is open over a screen (the palette, a menu, a select) and leaves the screen where it is, so that a key
 * pressed to close something leaves no document for a new tab and closes no Settings. The browser's own back goes
 * through [BrowserHistory].
 */
val systemBackLeavesScreens: Boolean
    @Composable @ReadOnlyComposable
    get() = when (PantrixTheme.platform.type) {
        PlatformType.Android, PlatformType.Ios -> true
        PlatformType.Desktop, PlatformType.Web -> false
    }

/**
 * Calls [onBack] when the system goes back, where it leaves screens ([systemBackLeavesScreens]) and the window has a
 * way of going back to listen to.
 */
@Composable
fun SystemBack(onBack: () -> Unit) {
    if (LocalNavigationEventDispatcherOwner.current == null) return
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = systemBackLeavesScreens,
        onBackCompleted = onBack
    )
}
