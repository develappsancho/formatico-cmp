package com.developersancho.formatico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.developersancho.formatico.core.storage.SmallStore
import com.developersancho.formatico.core.storage.storageModule
import com.developersancho.formatico.feature.start.DocumentHistory
import com.developersancho.formatico.feature.start.StartRoute
import com.developersancho.formatico.feature.start.startModule
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkbenchKeeper
import com.developersancho.formatico.feature.workspace.WorkspaceRoute
import com.developersancho.formatico.feature.workspace.workspaceModule
import com.pantrixui.components.container.scaffold.PantrixScaffold
import com.pantrixui.components.overlay.snackbar.rememberPantrixSnackbarState
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration
import kotlin.time.Clock

/**
 * Formatico, on every platform: its theme, its bar, and the screen at the top of the back stack: Start for a new tab,
 * over it the workspace of the document whose tab is chosen. Going back from a document goes to a new tab, and the
 * document stays open in its own.
 *
 * The theme is the system's until the bar's switch is pressed, and the choice is kept on this device. A change made
 * to a document in its workspace is told to Start's history. In a browser, the tab's history and address follow the
 * screen shown, once the documents open when the app last closed are back.
 */
@Composable
fun App() {
    App(systemDark = isSystemInDarkTheme(), storage = storageModule, followsBrowser = true)
}

/**
 * The app with the system's dark mode and the stores given: a test pins the one, as it pins the rest of the theme,
 * and gives stores that keep nothing past it. Where [followsBrowser] says so, a browser's history follows the app; a
 * test leaves it out, since its page holds many apps one after another.
 */
@Composable
internal fun App(systemDark: Boolean, storage: Module, followsBrowser: Boolean = false) {
    val configuration = remember(storage) { koinConfiguration { modules(storage, startModule, workspaceModule) } }
    KoinApplication(configuration = configuration, logLevel = Level.ERROR) {
        val theme = rememberKeptTheme(small = koinInject(), systemDark = systemDark)
        val darkTheme = theme.dark
        FormaticoTheme(darkTheme = darkTheme) {
            val snackbars = rememberPantrixSnackbarState()
            val workbench = koinInject<Workbench>()
            val history = koinInject<DocumentHistory>()
            LaunchedEffect(workbench, history) { followEdits(workbench, history, Clock.System) }
            val open by workbench.state.collectAsState()
            val backStack = rememberNavBackStack(Destination.Saving, Destination.Start)
            LaunchedEffect(open.selectedId) { backStack.showDocument(open.selectedId) }
            val restored by koinInject<WorkbenchKeeper>().restored.collectAsState()
            if (followsBrowser && restored) FollowBrowserHistory(workbench)
            PantrixScaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { insets ->
                    AppTopBar(
                        insets = insets,
                        workbench = open,
                        onSelectTab = workbench::select,
                        darkTheme = darkTheme,
                        onToggleTheme = theme::toggle
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
                            StartRoute(
                                onOpenDocument = { workbench.open(it.name, it.text, it.format, it.historyId) }
                            )
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

/** The theme the app is in: the reader's [chosen] one, or the system's until there is one; [keep] keeps a choice. */
private class KeptTheme(
    private val chosen: MutableState<Boolean?>,
    private val systemDark: Boolean,
    private val keep: (Boolean) -> Unit
) {
    val dark: Boolean get() = chosen.value ?: systemDark

    /** Chooses the other theme, and keeps the choice. */
    fun toggle() {
        val dark = !dark
        chosen.value = dark
        keep(dark)
    }
}

/** The theme the app is in, read from [small] once and kept there when the reader chooses another. */
@Composable
private fun rememberKeptTheme(small: SmallStore, systemDark: Boolean): KeptTheme {
    val scope = rememberCoroutineScope()
    val chosen = rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(small) {
        if (chosen.value == null) chosen.value = small.get(THEME_KEY, Boolean.serializer())
    }
    return KeptTheme(chosen, systemDark) { dark -> scope.launch { small.put(THEME_KEY, dark, Boolean.serializer()) } }
}

/** Where the reader's choice of the theme is kept: true for the dark one. */
private const val THEME_KEY = "theme"

/** Tells [history] of each change made to a document of [workbench] that has a place in it, as it is made. */
internal suspend fun followEdits(workbench: Workbench, history: DocumentHistory, clock: Clock) {
    val texts = mutableMapOf<Long, String>()
    workbench.state.collect { state ->
        state.documents.forEach { document ->
            val before = texts.put(document.id, document.text)
            val historyId = document.historyId
            if (before != null && before != document.text && historyId != null) {
                history.edit(historyId, document.text, clock.now())
            }
        }
        texts.keys.retainAll(state.documents.map { it.id }.toSet())
    }
}
