package com.developersancho.formatico

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developersancho.formatico.core.designsystem.FormaticoIcons
import com.developersancho.formatico.feature.workspace.DocumentPane
import com.developersancho.formatico.feature.workspace.OpenDocument
import com.developersancho.formatico.feature.workspace.WorkbenchState
import com.pantrixui.components.container.surface.PantrixSurface
import com.pantrixui.components.container.tab.PantrixTabs
import com.pantrixui.components.container.tab.TabBadge
import com.pantrixui.components.container.tab.TabItem
import com.pantrixui.components.container.tab.TabsColors
import com.pantrixui.components.elements.badge.BadgeTone
import com.pantrixui.components.elements.badge.PantrixBadge
import com.pantrixui.components.elements.button.ButtonColors
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.ButtonVariant
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixDivider
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.overlay.menu.PantrixMenu
import com.pantrixui.components.overlay.menu.PantrixMenuItem
import com.pantrixui.foundation.adaptive.WindowWidthClass
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.platform.LocalOperatingSystem
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.app.shared.generated.resources.Res
import formatico.app.shared.generated.resources.app_name
import formatico.app.shared.generated.resources.changes_not_saved
import formatico.app.shared.generated.resources.new_tab
import formatico.app.shared.generated.resources.open_documents
import formatico.app.shared.generated.resources.search_or_run_a_command
import formatico.app.shared.generated.resources.settings
import formatico.app.shared.generated.resources.switch_documents
import formatico.app.shared.generated.resources.switch_documents_from_new_tab
import formatico.app.shared.generated.resources.untitled
import formatico.app.shared.generated.resources.use_dark_theme
import formatico.app.shared.generated.resources.use_light_theme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The launcher's width in a wide window, as the design draws it. */
private val LauncherWidth = 340.dp

/** The bar's height: 52 in a wide window, where a pointer leads, and 56 elsewhere. */
private val WideBarHeight = 52.dp
private val BarHeight = 56.dp

/**
 * Formatico's bar, over every screen: the mark and the name, the documents' tabs, the launcher of the command palette,
 * the theme's switch and the settings. A phone keeps the mark, the name, the launcher's button and the settings over a
 * new tab, and over a document the document's name, which switches documents, how many are open, the launcher's
 * button and the document's "more" menu; over a document being converted, none: that screen has a bar of its own.
 *
 * The palette and the settings come with their screens; until then their buttons are drawn and not pressed.
 *
 * @param insets the window's insets at the bar's edges: the bar's fill runs behind them.
 * @param workbench the open documents, and the chosen tab.
 * @param onSelectTab chooses the tab of a document, by its id, or a new tab for null.
 * @param darkTheme whether the dark theme is in effect.
 * @param onToggleTheme asks for the other theme.
 * @param documentMenu the "more" menu of the document a phone's bar is over, by its id.
 */
@Composable
internal fun AppTopBar(
    insets: PaddingValues,
    workbench: WorkbenchState,
    onSelectTab: (Long?) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    documentMenu: @Composable (documentId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val window = PantrixTheme.window
    val spacing = PantrixTheme.spacing
    val phone = window.isNarrow
    val wide = window.widthClass >= WindowWidthClass.Expanded
    val shown = workbench.selected
    Column(modifier = modifier.fillMaxWidth().background(PantrixTheme.colors.surface).padding(insets)) {
        // A phone's conversion is a screen with a bar of its own, Convert-390's, in this one's place.
        if (phone && shown?.view?.pane == DocumentPane.Convert) return@Column
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (wide) WideBarHeight else BarHeight)
                .padding(start = if (phone) spacing.sm else spacing.lg, end = if (phone) spacing.xs else spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            when {
                phone -> {
                    AnimatedPhoneBar(
                        workbench = workbench,
                        onSelectTab = onSelectTab,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        documentMenu = documentMenu,
                        modifier = Modifier.weight(1f)
                    )
                }

                else -> {
                    // A tablet over a document keeps the mark, the tabs and two buttons, as its boards draw it.
                    val roomy = wide || shown == null
                    Brand(phone = false, withName = roomy)
                    DocumentTabs(
                        workbench = workbench,
                        onSelectTab = onSelectTab,
                        size = if (wide) ButtonSize.Small else ButtonSize.Medium,
                        modifier = Modifier.weight(1f).padding(start = if (wide) spacing.lg else spacing.md)
                    )
                    BarActions(
                        phone = false,
                        wide = wide,
                        withTheme = roomy,
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme
                    )
                }
            }
        }
        PantrixHorizontalDivider()
    }
}

/**
 * A phone's bar, changing as its screen does and over the same time: on iOS the screen slides in, and a bar that
 * changed at once stood over the screen it had left.
 */
@Composable
private fun AnimatedPhoneBar(
    workbench: WorkbenchState,
    onSelectTab: (Long?) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    documentMenu: @Composable (documentId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = PantrixTheme.motion
    AnimatedContent(
        targetState = workbench,
        modifier = modifier,
        transitionSpec = {
            val time = tween<Float>(motion.duration(motion.slow))
            fadeIn(time) togetherWith fadeOut(time)
        },
        contentKey = { it.selectedId to it.documents.isEmpty() },
        label = "phone bar"
    ) { shown ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.xs)
        ) {
            PhoneBar(
                workbench = shown,
                onSelectTab = onSelectTab,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                documentMenu = documentMenu
            )
        }
    }
}

/**
 * A phone's bar: over a document, the way between the tabs, the launcher and the document's "more" menu; over a new
 * tab beside open documents, the way back to them, as Start-390 draws it; over Start alone, the mark and the name.
 */
@Composable
private fun RowScope.PhoneBar(
    workbench: WorkbenchState,
    onSelectTab: (Long?) -> Unit,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    documentMenu: @Composable (documentId: Long) -> Unit
) {
    val shown = workbench.selected
    when {
        shown != null -> {
            DocumentSwitcher(workbench = workbench, shown = shown, onSelectTab = onSelectTab)
            Spacer(modifier = Modifier.weight(1f))
            PantrixIconButton(
                imageVector = PantrixIcons.Search,
                contentDescription = stringResource(Res.string.search_or_run_a_command),
                onClick = {},
                size = ButtonSize.Large,
                enabled = false,
                tagKey = "launcher"
            )
            documentMenu(shown.id)
        }

        workbench.documents.isNotEmpty() -> {
            DocumentSwitcher(workbench = workbench, shown = null, onSelectTab = onSelectTab)
            Spacer(modifier = Modifier.weight(1f))
            BarActions(
                phone = true,
                wide = false,
                withTheme = false,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }

        else -> {
            Brand(phone = true, modifier = Modifier.padding(start = PantrixTheme.spacing.sm).weight(1f))
            BarActions(
                phone = true,
                wide = false,
                withTheme = false,
                darkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

/** A document's name in its tab: its file's, or "Untitled". */
@Composable
private fun OpenDocument.label(): String = name ?: stringResource(Res.string.untitled)

/**
 * The open documents' tabs, and a new tab's while it is chosen, with the button that opens a new tab. A document with
 * changes not saved to its file has a dot after its name.
 */
@Composable
private fun DocumentTabs(
    workbench: WorkbenchState,
    onSelectTab: (Long?) -> Unit,
    size: ButtonSize,
    modifier: Modifier = Modifier
) {
    val notSaved = stringResource(Res.string.changes_not_saved)
    val documents = workbench.documents
    val tabs = documents.map { document ->
        TabItem(
            label = document.label(),
            tagKey = "document_${document.id}",
            badge = if (document.edited) TabBadge(contentDescription = notSaved, tone = BadgeTone.Neutral) else null
        )
    } + listOfNotNull(TabItem(label = stringResource(Res.string.new_tab), tagKey = "new").takeIf { workbench.selected == null })
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PantrixTabs(
            tabs = tabs,
            selectedIndex = documents.indexOfFirst { it.id == workbench.selectedId }.takeIf { it >= 0 } ?: documents.size,
            onSelect = { index -> onSelectTab(documents.getOrNull(index)?.id) },
            modifier = Modifier.weight(1f, fill = false),
            scrollable = true,
            // The bar draws its own edge: the row's hairline would be a second one (PANTRIX_FINDINGS: "PantrixTabs'ın
            // altındaki çizgi yalnızca saydam bir renkle gizleniyor").
            colors = TabsColors(divider = Color.Transparent),
            tagKey = "documents"
        )
        PantrixIconButton(
            imageVector = PantrixIcons.Plus,
            contentDescription = stringResource(Res.string.new_tab),
            onClick = { onSelectTab(null) },
            size = size,
            enabled = workbench.selected != null,
            tagKey = "new_tab"
        )
    }
}

/**
 * A phone's way between its tabs: the name of the document [shown], or "New tab" where none is, which opens the list
 * of the open documents and a new tab, and how many are open.
 */
@Composable
private fun DocumentSwitcher(workbench: WorkbenchState, shown: OpenDocument?, onSelectTab: (Long?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val count = workbench.documents.size
    val name = shown?.label() ?: stringResource(Res.string.new_tab)
    PantrixMenu(
        visible = open,
        onDismissRequest = { open = false },
        anchor = {
            PantrixButton(
                text = name,
                onClick = { open = true },
                variant = ButtonVariant.Text,
                size = ButtonSize.Large,
                trailingIcon = { PantrixIcon(imageVector = PantrixIcons.ChevronDown, contentDescription = null) },
                // The document's name in the text's colour, as a title, where a text button takes the brand's.
                colors = ButtonColors(
                    container = Color.Transparent,
                    content = PantrixTheme.colors.textPrimary,
                    border = Color.Transparent
                ),
                contentDescription = if (shown != null) {
                    stringResource(Res.string.switch_documents, name, count)
                } else {
                    pluralStringResource(Res.plurals.switch_documents_from_new_tab, count, count)
                },
                tagKey = "switch_documents"
            )
        },
        tagKey = "documents"
    ) {
        workbench.documents.forEach { document ->
            PantrixMenuItem(
                text = document.label(),
                onClick = { onSelectTab(document.id) },
                selected = document.id == shown?.id,
                tagKey = "document_${document.id}"
            )
        }
        PantrixDivider()
        PantrixMenuItem(
            text = stringResource(Res.string.new_tab),
            onClick = { onSelectTab(null) },
            icon = PantrixIcons.Plus,
            selected = shown == null,
            tagKey = "new"
        )
    }
    PantrixBadge(
        count = count,
        tone = BadgeTone.Neutral,
        contentDescription = pluralStringResource(Res.plurals.open_documents, count, count),
        tagKey = "open_documents"
    )
}

/** The launcher, the theme's switch where there is room for it ([withTheme]), and the settings. */
@Composable
private fun BarActions(
    phone: Boolean,
    wide: Boolean,
    withTheme: Boolean,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val size = when {
        wide -> ButtonSize.Small
        phone -> ButtonSize.Large
        else -> ButtonSize.Medium
    }
    if (wide) {
        Launcher()
    } else {
        PantrixIconButton(
            imageVector = PantrixIcons.Search,
            contentDescription = stringResource(Res.string.search_or_run_a_command),
            onClick = {},
            size = size,
            enabled = false,
            tagKey = "launcher"
        )
    }
    if (withTheme) {
        PantrixIconButton(
            imageVector = if (darkTheme) FormaticoIcons.Sun else FormaticoIcons.Moon,
            contentDescription = stringResource(
                if (darkTheme) Res.string.use_light_theme else Res.string.use_dark_theme
            ),
            onClick = onToggleTheme,
            size = size,
            tagKey = "theme"
        )
    }
    PantrixIconButton(
        imageVector = FormaticoIcons.Settings,
        contentDescription = stringResource(Res.string.settings),
        onClick = {},
        size = size,
        enabled = false,
        tagKey = "settings"
    )
}

/** Formatico's mark, `{ }` on the brand's colour, and its name where there is room for it ([withName]). */
@Composable
private fun Brand(phone: Boolean, modifier: Modifier = Modifier, withName: Boolean = true) {
    val primary = PantrixTheme.colors.primary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PantrixTheme.spacing.sm)
    ) {
        PantrixSurface(
            modifier = Modifier.size(MarkSize),
            color = primary.color,
            shape = RoundedCornerShape(MarkCorner)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PantrixText(
                    text = "{ }",
                    style = PantrixTheme.typography.code.copy(
                        fontSize = MarkTextSize,
                        letterSpacing = MarkLetterSpacing
                    ),
                    color = primary.onColor
                )
            }
        }
        if (withName) {
            PantrixText(
                text = stringResource(Res.string.app_name),
                style = (if (phone) PantrixTheme.typography.body else PantrixTheme.typography.label)
                    .copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

private val MarkSize = 28.dp
private val MarkCorner = 7.dp
private val MarkTextSize = 12.sp
private val MarkLetterSpacing = (-1).sp

/** What opens the command palette in a wide window: a field's look, with the palette's keys at its end. */
@Composable
private fun Launcher() {
    val colors = PantrixTheme.colors
    val shortcut = PantrixShortcut(Key.K, command = true)
    val label = stringResource(Res.string.search_or_run_a_command)
    PantrixButton(
        onClick = {},
        contentDescription = label,
        modifier = Modifier.width(LauncherWidth),
        size = ButtonSize.Small,
        enabled = false,
        shape = PantrixTheme.shapes.md,
        colors = ButtonColors(container = colors.background, content = colors.textSecondary, border = colors.border),
        tagKey = "launcher"
    ) {
        PantrixIcon(imageVector = PantrixIcons.Search, contentDescription = null)
        PantrixText(text = label, modifier = Modifier.weight(1f).padding(horizontal = PantrixTheme.spacing.sm))
        PantrixText(
            text = shortcut.writtenOn(LocalOperatingSystem.current),
            style = PantrixTheme.typography.caption
        )
    }
}
