package appteam.onboardingkit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import appteam.onboardingkit.analytics.NoOpOnboardingAnalytics
import appteam.onboardingkit.analytics.OnboardingAnalytics
import appteam.onboardingkit.flow.OnboardingFlowProvider
import kotlinx.coroutines.flow.first

/**
 * The onboarding: loads its flow, shows it and reports when it is done.
 *
 * ```kotlin
 * if (isOnboardingCompleted) {
 *     MainScreen()
 * } else {
 *     OnboardingView(
 *         provider = StaticFlowProvider(),
 *         analytics = AppAnalytics(),
 *         onComplete = { isOnboardingCompleted = true },  // and save it
 *     )
 * }
 * ```
 *
 * When it is first shown it asks [provider] for the flow once and shows the
 * loading screen ([loadingStyle]) until the flow comes. How long that may take
 * is the provider's decision. Then it shows the steps with their bar: the back
 * button, the progress bar and Skip.
 *
 * ## Customizing the look
 *
 * Every part the onboarding draws itself has a style, set with a parameter.
 * Each parameter changes one part, and anything not set keeps its default:
 *
 * | Parameter | What it changes | Default |
 * |---|---|---|
 * | [loadingStyle] | The screen shown while the provider loads the flow | `Automatic`: an activity indicator |
 * | [progressBarStyle] | The progress bar in the bar | `Line`; also built in: `Segmented` |
 * | [skipButtonStyle] | The Skip button | `Automatic`: a text button titled "Skip" |
 * | [backButtonStyle] | The back button | `Automatic`: a back arrow |
 * | [chromePlacement] | Whether each step has its own bar, or one bar stays in place | `PerStep` |
 *
 * ```kotlin
 * OnboardingView(
 *     provider = provider,
 *     onComplete = { ... },
 *     loadingStyle = BrandLoadingStyle,
 *     progressBarStyle = ProgressBarStyle.Segmented,
 *     skipButtonStyle = NotNowSkipButtonStyle,
 * )
 * ```
 *
 * A custom style is a type that implements the style's interface
 * ([OnboardingLoadingStyle], [ProgressBarStyle], [SkipButtonStyle],
 * [BackButtonStyle]); each interface's documentation has an example.
 *
 * Each parameter defaults to its `Local…` value (`LocalProgressBarStyle` and
 * the others), so a style can also be provided once, above every onboarding.
 *
 * Which items a step shows (back, progress bar, Skip) is not a style: each
 * step sets it in its `StepConfiguration`. The step's content is the step's own composable.
 *
 * The onboarding saves nothing. Whether it was completed is the app's to
 * remember; on the next launch the app decides whether to show it at all,
 * and the provider is not called when it is not shown.
 *
 * ## The activity being recreated
 *
 * Rotation, folding and a new window size keep the onboarding where it was:
 * the same flow and the same step, with no second call of the provider and no
 * second `Started`. After the app's process was killed in the background, the
 * onboarding starts again from the first step.
 *
 * ## Texts and localization
 *
 * The built-in texts are the strings `onboardingkit_skip` ("Skip"),
 * `onboardingkit_back` ("Back") and `onboardingkit_loading` ("Loading..."), in
 * English. To translate them, declare strings with the same names in the app;
 * for other wording, set a custom style.
 *
 * @param provider Decides which flow to show; asked once, when the onboarding is first shown.
 * @param onComplete Called once, when the user moves forward from the last step.
 *   Also called, without showing any step, if the provider returns an empty
 *   flow (which breaks its contract). Remember the completion here and stop
 *   showing the onboarding.
 * @param modifier Applied to the whole onboarding; it fills the space it is given.
 * @param analytics Receives the flow's events. Defaults to none.
 * @param chromePlacement Whether each step has its own bar, or one bar stays in place.
 * @param progressBarStyle How the progress bar looks.
 * @param skipButtonStyle How the Skip button looks.
 * @param backButtonStyle How the back button looks.
 * @param loadingStyle How the screen looks while the provider loads the flow.
 */
@Composable
public fun OnboardingView(
    provider: OnboardingFlowProvider,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    analytics: OnboardingAnalytics = NoOpOnboardingAnalytics,
    chromePlacement: OnboardingChromePlacement = LocalOnboardingChromePlacement.current,
    progressBarStyle: ProgressBarStyle = LocalProgressBarStyle.current,
    skipButtonStyle: SkipButtonStyle = LocalSkipButtonStyle.current,
    backButtonStyle: BackButtonStyle = LocalBackButtonStyle.current,
    loadingStyle: OnboardingLoadingStyle = LocalOnboardingLoadingStyle.current,
) {
    val session = retain { OnboardingSession(provider = provider, analytics = analytics) }

    val currentOnComplete by rememberUpdatedState(onComplete)
    LaunchedEffect(session) {
        snapshotFlow { session.isFinished }.first { it }
        if (session.consumeCompletion()) currentOnComplete()
    }

    val coordinator = session.coordinator
    Box(modifier) {
        if (coordinator != null) {
            OnboardingFlowView(
                coordinator = coordinator,
                chromePlacement = chromePlacement,
                progressBarStyle = progressBarStyle,
                skipButtonStyle = skipButtonStyle,
                backButtonStyle = backButtonStyle,
            )
        } else {
            loadingStyle.Body(OnboardingLoadingConfiguration())
        }
    }
}
