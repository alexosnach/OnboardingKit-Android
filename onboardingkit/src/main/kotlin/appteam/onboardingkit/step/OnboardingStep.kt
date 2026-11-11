package appteam.onboardingkit.step

import androidx.compose.runtime.Composable

/**
 * A single screen of the onboarding flow.
 *
 * A step describes *what* to show: its identity, content and chrome configuration.
 * It does not decide *where* the flow goes next: the order of steps comes from
 * `OnboardingFlowProvider`, and the onboarding moves through them.
 *
 * The step is created by the provider, so dependencies (services, a model
 * shared between steps) come through its constructor; see `OnboardingFlowProvider`.
 *
 * ## Implementing a step
 *
 * ```kotlin
 * class WelcomeStep : OnboardingStep {
 *     override val id = "welcome"
 *
 *     @Composable
 *     override fun Content() {
 *         WelcomeScreen()
 *     }
 * }
 * ```
 *
 * ## Moving to the next step
 *
 * The step decides *when* it is done and tells the flow through the
 * `LocalOnboardingNext` action (`LocalOnboardingSkip` works the same way):
 *
 * - When the user decides (a button), call the action from the composable:
 *
 *   ```kotlin
 *   @Composable
 *   fun WelcomeScreen() {
 *       val next = LocalOnboardingNext.current
 *
 *       Button(onClick = { next() }) { Text("Continue") }
 *   }
 *   ```
 *
 * - When a view model decides (a server response, a timer), pass the action
 *   to it as a lambda, so the view model stays independent of Compose:
 *
 *   ```kotlin
 *   LaunchedEffect(Unit) { viewModel.onFinished = { next() } }
 *   ```
 *
 * The actions work only inside `OnboardingView`; elsewhere they do nothing.
 *
 * There is no action to go back or to leave the onboarding early: going back
 * is the job of the back button and the system back, and the onboarding ends
 * only by moving forward from the last step. A step that ends the flow early,
 * for example "I already have an account", is placed last, or the app stops
 * showing `OnboardingView`.
 *
 * ## Writing the content
 *
 * The content is responsible for its own padding and scrolling. Make long
 * content scrollable: the onboarding does not scroll it. The bar with the back
 * button, progress bar and Skip is drawn above the content, never inside it:
 * the content starts below the bar and cannot be placed under it.
 *
 * ## Filling the screen behind the bar
 *
 * The step is one screen with its bar. What fills that whole screen, such as a
 * color, a gradient or an image, goes to [Background]: it is drawn edge to
 * edge, behind the bar and behind the content.
 *
 * ```kotlin
 * @Composable
 * override fun Background() {
 *     Box(Modifier.fillMaxSize().background(brandGradient))
 * }
 * ```
 *
 * ## Sending the step's own analytics events
 *
 * The onboarding reports only the funnel (see `OnboardingEvent`). Events inside
 * the step, such as the answer to a permission request, the step sends to the
 * app's analytics itself. To tie them to the funnel, read the step's place and
 * send the same `flow_id` and `step_id`:
 *
 * ```kotlin
 * val step = LocalOnboardingStepInfo.current
 * val flowId = LocalOnboardingFlowId.current
 *
 * // In the view model, after the permission request:
 * analytics.send("notifications_permission", mapOf(
 *     "flow_id" to flowId.orEmpty(), "step_id" to step?.id.orEmpty(), "granted" to granted,
 * ))
 * ```
 *
 * Keep [id] when only the step's look or text changes between A/B variants:
 * the variant is told apart by `flow_id`.
 */
public interface OnboardingStep {
    /**
     * A stable identifier of the step.
     *
     * Must be unique within a flow and must not change between app versions,
     * because analytics reports it: the coordinator sends it as
     * `OnboardingStepInfo.id` (`step_id` in `OnboardingAnalyticsEvent.parameters`).
     * Navigation does not use it: steps are told apart by their index.
     * Do not use generated values such as `UUID.randomUUID()`.
     */
    public val id: String

    /**
     * The chrome elements visible on this step.
     *
     * Defaults to `StepConfiguration()` when not overridden.
     */
    public val configuration: StepConfiguration
        get() = StepConfiguration()

    /**
     * The screen displayed inside the onboarding chrome.
     *
     * Provide only the screen content. The back button, progress bar and Skip
     * button are drawn by the onboarding container.
     */
    @Composable
    public fun Content()

    /**
     * What fills the step's whole screen, behind the bar and behind [Content].
     *
     * It gets the whole screen, up to every edge of the window, also under the
     * status bar and the onboarding bar. It moves with the step in a
     * transition. Empty when not overridden: the step then has the theme's
     * background.
     *
     * Draw only the backdrop here. What the user reads or taps belongs to
     * [Content], which is kept below the bar.
     */
    @Composable
    public fun Background() {}
}
