package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.testing.untranslated
import com.developersancho.formatico.feature.start.strings.StartStringsEnglish
import com.developersancho.formatico.feature.start.strings.StartStringsTurkish
import kotlin.test.Test
import kotlin.test.assertEquals

/** Start's words are Turkish in Turkish. */
class StringsTest {
    @Test
    fun everyWordIsTurkishInTurkish() {
        // An example of an address is one in every language.
        assertEquals(setOf("addressPlaceholder"), untranslated(StartStringsEnglish, StartStringsTurkish))
    }
}
