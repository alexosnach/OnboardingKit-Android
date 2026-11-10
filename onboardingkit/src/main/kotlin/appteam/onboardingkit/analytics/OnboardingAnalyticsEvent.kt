package appteam.onboardingkit.analytics

/**
 * An event as analytics receives it: what happened and in which flow.
 *
 * New fields are added here, so adapters keep compiling when the event grows.
 *
 * The constructor is public for tests of an analytics implementation.
 *
 * @property kind What happened, with typed data: use it for logic.
 * @property flowId `OnboardingFlow.flowId` of the flow the provider returned, for
 *   example a variant of an A/B test; `null` when the flow has none.
 */
public class OnboardingAnalyticsEvent(
    public val kind: OnboardingEvent,
    public val flowId: String?,
) {
    /** The event's name for analytics systems: snake_case with the `onboarding_` prefix. */
    public val name: String
        get() = when (kind) {
            OnboardingEvent.Started -> "onboarding_started"
            is OnboardingEvent.StepShown -> "onboarding_step_shown"
            is OnboardingEvent.StepContinued -> "onboarding_step_continued"
            is OnboardingEvent.StepSkipped -> "onboarding_step_skipped"
            is OnboardingEvent.StepBack -> "onboarding_step_back"
            OnboardingEvent.Completed -> "onboarding_completed"
        }

    /**
     * The event's data as a flat map for analytics systems, ready to send.
     *
     * Keys: `flow_id` when the flow has one; `step_id`, `step_index` and
     * `step_count` for events about a step. Values are `String` or `Int`.
     * To use other names, build them from [step] and [flowId] instead.
     */
    public val parameters: Map<String, Any>
        get() = buildMap {
            flowId?.let { put("flow_id", it) }
            step?.let {
                put("step_id", it.id)
                put("step_index", it.index)
                put("step_count", it.total)
            }
        }

    /** The step the event is about; `null` for events about the whole flow. */
    public val step: OnboardingStepInfo?
        get() = when (kind) {
            is OnboardingEvent.StepShown -> kind.step
            is OnboardingEvent.StepContinued -> kind.step
            is OnboardingEvent.StepSkipped -> kind.step
            is OnboardingEvent.StepBack -> kind.step
            OnboardingEvent.Started, OnboardingEvent.Completed -> null
        }

    override fun toString(): String = "OnboardingAnalyticsEvent(kind=$kind, flowId=$flowId)"
}
