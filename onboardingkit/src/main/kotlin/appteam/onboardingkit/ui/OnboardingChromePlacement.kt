package appteam.onboardingkit.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Where the onboarding chrome — back button, progress bar and Skip — is drawn.
 *
 * A closed set defined by the onboarding, so it is an `enum`.
 */
public enum class OnboardingChromePlacement {
    /**
     * Each step has its own bar. The default.
     *
     * The chrome slides in and out together with the step, like a screen
     * pushed onto a navigation stack.
     */
    PerStep,

    /**
     * One bar for the whole onboarding that never moves.
     *
     * Only the step content slides during transitions; the back button,
     * progress bar and Skip stay in place and appear or disappear according to
     * each step's `StepConfiguration`.
     */
    Pinned,
}

/**
 * Where the onboarding chrome is drawn. Defaults to [OnboardingChromePlacement.PerStep].
 *
 * `OnboardingView` reads it as the default of its `chromePlacement` parameter;
 * provide it to set the placement for every onboarding below.
 */
public val LocalOnboardingChromePlacement: ProvidableCompositionLocal<OnboardingChromePlacement> =
    staticCompositionLocalOf { OnboardingChromePlacement.PerStep }
