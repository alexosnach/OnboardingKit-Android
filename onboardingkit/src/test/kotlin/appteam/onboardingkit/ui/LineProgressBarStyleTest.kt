package appteam.onboardingkit.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineProgressBarStyleTest {

    /** Unspecified colors are the shared defaults: the style takes them from the theme. */
    @Test
    fun defaultStyleUsesSharedDefaultColors() {
        val style = ProgressBarStyle.Line

        assertEquals(Color.Unspecified, style.filledColor)
        assertEquals(Color.Unspecified, style.emptyColor)
    }

    @Test
    fun customColorsAreApplied() {
        val style = ProgressBarStyle.line(filledColor = Color.Magenta, emptyColor = Color.Yellow)

        assertEquals(Color.Magenta, style.filledColor)
        assertEquals(Color.Yellow, style.emptyColor)
    }

    @Test
    fun settingOneColorKeepsTheOtherDefault() {
        val style = ProgressBarStyle.line(filledColor = Color.Magenta)

        assertEquals(Color.Unspecified, style.emptyColor)
    }

    /**
     * Both built-in styles start from the same colors, so switching between them
     * does not change the look of the app.
     */
    @Test
    fun lineAndSegmentedShareDefaults() {
        val line = ProgressBarStyle.Line
        val segmented = ProgressBarStyle.Segmented

        assertEquals(segmented.filledColor, line.filledColor)
        assertEquals(segmented.emptyColor, line.emptyColor)
    }

    @Test
    fun lineShorthandIsTheLineStyle() {
        val style: ProgressBarStyle = ProgressBarStyle.Line

        assertTrue(style is LineProgressBarStyle)
    }

    /** Styles with the same colors are equal, so setting the same style again redraws nothing. */
    @Test
    fun stylesWithTheSameColorsAreEqual() {
        assertEquals(ProgressBarStyle.Line, ProgressBarStyle.Line)
        assertEquals(ProgressBarStyle.line(filledColor = Color.Red), ProgressBarStyle.line(filledColor = Color.Red))
    }
}
