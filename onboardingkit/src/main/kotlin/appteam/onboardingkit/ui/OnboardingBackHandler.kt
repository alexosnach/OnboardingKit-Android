package appteam.onboardingkit.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import appteam.onboardingkit.flow.OnboardingCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Handles the system back: the button and the gesture.
 *
 * It follows the back button of the bar, by the same rule ([ChromeVisibility]):
 *
 * - Where the button is visible, back returns to the previous step.
 * - On a later step that hides the button, back is taken and does nothing:
 *   the step turned going back off.
 * - After completion, on a later step, back is taken and does nothing, like
 *   every other action after completion.
 * - On the first step back is not taken, so the system does what it does
 *   without the onboarding.
 *
 * During the back gesture (Android 14 and later) the previous step is shown
 * under the finger, by moving [transitionState] with the gesture. The
 * coordinator changes only when the gesture ends: a cancelled gesture changes
 * nothing, so it reports nothing to analytics and makes no lifecycle calls.
 */
@Composable
internal fun OnboardingBackHandler(
    coordinator: OnboardingCoordinator,
    transitionState: SeekableTransitionState<Int>,
) {
    val index = coordinator.currentIndex
    val returnsToPreviousStep = !coordinator.isCompleted &&
        ChromeVisibility(configuration = coordinator.steps[index].configuration, index = index).isBackButtonVisible
    val scope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = index > 0) { progress ->
        try {
            progress.collect { event ->
                if (returnsToPreviousStep) transitionState.seekTo(event.progress, targetState = index - 1)
            }
            // The gesture ended, or the back button was pressed: the transition
            // goes on from where the gesture left it.
            if (returnsToPreviousStep) coordinator.back()
        } catch (cancellation: CancellationException) {
            // The gesture was cancelled: the step slides back into place.
            if (returnsToPreviousStep) scope.launch { transitionState.animateTo(index) }
            throw cancellation
        }
    }
}
