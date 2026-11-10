package appteam.onboardingkit.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.setValue
import appteam.onboardingkit.analytics.OnboardingAnalytics
import appteam.onboardingkit.flow.OnboardingCoordinator
import appteam.onboardingkit.flow.OnboardingFlowProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * One showing of the onboarding: loads the flow and holds its coordinator.
 *
 * `OnboardingView` keeps it with `retain`, so it lives through the activity
 * being recreated (rotation, folding, a new window size): the provider is not
 * asked again, the current step stays, and analytics gets no second `Started`.
 * It ends when `OnboardingView` leaves the screen for good.
 *
 * The provider and analytics are the ones given when the onboarding was first
 * shown; later values are not used, as the flow is loaded once.
 */
internal class OnboardingSession(
    private val provider: OnboardingFlowProvider,
    private val analytics: OnboardingAnalytics,
) : RetainObserver {
    /** The loaded flow; `null` while the provider has not answered yet, and for an empty flow. */
    var coordinator: OnboardingCoordinator? by mutableStateOf(null)
        private set

    /**
     * Whether the provider gave no steps to show: a flow with no steps, or no
     * flow at all because its own waiting was cancelled (`withTimeout` in
     * place of `withTimeoutOrNull`). It breaks the provider's contract, but
     * must neither crash the app nor leave the loading screen forever: the
     * onboarding is finished at once, without showing any step.
     */
    private var isFlowEmpty by mutableStateOf(false)

    /** Whether the onboarding is over: the user passed the last step, or there were no steps. */
    val isFinished: Boolean
        get() = isFlowEmpty || coordinator?.isCompleted == true

    private var isCompletionReported = false

    /** Runs the loading. Outlives a recreated activity; cancelled when the session ends. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Says whether the completion is to be reported now: `true` exactly once,
     * so `onComplete` is never called twice, also after the activity is recreated.
     */
    fun consumeCompletion(): Boolean {
        if (!isFinished || isCompletionReported) return false
        isCompletionReported = true
        return true
    }

    /** The onboarding is shown for the first time: ask the provider, once. */
    override fun onRetained() {
        scope.launch {
            val flow = try {
                provider.makeFlow()
            } catch (cancellation: CancellationException) {
                // The session itself was cancelled: nobody waits for the flow.
                ensureActive()
                // Otherwise the provider cancelled its own work and returned nothing.
                null
            }
            if (flow == null || flow.steps.isEmpty()) {
                isFlowEmpty = true
            } else {
                coordinator = OnboardingCoordinator(steps = flow.steps, flowId = flow.flowId, analytics = analytics)
            }
        }
    }

    /** The onboarding left the screen for good: a late answer of the provider is dropped. */
    override fun onRetired() {
        scope.cancel()
    }

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onUnused() {
        scope.cancel()
    }
}
