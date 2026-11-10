package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.step.StepConfiguration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The two chrome placements: in `PerStep` the bar slides with its step, in
 * `Pinned` one bar stays and only the content slides.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingChromePlacementTest {
    @get:Rule
    val compose = createComposeRule()

    private val analytics = RecordingAnalytics()
    private var completions = 0

    private fun show(flow: OnboardingFlow, placement: OnboardingChromePlacement) {
        compose.setContent {
            MaterialTheme {
                OnboardingView(
                    provider = StubProvider(flow),
                    onComplete = { completions++ },
                    analytics = analytics,
                    chromePlacement = placement,
                )
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }
        compose.waitForIdle()
    }

    /** Taps Continue and stops in the middle of the transition to the next step. */
    private fun continueAndStopMidTransition() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Continue").performClick()
        compose.mainClock.advanceTimeBy(100)
    }

    private fun finishTransition() {
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private fun count(tag: String): Int = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    // During a transition

    @Test
    fun perStepHasABarForEachStepDuringTheTransition() {
        show(textFlow("a", "b"), OnboardingChromePlacement.PerStep)

        continueAndStopMidTransition()

        assertEquals(2, count(OnboardingTestTags.ProgressBar))
        compose.onNodeWithText("step:a").assertExists()
        compose.onNodeWithText("step:b").assertExists()
    }

    @Test
    fun pinnedKeepsOneBarDuringTheTransition() {
        show(textFlow("a", "b"), OnboardingChromePlacement.Pinned)

        continueAndStopMidTransition()

        assertEquals(1, count(OnboardingTestTags.ProgressBar))
        compose.onNodeWithText("step:a").assertExists()
        compose.onNodeWithText("step:b").assertExists()
    }

    // After a transition

    @Test
    fun perStepShowsOnlyTheNewStepAfterTheTransition() {
        show(textFlow("a", "b"), OnboardingChromePlacement.PerStep)

        continueAndStopMidTransition()
        finishTransition()

        compose.onNodeWithText("step:a").assertDoesNotExist()
        compose.onNodeWithText("step:b").assertExists()
        assertEquals(1, count(OnboardingTestTags.ProgressBar))
    }

    @Test
    fun pinnedShowsOnlyTheNewStepAfterTheTransition() {
        show(textFlow("a", "b"), OnboardingChromePlacement.Pinned)

        continueAndStopMidTransition()
        finishTransition()

        compose.onNodeWithText("step:a").assertDoesNotExist()
        compose.onNodeWithText("step:b").assertExists()
        assertEquals(1, compose.onAllNodesWithText("Continue").fetchSemanticsNodes().size)
    }

    // The pinned bar follows the current step

    @Test
    fun pinnedBarShowsWhatTheCurrentStepAllows() {
        val skippable = StepConfiguration(isSkipButtonShown = true)
        val bare = StepConfiguration(isBackButtonShown = false, isProgressBarShown = false)
        show(
            OnboardingFlow(steps = listOf(TextStep("a", skippable), TextStep("b"), TextStep("c", bare))),
            OnboardingChromePlacement.Pinned,
        )
        compose.onNodeWithText("Skip").assertExists()
        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertDoesNotExist()

        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Skip").assertDoesNotExist()
        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertExists()
        compose.onNodeWithTag(OnboardingTestTags.ProgressBar).assertExists()

        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertDoesNotExist()
        compose.onNodeWithTag(OnboardingTestTags.ProgressBar).assertDoesNotExist()
    }

    /** The whole flow works the same with a pinned bar. */
    @Test
    fun pinnedFlowRunsToCompletion() {
        show(textFlow("a", "b"), OnboardingChromePlacement.Pinned)

        repeat(2) {
            compose.onNodeWithText("Continue").performClick()
            compose.waitForIdle()
        }
        compose.waitUntil { completions > 0 }

        assertEquals(1, completions)
        assertEquals(OnboardingEvent.Completed, analytics.kinds.last())
    }

    // Taps during a transition

    /**
     * Taps Continue on everything on screen in the middle of a transition.
     * They are ignored, as in a navigation push: a second tap must not pass a
     * step the user has not seen. After the transition taps work again.
     */
    private fun assertTapsDuringTheTransitionAreIgnored(placement: OnboardingChromePlacement) {
        show(textFlow("a", "b", "c"), placement)
        continueAndStopMidTransition()

        val buttons = compose.onAllNodesWithText("Continue")
        repeat(buttons.fetchSemanticsNodes().size) { buttons[it].performClick() }
        finishTransition()

        compose.onNodeWithText("step:b").assertExists()
        assertEquals(1, analytics.kinds.count { it is OnboardingEvent.StepContinued })

        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("step:c").assertExists()
    }

    @Test
    fun perStepIgnoresTapsDuringTheTransition() {
        assertTapsDuringTheTransitionAreIgnored(OnboardingChromePlacement.PerStep)
    }

    @Test
    fun pinnedIgnoresTapsDuringTheTransition() {
        assertTapsDuringTheTransitionAreIgnored(OnboardingChromePlacement.Pinned)
    }

    // Set above the onboarding

    /** The placement can be set once, above the onboarding, without the parameter. */
    @Test
    fun placementProvidedAboveTheOnboardingIsUsed() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalOnboardingChromePlacement provides OnboardingChromePlacement.Pinned) {
                    OnboardingView(provider = StubProvider(textFlow("a", "b")), onComplete = {}, analytics = analytics)
                }
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }
        compose.waitForIdle()

        continueAndStopMidTransition()

        assertEquals(1, count(OnboardingTestTags.ProgressBar))
        finishTransition()
    }
}
