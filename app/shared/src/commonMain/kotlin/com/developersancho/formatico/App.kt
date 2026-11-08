package com.developersancho.formatico

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.developersancho.formatico.Destination.Companion.show
import com.developersancho.formatico.core.designsystem.FormaticoTheme
import com.developersancho.formatico.core.settings.SettingsStore
import com.developersancho.formatico.core.settings.ThemeChoice
import com.developersancho.formatico.core.settings.settingsModule
import com.developersancho.formatico.core.storage.storageModule
import com.developersancho.formatico.feature.settings.SettingsRoute
import com.developersancho.formatico.feature.settings.settingsFeatureModule
import com.developersancho.formatico.feature.start.DocumentHistory
import com.developersancho.formatico.feature.start.OpenedDocument
import com.developersancho.formatico.feature.start.StartRoute
import com.developersancho.formatico.feature.start.UnknownFormatDialog
import com.developersancho.formatico.feature.start.startModule
import com.developersancho.formatico.feature.workspace.DocumentMoreMenu
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkbenchKeeper
import com.developersancho.formatico.feature.workspace.WorkspaceRoute
import com.developersancho.formatico.feature.workspace.editor.LocalWrapLines
import com.developersancho.formatico.feature.workspace.workspaceModule
import com.pantrixui.components.container.scaffold.PantrixScaffold
import com.pantrixui.components.overlay.snackbar.rememberPantrixSnackbarState
import com.pantrixui.foundation.l10n.PantrixLocale
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.koinConfiguration
import kotlin.time.Clock

/**
 * Formatico, on every platform: its theme, its bar, and the screen at the top of the back stack: Start for a new tab,
 * over it the workspace of the document whose tab is chosen, and Settings over either while it is shown. Going back
 * from a document goes to a new tab, and the document stays open in its own.
 *
 * The command palette opens from the bar and by Command or Control and K, anywhere; its commands run by their own keys
 * too. The theme and the language are the reader's, as Settings keeps them: the theme the system's until one is
 * chosen, by Settings or the bar's switch. A change made to a document in its workspace is told to Start's history,
 * which keeps documents as long as Settings says. In a browser, the tab's history and address follow the screen
 * shown, once the documents open when the app last closed are back.
 */
@Composable
fun App() {
    App(systemDark = isSystemInDarkTheme(), storage = storageModule, followsBrowser = true)
}

/**
 * The app with the system's dark mode and the stores given: a test pins the one, as it pins the rest of the theme,
 * and gives stores that keep nothing past it. Where [followsBrowser] says so, a browser's history follows the app; a
 * test leaves it out, since its page holds many apps one after another.
 *
 * A language chosen in Settings starts the app's composition over, under it: the words are read again in it. The back
 * stack and whether Settings is shown stand above, and outlive it.
 */
@Composable
internal fun App(systemDark: Boolean, storage: Module, followsBrowser: Boolean = false) {
    val configuration = remember(storage) {
        koinConfiguration { modules(storage, settingsModule, startModule, workspaceModule, settingsFeatureModule, appModule) }
    }
    KoinApplication(configuration = configuration, logLevel = Level.ERROR) {
        val settings = koinInject<SettingsStore>()
        val preferences by settings.preferences.collectAsState()
        val darkTheme = when (preferences.theme) {
            ThemeChoice.Light -> false
            ThemeChoice.Dark -> true
            ThemeChoice.System -> systemDark
        }
        val backStack = rememberNavBackStack(Destination.Saving, Destination.Start)
        val settingsShown = rememberSaveable { mutableStateOf(false) }
        val language = preferences.language?.tag
        CompositionLocalProvider(LocalAppLocale provides language, LocalWrapLines provides preferences.wrapLines) {
            key(language) {
                FormaticoTheme(darkTheme = darkTheme, locale = language?.let(PantrixLocale::forLanguageTag)) {
                    AppContent(
                        backStack = backStack,
                        settingsShown = settingsShown,
                        darkTheme = darkTheme,
                        followsBrowser = followsBrowser,
                        onToggleTheme = { settings.update { it.copy(theme = if (darkTheme) ThemeChoice.Light else ThemeChoice.Dark) } }
                    )
                }
            }
        }
    }
}

/**
 * The app's bar and screens, its palette, and the question a text of no format told asks: drawn in the theme and the
 * language chosen. [settingsShown] says whether Settings is shown over the tab chosen; choosing a tab leaves it.
 */
@Composable
private fun AppContent(
    backStack: NavBackStack<NavKey>,
    settingsShown: MutableState<Boolean>,
    darkTheme: Boolean,
    followsBrowser: Boolean,
    onToggleTheme: () -> Unit
) {
    val snackbars = rememberPantrixSnackbarState()
    val workbench = koinInject<Workbench>()
    val history = koinInject<DocumentHistory>()
    val settings = koinInject<SettingsStore>()
    LaunchedEffect(workbench, history) { followHistory(workbench, history, Clock.System) }
    LaunchedEffect(settings, history) { keepHistory(settings, history, Clock.System) }
    val open by workbench.state.collectAsState()
    var showSettings by settingsShown
    LaunchedEffect(open.selectedId, showSettings) { backStack.show(open.selectedId, showSettings) }
    val restored by koinInject<WorkbenchKeeper>().restored.collectAsState()
    if (followsBrowser && restored) FollowBrowserHistory(workbench, settingsShown)
    val opening = remember(workbench, settings) { DocumentOpen(workbench, settings) }
    val place = AppPlace(settingsShown = showSettings, onShowSettings = { showSettings = it })
    val palette = rememberAppPalette(workbench, open, place, darkTheme, onToggleTheme, onOpen = opening::open)
    PantrixScaffold(
        modifier = Modifier.fillMaxSize().appShortcuts(palette),
        topBar = { insets ->
            AppTopBar(
                insets = insets,
                workbench = open,
                settingsShown = showSettings,
                onSelectTab = { id ->
                    showSettings = false
                    workbench.select(id)
                },
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onOpenPalette = palette::open,
                onOpenSettings = { showSettings = true },
                documentMenu = { DocumentMoreMenu(documentId = it, workbench = workbench) }
            )
        },
        snackbars = snackbars
    ) { padding ->
        // Until what was open is back, the screen stays empty: Start for a moment, then the document, would be
        // a screen the reader did not ask for.
        if (!restored) return@PantrixScaffold
        AppScreens(
            backStack = backStack,
            workbench = workbench,
            onOpen = opening::open,
            onCloseSettings = { showSettings = false },
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        )
    }
    AppPalette(palette)
    UnknownFormatDialog(document = opening.asking, onOpen = opening::answer, onDismiss = opening::dismiss)
}

/**
 * The screen at the top of [backStack]: Start for a new tab, the workspace of a document of [workbench], or Settings.
 * Going back from a document goes to a new tab, and from Settings to the screen under it. What Start opens goes to
 * [onOpen].
 */
@Composable
private fun AppScreens(
    backStack: NavBackStack<NavKey>,
    workbench: Workbench,
    onOpen: (OpenedDocument) -> Unit,
    onCloseSettings: () -> Unit,
    modifier: Modifier
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        onBack = {
            when (backStack.lastOrNull()) {
                Destination.Settings -> onCloseSettings()
                is Destination.Workspace -> workbench.select(null)
                else -> backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<Destination.Start> { StartRoute(onOpenDocument = onOpen) }
            entry<Destination.Workspace> { destination -> WorkspaceRoute(documentId = destination.documentId) }
            entry<Destination.Settings> { SettingsRoute(onClose = onCloseSettings) }
        }
    )
}
