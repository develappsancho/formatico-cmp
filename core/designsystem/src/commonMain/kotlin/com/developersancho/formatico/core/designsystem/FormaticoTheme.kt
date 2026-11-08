package com.developersancho.formatico.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.pantrixui.foundation.l10n.PantrixLocale
import com.pantrixui.foundation.theme.PantrixTheme
import com.pantrixui.foundation.token.color.rememberPantrixColors

/**
 * Formatico's theme: Pantrix's theme built on [FormaticoPreset], and the syntax colours that go with it.
 *
 * @param darkTheme whether it is dark; the system's dark mode unless the reader has chosen.
 * @param locale the language Pantrix's components speak, the one the reader chose in Settings; the system's where it
 *   is null.
 * @param content what the theme is in effect for: the app.
 */
@Composable
fun FormaticoTheme(darkTheme: Boolean = isSystemInDarkTheme(), locale: PantrixLocale? = null, content: @Composable () -> Unit) {
    val colors = rememberPantrixColors(FormaticoPreset, darkTheme = darkTheme)
    // The syntax colours follow the darkTheme given here, as a component cannot read whether the theme around it is
    // dark (PANTRIX_FINDINGS, "Bir bileşen, içinde durduğu temanın koyu olup olmadığını okuyamıyor").
    val syntax = @Composable {
        CompositionLocalProvider(
            LocalSyntaxColors provides if (darkTheme) SyntaxColors.Dark else SyntaxColors.Light,
            content = content
        )
    }
    // The theme's own language is the system's, or an enclosing theme's: one is given only where the reader chose it.
    if (locale == null) {
        PantrixTheme(preset = FormaticoPreset, colors = colors, content = syntax)
    } else {
        PantrixTheme(preset = FormaticoPreset, colors = colors, locale = locale, content = syntax)
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
