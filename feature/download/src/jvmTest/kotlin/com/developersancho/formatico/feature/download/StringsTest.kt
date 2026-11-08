package com.developersancho.formatico.feature.download

import com.developersancho.formatico.core.testing.untranslated
import com.developersancho.formatico.feature.download.strings.DownloadStringsEnglish
import com.developersancho.formatico.feature.download.strings.DownloadStringsTurkish
import kotlin.test.Test
import kotlin.test.assertEquals

/** The download menu's words are Turkish in Turkish. */
class StringsTest {
    @Test
    fun everyWordIsTurkishInTurkish() {
        // The names of Formatico and of the platforms, and what is only the values a row holds, are one in every language.
        assertEquals(
            setOf("formatico", "macOs", "windows", "android", "releaseUndated"),
            untranslated(DownloadStringsEnglish, DownloadStringsTurkish)
        )
    }
}
