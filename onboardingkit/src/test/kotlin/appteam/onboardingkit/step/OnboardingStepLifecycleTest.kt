package appteam.onboardingkit.step

import androidx.compose.runtime.Composable
import appteam.onboardingkit.flow.OnboardingCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A step that writes its lifecycle calls to a shared journal, in the order they came. */
private class SpyStep(
    override val id: String,
    private val journal: MutableList<String>,
) : OnboardingStep, OnboardingStepLifecycle {
    @Composable
    override fun Content() {}

    override fun didBecomeCurrent() {
        journal += "become:$id"
    }

    override fun didResignCurrent() {
        journal += "resign:$id"
    }
}

/** A step without lifecycle hooks. */
private class PlainStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {}
}

/**
 * The coordinator tells steps implementing `OnboardingStepLifecycle` when they
 * become and stop being current: once per change, resign before become,
 * nothing on creation, and nothing after completion.
 */
class OnboardingStepLifecycleTest {
    private val journal = mutableListOf<String>()

    private fun coordinator(vararg ids: String): OnboardingCoordinator =
        OnboardingCoordinator(steps = ids.map { SpyStep(it, journal) })

    // start()

    @Test
    fun creatingTheCoordinatorCallsNothing() {
        coordinator("welcome", "name")

        assertTrue(journal.isEmpty())
    }

    @Test
    fun startMakesTheFirstStepCurrent() {
        coordinator("welcome", "name").start()

        assertEquals(listOf("become:welcome"), journal)
    }

    @Test
    fun startCountsOnlyOnce() {
        val flow = coordinator("welcome", "name")
        flow.start()
        flow.start()

        assertEquals(listOf("become:welcome"), journal)
    }

    @Test
    fun startOnEmptyFlowDoesNothing() {
        OnboardingCoordinator(steps = emptyList()).start()

        assertTrue(journal.isEmpty())
    }

    // Moving between steps

    @Test
    fun nextResignsTheOldStepBeforeTheNewOneBecomesCurrent() {
        val flow = coordinator("welcome", "name")
        flow.start()
        journal.clear()

        flow.next()

        assertEquals(listOf("resign:welcome", "become:name"), journal)
    }

    @Test
    fun skipMakesTheSameCallsAsNext() {
        val flow = coordinator("welcome", "name")
        flow.start()
        journal.clear()

        flow.skip()

        assertEquals(listOf("resign:welcome", "become:name"), journal)
    }

    @Test
    fun backMakesTheReverseCalls() {
        val flow = coordinator("welcome", "name")
        flow.start()
        flow.next()
        journal.clear()

        flow.back()

        assertEquals(listOf("resign:name", "become:welcome"), journal)
    }

    @Test
    fun backOnFirstStepCallsNothing() {
        val flow = coordinator("welcome", "name")
        flow.start()
        journal.clear()

        flow.back()

        assertTrue(journal.isEmpty())
    }

    @Test
    fun wholeFlowGivesEveryStepOneBecomeAndOneResign() {
        val flow = coordinator("welcome", "name", "age")
        flow.start()
        flow.next()
        flow.next()
        flow.next()

        assertEquals(
            listOf(
                "become:welcome",
                "resign:welcome", "become:name",
                "resign:name", "become:age",
                "resign:age",
            ),
            journal,
        )
    }

    /**
     * Flows of 0 to 3 steps, straight through: each step becomes current
     * once and resigns once, in order.
     */
    @Test
    fun straightThroughGivesEveryStepOnePair() {
        for (count in 0..3) {
            journal.clear()
            val ids = List(count) { "step$it" }
            val flow = coordinator(*ids.toTypedArray())
            flow.start()
            repeat(count) { flow.next() }

            assertEquals(ids.flatMap { listOf("become:$it", "resign:$it") }, journal)
            assertTrue(flow.isCompleted)
        }
    }

    // Completion

    @Test
    fun completingResignsTheLastStepWithoutBecome() {
        val flow = coordinator("welcome")
        flow.start()
        journal.clear()

        flow.next()

        assertTrue(flow.isCompleted)
        assertEquals(listOf("resign:welcome"), journal)
    }

    @Test
    fun actionsAfterCompletionCallNothing() {
        val flow = coordinator("welcome", "name")
        flow.start()
        flow.next()
        flow.next()
        journal.clear()

        flow.next()
        flow.skip()
        flow.back()
        flow.start()

        assertTrue(journal.isEmpty())
        assertEquals(1, flow.currentIndex)
    }

    // Steps without hooks

    @Test
    fun stepsWithoutHooksAreSkippedQuietly() {
        val flow = OnboardingCoordinator(
            steps = listOf(
                SpyStep("welcome", journal),
                PlainStep("plain"),
                SpyStep("age", journal),
            ),
        )
        flow.start()
        flow.next()
        flow.next()
        flow.back()

        assertEquals(
            listOf(
                "become:welcome",
                "resign:welcome",
                "become:age",
                "resign:age",
            ),
            journal,
        )
    }
}
