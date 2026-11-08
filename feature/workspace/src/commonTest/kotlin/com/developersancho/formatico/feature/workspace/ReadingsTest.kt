package com.developersancho.formatico.feature.workspace

import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.platform.PlatformType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingsTest {
    private val workbench = Workbench(worker = UnconfinedTestDispatcher())

    private fun Readings.keep(id: Long, text: String) = keep(id, text, Format.Json, readDocument(text, Format.Json) to TextStats.of(text))

    @Test
    fun aDocumentIsKeptWhileItIsOpenAndItsTextTheSame() {
        val readings = Readings(workbench)
        val id = workbench.open("a.json", "[1]", Format.Json)
        readings.keep(id, "[1]")

        assertNotNull(readings.of(id, "[1]", Format.Json))
        assertNull(readings.of(id, "[2]", Format.Json))
        assertNull(readings.of(id, "[1]", Format.Yaml))
        workbench.close(id)
        assertNull(readings.of(id, "[1]", Format.Json))
    }

    @Test
    fun pastItsBudgetTheReadingUsedLongestAgoIsLetGo() {
        // Room for two texts of ten characters, not three.
        val readings = Readings(workbench, budget = 25)
        val texts = listOf("[1,2,3,4] ", "[5,6,7,8] ", "[9,0,1,2] ")
        val (a, b, c) = texts.map { workbench.open(null, it, Format.Json) }
        readings.keep(a, texts[0])
        readings.keep(b, texts[1])
        // A is used again: B is the one used longest ago when C comes.
        assertNotNull(readings.of(a, texts[0], Format.Json))
        readings.keep(c, texts[2])

        assertNotNull(readings.of(a, texts[0], Format.Json))
        assertNull(readings.of(b, texts[1], Format.Json))
        assertNotNull(readings.of(c, texts[2], Format.Json))
    }

    @Test
    fun aDocumentLargerThanTheBudgetIsStillKeptWhileItIsTheOneShown() {
        val readings = Readings(workbench, budget = 4)
        val small = workbench.open(null, "[1]", Format.Json)
        val large = workbench.open(null, "[1, 2, 3]", Format.Json)
        readings.keep(small, "[1]")
        readings.keep(large, "[1, 2, 3]")

        assertNotNull(readings.of(large, "[1, 2, 3]", Format.Json))
        assertNull(readings.of(small, "[1]", Format.Json))
    }

    @Test
    fun aPhoneKeepsOneLargeDocumentsReadingAndADesktopOneOfFiftyMegabytes() {
        assertEquals(12_000_000L, readingsBudget(PlatformType.Android))
        assertEquals(12_000_000L, readingsBudget(PlatformType.Web))
        assertEquals(60_000_000L, readingsBudget(PlatformType.Desktop))
    }
}
