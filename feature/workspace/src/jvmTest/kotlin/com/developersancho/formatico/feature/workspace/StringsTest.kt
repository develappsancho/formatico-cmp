package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.testing.untranslated
import com.developersancho.formatico.feature.workspace.strings.CompareStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.CompareStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.ConvertStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.ConvertStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.PaletteStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.PaletteStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.ProblemStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.ProblemStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.QueryStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.QueryStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.ShareStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.ShareStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.SummaryStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.SummaryStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.TableStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.TableStringsTurkish
import com.developersancho.formatico.feature.workspace.strings.WorkspaceStringsEnglish
import com.developersancho.formatico.feature.workspace.strings.WorkspaceStringsTurkish
import kotlin.test.Test
import kotlin.test.assertEquals

/** The workspace's words are Turkish in Turkish, on each of its screens: only JSON's null is the same in both. */
class StringsTest {
    @Test
    fun everyWordIsTurkishInTurkish() {
        assertEquals(setOf("kindNull"), untranslated(WorkspaceStringsEnglish, WorkspaceStringsTurkish))
        assertEquals(setOf("typeNull"), untranslated(TableStringsEnglish, TableStringsTurkish))
        assertEquals(setOf("kindNull"), untranslated(SummaryStringsEnglish, SummaryStringsTurkish))
        listOf(
            ProblemStringsEnglish to ProblemStringsTurkish,
            QueryStringsEnglish to QueryStringsTurkish,
            ConvertStringsEnglish to ConvertStringsTurkish,
            CompareStringsEnglish to CompareStringsTurkish,
            ShareStringsEnglish to ShareStringsTurkish,
            PaletteStringsEnglish to PaletteStringsTurkish
        ).forEach { (english, turkish) -> assertEquals(emptySet(), untranslated(english, turkish), english::class.simpleName) }
    }
}
