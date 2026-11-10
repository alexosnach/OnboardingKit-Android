package appteam.onboardingkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.step.StepConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val BarTag = "bar"
private const val SkipTag = "skip"

/** Where the parts of the onboarding bar are placed, on different screens and settings. */
@RunWith(AndroidJUnit4::class)
class OnboardingLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    /** Shows the bar alone, with buttons of a fixed size in place of the styles. */
    private fun showBar(
        visibility: ChromeVisibility,
        direction: LayoutDirection = LayoutDirection.Ltr,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                OnboardingTopBar(
                    visibility = visibility,
                    backButton = { Box(Modifier.size(48.dp)) },
                    progressBar = { Box(Modifier.fillMaxWidth().height(4.dp)) },
                    skipButton = { Box(Modifier.size(48.dp).testTag(SkipTag)) },
                    modifier = Modifier.testTag(BarTag),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun dp(pixels: Float): Dp = with(compose.density) { pixels.toDp() }

    private fun assertDp(expected: Dp, actualPixels: Float) {
        assertEquals(expected.value, dp(actualPixels).value, 0.5f)
    }

    private val everything = ChromeVisibility(
        isBackButtonVisible = true,
        isProgressBarVisible = true,
        isSkipButtonVisible = true,
    )

    private val progressOnly = ChromeVisibility(
        isBackButtonVisible = false,
        isProgressBarVisible = true,
        isSkipButtonVisible = false,
    )

    private val nothing = ChromeVisibility(
        isBackButtonVisible = false,
        isProgressBarVisible = false,
        isSkipButtonVisible = false,
    )

    // Left-to-right

    @Test
    fun backIsOnTheStartSideAndSkipOnTheEndSide() {
        showBar(everything)

        val back = bounds(OnboardingTestTags.BackButton)
        val progress = bounds(OnboardingTestTags.ProgressBar)
        val skip = bounds(SkipTag)

        assertTrue(back.right <= progress.left)
        assertTrue(progress.right <= skip.left)
    }

    /** The gap between the progress bar and a button is 32 dp, as on iOS. */
    @Test
    fun progressBarKeepsTheSpacingToBothButtons() {
        showBar(everything)

        val back = bounds(OnboardingTestTags.BackButton)
        val progress = bounds(OnboardingTestTags.ProgressBar)
        val skip = bounds(SkipTag)

        assertDp(32.dp, progress.left - back.right)
        assertDp(32.dp, skip.left - progress.right)
    }

    /** Without buttons the progress bar reaches the bar's margins. */
    @Test
    fun progressBarReachesTheMarginsWithoutButtons() {
        showBar(progressOnly)

        val bar = bounds(BarTag)
        val progress = bounds(OnboardingTestTags.ProgressBar)

        assertDp(16.dp, progress.left - bar.left)
        assertDp(16.dp, bar.right - progress.right)
    }

    /** The bar keeps its height on a step that shows nothing, so the content does not jump. */
    @Test
    fun barKeepsItsHeightWithNothingShown() {
        showBar(nothing)

        assertDp(64.dp, bounds(BarTag).height)
    }

    @Test
    fun barHasTheSameHeightWithEverythingShown() {
        showBar(everything)

        assertDp(64.dp, bounds(BarTag).height)
    }

    // Right-to-left

    @Test
    fun rightToLeftMirrorsTheBar() {
        showBar(everything, direction = LayoutDirection.Rtl)

        val back = bounds(OnboardingTestTags.BackButton)
        val progress = bounds(OnboardingTestTags.ProgressBar)
        val skip = bounds(SkipTag)

        assertTrue(skip.right <= progress.left)
        assertTrue(progress.right <= back.left)
        assertDp(32.dp, back.left - progress.right)
        assertDp(32.dp, progress.left - skip.right)
    }

    // Small screen, large text

    /** On a narrow screen with the largest text the progress bar still has room, next to back and Skip. */
    @Test
    fun narrowScreenWithLargeTextKeepsTheProgressBar() {
        val analytics = RecordingAnalytics()
        val skippable = StepConfiguration(isSkipButtonShown = true)
        val flow = OnboardingFlow(steps = listOf(TextStep("a"), TextStep("b", skippable)))
        compose.setContent {
            LargeText {
                MaterialTheme {
                    Box(Modifier.size(width = 320.dp, height = 480.dp)) {
                        OnboardingView(provider = StubProvider(flow), onComplete = {}, analytics = analytics)
                    }
                }
            }
        }
        compose.waitUntil { analytics.events.isNotEmpty() }
        compose.onNodeWithText("Continue").performClick()
        compose.waitForIdle()

        val progress = bounds(OnboardingTestTags.ProgressBar)

        compose.onNodeWithTag(OnboardingTestTags.BackButton).assertExists()
        compose.onNodeWithText("Skip").assertExists()
        assertTrue("progress bar width was ${dp(progress.width)}", dp(progress.width) >= 48.dp)
    }
}

/** Shows the content with text at 200%, the largest size of the system's setting. */
@Composable
private fun LargeText(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = 2f),
        content = content,
    )
}
