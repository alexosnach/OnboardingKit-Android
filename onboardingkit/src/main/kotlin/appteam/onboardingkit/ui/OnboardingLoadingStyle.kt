package appteam.onboardingkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The data a loading style needs to draw itself.
 *
 * Empty for now: the loading screen only waits. It is kept as a type, like the
 * configurations of the other styles, so fields can be added later without
 * breaking the styles apps have written.
 *
 * The constructor is public for previews and tests of a style.
 */
public class OnboardingLoadingConfiguration

/**
 * Defines how the screen looks while the onboarding waits for its flow
 * from `OnboardingFlowProvider`.
 *
 * The style draws the whole screen, edge to edge: background, indicator and
 * any text or logo. How long the wait may take is decided by the provider
 * (its timeout), not by the style.
 *
 * ```kotlin
 * object BrandLoadingStyle : OnboardingLoadingStyle {
 *     @Composable
 *     override fun Body(configuration: OnboardingLoadingConfiguration) {
 *         Column(
 *             modifier = Modifier.fillMaxSize().background(BrandBackground),
 *             verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
 *             horizontalAlignment = Alignment.CenterHorizontally,
 *         ) {
 *             Image(painterResource(R.drawable.logo), contentDescription = null)
 *             CircularProgressIndicator()
 *         }
 *     }
 * }
 * ```
 *
 * For light and dark appearance, see "Light and dark appearance" in [ProgressBarStyle].
 */
@Stable
public interface OnboardingLoadingStyle {
    /** Draws the loading screen for the given configuration. */
    @Composable
    public fun Body(configuration: OnboardingLoadingConfiguration)

    /** Holds the shorthands of the styles, such as `OnboardingLoadingStyle.Automatic`. */
    public companion object
}

/**
 * The style used to draw the onboarding's loading screen. Defaults to
 * `OnboardingLoadingStyle.Automatic`.
 *
 * `OnboardingView` reads it as the default of its `loadingStyle` parameter;
 * provide it to set the style for every onboarding below.
 */
public val LocalOnboardingLoadingStyle: ProvidableCompositionLocal<OnboardingLoadingStyle> =
    staticCompositionLocalOf { OnboardingLoadingStyle.Automatic }
