package appteam.onboardingkit.ui

import appteam.onboardingkit.step.StepConfiguration

/**
 * Which chrome elements are visible on a step.
 *
 * The single place for the visibility rules, shared by both
 * [OnboardingChromePlacement] modes and by the system back, so they can never disagree.
 *
 * @property isBackButtonVisible Whether the back button is visible, and with it
 *   whether the system back returns to the previous step.
 * @property isProgressBarVisible Whether the progress bar is visible.
 * @property isSkipButtonVisible Whether the Skip button is visible.
 */
internal data class ChromeVisibility(
    val isBackButtonVisible: Boolean,
    val isProgressBarVisible: Boolean,
    val isSkipButtonVisible: Boolean,
) {
    /**
     * The visibility for the step at [index] with the given configuration.
     *
     * The back button is never visible on the first step: there is nowhere to go back to.
     */
    constructor(configuration: StepConfiguration, index: Int) : this(
        isBackButtonVisible = configuration.isBackButtonShown && index > 0,
        isProgressBarVisible = configuration.isProgressBarShown,
        isSkipButtonVisible = configuration.isSkipButtonShown,
    )
}
