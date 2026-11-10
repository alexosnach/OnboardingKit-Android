package appteam.onboardingkit.analytics

/**
 * What happened in the onboarding flow.
 *
 * Every move between steps reports what the user did, then where they got to:
 * [StepContinued] for the step left, then [StepShown] for the step reached.
 * Actions that change nothing, such as back on the first step or anything
 * after completion, report nothing.
 *
 * A full pass of a two-step flow reports:
 * `Started`, `StepShown(0)`, `StepContinued(0)`, `StepShown(1)`, `StepContinued(1)`, `Completed`.
 *
 * There is no event for leaving the onboarding unfinished: the app can be
 * closed without notice. Count it as `Started` without `Completed`.
 */
public sealed interface OnboardingEvent {
    /**
     * The onboarding began. It is the first event of the flow and comes exactly
     * once, when the first step is first shown, right before its [StepShown].
     * It is never reported again: not on a return to the first step, not anywhere else.
     */
    public data object Started : OnboardingEvent

    /** The step became the current one: on start, or after any move to it. */
    public data class StepShown(val step: OnboardingStepInfo) : OnboardingEvent

    /** The user moved forward from the step with `LocalOnboardingNext`. */
    public data class StepContinued(val step: OnboardingStepInfo) : OnboardingEvent

    /** The user moved forward from the step with the Skip button or `LocalOnboardingSkip`. */
    public data class StepSkipped(val step: OnboardingStepInfo) : OnboardingEvent

    /** The user went back from the step, with the back button or the system back. */
    public data class StepBack(val step: OnboardingStepInfo) : OnboardingEvent

    /**
     * The user moved forward from the last step; comes after its
     * [StepContinued] or [StepSkipped], and is the last event of the flow.
     */
    public data object Completed : OnboardingEvent
}
