package appteam.onboardingkit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import appteam.onboardingkit.analytics.OnboardingAnalytics
import appteam.onboardingkit.analytics.OnboardingAnalyticsEvent
import appteam.onboardingkit.analytics.OnboardingEvent
import appteam.onboardingkit.flow.LocalOnboardingNext
import appteam.onboardingkit.flow.OnboardingFlow
import appteam.onboardingkit.flow.OnboardingFlowProvider
import appteam.onboardingkit.step.OnboardingStep
import appteam.onboardingkit.step.StepConfiguration
import kotlinx.coroutines.CompletableDeferred

/** A step that shows its id as "step:id" and a Continue button. */
internal class TextStep(
    override val id: String,
    override val configuration: StepConfiguration = StepConfiguration(),
) : OnboardingStep {
    @Composable
    override fun Content() {
        val next = LocalOnboardingNext.current
        Column {
            Text("step:$id")
            Button(onClick = { next() }) { Text("Continue") }
        }
    }
}

/** Records every event it receives. */
internal class RecordingAnalytics : OnboardingAnalytics {
    val events = mutableListOf<OnboardingAnalyticsEvent>()
    val kinds: List<OnboardingEvent> get() = events.map { it.kind }

    override fun track(event: OnboardingAnalyticsEvent) {
        events += event
    }
}

/**
 * A provider that counts its calls and answers when the test opens its gate.
 *
 * @param isOpen Whether it answers at once, without waiting for [open].
 */
internal class StubProvider(
    private val flow: OnboardingFlow,
    isOpen: Boolean = true,
) : OnboardingFlowProvider {
    var calls = 0
        private set

    private val gate = CompletableDeferred<Unit>().apply { if (isOpen) complete(Unit) }

    /** Lets the provider answer. */
    fun open() {
        gate.complete(Unit)
    }

    override suspend fun makeFlow(): OnboardingFlow {
        calls++
        gate.await()
        return flow
    }
}

/** A flow of [TextStep]s with the given ids. */
internal fun textFlow(vararg ids: String, flowId: String? = null): OnboardingFlow =
    OnboardingFlow(steps = ids.map { TextStep(it) }, flowId = flowId)
