package appteam.onboardingkit.flow

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import appteam.onboardingkit.step.OnboardingStep
import appteam.onboardingkit.step.StepConfiguration
import appteam.onboardingkit.ui.ChromeVisibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A step defined as a class with a constructor parameter. */
private class ScenarioTextStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {
        Text(id)
    }
}

/** A step with its own configuration. */
private class FormStep(
    override val id: String,
    override val configuration: StepConfiguration,
) : OnboardingStep {
    @Composable
    override fun Content() {
        Text("Name")
    }
}

/** A step defined as an object: one instance for the whole app. */
private object SingletonStep : OnboardingStep {
    override val id = "singleton"

    @Composable
    override fun Content() {}
}

private fun coordinator(stepsCount: Int): OnboardingCoordinator =
    OnboardingCoordinator(steps = List(stepsCount) { ScenarioTextStep("$it") })

/**
 * Whether [action] changes state that [read] reads: what makes Compose draw
 * again the composables that read it.
 */
private fun notifies(read: () -> Unit, action: () -> Unit): Boolean {
    val reads = mutableSetOf<Any>()
    val writes = mutableSetOf<Any>()
    Snapshot.observe(readObserver = { reads += it }) { read() }
    Snapshot.observe(writeObserver = { writes += it }) { action() }
    return reads.any { it in writes }
}

/** The coordinator as an app uses it: mixed steps and realistic sequences of actions. */
class OnboardingUsageScenarioTest {

    // Steps

    @Test
    fun mixedStepTypesKeepTheirOwnConfiguration() {
        val coordinator = OnboardingCoordinator(
            steps = listOf(
                ScenarioTextStep("welcome"),
                FormStep("name", StepConfiguration(isSkipButtonShown = true)),
                SingletonStep,
                FormStep("age", StepConfiguration(isBackButtonShown = false, isProgressBarShown = false)),
            ),
        )

        val visited = List(coordinator.steps.size) {
            val step = coordinator.steps[coordinator.currentIndex]
            val visibility = ChromeVisibility(configuration = step.configuration, index = coordinator.currentIndex)
            coordinator.next()
            step.id to visibility
        }

        assertEquals(listOf("welcome", "name", "singleton", "age"), visited.map { it.first })
        assertFalse(visited[0].second.isBackButtonVisible)
        assertTrue(visited[1].second.isSkipButtonVisible)
        assertEquals(ChromeVisibility(configuration = StepConfiguration(), index = 2), visited[2].second)
        assertFalse(visited[3].second.isBackButtonVisible)
        assertFalse(visited[3].second.isProgressBarVisible)
        assertTrue(coordinator.isCompleted)
    }

    /**
     * Ids are not used for navigation, so duplicates (a common copy-paste
     * mistake) do not break the flow.
     */
    @Test
    fun duplicateIdsStillNavigateByPosition() {
        val coordinator = OnboardingCoordinator(steps = List(3) { ScenarioTextStep("same") })

        coordinator.next()
        coordinator.next()
        coordinator.back()

        assertEquals(1, coordinator.currentIndex)
        assertFalse(coordinator.isCompleted)
    }

    @Test
    fun stepsKeepTheOrderTheyWereGiven() {
        val ids = List(20) { "step_$it" }
        val coordinator = OnboardingCoordinator(steps = ids.map(::ScenarioTextStep))

        assertEquals(ids, coordinator.steps.map { it.id })
    }

    // Navigation sequences

    @Test
    fun fullRoundTripReturnsToStart() {
        val coordinator = coordinator(stepsCount = 5)

        repeat(4) { coordinator.next() }
        repeat(4) { coordinator.back() }

        assertEquals(0, coordinator.currentIndex)
        assertFalse(coordinator.isCompleted)
    }

    @Test
    fun extraBackCallsNeverGoBelowFirstStep() {
        val coordinator = coordinator(stepsCount = 3)

        coordinator.next()
        repeat(10) { coordinator.back() }

        assertEquals(0, coordinator.currentIndex)
    }

    /** A user going back and forth, skipping some steps, finishes exactly once. */
    @Test
    fun mixedNextSkipBackSequence() {
        val coordinator = coordinator(stepsCount = 4)

        coordinator.next() // 1
        coordinator.skip() // 2
        coordinator.back() // 1
        coordinator.skip() // 2
        coordinator.next() // 3

        assertEquals(3, coordinator.currentIndex)
        assertFalse(coordinator.isCompleted)

        coordinator.skip() // complete

        assertTrue(coordinator.isCompleted)
    }

    /** Double taps on Continue after the last step must not move or "uncomplete" the flow. */
    @Test
    fun callsAfterCompletionKeepItCompleted() {
        val coordinator = coordinator(stepsCount = 2)

        coordinator.next()
        coordinator.next()
        coordinator.next()
        coordinator.skip()

        assertTrue(coordinator.isCompleted)
        assertEquals(1, coordinator.currentIndex)
    }

    /**
     * Completion is final: going back after it does not reopen the flow.
     * The app shows its main screen once the onboarding is completed.
     */
    @Test
    fun backAfterCompletionDoesNotReopenTheFlow() {
        val coordinator = coordinator(stepsCount = 2)

        coordinator.next()
        coordinator.next()
        coordinator.back()

        assertTrue(coordinator.isCompleted)
    }

    @Test
    fun emptyFlowIgnoresEveryAction() {
        val coordinator = coordinator(stepsCount = 0)

        coordinator.next()
        coordinator.skip()
        coordinator.back()

        assertEquals(0, coordinator.currentIndex)
        assertTrue(coordinator.isCompleted)
    }

    // Observation (what makes Compose draw again)

    @Test
    fun nextNotifiesObserversOfCurrentIndex() {
        val coordinator = coordinator(stepsCount = 3)

        assertTrue(notifies(read = { coordinator.currentIndex }, action = coordinator::next))
    }

    @Test
    fun backNotifiesObserversOfCurrentIndex() {
        val coordinator = coordinator(stepsCount = 3)
        coordinator.next()

        assertTrue(notifies(read = { coordinator.currentIndex }, action = coordinator::back))
    }

    /** The onboarding reports completion by observing `isCompleted`. */
    @Test
    fun finishingNotifiesObserversOfCompletion() {
        val coordinator = coordinator(stepsCount = 1)

        assertTrue(notifies(read = { coordinator.isCompleted }, action = coordinator::next))
    }

    /** Ignored actions do not cause pointless redraws. */
    @Test
    fun backOnFirstStepDoesNotNotify() {
        val coordinator = coordinator(stepsCount = 3)

        assertFalse(notifies(read = { coordinator.currentIndex }, action = coordinator::back))
    }
}
