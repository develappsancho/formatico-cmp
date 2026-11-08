package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.FormatDetector
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SampleSourceTest {
    @Test
    fun everySampleIsInTheModulesResourcesInTheFormatItSays() = runTest {
        val source = ResourceSampleSource()
        Sample.entries.forEach { sample ->
            val text = source.read(sample)
            assertEquals(sample.format, FormatDetector.detect(text, sample.fileName), sample.fileName)
        }
    }
}
