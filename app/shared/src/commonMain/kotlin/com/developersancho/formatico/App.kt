package com.developersancho.formatico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.developersancho.formatico.core.designsystem.FormaticoTheme
import com.developersancho.formatico.feature.start.StartRoute
import com.developersancho.formatico.feature.start.startModule
import com.pantrixui.components.container.scaffold.PantrixScaffold
import com.pantrixui.components.overlay.snackbar.rememberPantrixSnackbarState
import org.koin.compose.KoinApplication
import org.koin.core.logger.Level
import org.koin.dsl.koinConfiguration

/** What the app is built from, in Koin. */
private val Modules = koinConfiguration {
    modules(startModule)
}

/**
 * Formatico, on every platform: its theme, its bar, and the screen at the top of the back stack.
 *
 * The theme is the system's until the bar's switch is pressed; Settings will keep the choice.
 */
@Composable
fun App() {
    KoinApplication(configuration = Modules, logLevel = Level.ERROR) {
        val systemDark = isSystemInDarkTheme()
        var chosenDark by rememberSaveable { mutableStateOf<Boolean?>(null) }
        val darkTheme = chosenDark ?: systemDark
        FormaticoTheme(darkTheme = darkTheme) {
            val snackbars = rememberPantrixSnackbarState()
            val backStack = rememberNavBackStack(Destination.Saving, Destination.Start)
            PantrixScaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { insets ->
                    AppTopBar(insets = insets, darkTheme = darkTheme, onToggleTheme = { chosenDark = !darkTheme })
                },
                snackbars = snackbars
            ) { padding ->
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<Destination.Start> {
                            // The workspace that shows what was opened is the next screen to be written.
                            StartRoute(onOpenDocument = {})
                        }
                    }
                )
            }
        }
    }
}
