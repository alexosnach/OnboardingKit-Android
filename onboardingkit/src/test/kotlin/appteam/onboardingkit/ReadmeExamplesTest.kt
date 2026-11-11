package appteam.onboardingkit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.analytics.NoOpOnboardingAnalytics
import appteam.onboardingkit.analytics.OnboardingAnalytics
import appteam.onboardingkit.analytics.OnboardingAnalyticsEvent
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.analytics.OnboardingStepInfo
import appteam.onboardingkit.flow.LocalOnboardingNext
import appteam.onboardingkit.flow.LocalOnboardingSkip
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.flow.OnboardingFlowProvider
import appteam.onboardingkit.step.LocalOnboardingFlowId
import appteam.onboardingkit.step.LocalOnboardingStepInfo
import appteam.onboardingkit.step.OnboardingStep
import appteam.onboardingkit.step.OnboardingStepLifecycle
import appteam.onboardingkit.step.StepConfiguration
import appteam.onboardingkit.ui.BackButtonConfiguration
import appteam.onboardingkit.ui.BackButtonStyle
import appteam.onboardingkit.ui.LocalProgressBarStyle
import appteam.onboardingkit.ui.OnboardingChromePlacement
import appteam.onboardingkit.ui.OnboardingLoadingConfiguration
import appteam.onboardingkit.ui.OnboardingLoadingStyle
import appteam.onboardingkit.ui.OnboardingView
import appteam.onboardingkit.ui.ProgressBarConfiguration
import appteam.onboardingkit.ui.ProgressBarDefaults
import appteam.onboardingkit.ui.ProgressBarStyle
import appteam.onboardingkit.ui.Segmented
import appteam.onboardingkit.ui.SkipButtonConfiguration
import appteam.onboardingkit.ui.SkipButtonStyle
import appteam.onboardingkit.ui.line
import appteam.onboardingkit.ui.themedColor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The examples of README.md, as an app would write them: only the public API,
// from outside the library's packages. If one stops compiling, the README is wrong.

private class WelcomeStep : OnboardingStep {
    override val id = "welcome"

    @Composable
    override fun Background() {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF7E57C2), Color(0xFF26C6DA)))))
    }

    @Composable
    override fun Content() {
        val next = LocalOnboardingNext.current

        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Welcome!", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = { next() }) { Text("Continue") }
        }
    }
}

private class NameStep : OnboardingStep, OnboardingStepLifecycle {
    override val id = "name"
    override val configuration = StepConfiguration(isSkipButtonShown = true)

    @Composable
    override fun Content() {
        val skip = LocalOnboardingSkip.current
        val step = LocalOnboardingStepInfo.current
        val flowId = LocalOnboardingFlowId.current

        Column {
            Text("Name: ${step?.id} in $flowId")
            Button(onClick = { skip() }) { Text("Prefer not to say") }
        }
    }

    override fun didBecomeCurrent() {}

    override fun didResignCurrent() {}
}

private class AppFlowProvider : OnboardingFlowProvider {
    override suspend fun makeFlow() = OnboardingFlow(
        steps = listOf(WelcomeStep(), NameStep()),
        flowId = "main_v1",
    )
}

private object DotsProgressBarStyle : ProgressBarStyle {
    @Composable
    override fun Body(configuration: ProgressBarConfiguration) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(configuration.total) { index ->
                val color = if (index < configuration.completedCount) {
                    ProgressBarDefaults.filledColor
                } else {
                    ProgressBarDefaults.emptyColor
                }
                Box(Modifier.size(8.dp).background(color, CircleShape))
            }
        }
    }
}

private object TextSkipButtonStyle : SkipButtonStyle {
    @Composable
    override fun Body(configuration: SkipButtonConfiguration) {
        TextButton(onClick = configuration.skip) { Text("Not now") }
    }
}

private object TextBackButtonStyle : BackButtonStyle {
    @Composable
    override fun Body(configuration: BackButtonConfiguration) {
        TextButton(onClick = configuration.back) { Text("Back") }
    }
}

private object BrandLoadingStyle : OnboardingLoadingStyle {
    @Composable
    override fun Body(configuration: OnboardingLoadingConfiguration) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brand")
        }
    }
}

private class RecordingAppAnalytics : OnboardingAnalytics {
    val names = mutableListOf<String>()

    override fun track(event: OnboardingAnalyticsEvent) {
        names += event.name
        event.parameters
        event.kind
    }
}

@RunWith(AndroidJUnit4::class)
class ReadmeExamplesTest {
    @get:Rule
    val compose = createComposeRule()

    /** The quick start with every customization of the README, from the first step to `onComplete`. */
    @Test
    fun quickStartAndCustomizationRun() {
        val analytics = RecordingAppAnalytics()
        var completions = 0
        val purple = Color(0xFF9C27B0)
        compose.setContent {
            MaterialTheme {
                OnboardingView(
                    provider = AppFlowProvider(),
                    onComplete = { completions++ },
                    analytics = analytics,
                    chromePlacement = OnboardingChromePlacement.Pinned,
                    progressBarStyle = DotsProgressBarStyle,
                    skipButtonStyle = TextSkipButtonStyle,
                    backButtonStyle = TextBackButtonStyle,
                    loadingStyle = BrandLoadingStyle,
                )
                // The built-in styles and the colors of the README; they only need to build.
                ProgressBarStyle.line(
                    filledColor = themedColor(light = purple, dark = Color.Cyan),
                    emptyColor = purple.copy(alpha = 0.2f),
                )
            }
        }
        compose.waitUntil { analytics.names.isNotEmpty() }

        compose.onNodeWithText("Welcome!").assertExists()
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Name: name in main_v1").assertExists()
        compose.onNodeWithText("Back").assertExists()
        compose.onNodeWithText("Not now").performClick()
        compose.waitUntil { completions > 0 }

        assertEquals(1, completions)
        assertEquals(
            listOf(
                "onboarding_started",
                "onboarding_step_shown",
                "onboarding_step_continued",
                "onboarding_step_shown",
                "onboarding_step_skipped",
                "onboarding_completed",
            ),
            analytics.names,
        )
    }

    /** A style set once, above the onboarding, reaches it without a parameter. */
    @Test
    fun styleCanBeProvidedAboveTheOnboarding() {
        lateinit var style: ProgressBarStyle
        compose.setContent {
            CompositionLocalProvider(LocalProgressBarStyle provides ProgressBarStyle.Segmented) {
                style = LocalProgressBarStyle.current
            }
        }
        compose.waitForIdle()

        assertEquals(ProgressBarStyle.Segmented, style)
    }

    @Test
    fun providerReturnsTheStepsInOrder() = runTest {
        val flow = AppFlowProvider().makeFlow()

        assertEquals(listOf("welcome", "name"), flow.steps.map { it.id })
        assertEquals("main_v1", flow.flowId)
    }

    @Test
    fun publicConstructorsAreAvailable() {
        val configuration = ProgressBarConfiguration(currentIndex = 1, total = 4)

        assertEquals(2, configuration.completedCount) // step 2 of 4 fills two segments
        assertEquals(2, OnboardingStepInfo(id = "name", index = 1, total = 2).total)
        assertFalse(StepConfiguration().isSkipButtonShown)
        NoOpOnboardingAnalytics.track(OnboardingAnalyticsEvent(kind = OnboardingEvent.Started, flowId = null))
    }
}
