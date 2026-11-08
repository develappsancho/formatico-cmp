package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import com.developersancho.formatico.feature.start.DocumentOpening
import com.developersancho.formatico.feature.workspace.DocumentPalette
import com.developersancho.formatico.feature.workspace.Workbench
import com.developersancho.formatico.feature.workspace.WorkbenchState
import com.developersancho.formatico.feature.workspace.documentPaletteCommands
import com.developersancho.formatico.feature.workspace.documentShortcutCommands
import com.developersancho.formatico.feature.workspace.rememberDocumentPalette
import com.developersancho.formatico.strings.appStrings
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.components.overlay.commandpalette.PantrixCommandPalette
import com.pantrixui.components.overlay.commandpalette.commandShortcuts
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.keyboard.onShortcut
import com.pantrixui.foundation.keyboard.shortcutRoot
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme

/** The keys of the palette's commands of the app itself, the same in every language. */
internal object AppCommandKeys {
    const val PALETTE = "palette"
    const val NEW = "new"
    const val OPEN = "open"
    const val PASTE = "paste"
    const val THEME = "theme"
    const val SETTINGS = "settings"
}

/**
 * The keys of the app's own commands, as the Palette boards write them; the palette's own is Command and K. A browser
 * keeps Command and T for a tab of its own: there a new tab has none.
 */
internal object AppShortcuts {
    val Palette = PantrixShortcut(Key.K, command = true)
    val NewTab = PantrixShortcut(Key.T, command = true)
    val Open = PantrixShortcut(Key.O, command = true)
    val Paste = PantrixShortcut(Key.V, command = true, shift = true)
    val Settings = PantrixShortcut(Key.Comma, command = true)
}

/**
 * The palette's commands of the app itself, under Formatico's name, as the Palette boards list them: a new tab, a file,
 * the clipboard, the theme and the settings. A phone, which has no tabs, makes a new document in a new tab's place,
 * lists no theme, and writes no shortcuts. A new tab cannot be opened while one is shown, nor Settings while it is.
 *
 * @param workbench the open documents, and the chosen tab.
 * @param darkTheme whether the dark theme is in effect: the theme's command chooses the other.
 * @param settingsShown whether Settings is shown.
 */
@Composable
internal fun appPaletteCommands(workbench: WorkbenchState, darkTheme: Boolean, settingsShown: Boolean): List<PantrixCommand> {
    val phone = PantrixTheme.window.isNarrow
    val browser = PantrixTheme.platform.type == PlatformType.Web
    val keys: (PantrixShortcut) -> PantrixShortcut? = { if (phone) null else it }
    val group = appStrings.appName
    return listOfNotNull(
        if (phone) {
            PantrixCommand(
                key = AppCommandKeys.NEW,
                label = appStrings.paletteNewDocument,
                group = group,
                icon = PantrixIcons.Plus
            )
        } else {
            PantrixCommand(
                key = AppCommandKeys.NEW,
                label = appStrings.newTab,
                group = group,
                icon = PantrixIcons.Plus,
                shortcut = keys(AppShortcuts.NewTab).takeUnless { browser },
                enabled = workbench.selected != null || settingsShown
            )
        },
        PantrixCommand(
            key = AppCommandKeys.OPEN,
            label = appStrings.paletteOpenFile,
            group = group,
            icon = PantrixIcons.File,
            shortcut = keys(AppShortcuts.Open)
        ),
        PantrixCommand(
            key = AppCommandKeys.PASTE,
            label = appStrings.palettePaste,
            group = group,
            shortcut = keys(AppShortcuts.Paste)
        ).takeUnless { phone },
        PantrixCommand(
            key = AppCommandKeys.THEME,
            label = if (darkTheme) appStrings.useLightTheme else appStrings.useDarkTheme,
            group = group
        ).takeUnless { phone },
        PantrixCommand(
            key = AppCommandKeys.SETTINGS,
            label = appStrings.settings,
            group = group,
            shortcut = keys(AppShortcuts.Settings),
            enabled = !settingsShown
        )
    )
}

/**
 * What Settings' Keyboard shortcuts lists, as the palette has it where the app runs: the palette's own keys first, under
 * Formatico's name, then the app's commands and a document's that have a shortcut. A phone writes none.
 */
@Composable
internal fun shortcutCommands(): List<PantrixCommand> {
    if (PantrixTheme.window.isNarrow) return emptyList()
    val palette = PantrixCommand(
        key = AppCommandKeys.PALETTE,
        label = appStrings.searchOrRunACommand,
        group = appStrings.appName,
        shortcut = AppShortcuts.Palette
    )
    // Which documents are open makes no command's name nor its keys: only whether it can run now, which a list does not.
    val app = appPaletteCommands(WorkbenchState(), darkTheme = false, settingsShown = true)
    return (listOf(palette) + app + documentShortcutCommands()).filter { it.shortcut != null }
}

/**
 * The command palette of the app, whether it is open, and the commands it lists: those of the document shown, then the
 * app's own. What they do: [DocumentPalette.run] for the document's, and here for the app's.
 */
@Stable
internal class AppPaletteState(
    private val shown: MutableState<Boolean>,
    val commands: List<PantrixCommand>,
    private val runCommand: (PantrixCommand) -> Unit
) {
    /** Whether the palette is open. */
    val visible: Boolean get() = shown.value

    /** Opens the palette. */
    fun open() {
        shown.value = true
    }

    /** Closes the palette. */
    fun close() {
        shown.value = false
    }

    /** Runs [command], from the palette or by its keys. */
    fun run(command: PantrixCommand) = runCommand(command)
}

/** Where the app is beside the tab chosen: whether [settingsShown], and [onShowSettings] to show or leave it. */
internal class AppPlace(val settingsShown: Boolean, val onShowSettings: (Boolean) -> Unit)

/**
 * The app's palette over [workbench], whose state is [open], at [place]: its commands for the document shown, none
 * over Settings, and the app's own, and what they do. [onToggleTheme] chooses the other theme than [darkTheme]; what is
 * pasted or opened goes through [opening], over whatever is shown.
 */
@Composable
internal fun rememberAppPalette(
    workbench: Workbench,
    open: WorkbenchState,
    place: AppPlace,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    opening: DocumentOpening
): AppPaletteState {
    val shown = rememberSaveable { mutableStateOf(false) }
    val documents = rememberDocumentPalette(workbench)
    val document = open.selected.takeUnless { place.settingsShown }
    val commands = document?.let { documentPaletteCommands(it) }.orEmpty() + appPaletteCommands(open, darkTheme, place.settingsShown)
    val phone = PantrixTheme.window.isNarrow
    return AppPaletteState(shown, commands) { command ->
        val ran = document != null && documents.run(command.key, document.id)
        if (!ran) {
            when (command.key) {
                AppCommandKeys.NEW -> {
                    place.onShowSettings(false)
                    if (phone) workbench.open(name = null, text = "", format = null) else workbench.select(null)
                }

                AppCommandKeys.OPEN -> opening.pickFile()

                AppCommandKeys.PASTE -> opening.paste()

                AppCommandKeys.THEME -> onToggleTheme()

                AppCommandKeys.SETTINGS -> place.onShowSettings(true)
            }
        }
    }
}

/** The app's shortcuts, heard anywhere in the window: the palette's own, and every command's of [palette]. */
internal fun Modifier.appShortcuts(palette: AppPaletteState): Modifier =
    shortcutRoot()
        .onShortcut(AppShortcuts.Palette) { palette.open() }
        .commandShortcuts(palette.commands, onRun = palette::run)

/** The command palette, open while [palette] says so: a command run closes it first. */
@Composable
internal fun AppPalette(palette: AppPaletteState) {
    PantrixCommandPalette(
        visible = palette.visible,
        onDismissRequest = palette::close,
        commands = palette.commands,
        onRun = { command ->
            palette.close()
            palette.run(command)
        },
        placeholder = appStrings.palettePlaceholder,
        emptyText = appStrings.paletteEmpty,
        tagKey = "commands"
    )
}
