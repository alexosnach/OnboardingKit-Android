package appteam.onboardingkit.ui

import appteam.onboardingkit.step.StepConfiguration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeVisibilityTest {

    @Test
    fun defaultConfigurationShowsBackAndProgressButNotSkip() {
        val visibility = ChromeVisibility(configuration = StepConfiguration(), index = 1)

        assertTrue(visibility.isBackButtonVisible)
        assertTrue(visibility.isProgressBarVisible)
        assertFalse(visibility.isSkipButtonVisible)
    }

    /** Nowhere to go back to on the first step, whatever the configuration says. */
    @Test
    fun backIsNeverVisibleOnFirstStep() {
        val visibility = ChromeVisibility(configuration = StepConfiguration(isBackButtonShown = true), index = 0)

        assertFalse(visibility.isBackButtonVisible)
    }

    @Test
    fun stepCanHideBack() {
        val visibility = ChromeVisibility(configuration = StepConfiguration(isBackButtonShown = false), index = 2)

        assertFalse(visibility.isBackButtonVisible)
    }

    @Test
    fun stepCanShowSkip() {
        val visibility = ChromeVisibility(configuration = StepConfiguration(isSkipButtonShown = true), index = 0)

        assertTrue(visibility.isSkipButtonVisible)
    }

    @Test
    fun stepCanHideProgress() {
        val visibility = ChromeVisibility(configuration = StepConfiguration(isProgressBarShown = false), index = 1)

        assertFalse(visibility.isProgressBarVisible)
    }
}
