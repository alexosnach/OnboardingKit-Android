package appteam.onboardingkit.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressBarConfigurationTest {

    /** Step N of M fills N segments. */
    @Test
    fun completedCountCountsCurrentStep() {
        for ((currentIndex, expected) in listOf(0 to 1, 1 to 2, 2 to 3)) {
            val configuration = ProgressBarConfiguration(currentIndex = currentIndex, total = 3)

            assertEquals(expected, configuration.completedCount)
        }
    }

    @Test
    fun firstStepShowsOneSegmentNotZero() {
        val configuration = ProgressBarConfiguration(currentIndex = 0, total = 4)

        assertEquals(0.25f, configuration.fraction)
    }

    @Test
    fun lastStepShowsFullBar() {
        val configuration = ProgressBarConfiguration(currentIndex = 2, total = 3)

        assertEquals(1f, configuration.fraction)
    }

    /** Guards against integer division: `1 / 3` would be `0`. */
    @Test
    fun fractionIsNotRoundedDown() {
        val configuration = ProgressBarConfiguration(currentIndex = 0, total = 3)

        assertEquals(1f / 3f, configuration.fraction, 0.0001f)
    }

    @Test
    fun emptyFlowHasZeroFractionWithoutCrashing() {
        val configuration = ProgressBarConfiguration(currentIndex = 0, total = 0)

        assertEquals(0f, configuration.fraction)
    }

    // Edge cases

    /** A one-step flow is already complete on its only step. */
    @Test
    fun singleStepFlowShowsFullBar() {
        val configuration = ProgressBarConfiguration(currentIndex = 0, total = 1)

        assertEquals(1, configuration.completedCount)
        assertEquals(1f, configuration.fraction)
    }

    /** An index past the end (e.g. from a custom style preview) never overfills the bar. */
    @Test
    fun indexPastTheEndIsClamped() {
        for (currentIndex in listOf(3, 4, 100)) {
            val configuration = ProgressBarConfiguration(currentIndex = currentIndex, total = 3)

            assertEquals(3, configuration.completedCount)
            assertEquals(1f, configuration.fraction)
        }
    }

    /** A negative index never produces negative progress. */
    @Test
    fun negativeIndexIsClamped() {
        for (currentIndex in listOf(-1, -5)) {
            val configuration = ProgressBarConfiguration(currentIndex = currentIndex, total = 3)

            assertEquals(0, configuration.completedCount)
            assertEquals(0f, configuration.fraction)
        }
    }

    @Test
    fun emptyFlowHasNoCompletedSegments() {
        val configuration = ProgressBarConfiguration(currentIndex = 0, total = 0)

        assertEquals(0, configuration.completedCount)
    }

    /** Progress only grows as the flow moves forward. */
    @Test
    fun fractionGrowsMonotonically() {
        val fractions = (0 until 10).map { ProgressBarConfiguration(currentIndex = it, total = 10).fraction }

        assertEquals(fractions.sorted(), fractions)
        assertEquals(fractions.size, fractions.toSet().size)
    }
}
