package appteam.onboardingkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The data a back button style needs to draw itself.
 *
 * The constructor is public for previews and tests of a style.
 *
 * @property back Returns to the previous step. Call it from the style's button.
 *   The style does not know how going back works: the onboarding provides this action.
 */
public class BackButtonConfiguration(
    public val back: () -> Unit,
)

/**
 * Defines how the onboarding back button looks.
 *
 * The button is on the start side of the onboarding bar. Whether it is
 * visible on a step is decided by `StepConfiguration.isBackButtonShown`, not
 * by the style. It is never shown on the first step.
 *
 * The style changes only the button in the bar. The system back (button and
 * gesture) works the same with any style.
 *
 * ```kotlin
 * object CloseBackButtonStyle : BackButtonStyle {
 *     @Composable
 *     override fun Body(configuration: BackButtonConfiguration) {
 *         TextButton(onClick = configuration.back) { Text("Back") }
 *     }
 * }
 * ```
 *
 * For light and dark appearance, see "Light and dark appearance" in [ProgressBarStyle].
 */
@Stable
public interface BackButtonStyle {
    /** Draws the back button for the given configuration. */
    @Composable
    public fun Body(configuration: BackButtonConfiguration)

    /** Holds the shorthands of the styles, such as `BackButtonStyle.Automatic`. */
    public companion object
}

/**
 * The style used to draw the onboarding back button. Defaults to `BackButtonStyle.Automatic`.
 *
 * `OnboardingView` reads it as the default of its `backButtonStyle` parameter;
 * provide it to set the style for every onboarding below.
 */
public val LocalBackButtonStyle: ProvidableCompositionLocal<BackButtonStyle> =
    staticCompositionLocalOf { BackButtonStyle.Automatic }
