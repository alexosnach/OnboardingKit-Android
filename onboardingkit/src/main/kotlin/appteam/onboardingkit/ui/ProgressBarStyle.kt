package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The data a progress bar style needs to draw itself.
 *
 * Progress counts the current step as reached: on step N of M,
 * N segments are filled. The first step shows one segment,
 * the last step shows the bar full.
 *
 * The constructor is public for previews and tests of a style.
 *
 * @property currentIndex The zero-based index of the current step.
 * @property total The total number of steps in the flow.
 */
public class ProgressBarConfiguration(
    public val currentIndex: Int,
    public val total: Int,
) {
    /**
     * The number of filled segments: step N of M fills N.
     *
     * Always within `0..total`, even for an out-of-range [currentIndex]
     * (for example, one passed by a preview of a custom style).
     */
    public val completedCount: Int
        get() = (currentIndex + 1).coerceIn(0, total.coerceAtLeast(0))

    /**
     * The completed part of the flow, from `0` to `1`.
     *
     * Use it for continuous styles (a line, a moving marker).
     * Returns `0` for an empty flow.
     */
    public val fraction: Float
        get() = if (total > 0) completedCount.toFloat() / total else 0f

    override fun equals(other: Any?): Boolean =
        other is ProgressBarConfiguration && currentIndex == other.currentIndex && total == other.total

    override fun hashCode(): Int = 31 * currentIndex + total

    override fun toString(): String = "ProgressBarConfiguration(currentIndex=$currentIndex, total=$total)"
}

/**
 * Defines how the onboarding progress bar looks.
 *
 * A style only draws the bar from the given configuration. Whether the bar
 * is visible on a step is decided by `StepConfiguration.isProgressBarShown`.
 *
 * ```kotlin
 * object DotsProgressBarStyle : ProgressBarStyle {
 *     @Composable
 *     override fun Body(configuration: ProgressBarConfiguration) {
 *         Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
 *             repeat(configuration.total) { index ->
 *                 val color = if (index < configuration.completedCount) {
 *                     ProgressBarDefaults.filledColor
 *                 } else {
 *                     ProgressBarDefaults.emptyColor
 *                 }
 *                 Box(Modifier.size(8.dp).background(color, CircleShape))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * To offer a style with a shorthand like the built-in ones, add an extension
 * to the companion: `val ProgressBarStyle.Companion.Dots get() = DotsProgressBarStyle`.
 *
 * ## Light and dark appearance
 *
 * [Body] is composable, so it reads the theme directly: use
 * `MaterialTheme.colorScheme`, or [themedColor] for a pair of fixed colors.
 * The same applies to [SkipButtonStyle], [BackButtonStyle] and [OnboardingLoadingStyle].
 */
@Stable
public interface ProgressBarStyle {
    /** Draws the progress bar for the given configuration. It takes the width it is given. */
    @Composable
    public fun Body(configuration: ProgressBarConfiguration)

    /** Holds the shorthands of the styles, such as `ProgressBarStyle.Line`. */
    public companion object
}

/**
 * The default colors and thickness shared by the built-in progress bar styles.
 *
 * Defined once so `Line` and `Segmented` look consistent.
 */
public object ProgressBarDefaults {
    /** The color of the completed part: the theme's primary color. */
    public val filledColor: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /** The color of the remaining part: a faint color from the theme. */
    public val emptyColor: Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

    /** The thickness of the built-in bars. */
    public val height: Dp = 4.dp
}

/**
 * The style used to draw the onboarding progress bar. Defaults to `ProgressBarStyle.Line`.
 *
 * `OnboardingView` reads it as the default of its `progressBarStyle` parameter;
 * provide it to set the style for every onboarding below.
 */
public val LocalProgressBarStyle: ProvidableCompositionLocal<ProgressBarStyle> =
    staticCompositionLocalOf { ProgressBarStyle.Line }
