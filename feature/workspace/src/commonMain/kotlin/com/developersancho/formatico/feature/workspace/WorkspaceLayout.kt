package com.developersancho.formatico.feature.workspace

import androidx.compose.runtime.Composable
import com.pantrixui.components.elements.button.ButtonSize
import com.pantrixui.foundation.adaptive.WindowWidthClass
import com.pantrixui.foundation.adaptive.isNarrow
import com.pantrixui.foundation.theme.PantrixTheme

/** The workspace's three layouts, by the window's width: the design's boards at 390, 834 and 1440. */
internal enum class WorkspaceLayout(val buttons: ButtonSize) {
    Phone(ButtonSize.Large),
    Tablet(ButtonSize.Medium),
    Wide(ButtonSize.Small)
}

/** The layout the window's width asks for. */
@Composable
internal fun workspaceLayout(): WorkspaceLayout {
    val window = PantrixTheme.window
    return when {
        window.isNarrow -> WorkspaceLayout.Phone
        window.widthClass >= WindowWidthClass.Expanded -> WorkspaceLayout.Wide
        else -> WorkspaceLayout.Tablet
    }
}
