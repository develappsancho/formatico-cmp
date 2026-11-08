package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.text.Indentation
import com.developersancho.formatico.core.settings.IndentChoice
import com.developersancho.formatico.core.settings.Preferences

/** How Format writes a document, as these preferences choose: their indentation, and whether keys are sorted. */
val Preferences.formatOptions: FormatOptions
    get() = FormatOptions(indentation = indentation.indentation, sortKeys = sortKeys)

/** The indentation a choice of Settings stands for. */
internal val IndentChoice.indentation: Indentation
    get() = when (this) {
        IndentChoice.TwoSpaces -> Indentation.Spaces(2)
        IndentChoice.FourSpaces -> Indentation.Spaces(4)
        IndentChoice.Tabs -> Indentation.Tabs
    }
