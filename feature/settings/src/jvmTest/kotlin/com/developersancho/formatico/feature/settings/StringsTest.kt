package com.developersancho.formatico.feature.settings

import com.developersancho.formatico.core.testing.untranslated
import com.developersancho.formatico.feature.settings.strings.SettingsStringsEnglish
import com.developersancho.formatico.feature.settings.strings.SettingsStringsTurkish
import kotlin.test.Test
import kotlin.test.assertEquals

/** Settings' words are Turkish in Turkish: only the app's name and its version are the same in both. */
class StringsTest {
    @Test
    fun everyWordIsTurkishInTurkish() {
        assertEquals(setOf("version"), untranslated(SettingsStringsEnglish, SettingsStringsTurkish))
    }
}
