package com.developersancho.formatico.feature.start

import com.developersancho.formatico.core.format.Format
import com.pantrixui.foundation.files.PantrixFileType
import com.pantrixui.foundation.platform.PlatformType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentFilesTest {
    @Test
    fun aDesktopOpensFiftyMegabytesAndEveryOtherPlatformTen() {
        assertEquals(50_000_000L, maxFileSize(PlatformType.Desktop))
        PlatformType.entries.filter { it != PlatformType.Desktop }.forEach {
            assertEquals(10_000_000L, maxFileSize(it), "$it")
        }
    }

    @Test
    fun thePickerTakesEveryFormatsExtensionsAndPlainText() {
        Format.entries.forEach { format ->
            assertTrue(DocumentFileTypes.any { it.extensions == format.extensions }, "$format")
        }
        assertTrue(PantrixFileType.Text in DocumentFileTypes)
        assertEquals("application/json", DocumentFileTypes.first { "json" in it.extensions }.mimeType)
    }
}
