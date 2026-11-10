package appteam.onboardingkit.step

/**
 * A step that wants to know when it becomes and stops being the current one.
 *
 * Implement it on a step that needs it; a step without it gets no calls.
 *
 * A step is created once by the provider. Forward the hooks to an object the
 * step holds, such as its view model, and give the same object to the content:
 *
 * ```kotlin
 * class PaywallStep(private val model: PaywallModel) : OnboardingStep, OnboardingStepLifecycle {
 *     override val id = "paywall"
 *
 *     @Composable
 *     override fun Content() { PaywallScreen(model) }
 *
 *     override fun didBecomeCurrent() { model.startTimer() }
 *     override fun didResignCurrent() { model.stopTimer() }
 * }
 * ```
 *
 * For work tied to the content being on screen, use `LaunchedEffect` and
 * `DisposableEffect` in the content instead: the hooks follow the flow, not the screen.
 *
 * The calls follow the flow's state, not the screen: they come before the
 * transition animation, so a step that became current may not be visible yet.
 * They come on the main thread, in order: the previous step resigns, then the
 * next one becomes current. A cancelled back gesture changes nothing, so it
 * makes no calls.
 */
public interface OnboardingStepLifecycle {
    /**
     * The step became the current one: the onboarding started on it,
     * or the user moved to it.
     */
    public fun didBecomeCurrent()

    /**
     * The step stopped being the current one: the user moved away from it,
     * or the onboarding completed on it.
     */
    public fun didResignCurrent()
}
