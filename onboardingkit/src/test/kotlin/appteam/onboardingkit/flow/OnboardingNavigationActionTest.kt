package appteam.onboardingkit.flow

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.step.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class ActionStubStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {}
}

@RunWith(AndroidJUnit4::class)
class OnboardingNavigationActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun callingActionRunsHandler() {
        var calls = 0
        val action = OnboardingNavigationAction { calls++ }

        action()
        action()

        assertEquals(2, calls)
    }

    /** Outside `OnboardingView` the actions are harmless no-ops. */
    @Test
    fun defaultActionsDoNotCrash() {
        lateinit var next: OnboardingNavigationAction
        lateinit var skip: OnboardingNavigationAction
        compose.setContent {
            next = LocalOnboardingNext.current
            skip = LocalOnboardingSkip.current
        }
        compose.waitForIdle()

        next()
        skip()
    }

    @Test
    fun actionsDriveCoordinatorLikeOnboardingViewDoes() {
        val coordinator = OnboardingCoordinator(steps = List(3) { ActionStubStep("step_$it") })
        val next = OnboardingNavigationAction { coordinator.next() }
        val skip = OnboardingNavigationAction { coordinator.skip() }

        next()
        skip()

        assertEquals(2, coordinator.currentIndex)
    }
}
