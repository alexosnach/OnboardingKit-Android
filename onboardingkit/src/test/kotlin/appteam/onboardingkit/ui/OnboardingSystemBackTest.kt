package appteam.onboardingkit.ui

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.analytics.OnboardingStepInfo
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.step.StepConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The system back (button and gesture) follows the back button of the bar:
 * it returns one step where the button is shown, does nothing on a step that
 * hides it, and is left to the system on the first step.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingSystemBackTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val analytics = RecordingAnalytics()

    private fun show(flow: OnboardingFlow) {
        compose.setContent {
            MaterialTheme {
                OnboardingView(provider = StubProvider(flow), onComplete = {}, analytics = analytics)
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }
    }

    private fun continueToNextStep() {
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
    }

    private fun pressSystemBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    /** Whether the onboarding takes the system back, instead of leaving it to the system. */
    private val handlesSystemBack: Boolean
        get() = compose.runOnUiThread { compose.activity.onBackPressedDispatcher.hasEnabledCallbacks() }

    @Test
    fun systemBackReturnsToThePreviousStep() {
        show(textFlow("a", "b", "c"))
        continueToNextStep()
        continueToNextStep()

        pressSystemBack()

        compose.onNodeWithText("step:b").assertExists()
    }

    /** It is the same move as the back button: reported as `StepBack`, then `StepShown`. */
    @Test
    fun systemBackIsReportedLikeTheBackButton() {
        show(textFlow("a", "b"))
        continueToNextStep()
        analytics.events.clear()

        pressSystemBack()

        assertEquals(
            listOf(
                OnboardingEvent.StepBack(OnboardingStepInfo(id = "b", index = 1, total = 2)),
                OnboardingEvent.StepShown(OnboardingStepInfo(id = "a", index = 0, total = 2)),
            ),
            analytics.kinds,
        )
    }

    /** Each press goes back one step. */
    @Test
    fun repeatedSystemBackReachesTheFirstStep() {
        show(textFlow("a", "b", "c"))
        continueToNextStep()
        continueToNextStep()

        pressSystemBack()
        pressSystemBack()

        compose.onNodeWithText("step:a").assertExists()
    }

    /** On the first step the onboarding does not take the system back: the system handles it. */
    @Test
    fun firstStepLeavesSystemBackToTheSystem() {
        show(textFlow("a", "b"))

        assertFalse(handlesSystemBack)
    }

    @Test
    fun laterStepTakesSystemBack() {
        show(textFlow("a", "b"))
        continueToNextStep()

        assertTrue(handlesSystemBack)
    }

    /** Back to the first step: the system back is the system's again. */
    @Test
    fun systemBackIsLeftToTheSystemAgainAfterReturningToTheFirstStep() {
        show(textFlow("a", "b"))
        continueToNextStep()

        pressSystemBack()

        assertFalse(handlesSystemBack)
    }

    /** A step that hides the back button turns the system back off too. */
    @Test
    fun stepWithoutBackButtonIgnoresSystemBack() {
        val noBack = StepConfiguration(isBackButtonShown = false)
        show(OnboardingFlow(steps = listOf(TextStep("a"), TextStep("paywall", noBack))))
        continueToNextStep()
        analytics.events.clear()

        pressSystemBack()

        compose.onNodeWithText("step:paywall").assertExists()
        assertTrue(handlesSystemBack) // taken, so the app is not left either
        assertTrue(analytics.events.isEmpty())
    }

    // The back gesture (predictive back)

    private fun startBackGesture(progress: Float) {
        compose.runOnUiThread {
            val dispatcher = compose.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 0f, progress, BackEventCompat.EDGE_LEFT))
        }
        compose.waitForIdle()
    }

    private fun cancelBackGesture() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        compose.waitForIdle()
    }

    /** While the finger is down the previous step is seen under the current one, and nothing has moved yet. */
    @Test
    fun gestureShowsThePreviousStepWithoutMovingTheFlow() {
        show(textFlow("a", "b"))
        continueToNextStep()
        analytics.events.clear()

        startBackGesture(progress = 0.5f)

        compose.onNodeWithText("step:a").assertExists()
        compose.onNodeWithText("step:b").assertExists()
        assertTrue(analytics.events.isEmpty())
    }

    @Test
    fun finishedGestureReturnsToThePreviousStep() {
        show(textFlow("a", "b"))
        continueToNextStep()
        analytics.events.clear()
        startBackGesture(progress = 0.5f)

        pressSystemBack() // the finger is lifted: the system confirms the back

        compose.onNodeWithText("step:a").assertExists()
        compose.onNodeWithText("step:b").assertDoesNotExist()
        assertEquals(
            listOf(
                OnboardingEvent.StepBack(OnboardingStepInfo(id = "b", index = 1, total = 2)),
                OnboardingEvent.StepShown(OnboardingStepInfo(id = "a", index = 0, total = 2)),
            ),
            analytics.kinds,
        )
    }

    /** A cancelled gesture changes nothing: the step slides back, and nothing is reported. */
    @Test
    fun cancelledGestureChangesNothing() {
        show(textFlow("a", "b"))
        continueToNextStep()
        analytics.events.clear()
        startBackGesture(progress = 0.5f)

        cancelBackGesture()

        compose.onNodeWithText("step:b").assertExists()
        compose.onNodeWithText("step:a").assertDoesNotExist()
        assertTrue(analytics.events.isEmpty())
        assertTrue(handlesSystemBack) // still on the second step, ready for the next gesture
    }

    /** A step that hides the back button does not move with the gesture either. */
    @Test
    fun gestureOnStepWithoutBackButtonMovesNothing() {
        val noBack = StepConfiguration(isBackButtonShown = false)
        show(OnboardingFlow(steps = listOf(TextStep("a"), TextStep("paywall", noBack))))
        continueToNextStep()
        analytics.events.clear()

        startBackGesture(progress = 0.5f)

        compose.onNodeWithText("step:a").assertDoesNotExist()
        compose.onNodeWithText("step:paywall").assertExists()

        pressSystemBack()

        compose.onNodeWithText("step:paywall").assertExists()
        assertTrue(analytics.events.isEmpty())
    }

    // After completion

    /**
     * The app may keep the onboarding on screen after `onComplete`, for example
     * while it saves the result. Back then does nothing, like every other
     * action after completion, and does not leave the app.
     */
    @Test
    fun systemBackAfterCompletionIsTakenAndDoesNothing() {
        show(textFlow("a", "b"))
        continueToNextStep()
        continueToNextStep() // passes the last step; this test keeps the onboarding on screen
        analytics.events.clear()

        pressSystemBack()

        assertTrue(handlesSystemBack)
        compose.onNodeWithText("step:b").assertExists()
        assertTrue(analytics.events.isEmpty())
    }
}
