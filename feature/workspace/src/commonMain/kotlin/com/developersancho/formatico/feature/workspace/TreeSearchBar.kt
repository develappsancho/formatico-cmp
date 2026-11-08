package com.developersancho.formatico.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.developersancho.formatico.feature.workspace.strings.workspaceStrings
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.components.elements.icon.PantrixIcon
import com.pantrixui.components.elements.iconbutton.PantrixIconButton
import com.pantrixui.components.elements.text.PantrixText
import com.pantrixui.components.inputs.textfield.PantrixTextField
import com.pantrixui.foundation.icons.PantrixIcons
import com.pantrixui.foundation.theme.PantrixTheme

/** How wide the search's field is at most, as the boards draw it: 240 in a wide window, 280 on a tablet. */
private fun searchWidth(wide: Boolean) = if (wide) 240.dp else 280.dp

/** The widest the matches' count is set before it is cut: "12 of 3,456" with room to spare. */
private val PositionWidth = 120.dp

/**
 * The bar the tree is searched in: the field, which match of how many is chosen ([position]), the previous and the
 * next one, and closing it. Enter goes to the next match and Escape closes the bar. [focus] puts the caret in its field.
 */
@Composable
internal fun TreeSearchBar(
    query: TextFieldState,
    position: String?,
    layout: WorkspaceLayout,
    onStep: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    focus: FocusRequester = remember { FocusRequester() }
) {
    val spacing = PantrixTheme.spacing
    val wide = layout == WorkspaceLayout.Wide
    val phone = layout == WorkspaceLayout.Phone
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (wide) 48.dp else 56.dp)
            .background(if (phone) PantrixTheme.colors.surface else PantrixTheme.colors.background)
            .padding(start = spacing.md, end = if (phone) spacing.xs else spacing.sm)
            .onPreviewKeyEvent { event ->
                val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                if (escape) onClose()
                escape
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        val size = layout.buttons
        // The field, the matches' count and the steps take what the close button leaves: in a narrow pane the field is
        // narrower and the count is cut, never the close button.
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            PantrixTextField(
                state = query,
                // A phone's field takes what the buttons leave; a wider window's is as wide as its board draws it, or
                // as wide as the pane leaves it.
                modifier = (if (phone) Modifier.weight(1f) else Modifier.weight(1f, fill = false).widthIn(max = searchWidth(wide)))
                    .focusRequester(focus),
                leadingIcon = { PantrixIcon(imageVector = PantrixIcons.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { onStep(1) },
                contentDescription = workspaceStrings.find,
                tagKey = "find"
            )
            SearchSteps(position = position, size = size, onStep = onStep)
        }
        PantrixIconButton(
            imageVector = PantrixIcons.Close,
            contentDescription = workspaceStrings.closeSearch,
            onClick = onClose,
            size = size,
            tagKey = "close_search"
        )
    }
}

/** Which match of how many is chosen ([position]), cut where the bar is narrow, and the steps to the previous and the next. */
@Composable
private fun SearchSteps(position: String?, size: ButtonSize, onStep: (Int) -> Unit) {
    val strings = PantrixTheme.strings
    position?.let {
        PantrixText(
            text = it,
            modifier = Modifier.padding(horizontal = PantrixTheme.spacing.sm).widthIn(max = PositionWidth),
            style = PantrixTheme.typography.footnote.copy(fontWeight = FontWeight.Medium),
            color = PantrixTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    // Back and forward stand for up and down (PANTRIX_FINDINGS, "PantrixIcons'ta ChevronUp yok; aramanın önceki ve
    // sonraki eşleşmesi yukarı ve aşağı çizilemiyor").
    PantrixIconButton(
        imageVector = PantrixIcons.ChevronBack,
        contentDescription = strings.previousMatch,
        onClick = { onStep(-1) },
        size = size,
        tagKey = "previous_match"
    )
    PantrixIconButton(
        imageVector = PantrixIcons.ChevronForward,
        contentDescription = strings.nextMatch,
        onClick = { onStep(1) },
        size = size,
        tagKey = "next_match"
    )
}
