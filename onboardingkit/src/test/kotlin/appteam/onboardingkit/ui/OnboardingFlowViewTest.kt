package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.analytics.OnboardingStepInfo
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.step.LocalOnboardingFlowId
import appteam.onboardingkit.step.LocalOnboardingStepInfo
import appteam.onboardingkit.step.OnboardingStep
import appteam.onboardingkit.step.StepConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A step that shows the place in the flow its content was given. */
private class PlaceStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {
        val step = LocalOnboardingStepInfo.current
        Text("place:${step?.id}:${step?.index}:${step?.total}:${LocalOnboardingFlowId.current}")
    }
}

/** The onboarding as the user sees it: the steps, the bar and its buttons. */
@RunWith(AndroidJUnit4::class)
class OnboardingFlowViewTest {
    @get:Rule
    val compose = createComposeRule()

    private val analytics = RecordingAnalytics()
    private var completions = 0

    private fun show(flow: OnboardingFlow) {
        compose.setContent {
            MaterialTheme {
                OnboardingView(
                    provider = StubProvider(flow),
                    onComplete = { completions++ },
                    analytics = analytics,
                )
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }
    }

    private fun continueToNextStep() {
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
    }

    // The steps

    @Test
    fun continueMovesThroughTheStepsAndCompletes() {
        show(textFlow("welcome", "name"))
        compose.onNodeWithText("step:welcome").assertExists()

        continueToNextStep()
        compose.onNodeWithText("step:name").assertExists()
        compose.onNodeWithText("step:welcome").assertDoesNotExist()

        continueToNextStep()
        compose.waitUntil { completions > 0 }

        assertEquals(1, completions)
        assertEquals(OnboardingEvent.Completed, analytics.kinds.last())
    }

    /** The content can tie its own analytics to the funnel. */
    @Test
    fun contentGetsItsPlaceInTheFlow() {
        show(OnboardingFlow(steps = listOf(TextStep("welcome"), PlaceStep("name")), flowId = "main_v1"))

        continueToNextStep()

        compose.onNodeWithText("place:name:1:2:main_v1").assertExists()
    }

    // The bar

    @Test
    fun firstStepShowsTheProgressBarWithoutBackAndSkip() {
        show(textFlow("welcome", "name"))

        compose.onNodeWithTag(OnboardingTestTags.ProgressBar).assertExists()
        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertDoesNotExist()
        compose.onNodeWithText("Skip").assertDoesNotExist()
    }

    @Test
    fun laterStepShowsTheBackButton() {
        show(textFlow("welcome", "name"))

        continueToNextStep()

        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertExists()
    }

    @Test
    fun stepCanHideTheBackButtonAndTheProgressBar() {
        val hidden = StepConfiguration(isBackButtonShown = false, isProgressBarShown = false)
        show(OnboardingFlow(steps = listOf(TextStep("welcome"), TextStep("paywall", hidden))))

        continueToNextStep()

        compose.onNodeWithText("step:paywall").assertExists()
        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertDoesNotExist()
        compose.onNodeWithTag(OnboardingTestTags.ProgressBar).assertDoesNotExist()
    }

    @Test
    fun backButtonReturnsToThePreviousStep() {
        show(textFlow("welcome", "name"))
        continueToNextStep()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("step:welcome").assertExists()
        assertEquals(
            OnboardingEvent.StepShown(OnboardingStepInfo(id = "welcome", index = 0, total = 2)),
            analytics.kinds.last(),
        )
    }

    @Test
    fun skipButtonSkipsTheStepThatAllowsIt() {
        val skippable = StepConfiguration(isSkipButtonShown = true)
        show(OnboardingFlow(steps = listOf(TextStep("welcome", skippable), TextStep("name"))))

        compose.onNodeWithText("Skip").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("step:name").assertExists()
        compose.onNodeWithText("Skip").assertDoesNotExist()
        assertEquals(
            OnboardingEvent.StepSkipped(OnboardingStepInfo(id = "welcome", index = 0, total = 2)),
            analytics.kinds[2],
        )
    }

    // Defaults

    @Test
    fun chromeIsPerStepByDefault() {
        lateinit var placement: OnboardingChromePlacement
        compose.setContent { placement = LocalOnboardingChromePlacement.current }
        compose.waitForIdle()

        assertSame(OnboardingChromePlacement.PerStep, placement)
    }
}
