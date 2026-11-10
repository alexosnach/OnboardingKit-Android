package appteam.onboardingkit.step

import androidx.compose.runtime.Composable
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A step that relies on the interface's default configuration. */
private class DefaultConfigurationStep : OnboardingStep {
    override val id = "default"

    @Composable
    override fun Content() {}
}

/** A step that overrides the configuration. */
private class CustomConfigurationStep : OnboardingStep {
    override val id = "custom"
    override val configuration = StepConfiguration(isSkipButtonShown = true)

    @Composable
    override fun Content() {}
}

class StepConfigurationTest {

    /** Locks the documented defaults: changing one of them is a breaking change. */
    @Test
    fun defaultValuesMatchDocumentation() {
        val configuration = StepConfiguration()

        assertTrue(configuration.isBackButtonShown)
        assertTrue(configuration.isProgressBarShown)
        assertFalse(configuration.isSkipButtonShown)
    }

    @Test
    fun overridingOneValueKeepsOtherDefaults() {
        val configuration = StepConfiguration(isSkipButtonShown = true)

        assertTrue(configuration.isSkipButtonShown)
        assertTrue(configuration.isBackButtonShown)
        assertTrue(configuration.isProgressBarShown)
    }
}

class OnboardingStepTest {

    @Test
    fun stepWithoutConfigurationUsesDefaults() {
        val configuration = DefaultConfigurationStep().configuration

        assertTrue(configuration.isBackButtonShown)
        assertTrue(configuration.isProgressBarShown)
        assertFalse(configuration.isSkipButtonShown)
    }

    /**
     * Checks that the step's own configuration wins over the interface default
     * when the step is accessed as `OnboardingStep`, exactly how the coordinator
     * stores steps.
     */
    @Test
    fun customConfigurationOverridesDefaultThroughTheInterface() {
        val step: OnboardingStep = CustomConfigurationStep()

        assertTrue(step.configuration.isSkipButtonShown)
    }
}
