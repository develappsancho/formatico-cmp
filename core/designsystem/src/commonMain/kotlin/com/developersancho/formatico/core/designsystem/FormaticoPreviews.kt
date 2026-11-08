package com.developersancho.formatico.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pantrixui.foundation.adaptive.PantrixWindow
import com.pantrixui.foundation.date.LocalPantrixClock
import com.pantrixui.foundation.date.PantrixClock
import com.pantrixui.foundation.platform.PlatformType
import com.pantrixui.foundation.theme.PantrixTheme
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A board of the canvas: one of the three windows every screen is drawn in, by its width and height in dp and the
 * platform its controls are sized for. A screen's previews are drawn at each ([BoardPreviews]), and its screenshots.
 */
enum class Board(val width: Int, val height: Int, val platform: PlatformType) {
    /** The desktop's window. */
    Desktop(width = 1440, height = 900, platform = PlatformType.Desktop),

    /** A tablet's, upright. */
    Tablet(width = 834, height = 1194, platform = PlatformType.Android),

    /** A phone's. */
    Phone(width = 390, height = 844, platform = PlatformType.Android);

    /** The board as the window a screen is told it is in. */
    val window: PantrixWindow get() = PantrixWindow(DpSize(width.dp, height.dp))
}

/** A screen's previews: one at each [Board]'s width and height. Its cases say whether each is in the light theme or the dark. */
@Preview(name = "1440", group = "Desktop", widthDp = 1440, heightDp = 900)
@Preview(name = "834", group = "Tablet", widthDp = 834, heightDp = 1194)
@Preview(name = "390", group = "Phone", widthDp = 390, heightDp = 844)
annotation class BoardPreviews

/**
 * One case of a preview and of the screenshot drawn from it: [value], what a screen or a component is shown with,
 * by its [name], in the light theme or the [dark] one.
 */
class PreviewCase<T>(val name: String, val value: T, val dark: Boolean) {
    /** The case as its screenshot's file is named: `first-start-dark`. */
    val fileName: String get() = name.replace(' ', '-') + if (dark) "-dark" else "-light"

    override fun toString(): String = "$name, " + if (dark) "dark" else "light"
}

/**
 * Every one of [states], by its name, each in the light theme and the dark: the cases a screen's or a component's
 * previews are drawn with, and its screenshots.
 */
open class PreviewCases<T>(vararg states: Pair<String, T>) : PreviewParameterProvider<PreviewCase<T>> {
    private val cases = states.flatMap { (name, value) -> listOf(false, true).map { PreviewCase(name, value, it) } }

    override val values: Sequence<PreviewCase<T>> get() = cases.asSequence()

    override fun getDisplayName(index: Int): String = cases[index].toString()
}

/** The moment every preview is drawn at, in UTC: what "2 minutes ago" is counted from. */
val PreviewNow: Instant = Instant.parse("2026-11-03T09:41:00Z")

private val PreviewClock = PantrixClock(
    clock = object : Clock {
        override fun now(): Instant = PreviewNow
    },
    utcOffset = { Duration.ZERO }
)

/**
 * What a preview of [case] is drawn in: Formatico's theme, light or dark as the case is, on the theme's background,
 * with the clock standing at [PreviewNow]. A screenshot test draws the same inside Pantrix's test theme, which gives the
 * board's window and platform.
 */
@Composable
fun <T> FormaticoPreview(case: PreviewCase<T>, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    FormaticoTheme(darkTheme = case.dark) {
        CompositionLocalProvider(LocalPantrixClock provides PreviewClock) {
            Box(modifier = modifier.background(PantrixTheme.colors.background)) {
                content(case.value)
            }
        }
    }
}
