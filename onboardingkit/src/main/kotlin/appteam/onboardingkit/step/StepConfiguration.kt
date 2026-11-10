package appteam.onboardingkit.step

/**
 * Describes which chrome elements are visible on a step.
 *
 * Every property has a default value, so specify only what differs:
 * `StepConfiguration(isSkipButtonShown = true)`.
 *
 * @property isBackButtonShown Whether going back is offered: the back button in
 *   the bar and the system back (button and gesture). When `false`, neither
 *   returns to the previous step. Defaults to `true`. Has no effect on the first
 *   step of the flow.
 * @property isProgressBarShown Whether the progress bar is shown. Defaults to `true`.
 * @property isSkipButtonShown Whether the Skip button is shown, on the trailing
 *   side of the bar. Skipping moves the flow exactly like Continue (on the last
 *   step it completes the onboarding); only analytics tells them apart
 *   (`StepSkipped`). Defaults to `false`: enable it only on steps the user may
 *   safely skip.
 */
public class StepConfiguration(
    public val isBackButtonShown: Boolean = true,
    public val isProgressBarShown: Boolean = true,
    public val isSkipButtonShown: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        other is StepConfiguration &&
            isBackButtonShown == other.isBackButtonShown &&
            isProgressBarShown == other.isProgressBarShown &&
            isSkipButtonShown == other.isSkipButtonShown

    override fun hashCode(): Int {
        var result = isBackButtonShown.hashCode()
        result = 31 * result + isProgressBarShown.hashCode()
        result = 31 * result + isSkipButtonShown.hashCode()
        return result
    }

    override fun toString(): String =
        "StepConfiguration(isBackButtonShown=$isBackButtonShown, " +
            "isProgressBarShown=$isProgressBarShown, isSkipButtonShown=$isSkipButtonShown)"
}
