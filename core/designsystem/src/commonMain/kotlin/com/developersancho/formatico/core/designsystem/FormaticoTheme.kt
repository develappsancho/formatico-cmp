package com.developersancho.formatico.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.foundation.token.color.rememberPantrixColors

/**
 * Formatico's theme: Pantrix's theme built on [FormaticoPreset], and the syntax colours that go with it.
 *
 * @param darkTheme whether it is dark; the system's dark mode unless the reader has chosen.
 * @param content what the theme is in effect for: the app.
 */
@Composable
fun FormaticoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    PantrixTheme(
        preset = FormaticoPreset,
        colors = rememberPantrixColors(FormaticoPreset, darkTheme = darkTheme)
    ) {
        CompositionLocalProvider(
            LocalSyntaxColors provides if (darkTheme) SyntaxColors.Dark else SyntaxColors.Light,
            content = content
        )
    }
}

/** Formatico's own values of the theme in effect, beside Pantrix's `PantrixTheme`. */
object FormaticoTheme {
    /** The colours a document's text is drawn in. */
    val syntax: SyntaxColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSyntaxColors.current
}
