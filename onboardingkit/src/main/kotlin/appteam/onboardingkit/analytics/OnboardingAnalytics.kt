package appteam.onboardingkit.analytics

/**
 * Receives the onboarding's funnel events.
 *
 * The app implements it as an adapter to its own analytics: send
 * `event.name` and `event.parameters` as they are, or build its own names
 * from the typed fields (`event.kind`, `event.step`, `event.flowId`).
 *
 * ```kotlin
 * class AppAnalytics : OnboardingAnalytics {
 *     override fun track(event: OnboardingAnalyticsEvent) {
 *         AnalyticsManager.send(event.name, event.parameters)
 *     }
 * }
 *
 * OnboardingView(provider = provider, analytics = AppAnalytics(), onComplete = { ... })
 * ```
 *
 * `flow_id` comes from the `OnboardingFlow` the provider returned.
 *
 * Only the funnel comes here. Events inside a step (a permission answer, a
 * choice) the step sends to the app's analytics itself, with `flow_id` and
 * `step_id` read from `LocalOnboardingFlowId` and `LocalOnboardingStepInfo`.
 *
 * Events come on the main thread, synchronously, at the moment the flow's
 * state changes: before the transition animation. Keep [track] fast;
 * send to the network from your analytics manager, not from here.
 */
public fun interface OnboardingAnalytics {
    /** Called once for every event of the flow, in the order they happen. */
    public fun track(event: OnboardingAnalyticsEvent)
}

/** Analytics that ignores every event: the default when the app passes none. */
public object NoOpOnboardingAnalytics : OnboardingAnalytics {
    override fun track(event: OnboardingAnalyticsEvent) {}
}
