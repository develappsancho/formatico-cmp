package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/** iOS reads its words in the languages the app prefers: the one chosen leads them while it is chosen. */
actual object LocalAppLocale {
    private const val LANGUAGES = "AppleLanguages"
    private val system = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
    private val Tag = staticCompositionLocalOf { system }

    actual val current: String
        @Composable get() = Tag.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val defaults = NSUserDefaults.standardUserDefaults
        if (value == null) defaults.removeObjectForKey(LANGUAGES) else defaults.setObject(listOf(value), LANGUAGES)
        return Tag provides (value ?: system)
    }
}
