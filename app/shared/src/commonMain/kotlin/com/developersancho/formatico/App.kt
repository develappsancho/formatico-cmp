package com.developersancho.formatico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.developersancho.formatico.Destination.Companion.showDocument
import com.developersancho.formatico.core.designsystem.FormaticoTheme
import com.developersancho.formatico.feature.start.StartRoute
import com.developersancho.formatico.feature.start.startModule
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkspaceRoute
import com.developersancho.formatico.feature.workspace.workspaceModule
import com.pantrixui.components.container.scaffold.PantrixScaffold
import com.pantrixui.components.overlay.snackbar.rememberPantrixSnackbarState
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.logger.Level
import org.koin.dsl.koinConfiguration

/** What the app is built from, in Koin. */
private val Modules = koinConfiguration {
    modules(startModule, workspaceModule)
}

/**
 * Formatico, on every platform: its theme, its bar, and the screen at the top of the back stack: Start for a new tab,
 * over it the workspace of the document whose tab is chosen. Going back from a document goes to a new tab, and the
 * document stays open in its own.
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
            val workbench = koinInject<Workbench>()
            val open by workbench.state.collectAsState()
            val backStack = rememberNavBackStack(Destination.Saving, Destination.Start)
            LaunchedEffect(open.selectedId) { backStack.showDocument(open.selectedId) }
            PantrixScaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { insets ->
                    AppTopBar(
                        insets = insets,
                        workbench = open,
                        onSelectTab = workbench::select,
                        darkTheme = darkTheme,
                        onToggleTheme = { chosenDark = !darkTheme }
                    )
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
                    onBack = {
                        if (backStack.lastOrNull() is Destination.Workspace) {
                            workbench.select(null)
                        } else {
                            backStack.removeLastOrNull()
                        }
                    },
                    entryProvider = entryProvider {
                        entry<Destination.Start> {
                            StartRoute(onOpenDocument = { workbench.open(it.name, it.text, it.format) })
                        }
                        entry<Destination.Workspace> { destination ->
                            WorkspaceRoute(documentId = destination.documentId)
                        }
                    }
                )
            }
        }
    }
}
