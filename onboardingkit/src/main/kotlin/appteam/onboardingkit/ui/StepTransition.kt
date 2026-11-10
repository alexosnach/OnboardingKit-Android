package appteam.onboardingkit.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/**
 * Remembers which step is on screen and animates to [currentIndex] whenever it changes.
 *
 * The state can also be moved by hand, between two steps: the back gesture
 * uses it to show the previous step under the finger (see [OnboardingBackHandler]).
 */
@Composable
internal fun rememberStepTransitionState(currentIndex: Int): SeekableTransitionState<Int> {
    val state = remember { SeekableTransitionState(currentIndex) }
    LaunchedEffect(currentIndex) { state.animateTo(currentIndex) }
    return state
}

/**
 * Shows the step [state] is on, and slides between steps like a navigation push.
 *
 * What a step looks like is given by [content]: the whole screen with its bar
 * in `PerStep`, only the step's content in `Pinned`. While a transition runs,
 * [content] is on screen for both steps, each with its own index.
 *
 * Every step is opaque, with the theme's background: the step on top covers
 * the one below it instead of showing it through.
 */
@Composable
internal fun StepTransition(
    state: SeekableTransitionState<Int>,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int) -> Unit,
) {
    val transition = rememberTransition(state, label = "OnboardingStep")
    transition.AnimatedContent(
        modifier = modifier,
        transitionSpec = { pushTransform() },
    ) { index ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            content(index)
        }
    }
}

/**
 * Slides like a navigation push.
 *
 * The step on top moves the full width; the step below moves
 * [CoveredFraction] of it. Forward: the new step comes over from the end edge.
 * Back: the current step leaves over the earlier one towards the end edge.
 * Mirrors in right-to-left layouts.
 */
private fun AnimatedContentTransitionScope<Int>.pushTransform(): ContentTransform {
    val isForward = targetState > initialState
    val transform = if (isForward) {
        slideIntoContainer(SlideDirection.Start, PushSpec) togetherWith
            slideOutOfContainer(SlideDirection.Start, PushSpec) { covered(it) }
    } else {
        slideIntoContainer(SlideDirection.End, PushSpec) { covered(it) } togetherWith
            slideOutOfContainer(SlideDirection.End, PushSpec)
    }
    // The later step is always on top: it slides over on forward,
    // and slides away over the earlier one on back.
    transform.targetContentZIndex = targetState.toFloat()
    return transform
}

/** How far the covered step moves, for a step of the given width. */
private fun covered(width: Int): Int = (width * CoveredFraction).toInt()

/** The share of its width the covered step moves: an approximation of a navigation push. */
private const val CoveredFraction = 0.3f

/** Smooth, without a bounce. */
private val PushSpec: FiniteAnimationSpec<IntOffset> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)
