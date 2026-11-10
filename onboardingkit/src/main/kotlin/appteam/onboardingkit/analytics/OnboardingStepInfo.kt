package appteam.onboardingkit.analytics

/**
 * The step an event is about, with its place in the flow.
 *
 * The step's content reads the same value from `LocalOnboardingStepInfo.current`,
 * so its own events can carry the same `step_id` as the funnel.
 *
 * @property id The step's stable `OnboardingStep.id`.
 * @property index The step's position in the flow, starting at 0.
 * @property total The number of steps in the flow.
 */
public data class OnboardingStepInfo(
    val id: String,
    val index: Int,
    val total: Int,
)
