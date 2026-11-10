package appteam.onboardingkit.flow

import appteam.onboardingkit.step.OnboardingStep

/**
 * The flow to show: its steps and the identifier analytics reports.
 *
 * Returned by [OnboardingFlowProvider]. It holds what the flow is made of;
 * where its events go (`OnboardingAnalytics`) is given separately.
 *
 * @property steps The steps, in display order. Must not be empty.
 * @property flowId Identifies the flow in analytics, for example a variant of an
 *   A/B test; sent as `flow_id` with every event. `null` when the app does not
 *   tell flows apart.
 */
public class OnboardingFlow(
    steps: List<OnboardingStep>,
    public val flowId: String? = null,
) {
    /** A copy of the given list: changing that list later does not change the flow. */
    public val steps: List<OnboardingStep> = steps.toList()
}
