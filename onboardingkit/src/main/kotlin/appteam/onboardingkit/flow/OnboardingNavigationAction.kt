package appteam.onboardingkit.flow

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * An action a step calls to move the onboarding flow.
 *
 * Read it from the composition and call it like a function:
 *
 * ```kotlin
 * val next = LocalOnboardingNext.current
 *
 * Button(onClick = { next() }) { Text("Continue") }
 * ```
 *
 * The step does not know who handles the action or where the flow goes next.
 */
public class OnboardingNavigationAction internal constructor(
    private val handler: () -> Unit,
) {
    public operator fun invoke() {
        handler()
    }
}

/**
 * Moves to the next step, or completes the onboarding on the last step.
 *
 * Provided by `OnboardingView`. Outside of it the action does nothing.
 */
public val LocalOnboardingNext: ProvidableCompositionLocal<OnboardingNavigationAction> =
    staticCompositionLocalOf { OnboardingNavigationAction {} }

/**
 * Skips the current step.
 *
 * Provided by `OnboardingView`. Outside of it the action does nothing.
 */
public val LocalOnboardingSkip: ProvidableCompositionLocal<OnboardingNavigationAction> =
    staticCompositionLocalOf { OnboardingNavigationAction {} }
