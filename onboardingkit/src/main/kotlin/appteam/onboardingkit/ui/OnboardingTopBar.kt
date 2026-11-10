package appteam.onboardingkit.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The onboarding bar: the back button on the start side, the progress bar in
 * the middle and Skip on the end side.
 *
 * It only lays the three parts out; what each of them looks like is given by
 * the caller, and which of them are shown by [visibility].
 *
 * The bar keeps clear of the status bar and of display cutouts at the top and
 * on the sides, on any device and in any orientation. It keeps its height on a
 * step that shows nothing, so the content below does not jump between steps.
 *
 * The buttons are added and removed, never left in the bar with empty content:
 * the progress bar takes the room of a button that is not there. When a bar
 * that stays on screen gets other buttons (`Pinned` chrome), they fade in and
 * out and the progress bar moves smoothly to its new width. A bar that comes
 * on screen with its step (`PerStep` chrome) is laid out at once.
 */
@Composable
internal fun OnboardingTopBar(
    visibility: ChromeVisibility,
    backButton: @Composable () -> Unit,
    progressBar: @Composable () -> Unit,
    skipButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(OnboardingTopBarDefaults.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = visibility.isBackButtonVisible,
            enter = ButtonEnter,
            exit = ButtonExit,
        ) {
            Box(
                Modifier
                    .padding(start = OnboardingTopBarDefaults.buttonMargin)
                    .testTag(OnboardingTestTags.BackButton),
            ) {
                backButton()
            }
        }

        val startPadding by animateDpAsState(
            targetValue = ProgressBarLayout.padding(hasNeighbor = visibility.isBackButtonVisible),
            label = "OnboardingProgressStart",
        )
        val endPadding by animateDpAsState(
            targetValue = ProgressBarLayout.padding(hasNeighbor = visibility.isSkipButtonVisible),
            label = "OnboardingProgressEnd",
        )
        // Always in the row, also without the progress bar: it keeps Skip on the end side.
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = startPadding, end = endPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (visibility.isProgressBarVisible) {
                Box(Modifier.testTag(OnboardingTestTags.ProgressBar)) {
                    progressBar()
                }
            }
        }

        AnimatedVisibility(
            visible = visibility.isSkipButtonVisible,
            enter = ButtonEnter,
            exit = ButtonExit,
        ) {
            Box(Modifier.padding(end = OnboardingTopBarDefaults.buttonMargin)) {
                skipButton()
            }
        }
    }
}

/** A button comes into the bar: it takes its room and fades in, and the progress bar gives way. */
private val ButtonEnter = fadeIn() + expandHorizontally()

/** A button leaves the bar: it fades out and gives its room to the progress bar. */
private val ButtonExit = fadeOut() + shrinkHorizontally()

/** The sizes of the onboarding bar. */
internal object OnboardingTopBarDefaults {
    /** The bar's height below the status bar: the height of a standard Material 3 top bar. */
    val height = 64.dp

    /** The gap between a button and the bar's edge, as for the buttons of a standard top bar. */
    val buttonMargin = 4.dp
}

/** The tags UI tests find the onboarding chrome by: the same strings as on iOS. */
internal object OnboardingTestTags {
    const val BackButton = "onboarding.backButton"
    const val ProgressBar = "onboarding.progressBar"
}
