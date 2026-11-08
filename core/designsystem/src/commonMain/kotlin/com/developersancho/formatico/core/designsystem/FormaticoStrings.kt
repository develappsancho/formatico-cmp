package com.developersancho.formatico.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.pantrixui.foundation.theme.PantrixTheme

/**
 * Of a feature's words in [english] and in [turkish], the ones of the theme's language: Turkish for Turkish, English
 * for every other. Each feature keeps its words in its own `strings` package, an interface and an object of it for
 * each language, as Pantrix keeps its components' (`PantrixStrings`); the language is the theme's, which the reader
 * chose in Settings, the system's until then, and a test's own in `PantrixTestTheme`.
 */
@Composable
@ReadOnlyComposable
fun <T> inTheLanguage(english: T, turkish: T): T = inLanguage(PantrixTheme.locale.language, english, turkish)

/** Of [english] and [turkish], the words of the language whose tag is [language], away from a screen. */
fun <T> inLanguage(language: String, english: T, turkish: T): T =
    if (language.substringBefore('-').lowercase() == TURKISH) turkish else english

private const val TURKISH = "tr"
