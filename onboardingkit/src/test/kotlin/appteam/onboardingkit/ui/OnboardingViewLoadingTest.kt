package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.analytics.OnboardingStepInfo
import appteam.onboardingkit.flow.LocalOnboardingNext
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.flow.OnboardingFlowProvider
import appteam.onboardingkit.step.OnboardingStep
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A loading style with a text the tests can find. */
private object TextLoadingStyle : OnboardingLoadingStyle {
    @Composable
    override fun Body(configuration: OnboardingLoadingConfiguration) {
        Text("Preparing")
    }
}

/** A step that moves on by itself as soon as it is shown. */
private class AutoContinueStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {
        val next = LocalOnboardingNext.current
        Text("auto")
        LaunchedEffect(Unit) { next() }
    }
}

@RunWith(AndroidJUnit4::class)
class OnboardingViewLoadingTest {
    @get:Rule
    val compose = createComposeRule()

    private val analytics = RecordingAnalytics()

    @Test
    fun showsLoadingStyleWhileProviderWaits() {
        val provider = StubProvider(textFlow("a"), isOpen = false)
        compose.setContent {
            MaterialTheme {
                OnboardingView(
                    provider = provider,
                    onComplete = {},
                    analytics = analytics,
                    loadingStyle = TextLoadingStyle,
                )
            }
        }

        compose.waitUntil { provider.calls == 1 }

        compose.onNodeWithText("Preparing").assertExists()
        assertTrue(analytics.events.isEmpty()) // no flow yet, nothing started
    }

    @Test
    fun showsTheFirstStepOnceTheProviderAnswers() {
        val provider = StubProvider(textFlow("a"), isOpen = false)
        compose.setContent {
            MaterialTheme {
                OnboardingView(provider = provider, onComplete = {}, loadingStyle = TextLoadingStyle)
            }
        }
        compose.waitUntil { provider.calls == 1 }

        provider.open()

        compose.waitUntil { compose.onNodeWithText("step:a").isDisplayed() }
        compose.onNodeWithText("Preparing").assertDoesNotExist()
    }

    @Test
    fun startsTheLoadedFlowWithItsIdAndAnalytics() {
        val provider = StubProvider(textFlow("a", "b", flowId = "variant_b"))
        compose.setContent {
            MaterialTheme { OnboardingView(provider = provider, onComplete = {}, analytics = analytics) }
        }

        compose.waitUntil { analytics.events.size >= 2 }

        assertEquals(
            listOf(
                OnboardingEvent.Started,
                OnboardingEvent.StepShown(OnboardingStepInfo(id = "a", index = 0, total = 2)),
            ),
            analytics.kinds,
        )
        assertTrue(analytics.events.all { it.flowId == "variant_b" })
    }

    @Test
    fun asksTheProviderOnlyOnce() {
        val provider = StubProvider(textFlow("a"))
        var redraws by mutableStateOf(0)
        compose.setContent {
            MaterialTheme {
                // A new lambda on every redraw, as an app's code would pass.
                OnboardingView(provider = provider, onComplete = { redraws.toString() }, analytics = analytics)
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }

        repeat(3) {
            redraws++
            compose.waitForIdle()
        }

        assertEquals(1, provider.calls)
    }

    @Test
    fun callsOnCompleteOnceWhenTheLastStepIsPassed() {
        val provider = StubProvider(OnboardingFlow(steps = listOf(AutoContinueStep("only"))))
        var completions = 0
        compose.setContent {
            MaterialTheme {
                OnboardingView(provider = provider, onComplete = { completions++ }, analytics = analytics)
            }
        }

        compose.waitUntil { completions > 0 }
        compose.waitForIdle()

        assertEquals(1, completions)
        assertEquals(OnboardingEvent.Completed, analytics.kinds.last())
    }

    /** The contract forbids it, but a broken provider must not crash the app. */
    @Test
    fun emptyFlowCompletesAtOnceWithoutShowingSteps() {
        val provider = StubProvider(OnboardingFlow(steps = emptyList()))
        var completions = 0
        compose.setContent {
            MaterialTheme {
                OnboardingView(provider = provider, onComplete = { completions++ }, analytics = analytics)
            }
        }

        compose.waitUntil { completions > 0 }
        compose.waitForIdle()

        assertEquals(1, completions)
        assertTrue(analytics.events.isEmpty())
    }

    /**
     * The user leaves before the provider answers (the app stopped showing the
     * onboarding): the late answer starts nothing and completes nothing.
     */
    @Test
    fun lateAnswerAfterTheOnboardingIsGoneIsDropped() {
        val provider = StubProvider(textFlow("a"), isOpen = false)
        var completions = 0
        var isShown by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (isShown) {
                    OnboardingView(provider = provider, onComplete = { completions++ }, analytics = analytics)
                }
            }
        }
        compose.waitUntil { provider.calls == 1 }

        isShown = false
        compose.waitForIdle()
        provider.open()
        compose.waitForIdle()

        assertTrue(analytics.events.isEmpty())
        assertEquals(0, completions)
    }

    /**
     * A provider whose own waiting is cancelled returns no flow (`withTimeout`
     * in place of `withTimeoutOrNull`). The contract forbids it, but the
     * loading screen must not stay forever: the onboarding completes at once.
     */
    @Test
    fun providerThatGivesUpCompletesAtOnceWithoutShowingSteps() {
        val provider = object : OnboardingFlowProvider {
            override suspend fun makeFlow(): OnboardingFlow = throw CancellationException("timed out")
        }
        var completions = 0
        compose.setContent {
            MaterialTheme {
                OnboardingView(provider = provider, onComplete = { completions++ }, analytics = analytics)
            }
        }

        compose.waitUntil { completions > 0 }
        compose.waitForIdle()

        assertEquals(1, completions)
        assertTrue(analytics.events.isEmpty())
    }
}
