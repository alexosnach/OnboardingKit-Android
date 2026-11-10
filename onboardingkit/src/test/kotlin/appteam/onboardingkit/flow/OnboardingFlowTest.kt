package appteam.onboardingkit.flow

import appteam.onboardingkit.step.OnboardingStep
import appteam.onboardingkit.ui.StubStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingFlowTest {
    @Test
    fun flowHasNoIdByDefault() {
        assertNull(OnboardingFlow(steps = listOf(StubStep("a"))).flowId)
    }

    /** As a Swift array: the flow has its own steps, whatever happens to the list it was made from. */
    @Test
    fun changingTheGivenListLaterDoesNotChangeTheFlow() {
        val steps = mutableListOf<OnboardingStep>(StubStep("a"), StubStep("b"))
        val flow = OnboardingFlow(steps = steps, flowId = "v1")

        steps.clear()

        assertEquals(listOf("a", "b"), flow.steps.map { it.id })
    }
}
