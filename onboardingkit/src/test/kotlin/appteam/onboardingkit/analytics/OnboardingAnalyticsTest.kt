package appteam.onboardingkit.analytics

import androidx.compose.runtime.Composable
import appteam.onboardingkit.flow.OnboardingCoordinator
import appteam.onboardingkit.step.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records every event it receives. */
private class SpyAnalytics : OnboardingAnalytics {
    val events = mutableListOf<OnboardingAnalyticsEvent>()
    val kinds: List<OnboardingEvent> get() = events.map { it.kind }

    override fun track(event: OnboardingAnalyticsEvent) {
        events += event
    }
}

private class PlainStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {}
}

/**
 * The coordinator reports the funnel: started, every step shown, what the
 * user did on it, and completed. Commands that change nothing report nothing.
 */
class OnboardingAnalyticsTest {
    private val analytics = SpyAnalytics()

    private fun coordinator(ids: List<String>, flowId: String? = null): OnboardingCoordinator =
        OnboardingCoordinator(steps = ids.map(::PlainStep), flowId = flowId, analytics = analytics)

    private fun coordinator(vararg ids: String): OnboardingCoordinator = coordinator(ids.toList())

    private fun info(id: String, index: Int, of: Int) = OnboardingStepInfo(id = id, index = index, total = of)

    // The funnel

    @Test
    fun creatingTheCoordinatorReportsNothing() {
        coordinator("welcome", "name")

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun startReportsStartedAndTheFirstStep() {
        coordinator("welcome", "name").start()

        assertEquals(
            listOf(OnboardingEvent.Started, OnboardingEvent.StepShown(info("welcome", 0, of = 2))),
            analytics.kinds,
        )
    }

    @Test
    fun startReportsOnlyOnce() {
        val flow = coordinator("welcome", "name")
        flow.start()
        flow.start()

        assertEquals(2, analytics.kinds.size)
    }

    @Test
    fun wholeFlowReportsTheFunnelInOrder() {
        val flow = coordinator("welcome", "name", "age")
        flow.start()
        flow.next()
        flow.skip()
        flow.back()
        flow.next()
        flow.next()

        assertEquals(
            listOf(
                OnboardingEvent.Started,
                OnboardingEvent.StepShown(info("welcome", 0, of = 3)),
                OnboardingEvent.StepContinued(info("welcome", 0, of = 3)),
                OnboardingEvent.StepShown(info("name", 1, of = 3)),
                OnboardingEvent.StepSkipped(info("name", 1, of = 3)),
                OnboardingEvent.StepShown(info("age", 2, of = 3)),
                OnboardingEvent.StepBack(info("age", 2, of = 3)),
                OnboardingEvent.StepShown(info("name", 1, of = 3)),
                OnboardingEvent.StepContinued(info("name", 1, of = 3)),
                OnboardingEvent.StepShown(info("age", 2, of = 3)),
                OnboardingEvent.StepContinued(info("age", 2, of = 3)),
                OnboardingEvent.Completed,
            ),
            analytics.kinds,
        )
    }

    // Flows of 0 to 3 steps

    /** Going straight through: started, then each step shown and continued, then completed. */
    @Test
    fun straightThroughReportsEveryStep() {
        for (count in 0..3) {
            analytics.events.clear()
            val ids = List(count) { "step$it" }
            val flow = coordinator(ids)
            flow.start()
            repeat(count) { flow.next() }

            val expected = buildList {
                if (count > 0) {
                    add(OnboardingEvent.Started)
                    for (index in 0 until count) {
                        val step = info(ids[index], index, of = count)
                        add(OnboardingEvent.StepShown(step))
                        add(OnboardingEvent.StepContinued(step))
                    }
                    add(OnboardingEvent.Completed)
                }
            }
            assertEquals(expected, analytics.kinds)
            assertTrue(flow.isCompleted)
        }
    }

    /**
     * Back from every step to the first, then one more: one back and one
     * shown per move, never below the first step.
     */
    @Test
    fun backToTheFirstStepAndForwardAgain() {
        for (count in 1..3) {
            val ids = List(count) { "step$it" }
            val flow = coordinator(ids)
            flow.start()
            repeat(count - 1) { flow.next() }
            analytics.events.clear()

            repeat(count) { flow.back() } // one more than possible

            val expected = buildList {
                for (index in count - 1 downTo 1) {
                    add(OnboardingEvent.StepBack(info(ids[index], index, of = count)))
                    add(OnboardingEvent.StepShown(info(ids[index - 1], index - 1, of = count)))
                }
            }
            assertEquals(expected, analytics.kinds)
            assertEquals(0, flow.currentIndex)
            assertFalse(flow.isCompleted)
        }
    }

    /** Skip on every step ends the flow like next, only reported as skipped. */
    @Test
    fun skippingEveryStepCompletes() {
        for (count in 1..3) {
            analytics.events.clear()
            val flow = coordinator(List(count) { "step$it" })
            flow.start()
            repeat(count) { flow.skip() }

            assertEquals(count, analytics.kinds.count { it is OnboardingEvent.StepSkipped })
            assertEquals(OnboardingEvent.Completed, analytics.kinds.last())
            assertTrue(flow.isCompleted)
        }
    }

    // Commands that change nothing

    @Test
    fun backOnFirstStepReportsNothing() {
        val flow = coordinator("welcome", "name")
        flow.start()
        analytics.events.clear()

        flow.back()

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun actionsAfterCompletionReportNothing() {
        val flow = coordinator("welcome")
        flow.start()
        flow.next()
        analytics.events.clear()

        flow.next()
        flow.skip()
        flow.back()
        flow.start()

        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun emptyFlowReportsNothing() {
        val flow = coordinator()
        flow.start()
        flow.next()

        assertTrue(analytics.events.isEmpty())
    }

    // Flow id

    @Test
    fun everyEventCarriesTheFlowId() {
        val flow = coordinator(listOf("welcome"), flowId = "variant_b")
        flow.start()
        flow.next()

        assertEquals(4, analytics.events.size)
        assertTrue(analytics.events.all { it.flowId == "variant_b" })
    }

    @Test
    fun flowIdIsOptional() {
        coordinator("welcome").start()

        assertTrue(analytics.events.all { it.flowId == null })
    }

    @Test
    fun coordinatorWithoutAnalyticsStillWorks() {
        val flow = OnboardingCoordinator(steps = listOf(PlainStep("welcome")))
        flow.start()
        flow.next()

        assertTrue(flow.isCompleted)
    }

    /**
     * Steps read `LocalOnboardingStepInfo` to tie their own events to the
     * funnel, so it must be the same value the coordinator reports.
     */
    @Test
    fun stepInfoMatchesWhatAnalyticsReports() {
        val flow = coordinator("welcome", "name", "age")
        flow.start()
        flow.next()

        assertEquals(info("name", 1, of = 3), flow.stepInfo(at = 1))
        assertEquals(OnboardingEvent.StepShown(flow.stepInfo(at = 1)), analytics.kinds.last())
    }
}

/** The flat form that analytics systems receive. */
class OnboardingAnalyticsEventTest {
    private val step = OnboardingStepInfo(id = "name", index = 1, total = 6)

    @Test
    fun eachEventHasItsName() {
        val any = OnboardingStepInfo(id = "a", index = 0, total = 1)
        val names = listOf(
            OnboardingEvent.Started to "onboarding_started",
            OnboardingEvent.StepShown(any) to "onboarding_step_shown",
            OnboardingEvent.StepContinued(any) to "onboarding_step_continued",
            OnboardingEvent.StepSkipped(any) to "onboarding_step_skipped",
            OnboardingEvent.StepBack(any) to "onboarding_step_back",
            OnboardingEvent.Completed to "onboarding_completed",
        )

        for ((kind, name) in names) {
            assertEquals(name, OnboardingAnalyticsEvent(kind = kind, flowId = null).name)
        }
    }

    @Test
    fun stepEventHasFlowAndStepParameters() {
        val parameters = OnboardingAnalyticsEvent(OnboardingEvent.StepShown(step), flowId = "variant_b").parameters

        assertEquals(
            mapOf<String, Any>("flow_id" to "variant_b", "step_id" to "name", "step_index" to 1, "step_count" to 6),
            parameters,
        )
    }

    @Test
    fun flowEventHasNoStepParameters() {
        val parameters = OnboardingAnalyticsEvent(OnboardingEvent.Completed, flowId = "variant_b").parameters

        assertEquals(setOf("flow_id"), parameters.keys)
    }

    @Test
    fun eventWithoutFlowIdHasNoFlowParameter() {
        val parameters = OnboardingAnalyticsEvent(OnboardingEvent.StepBack(step), flowId = null).parameters

        assertNull(parameters["flow_id"])
        assertEquals(3, parameters.size)
    }
}
