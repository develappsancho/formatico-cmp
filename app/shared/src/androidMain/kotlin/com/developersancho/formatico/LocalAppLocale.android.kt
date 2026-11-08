package com.developersancho.formatico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Android reads its words in its configuration's language: the one chosen is the configuration's while it is chosen. */
actual object LocalAppLocale {
    private var system: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toLanguageTag()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val chosen = if (value == null) {
            system.also { system = null }
        } else {
            if (system == null) system = Locale.getDefault()
            Locale.forLanguageTag(value)
        }
        if (chosen != null) {
            Locale.setDefault(chosen)
            configuration.setLocale(chosen)
            val resources = LocalContext.current.resources
            @Suppress("DEPRECATION") // The configuration is the one this composition reads, given back to it below.
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        return LocalConfiguration provides configuration
    }
}
