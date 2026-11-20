package com.frame.app.ui.theme

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guardrails for the design system: if the type scale or spacing scale drifts
 * (a fifth text size, a third weight, an off-grid spacing value), these fail.
 */
class DesignSystemTest {

    private val allStyles = with(FrameTypography) {
        listOf(
            displayLarge, displayMedium, displaySmall,
            headlineLarge, headlineMedium, headlineSmall,
            titleLarge, titleMedium, titleSmall,
            bodyLarge, bodyMedium, bodySmall,
            labelLarge, labelMedium, labelSmall,
        )
    }

    @Test
    fun typography_usesExactlyFourSizes() {
        val sizes = allStyles.map { it.fontSize.value }.toSet()
        assertEquals(setOf(20f, 16f, 14f, 12f), sizes)
    }

    @Test
    fun typography_usesExactlyTwoWeights() {
        val weights = allStyles.map { it.fontWeight }.toSet()
        assertEquals(setOf(FontWeight.Normal, FontWeight.SemiBold), weights)
    }

    @Test
    fun spacing_isOnTheFourPointGrid() {
        Spacing.all.forEach { dp ->
            assertEquals("$dp is off the 4dp grid", 0f, dp.value % 4f, 0f)
        }
    }

    @Test
    fun spacing_isStrictlyIncreasing() {
        val values = Spacing.all.map { it.value }
        assertEquals(values.sorted(), values)
        assertEquals(values.distinct(), values)
    }

    @Test
    fun touchTarget_meetsAndroidMinimum() {
        assertTrue(Sizes.touchTarget.value >= 48f)
    }

    @Test
    fun pill_isNeverTallerThanItsTouchTarget() {
        assertTrue(Sizes.pillHeight.value <= Sizes.touchTarget.value)
    }
}
