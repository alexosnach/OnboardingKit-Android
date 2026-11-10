package appteam.onboardingkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import appteam.onboardingkit.flow.LocalOnboardingNext
import appteam.onboardingkit.flow.LocalOnboardingSkip
import appteam.onboardingkit.flow.OnboardingCoordinator
import appteam.onboardingkit.flow.OnboardingNavigationAction
import appteam.onboardingkit.step.LocalOnboardingFlowId
import appteam.onboardingkit.step.LocalOnboardingStepInfo

/**
 * Shows a loaded flow: the steps with their bar.
 *
 * Shown by `OnboardingView` once the provider has answered; apps use
 * `OnboardingView`, not this composable.
 *
 * It contains no navigation logic: it renders the coordinator's state and
 * forwards user actions back to it. When it is first shown it calls
 * `coordinator.start()`: the first step becomes current and analytics gets `Started`.
 *
 * The bar (see [OnboardingTopBar]) shows, for each step, what its
 * `StepConfiguration` allows (see [ChromeVisibility]): the back button, never
 * on the first step; the progress bar; and Skip.
 *
 * Steps change with a transition that slides like a navigation push (see
 * [StepTransition]). What slides depends on [chromePlacement]:
 *
 * - `PerStep`: each step has its own bar, and the bar slides with its step.
 *   Each bar shows its own step's progress, also while two steps are on screen.
 * - `Pinned`: one bar stays in place and follows the current step; only the
 *   step's content slides below it.
 *
 * The system back is handled by [OnboardingBackHandler].
 *
 * The step's content gets all the room below the bar, up to the window's
 * edges. The top inset is taken by the bar; the bottom and side insets are
 * the content's own to handle, for example with `Modifier.safeDrawingPadding()`.
 *
 * `coordinator.steps` must not be empty: `OnboardingView` reports an empty
 * flow as completed and never shows this composable for it.
 */
@Composable
internal fun OnboardingFlowView(
    coordinator: OnboardingCoordinator,
    chromePlacement: OnboardingChromePlacement,
    progressBarStyle: ProgressBarStyle,
    skipButtonStyle: SkipButtonStyle,
    backButtonStyle: BackButtonStyle,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(coordinator) { coordinator.start() }

    val next = remember(coordinator) { OnboardingNavigationAction { coordinator.next() } }
    val skip = remember(coordinator) { OnboardingNavigationAction { coordinator.skip() } }

    val transitionState = rememberStepTransitionState(currentIndex = coordinator.currentIndex)
    OnboardingBackHandler(coordinator = coordinator, transitionState = transitionState)

    // While one step replaces another both are on screen. Neither takes touches
    // until the transition ends, as in a navigation push: a second tap on
    // Continue must not pass a step the user has not seen.
    val isInTransition = transitionState.currentState != coordinator.currentIndex ||
        transitionState.currentState != transitionState.targetState
    val rootModifier = modifier.ignoringTouches(isInTransition)

    // The bar for the step at the index, drawn with the styles the app has set.
    val bar: @Composable (Int) -> Unit = { index ->
        OnboardingTopBar(
            visibility = ChromeVisibility(configuration = coordinator.steps[index].configuration, index = index),
            backButton = {
                backButtonStyle.Body(BackButtonConfiguration(back = { coordinator.back() }))
            },
            progressBar = {
                progressBarStyle.Body(
                    ProgressBarConfiguration(currentIndex = index, total = coordinator.steps.size),
                )
            },
            skipButton = {
                skipButtonStyle.Body(SkipButtonConfiguration(skip = { coordinator.skip() }))
            },
        )
    }

    CompositionLocalProvider(
        LocalOnboardingNext provides next,
        LocalOnboardingSkip provides skip,
        LocalOnboardingFlowId provides coordinator.flowId,
    ) {
        when (chromePlacement) {
            OnboardingChromePlacement.PerStep ->
                StepTransition(state = transitionState, modifier = rootModifier.fillMaxSize()) { index ->
                    Column {
                        // The step's own index, not the current one: during a
                        // transition two steps are on screen, each with its own bar.
                        bar(index)
                        StepContent(coordinator = coordinator, index = index, modifier = Modifier.weight(1f))
                    }
                }

            OnboardingChromePlacement.Pinned ->
                Column(
                    rootModifier
                        .fillMaxSize()
                        // Under the bar, which has no background of its own.
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    bar(coordinator.currentIndex)
                    StepTransition(state = transitionState, modifier = Modifier.weight(1f)) { index ->
                        StepContent(coordinator = coordinator, index = index)
                    }
                }
        }
    }
}

/**
 * The content of the step at [index], with the step's place in the flow
 * available to it as `LocalOnboardingStepInfo`.
 */
@Composable
private fun StepContent(coordinator: OnboardingCoordinator, index: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            // The bar has taken the top inset; the content must not add it again.
            .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        CompositionLocalProvider(LocalOnboardingStepInfo provides coordinator.stepInfo(at = index)) {
            coordinator.steps[index].Content()
        }
    }
}

/** Takes every touch before the content gets it, while [isIgnoring]. */
private fun Modifier.ignoringTouches(isIgnoring: Boolean): Modifier =
    if (!isIgnoring) {
        this
    } else {
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
    }
