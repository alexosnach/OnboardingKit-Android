package appteam.onboardingkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.flow.LocalOnboardingNext
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.step.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val RootTag = "root"

/** A step with a background, and content that fills the room it is given. */
private class BackdropStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {
        val next = LocalOnboardingNext.current
        Box(Modifier.fillMaxSize().testTag("content:$id")) {
            Button(onClick = { next() }) { Text("Continue $id") }
        }
    }

    @Composable
    override fun Background() {
        Box(Modifier.fillMaxSize().testTag("background:$id"))
    }
}

/** The step is one screen with its bar: the background behind the bar, the content below it. */
@RunWith(AndroidJUnit4::class)
class OnboardingStepBackgroundTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(placement: OnboardingChromePlacement, steps: List<OnboardingStep>) {
        val analytics = RecordingAnalytics()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(width = 360.dp, height = 640.dp).testTag(RootTag)) {
                    OnboardingView(
                        provider = StubProvider(OnboardingFlow(steps = steps)),
                        onComplete = {},
                        analytics = analytics,
                        chromePlacement = placement,
                    )
                }
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun assertBackgroundBehindTheBarAndContentBelowIt(placement: OnboardingChromePlacement) {
        show(placement, listOf(BackdropStep("a"), BackdropStep("b")))

        val root = bounds(RootTag)
        val progress = bounds(OnboardingTestTags.ProgressBar)
        val barBottom = root.top + with(compose.density) { OnboardingTopBarDefaults.height.toPx() }

        assertEquals(root, bounds("background:a"))
        assertEquals(barBottom, bounds("content:a").top, 1f)
        assertEquals(root.bottom, bounds("content:a").bottom, 1f)
        assertTrue(progress.bottom <= bounds("content:a").top)

        // The next step is laid out the same way once it has slid in.
        compose.onNodeWithText("Continue a").performClick()
        compose.waitForIdle()

        assertEquals(root, bounds("background:b"))
        assertEquals(barBottom, bounds("content:b").top, 1f)
        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertExists()
    }

    @Test
    fun pinnedDrawsTheBackgroundBehindTheBarAndTheContentBelowIt() {
        assertBackgroundBehindTheBarAndContentBelowIt(OnboardingChromePlacement.Pinned)
    }

    @Test
    fun perStepDrawsTheBackgroundBehindTheBarAndTheContentBelowIt() {
        assertBackgroundBehindTheBarAndContentBelowIt(OnboardingChromePlacement.PerStep)
    }

    /** The bar is drawn over the background, so its buttons still take taps. */
    @Test
    fun backButtonOverTheBackgroundTakesTaps() {
        show(OnboardingChromePlacement.Pinned, listOf(BackdropStep("a"), BackdropStep("b")))
        compose.onNodeWithText("Continue a").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag(OnboardingTestTags.BackButton).performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Continue a").assertExists()
    }

    /** A step that does not override `Background` is laid out the same way. */
    @Test
    fun stepWithoutBackgroundKeepsItsContentBelowTheBar() {
        show(OnboardingChromePlacement.Pinned, listOf(TextStep("a")))

        val progress = bounds(OnboardingTestTags.ProgressBar)
        val title = compose.onNodeWithText("step:a").fetchSemanticsNode().boundsInRoot

        assertTrue(progress.bottom <= title.top)
    }
}
