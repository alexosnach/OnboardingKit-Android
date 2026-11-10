package appteam.onboardingkit.step

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import appteam.onboardingkit.analytics.OnboardingStepInfo

/**
 * The step this content belongs to: its `id`, position and the number of steps.
 *
 * Provided by `OnboardingView` to every step's content; `null` outside of it.
 * Use it to tie the step's own analytics events to the onboarding funnel,
 * with the same values the onboarding sends as `step_id`, `step_index`
 * and `step_count`:
 *
 * ```kotlin
 * val step = LocalOnboardingStepInfo.current
 * val flowId = LocalOnboardingFlowId.current
 *
 * LaunchedEffect(step, flowId) { viewModel.configure(step, flowId) }
 * ```
 */
public val LocalOnboardingStepInfo: ProvidableCompositionLocal<OnboardingStepInfo?> =
    compositionLocalOf { null }

/**
 * `OnboardingFlow.flowId` of the flow being shown, sent to analytics as `flow_id`.
 *
 * Provided by `OnboardingView`; `null` outside of it or when the flow has none.
 */
public val LocalOnboardingFlowId: ProvidableCompositionLocal<String?> =
    compositionLocalOf { null }
