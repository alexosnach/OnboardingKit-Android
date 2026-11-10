package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SegmentedProgressBarStyleTest {
    @get:Rule
    val compose = createComposeRule()

    private val configuration = ProgressBarConfiguration(currentIndex = 1, total = 3) // 2 of 3

    private val redBlue = ProgressBarStyle.segmented(filledColor = Color.Red, emptyColor = Color.Blue)

    private fun colors(currentIndex: Int, total: Int): List<Color> {
        val configuration = ProgressBarConfiguration(currentIndex = currentIndex, total = total)
        return List(total) { redBlue.color(forSegment = it, configuration) }
    }

    @Test
    fun defaultStyleUsesDefaultColors() {
        val style = ProgressBarStyle.Segmented

        assertEquals(Color.Unspecified, style.filledColor)
        assertEquals(Color.Unspecified, style.emptyColor)
    }

    @Test
    fun customColorsAreApplied() {
        val style = ProgressBarStyle.segmented(filledColor = Color.Magenta, emptyColor = Color.Yellow)

        assertEquals(Color.Magenta, style.color(forSegment = 0, configuration))
        assertEquals(Color.Magenta, style.color(forSegment = 1, configuration))
        assertEquals(Color.Yellow, style.color(forSegment = 2, configuration))
    }

    /** Setting only one color keeps the other one at its default. */
    @Test
    fun settingOneColorKeepsTheOtherDefault() {
        val style = ProgressBarStyle.segmented(filledColor = Color.Magenta)

        assertEquals(Color.Magenta, style.filledColor)
        assertEquals(Color.Unspecified, style.emptyColor)
    }

    /** Step N of M fills N segments, whatever the colors are. */
    @Test
    fun filledSegmentsFollowCompletedCount() {
        assertEquals(listOf(Color.Red, Color.Red, Color.Blue), colors(currentIndex = 1, total = 3))
    }

    // Edge cases

    @Test
    fun firstStepFillsOnlyFirstSegment() {
        assertEquals(listOf(Color.Red, Color.Blue, Color.Blue, Color.Blue), colors(currentIndex = 0, total = 4))
    }

    @Test
    fun lastStepFillsAllSegments() {
        assertEquals(List(4) { Color.Red }, colors(currentIndex = 3, total = 4))
    }

    @Test
    fun singleStepFlowFillsItsOnlySegment() {
        assertEquals(listOf(Color.Red), colors(currentIndex = 0, total = 1))
    }

    @Test
    fun indexPastTheEndFillsAllSegments() {
        assertEquals(List(3) { Color.Red }, colors(currentIndex = 9, total = 3))
    }

    /** An empty flow draws no segments and does not crash. */
    @Test
    fun emptyFlowBuildsBody() {
        compose.setContent {
            MaterialTheme { redBlue.Body(ProgressBarConfiguration(currentIndex = 0, total = 0)) }
        }

        compose.waitForIdle()
    }

    /** Long flows keep exactly one filled segment per reached step. */
    @Test
    fun longFlowFillsReachedSteps() {
        val result = colors(currentIndex = 29, total = 50)

        assertEquals(30, result.count { it == Color.Red })
        assertTrue(result.take(30).all { it == Color.Red })
    }
}
