package com.developersancho.formatico

import com.developersancho.formatico.core.testing.untranslated
import com.developersancho.formatico.strings.AppStringsEnglish
import com.developersancho.formatico.strings.AppStringsTurkish
import kotlin.test.Test
import kotlin.test.assertEquals

/** The app's own words are Turkish in Turkish: only its name is the same in both. */
class StringsTest {
    @Test
    fun everyWordIsTurkishInTurkish() {
        assertEquals(setOf("appName"), untranslated(AppStringsEnglish, AppStringsTurkish))
    }
}
