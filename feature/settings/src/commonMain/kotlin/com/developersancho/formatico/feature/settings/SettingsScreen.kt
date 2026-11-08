package com.developersancho.formatico.feature.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.core.base.MviScreen
import com.developersancho.formatico.core.navigation.SystemBack
import com.developersancho.formatico.core.settings.AppLanguage
import com.pantrixui.components.container.topbar.PantrixBackButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.scrollbar.pantrixVerticalScroll
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.elements.text.TextRole
import com.pantrixui.components.navigation.sidebar.PantrixSidebar
import com.pantrixui.components.navigation.sidebar.SidebarGroup
import com.pantrixui.components.navigation.sidebar.SidebarItem
import com.pantrixui.components.overlay.commandpalette.PantrixCommand
import com.pantrixui.foundation.adaptive.WindowWidthClass
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.feature.settings.generated.resources.Res
import formatico.feature.settings.generated.resources.settings_about
import formatico.feature.settings.generated.resources.settings_editor
import formatico.feature.settings.generated.resources.settings_formats
import formatico.feature.settings.generated.resources.settings_general
import formatico.feature.settings.generated.resources.settings_history
import formatico.feature.settings.generated.resources.settings_licences
import formatico.feature.settings.generated.resources.settings_shortcuts
import formatico.feature.settings.generated.resources.settings_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Settings' three layouts, by the window's width: the design's boards at 390, 834 and 1440. */
internal enum class SettingsLayout { Phone, Tablet, Wide }

/** The layout the window's width asks for. */
@Composable
internal fun settingsLayout(): SettingsLayout {
    val window = PantrixTheme.window
    return when {
        window.isNarrow -> SettingsLayout.Phone
        window.widthClass >= WindowWidthClass.Expanded -> SettingsLayout.Wide
        else -> SettingsLayout.Tablet
    }
}

/**
 * The sections of Settings, in order, by their [key]: what a wide window's sidebar goes to. A phone has neither
 * Formats, nor how long the history keeps a document, nor the keyboard's shortcuts, as its board has them not.
 */
internal enum class SettingsSection(val key: String, val title: StringResource) {
    General("general", Res.string.settings_general),
    Editor("editor", Res.string.settings_editor),
    Formats("formats", Res.string.settings_formats),
    History("history", Res.string.settings_history),
    Shortcuts("shortcuts", Res.string.settings_shortcuts),
    About("about", Res.string.settings_about)
}

/** The sections [layout] shows: Formats and the keyboard's shortcuts not on a phone, and the shortcuts where there are. */
internal fun shownSections(layout: SettingsLayout, shortcuts: List<PantrixCommand>): List<SettingsSection> =
    SettingsSection.entries.filter { section ->
        when (section) {
            SettingsSection.Formats -> layout != SettingsLayout.Phone
            SettingsSection.Shortcuts -> layout != SettingsLayout.Phone && shortcuts.any { it.shortcut != null }
            else -> true
        }
    }

/**
 * Settings with its view model: back from its sections leaves it, by [onClose]. [shortcuts] are the palette's commands
 * whose keys Keyboard shortcuts lists.
 */
@Composable
fun SettingsRoute(onClose: () -> Unit, shortcuts: List<PantrixCommand>, modifier: Modifier = Modifier) {
    val viewModel = koinViewModel<SettingsViewModel>()
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SettingsEffect.Close -> onClose()
            }
        }
    ) { state, dispatch ->
        SettingsScreen(state = state, onIntent = dispatch, modifier = modifier, shortcuts = shortcuts)
    }
}

/**
 * Settings, drawn from [state] for the window's width: a wide window's sections beside a sidebar that goes to each, a
 * tablet's under one another, and a phone's under a bar of their own whose back leaves a page. The system's back goes
 * back a page too, and leaves Settings from its sections. Keyboard shortcuts lists [shortcuts]' keys.
 *
 * [at] is the section it opens scrolled to, as a board draws one further down; where the window does not show that
 * section, the one shown before it.
 */
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    shortcuts: List<PantrixCommand> = emptyList(),
    at: SettingsSection? = null
) {
    val layout = settingsLayout()
    val sections = shownSections(layout, shortcuts)
    val scroll = rememberScrollState()
    val placed = remember { mutableStateMapOf<SettingsSection, Int>() }
    val opened = at?.let { wanted -> sections.lastOrNull { it.ordinal <= wanted.ordinal } }
    val openedAt = opened?.let { placed[it] }
    LaunchedEffect(openedAt) { openedAt?.let { scroll.scrollTo(it) } }
    SystemBack { onIntent(SettingsIntent.Back) }
    val pages = @Composable { pagesModifier: Modifier -> Pages(state, layout, sections, shortcuts, onIntent, placed, pagesModifier) }
    Box(modifier = modifier.fillMaxSize().background(PantrixTheme.colors.background)) {
        when (layout) {
            SettingsLayout.Wide -> Row(modifier = Modifier.fillMaxSize()) {
                SettingsSidebar(state.page, sections, scroll, placed)
                Box(modifier = Modifier.weight(1f).fillMaxHeight().pantrixVerticalScroll(scroll), contentAlignment = Alignment.TopCenter) {
                    pages(Modifier.padding(horizontal = 24.dp).widthIn(max = WideColumn).padding(vertical = 32.dp))
                }
            }

            SettingsLayout.Tablet -> Box(modifier = Modifier.fillMaxSize().pantrixVerticalScroll(scroll)) {
                pages(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp))
            }

            SettingsLayout.Phone -> Column(modifier = Modifier.fillMaxSize()) {
                PhoneSettingsBar(state.page, onIntent)
                Box(modifier = Modifier.weight(1f).pantrixVerticalScroll(scroll)) {
                    pages(Modifier.fillMaxWidth().padding(horizontal = PantrixTheme.spacing.md, vertical = PantrixTheme.spacing.lg))
                }
            }
        }
    }
}

/**
 * How wide a wide window's column of settings is: the board's 720, with 24 beside it where the window is only a little
 * wider than a tablet's.
 */
private val WideColumn = 720.dp

/** The page Settings is at; its [sections] tell [placed] where each stands, for the sidebar to go to. */
@Composable
private fun Pages(
    state: SettingsState,
    layout: SettingsLayout,
    sections: List<SettingsSection>,
    shortcuts: List<PantrixCommand>,
    onIntent: (SettingsIntent) -> Unit,
    placed: MutableMap<SettingsSection, Int>,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        when (val page = state.page) {
            SettingsPage.Settings -> Sections(state, layout, sections, shortcuts, onIntent, placed)
            SettingsPage.Licences -> LicencesPage(layout, onIntent)
            is SettingsPage.Licence -> LicencePage(page.library, layout, onIntent)
        }
    }
}

/** The [sections] shown, under Settings' title where there is no bar to hold it. */
@Composable
private fun Sections(
    state: SettingsState,
    layout: SettingsLayout,
    sections: List<SettingsSection>,
    shortcuts: List<PantrixCommand>,
    onIntent: (SettingsIntent) -> Unit,
    placed: MutableMap<SettingsSection, Int>
) {
    val phone = layout == SettingsLayout.Phone
    // Where the reader has chosen none, the system's language, or English where Formatico does not speak it.
    val language = state.preferences.language ?: AppLanguage.forSystem(PantrixTheme.locale.languageTag)
    Column(verticalArrangement = Arrangement.spacedBy(if (phone) 18.dp else 24.dp)) {
        if (!phone) {
            PantrixText(
                text = stringResource(Res.string.settings_title),
                style = PantrixTheme.typography.headline,
                role = TextRole.Heading(level = 1)
            )
        }
        sections.forEach { section ->
            Box(Modifier.onPlaced { placed[section] = it.positionInParent().y.toInt() }) {
                when (section) {
                    SettingsSection.General -> GeneralSection(state, language, layout, onIntent)
                    SettingsSection.Editor -> EditorSection(state, layout, onIntent)
                    SettingsSection.Formats -> FormatsSection(state, layout, onIntent)
                    SettingsSection.History -> HistorySection(state, layout, onIntent)
                    SettingsSection.Shortcuts -> ShortcutsSection(shortcuts, layout)
                    SettingsSection.About -> AboutSection(layout, onIntent)
                }
            }
        }
    }
}

/**
 * A wide window's sidebar: a destination for each of the [sections] shown, the one in view chosen; pressing one scrolls
 * to it. Over the licences, About is chosen.
 */
@Composable
private fun SettingsSidebar(
    page: SettingsPage,
    sections: List<SettingsSection>,
    scroll: ScrollState,
    placed: Map<SettingsSection, Int>
) {
    val scope = rememberCoroutineScope()
    val inView by remember(placed, sections) {
        derivedStateOf { sections.lastOrNull { section -> (placed[section] ?: Int.MAX_VALUE) <= scroll.value + 1 } }
    }
    val items = sections.map { SidebarItem(key = it.key, label = stringResource(it.title)) }
    PantrixSidebar(
        groups = listOf(SidebarGroup(items = items)),
        selected = if (page == SettingsPage.Settings) (inView ?: SettingsSection.General).key else SettingsSection.About.key,
        onSelect = { key ->
            val section = SettingsSection.entries.first { it.key == key }
            placed[section]?.let { scope.launch { scroll.animateScrollTo(it) } }
        },
        expanded = emptySet(),
        onExpandedChange = {},
        modifier = Modifier.fillMaxHeight(),
        adaptive = false,
        tagKey = "settings"
    )
}

/** A phone's bar over Settings: the way back, and the page's name, a library's over its licence. */
@Composable
private fun PhoneSettingsBar(page: SettingsPage, onIntent: (SettingsIntent) -> Unit) {
    val title = when (page) {
        SettingsPage.Settings -> stringResource(Res.string.settings_title)
        SettingsPage.Licences -> stringResource(Res.string.settings_licences)
        is SettingsPage.Licence -> page.library.title
    }
    Column(modifier = Modifier.fillMaxWidth().background(PantrixTheme.colors.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = PantrixTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xs)
        ) {
            PantrixBackButton(onClick = { onIntent(SettingsIntent.Back) }, tagKey = "back")
            PantrixText(
                text = title,
                modifier = Modifier.weight(1f),
                style = PantrixTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                role = TextRole.Heading(level = 1),
                maxLines = 1
            )
        }
        PantrixHorizontalDivider()
    }
}
