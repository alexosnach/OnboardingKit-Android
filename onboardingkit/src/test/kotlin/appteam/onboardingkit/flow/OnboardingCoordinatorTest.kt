package appteam.onboardingkit.flow

import androidx.compose.runtime.Composable
import appteam.onboardingkit.step.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A minimal step used only in tests: the coordinator does not care about content. */
private class StubStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {}
}

private fun makeCoordinator(stepsCount: Int): OnboardingCoordinator =
    OnboardingCoordinator(steps = List(stepsCount) { StubStep("step_$it") })

class OnboardingCoordinatorTest {

    // Initial state

    @Test
    fun startsOnFirstStepAndNotCompleted() {
        val coordinator = makeCoordinator(stepsCount = 3)

        assertEquals(0, coordinator.currentIndex)
        assertFalse(coordinator.isCompleted)
    }

    @Test
    fun emptyFlowIsCompletedImmediately() {
        val coordinator = makeCoordinator(stepsCount = 0)

        assertTrue(coordinator.isCompleted)
    }

    // next()

    @Test
    fun nextMovesToFollowingStep() {
        val coordinator = makeCoordinator(stepsCount = 3)

        coordinator.next()

        assertEquals(1, coordinator.currentIndex)
        assertFalse(coordinator.isCompleted)
    }

    @Test
    fun nextOnLastStepCompletesAndKeepsIndexInBounds() {
        val coordinator = makeCoordinator(stepsCount = 3)

        coordinator.next() // 1
        coordinator.next() // 2 (last)
        coordinator.next() // complete

        assertTrue(coordinator.isCompleted)
        assertEquals(2, coordinator.currentIndex)
    }

    @Test
    fun singleStepFlowCompletesOnFirstNext() {
        val coordinator = makeCoordinator(stepsCount = 1)

        coordinator.next()

        assertTrue(coordinator.isCompleted)
        assertEquals(0, coordinator.currentIndex)
    }

    @Test
    fun extraNextCallsNeverLeaveBounds() {
        val coordinator = makeCoordinator(stepsCount = 3)

        repeat(10) { coordinator.next() }

        assertEquals(2, coordinator.currentIndex)
        assertTrue(coordinator.isCompleted)
    }

    @Test
    fun nextOnEmptyFlowDoesNotCrash() {
        val coordinator = makeCoordinator(stepsCount = 0)

        coordinator.next()

        assertEquals(0, coordinator.currentIndex)
        assertTrue(coordinator.isCompleted)
    }

    // skip()

    /**
     * skip() and next() must navigate identically: this guards against
     * skip() missing the end-of-flow check.
     */
    @Test
    fun skipNavigatesExactlyLikeNext() {
        for (stepsCount in listOf(1, 2, 3, 5)) {
            val viaNext = makeCoordinator(stepsCount)
            val viaSkip = makeCoordinator(stepsCount)

            repeat(stepsCount + 1) {
                viaNext.next()
                viaSkip.skip()

                assertEquals(viaNext.currentIndex, viaSkip.currentIndex)
                assertEquals(viaNext.isCompleted, viaSkip.isCompleted)
            }
        }
    }

    @Test
    fun skipOnLastStepCompletes() {
        val coordinator = makeCoordinator(stepsCount = 2)

        coordinator.skip() // 1 (last)
        coordinator.skip() // complete

        assertTrue(coordinator.isCompleted)
        assertEquals(1, coordinator.currentIndex)
    }

    // back()

    @Test
    fun backOnFirstStepHasNoEffect() {
        val coordinator = makeCoordinator(stepsCount = 3)

        coordinator.back()

        assertEquals(0, coordinator.currentIndex)
    }

    @Test
    fun backReturnsToPreviousStep() {
        val coordinator = makeCoordinator(stepsCount = 3)

        coordinator.next() // 1
        coordinator.next() // 2
        coordinator.back() // 1

        assertEquals(1, coordinator.currentIndex)
    }

    @Test
    fun backOnEmptyFlowDoesNotGoNegative() {
        val coordinator = makeCoordinator(stepsCount = 0)

        coordinator.back()

        assertEquals(0, coordinator.currentIndex)
    }
}
