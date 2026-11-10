package appteam.onboardingkit.flow

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import appteam.onboardingkit.analytics.NoOpOnboardingAnalytics
import appteam.onboardingkit.analytics.OnboardingAnalytics
import appteam.onboardingkit.analytics.OnboardingAnalyticsEvent
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.analytics.OnboardingStepInfo
import appteam.onboardingkit.step.OnboardingStep
import appteam.onboardingkit.step.OnboardingStepLifecycle

/**
 * Decides which step of the onboarding flow is shown.
 *
 * The coordinator holds the navigation state and changes it in response to
 * [next], [skip] and [back]. It contains no UI: composables observe its state
 * and render the current step.
 *
 * On [start], on every change of the current step and on completion it
 * calls the steps that implement [OnboardingStepLifecycle] and reports the funnel
 * to [OnboardingAnalytics] (see [OnboardingEvent] for the events and their order).
 *
 * The state can be read from outside but is changed only by the coordinator itself.
 *
 * An empty flow has nothing to show, so it is completed immediately.
 *
 * @property steps The steps of the flow, in display order.
 * @property flowId Identifies the flow in analytics, for example a variant of an A/B test.
 * @param analytics Receives the flow's events.
 */
internal class OnboardingCoordinator(
    val steps: List<OnboardingStep>,
    val flowId: String? = null,
    private val analytics: OnboardingAnalytics = NoOpOnboardingAnalytics,
) {
    /**
     * Whether the onboarding has finished.
     *
     * Becomes `true` when moving forward from the last step,
     * or immediately on creation if [steps] is empty.
     *
     * Completion is final: once `true`, it never becomes `false` again,
     * even if [back] is called afterwards. Show the app's main screen when it is set.
     */
    var isCompleted: Boolean by mutableStateOf(steps.isEmpty())
        private set

    /**
     * The index of the current step in [steps].
     *
     * Always a valid index while [steps] is not empty.
     * Stays on the last step after the onboarding is completed.
     */
    var currentIndex: Int by mutableIntStateOf(0)
        private set

    /** Whether [start] was called. Not part of the UI state. */
    private var isStarted = false

    /**
     * Moves to the next step, or completes the onboarding on the last step.
     *
     * Reported as `StepContinued` for the step the user leaves.
     */
    fun next() {
        moveForward(reportedAs = OnboardingEvent::StepContinued)
    }

    /**
     * Skips the current step.
     *
     * Navigates exactly like [next]; the difference is only in meaning:
     * it is reported as `StepSkipped` instead of `StepContinued`.
     */
    fun skip() {
        moveForward(reportedAs = OnboardingEvent::StepSkipped)
    }

    /**
     * Returns to the previous step.
     *
     * Has no effect on the first step or after completion.
     * Reported as `StepBack` for the step the user leaves.
     */
    fun back() {
        if (isCompleted || currentIndex == 0) return
        track(OnboardingEvent.StepBack(currentStepInfo))
        moveTo(currentIndex - 1)
    }

    /**
     * Starts the onboarding on the first step: the step becomes current, and
     * analytics gets `Started`, then `StepShown` for it.
     *
     * `Started` is reported only here, only once and only on the first step.
     * Later calls do nothing, and so do calls after completion (an empty flow
     * reports nothing).
     *
     * Call it only on the first step, when the onboarding is shown. The
     * onboarding's flow screen does it once the flow has loaded, so the app
     * does not need to.
     */
    fun start() {
        if (isStarted || isCompleted) return
        isStarted = true
        lifecycle(of = currentIndex)?.didBecomeCurrent()
        track(OnboardingEvent.Started)
        track(OnboardingEvent.StepShown(currentStepInfo))
    }

    /**
     * The step at the index [at] with its place in the flow, as analytics reports it.
     *
     * [at] must be a valid index of [steps].
     */
    fun stepInfo(at: Int): OnboardingStepInfo =
        OnboardingStepInfo(id = steps[at].id, index = at, total = steps.size)

    /** The single place that decides how the flow moves forward. */
    private fun moveForward(reportedAs: (OnboardingStepInfo) -> OnboardingEvent) {
        if (steps.isEmpty() || isCompleted) return
        track(reportedAs(currentStepInfo))

        if (currentIndex < steps.lastIndex) {
            moveTo(currentIndex + 1)
        } else {
            isCompleted = true
            lifecycle(of = currentIndex)?.didResignCurrent()
            track(OnboardingEvent.Completed)
        }
    }

    /**
     * Changes the current step and tells the steps: the old one resigns, then
     * the new one becomes current; then analytics gets `StepShown` for the new step.
     */
    private fun moveTo(index: Int) {
        val previous = currentIndex
        currentIndex = index
        lifecycle(of = previous)?.didResignCurrent()
        lifecycle(of = index)?.didBecomeCurrent()
        track(OnboardingEvent.StepShown(currentStepInfo))
    }

    /** The step at the index, if it wants lifecycle calls. */
    private fun lifecycle(of: Int): OnboardingStepLifecycle? = steps[of] as? OnboardingStepLifecycle

    private val currentStepInfo: OnboardingStepInfo
        get() = stepInfo(at = currentIndex)

    private fun track(kind: OnboardingEvent) {
        analytics.track(OnboardingAnalyticsEvent(kind = kind, flowId = flowId))
    }
}
