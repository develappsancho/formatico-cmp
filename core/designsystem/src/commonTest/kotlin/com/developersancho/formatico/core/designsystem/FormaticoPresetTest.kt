package com.developersancho.formatico.core.designsystem

import androidx.compose.ui.graphics.Color
import com.pantrixui.foundation.token.color.Dark
import com.pantrixui.foundation.token.color.Light
import com.pantrixui.foundation.token.color.PantrixColors
import com.pantrixui.foundation.token.color.contrastRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FormaticoPresetTest {
    private val light = FormaticoPreset.colors(dark = false)
    private val dark = FormaticoPreset.colors(dark = true)

    @Test
    fun theBrandRoleIsTeal() {
        assertEquals(Color(0xFF0F766E), light.primary.color)
        assertEquals(Color(0xFF0D9488), light.focusRing)
        assertEquals(Color(0xFF2DD4BF), dark.primary.color)
        assertEquals(Color(0xFF2DD4BF), dark.focusRing)
    }

    @Test
    fun theRestOfTheThemeIsPantrixsOwn() {
        assertEquals(PantrixColors.Light.copy(primary = light.primary, focusRing = light.focusRing), light)
        assertEquals(PantrixColors.Dark.copy(primary = dark.primary, focusRing = dark.focusRing), dark)
    }

    @Test
    fun textOnTheBrandReads() {
        for (colors in listOf(light, dark)) {
            assertReads(colors.primary.onColor, colors.primary.color, "on the brand colour")
            assertReads(colors.primary.onContainer, colors.primary.container, "on the brand container")
        }
    }

    @Test
    fun everySyntaxColourReadsOnTheEditorsGrounds() {
        val themes = listOf(SyntaxColors.Light to light, SyntaxColors.Dark to dark)
        for ((syntax, colors) in themes) {
            val kinds = with(syntax) { listOf(key, string, number, literal, punctuation, comment) }
            for (kind in kinds) {
                assertReads(kind, colors.surface, "on the surface")
                assertReads(kind, syntax.currentLine, "on the current line")
            }
        }
    }

    private fun assertReads(foreground: Color, background: Color, where: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(ratio >= MINIMUM_TEXT_CONTRAST, "$foreground $where $background is $ratio:1")
    }

    private companion object {
        const val MINIMUM_TEXT_CONTRAST = 4.5f
    }
}
