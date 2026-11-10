package appteam.onboardingkit.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The data a Skip button style needs to draw itself.
 *
 * The constructor is public for previews and tests of a style.
 *
 * @property skip Skips the current step. Call it from the style's button.
 *   The style does not know how skipping works: the onboarding provides this action.
 */
public class SkipButtonConfiguration(
    public val skip: () -> Unit,
)

/**
 * Defines how the onboarding Skip button looks.
 *
 * A style draws the whole button: its text, icon and colors. The button is on
 * the end side of the onboarding bar. Whether it is visible on a step is
 * decided by `StepConfiguration.isSkipButtonShown`, not by the style.
 *
 * ```kotlin
 * object NotNowSkipButtonStyle : SkipButtonStyle {
 *     @Composable
 *     override fun Body(configuration: SkipButtonConfiguration) {
 *         TextButton(onClick = configuration.skip) {
 *             Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
 *         }
 *     }
 * }
 * ```
 *
 * For light and dark appearance, see "Light and dark appearance" in [ProgressBarStyle].
 */
@Stable
public interface SkipButtonStyle {
    /** Draws the Skip button for the given configuration. */
    @Composable
    public fun Body(configuration: SkipButtonConfiguration)

    /** Holds the shorthands of the styles, such as `SkipButtonStyle.Automatic`. */
    public companion object
}

/**
 * The style used to draw the onboarding Skip button. Defaults to `SkipButtonStyle.Automatic`.
 *
 * Applies only to the Skip button, not to buttons inside the steps' content.
 * `OnboardingView` reads it as the default of its `skipButtonStyle` parameter;
 * provide it to set the style for every onboarding below.
 */
public val LocalSkipButtonStyle: ProvidableCompositionLocal<SkipButtonStyle> =
    staticCompositionLocalOf { SkipButtonStyle.Automatic }
