package com.developersancho.formatico

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developersancho.formatico.core.designsystem.FormaticoIcons
import com.pantrixui.components.container.surface.PantrixSurface
import com.pantrixui.components.container.tab.PantrixTabs
import com.pantrixui.components.container.tab.TabItem
import com.pantrixui.components.elements.button.ButtonColors
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.button.PantrixButton
import com.pantrixui.components.elements.divider.PantrixHorizontalDivider
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.foundation.adaptive.WindowWidthClass
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.keyboard.PantrixShortcut
import com.pantrixui.foundation.platform.LocalOperatingSystem
import com.pantrixui.foundation.theme.PantrixTheme
import formatico.app.shared.generated.resources.Res
import formatico.app.shared.generated.resources.app_name
import formatico.app.shared.generated.resources.new_tab
import formatico.app.shared.generated.resources.search_or_run_a_command
import formatico.app.shared.generated.resources.settings
import formatico.app.shared.generated.resources.use_dark_theme
import formatico.app.shared.generated.resources.use_light_theme
import org.jetbrains.compose.resources.stringResource

/** The launcher's width in a wide window, as the design draws it. */
private val LauncherWidth = 340.dp

/** The bar's height: 52 in a wide window, where a pointer leads, and 56 elsewhere. */
private val WideBarHeight = 52.dp
private val BarHeight = 56.dp

/**
 * Formatico's bar, over every screen: the mark and the name, the documents' tabs, the launcher of the command palette,
 * the theme's switch and the settings. A phone keeps the mark, the name, the launcher's button and the settings.
 *
 * The tabs show the one tab there is; the tab that is added, the palette and the settings come with their screens,
 * and until then their buttons are drawn and not pressed.
 *
 * @param insets the window's insets at the bar's edges: the bar's fill runs behind them.
 * @param darkTheme whether the dark theme is in effect.
 * @param onToggleTheme asks for the other theme.
 */
@Composable
internal fun AppTopBar(
    insets: PaddingValues,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val window = PantrixTheme.window
    val spacing = PantrixTheme.spacing
    val phone = window.isNarrow
    val wide = window.widthClass >= WindowWidthClass.Expanded
    Column(modifier = modifier.fillMaxWidth().background(PantrixTheme.colors.surface).padding(insets)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (wide) WideBarHeight else BarHeight)
                .padding(start = spacing.lg, end = if (phone) spacing.xs else spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            Brand(phone = phone, modifier = if (phone) Modifier.weight(1f) else Modifier)
            if (!phone) {
                PantrixTabs(
                    tabs = listOf(TabItem(label = stringResource(Res.string.new_tab), tagKey = "new")),
                    selectedIndex = 0,
                    onSelect = {},
                    modifier = Modifier.weight(1f).padding(start = if (wide) spacing.lg else spacing.md),
                    scrollable = true,
                    tagKey = "documents"
                )
            }
            BarActions(phone = phone, wide = wide, darkTheme = darkTheme, onToggleTheme = onToggleTheme)
        }
        PantrixHorizontalDivider()
    }
}

/** The launcher, the theme's switch, which a phone leaves to Settings, and the settings. */
@Composable
private fun BarActions(phone: Boolean, wide: Boolean, darkTheme: Boolean, onToggleTheme: () -> Unit) {
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
    if (!phone) {
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

/** Formatico's mark, `{ }` on the brand's colour, and its name. */
@Composable
private fun Brand(phone: Boolean, modifier: Modifier = Modifier) {
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
        PantrixText(
            text = stringResource(Res.string.app_name),
            style = (if (phone) PantrixTheme.typography.body else PantrixTheme.typography.label)
                .copy(fontWeight = FontWeight.SemiBold)
        )
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
