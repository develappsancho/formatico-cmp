package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** The desktop reads its words in the JVM's language: the one chosen is the JVM's while it is chosen. */
actual object LocalAppLocale {
    private var system: Locale? = null
    private val Tag = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = Tag.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (value == null) {
            system?.let(Locale::setDefault)
            system = null
        } else {
            if (system == null) system = Locale.getDefault()
            Locale.setDefault(Locale.forLanguageTag(value))
        }
        return Tag provides Locale.getDefault().toLanguageTag()
    }
}
