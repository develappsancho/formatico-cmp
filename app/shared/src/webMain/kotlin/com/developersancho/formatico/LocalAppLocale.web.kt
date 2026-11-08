package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

/** The page's window, where `index.html` reads the language chosen in the browser's own place. */
@JsName("window")
private external object PageWindow {
    /** The language chosen, by index.html's name for it. */
    @JsName("__formaticoLanguage")
    var formaticoLanguage: String?
}

/**
 * A browser gives its words' language as `navigator.languages`, and the words read outside a composition as
 * `navigator.language`; `index.html` answers both with the one chosen while it is chosen.
 */
actual object LocalAppLocale {
    private val Tag = staticCompositionLocalOf { Locale.current.toLanguageTag() }

    actual val current: String
        @Composable get() = Tag.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        PageWindow.formaticoLanguage = value
        return Tag provides Locale.current.toLanguageTag()
    }
}
