package appteam.onboardingkit.ui

import androidx.compose.runtime.Composable
import appteam.onboardingkit.step.OnboardingStep

/** A minimal step shared by the UI tests: only its identity matters. */
internal class StubStep(override val id: String) : OnboardingStep {
    @Composable
    override fun Content() {}
}
