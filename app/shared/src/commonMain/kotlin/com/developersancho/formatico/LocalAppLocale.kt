package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * The language the app's own words are read in, the one the reader chose in Settings, over the system's: Compose's
 * resources read the platform's language, which each platform is told here. Given null, the system's is left as it
 * is. What reads the words again is a composition started over, under `key` of the language.
 */
expect object LocalAppLocale {
    /** The language in effect, as its tag. */
    val current: String
        @Composable get

    /** Gives [value], a language's tag, to the platform; null gives the system's back. */
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
