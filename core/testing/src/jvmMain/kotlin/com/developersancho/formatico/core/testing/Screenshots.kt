package com.developersancho.formatico.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.developersancho.formatico.core.designsystem.Board
import com.developersancho.formatico.core.designsystem.PREVIEW_TAG
import com.developersancho.formatico.core.designsystem.PreviewCase
import com.pantrixui.testing.PantrixTestTheme
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale

/**
 * Draws [content], the preview of [case], in [board]'s window and platform, light or dark as the case is, and writes
 * what the preview draws, a screen's window or a component at its own size, as
 * `screenshots/[name]/<case>-<board's width>.png` of the module under test: what `recordRoborazziJvm` writes and
 * `verifyRoborazziJvm` compares against.
 */
@OptIn(ExperimentalTestApi::class)
fun capturePreview(name: String, case: PreviewCase<*>, board: Board, content: @Composable () -> Unit) = inEnglish {
    runDesktopComposeUiTest(width = board.width, height = board.height) {
        setContent {
            PantrixTestTheme(platform = board.platform, dark = case.dark, window = board.window) { content() }
        }
        waitForIdle()
        onNodeWithTag(PREVIEW_TAG).captureRoboImage("screenshots/$name/${case.fileName}-${board.width}.png")
    }
}

/**
 * Draws [content], the preview of [case], as [capturePreview] does, presses the control whose automation key is
 * [opener] (a menu's button), and writes the menu it opens, which is a window of its own: a menu where it drops down,
 * or a phone's sheet over its scrim. A preview cannot draw a menu open; this is where an open one is seen.
 */
@OptIn(ExperimentalTestApi::class)
fun captureOpenMenu(name: String, case: PreviewCase<*>, board: Board, opener: String, content: @Composable () -> Unit) =
    inEnglish {
        runDesktopComposeUiTest(width = board.width, height = board.height) {
            setContent {
                PantrixTestTheme(platform = board.platform, dark = case.dark, window = board.window) { content() }
            }
            waitForIdle()
            onNode(hasClickAction() and hasKey(opener)).performClick()
            waitForIdle()
            onAllNodes(isRoot()).onLast().captureRoboImage("screenshots/$name/${case.fileName}-${board.width}.png")
        }
    }

/** A control whose automation key is [key], in whatever component and scope: `Button_more`, `Bar.IconButton_more`. */
private fun hasKey(key: String) = SemanticsMatcher("has the automation key $key") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)?.endsWith("_$key") == true
}

/**
 * Runs [block] in English, and gives the JVM its language back: an app's words are Compose resources, which the
 * desktop reads in the JVM's language, not the theme's.
 */
fun <T> inEnglish(block: () -> T): T {
    val locale = Locale.getDefault()
    Locale.setDefault(Locale.ENGLISH)
    return try {
        block()
    } finally {
        Locale.setDefault(locale)
    }
}
