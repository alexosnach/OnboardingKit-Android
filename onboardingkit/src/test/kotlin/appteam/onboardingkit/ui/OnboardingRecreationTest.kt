package appteam.onboardingkit.ui

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.flow.OnboardingFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/** What the hosted onboarding is given and what it did, as the test sees it from outside the activity. */
private object Recreation {
    lateinit var provider: StubProvider
    val analytics = RecordingAnalytics()
    var completions = 0

    fun reset(flow: OnboardingFlow, isOpen: Boolean) {
        provider = StubProvider(flow, isOpen)
        analytics.events.clear()
        completions = 0
    }
}

/**
 * A loading screen that does not move. The default one spins forever, and the
 * test waits for the screen to come to rest, which a running animation never allows.
 */
private object StillLoadingStyle : OnboardingLoadingStyle {
    @Composable
    override fun Body(configuration: OnboardingLoadingConfiguration) {}
}

/** Shows the onboarding as an app does: from `onCreate`, again in every new instance. */
class RecreationHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OnboardingView(
                    provider = Recreation.provider,
                    onComplete = { Recreation.completions++ },
                    analytics = Recreation.analytics,
                    loadingStyle = StillLoadingStyle,
                )
            }
        }
    }
}

/**
 * Rotation, folding and a new window size recreate the activity. The
 * onboarding stays where it was: the same flow, the same step, no second
 * call of the provider and no second `Started`.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingRecreationTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<RecreationHostActivity>

    private fun launch(isProviderOpen: Boolean = true) {
        Recreation.reset(textFlow("a", "b", "c"), isOpen = isProviderOpen)

        // The host activity exists only in the tests, so it is not in the manifest.
        val application = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(application.packageManager)
            .addActivityIfNotPresent(ComponentName(application, RecreationHostActivity::class.java))

        scenario = ActivityScenario.launch(RecreationHostActivity::class.java)
        compose.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    private fun recreate() {
        scenario.recreate()
        compose.waitForIdle()
    }

    private fun continueToNextStep() {
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()
    }

    @Test
    fun currentStepIsKept() {
        launch()
        continueToNextStep()
        compose.onNodeWithText("step:b").assertExists()

        recreate()

        compose.onNodeWithText("step:b").assertExists()
        compose.onNodeWithText("step:a").assertDoesNotExist()
    }

    @Test
    fun providerIsNotAskedAgain() {
        launch()

        recreate()
        recreate()

        assertEquals(1, Recreation.provider.calls)
    }

    @Test
    fun startedIsNotReportedAgain() {
        launch()
        continueToNextStep()

        recreate()

        assertEquals(1, Recreation.analytics.kinds.count { it == OnboardingEvent.Started })
        // Started, shown a, continued a, shown b: the recreation added nothing.
        assertEquals(4, Recreation.analytics.kinds.size)
    }

    /** The onboarding goes on after the recreation as if nothing happened. */
    @Test
    fun flowContinuesAfterRecreation() {
        launch()
        continueToNextStep()
        recreate()

        continueToNextStep()
        continueToNextStep()
        compose.waitUntil { Recreation.completions > 0 }

        assertEquals(1, Recreation.completions)
        assertEquals(OnboardingEvent.Completed, Recreation.analytics.kinds.last())
    }

    /** Recreated while the provider is still working: the same call is awaited, not a new one. */
    @Test
    fun loadingSurvivesRecreation() {
        launch(isProviderOpen = false)

        recreate()
        Recreation.provider.open()
        // Looking for the step lets the screen draw again; a plain condition would not.
        compose.waitUntil { compose.onNodeWithText("step:a").isDisplayed() }

        assertEquals(1, Recreation.provider.calls)
        assertEquals(1, Recreation.analytics.kinds.count { it == OnboardingEvent.Started })
    }

    /** `onComplete` was called before the recreation: it is not called again after it. */
    @Test
    fun completionIsNotReportedAgain() {
        launch()
        repeat(3) { continueToNextStep() }
        compose.waitUntil { Recreation.completions > 0 }

        recreate()

        assertEquals(1, Recreation.completions)
    }
}
