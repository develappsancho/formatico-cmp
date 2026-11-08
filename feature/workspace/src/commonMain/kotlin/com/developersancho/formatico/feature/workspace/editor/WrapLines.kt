package com.developersancho.formatico.feature.workspace.editor

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether the workspace's texts wrap a line too long for their pane under itself, as Settings' "Wrap long lines" says:
 * the app gives it; a preview or a test that does not lets lines scroll sideways.
 */
val LocalWrapLines = staticCompositionLocalOf { false }
