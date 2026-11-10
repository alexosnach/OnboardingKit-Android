package appteam.onboardingkit.flow

/**
 * Decides which flow the onboarding shows.
 *
 * The app implements it: a fixed list of steps, or a list built from a remote
 * configuration (for example, a variant of an A/B test). The onboarding does
 * not know where the flow comes from and runs no experiments itself.
 *
 * A remote implementation maps the identifiers it receives to the app's step
 * types; what to do with an unknown identifier is its decision.
 *
 * The provider is also where steps get what they need: it creates them, so it
 * passes services and shared models to their constructors. To share answers
 * between steps, give them one model object and read it in `onComplete`:
 *
 * ```kotlin
 * class StaticFlowProvider(
 *     private val answers: OnboardingAnswers,  // a state holder owned by the app
 * ) : OnboardingFlowProvider {
 *     override suspend fun makeFlow() = OnboardingFlow(
 *         steps = listOf(
 *             WelcomeStep(),
 *             NameStep(answers),
 *             NotificationsStep(),
 *         ),
 *     )
 * }
 * ```
 *
 * A remote provider waits for its configuration with a timeout and falls back
 * to a built-in flow:
 *
 * ```kotlin
 * class RemoteFlowProvider(
 *     private val config: RemoteConfig,
 *     private val answers: OnboardingAnswers,
 * ) : OnboardingFlowProvider {
 *     override suspend fun makeFlow(): OnboardingFlow {
 *         val variant = withTimeoutOrNull(2.seconds) { config.onboardingVariant() }
 *             ?: return defaultFlow
 *         val steps = variant.stepIds.mapNotNull(::makeStep)  // drops unknown ids
 *         return if (steps.isEmpty()) defaultFlow else OnboardingFlow(steps, flowId = variant.name)
 *     }
 *
 *     private fun makeStep(id: String): OnboardingStep? = when (id) {
 *         "welcome" -> WelcomeStep()
 *         "name" -> NameStep(answers)
 *         else -> null
 *     }
 *
 *     private val defaultFlow: OnboardingFlow get() = ...
 * }
 * ```
 *
 * While [makeFlow] runs, `OnboardingView` shows its loading screen
 * (`OnboardingLoadingStyle`).
 */
public fun interface OnboardingFlowProvider {
    /**
     * Returns the flow to show.
     *
     * Every implementation must keep these promises:
     * - It always returns a flow and never fails. If the flow depends on the
     *   network, the implementation waits up to its own timeout and, on a
     *   timeout or an error, returns its default flow.
     * - The flow has at least one step.
     * - The result stays valid for the whole pass: a newer configuration that
     *   arrives later applies to the next showing of the onboarding, not to this one.
     */
    public suspend fun makeFlow(): OnboardingFlow
}
