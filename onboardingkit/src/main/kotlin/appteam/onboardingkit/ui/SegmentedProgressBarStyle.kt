package appteam.onboardingkit.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A progress bar made of one segment per step.
 *
 * Fills one segment per reached step: on step N of M, N segments are filled.
 * The number of segments follows the number of steps automatically.
 *
 * Use the shorthand `ProgressBarStyle.Segmented` for the default colors, or
 * `ProgressBarStyle.segmented(filledColor, emptyColor)` to set your own:
 *
 * ```kotlin
 * OnboardingView(
 *     provider = provider,
 *     progressBarStyle = ProgressBarStyle.segmented(filledColor = Purple, emptyColor = Purple.copy(alpha = 0.2f)),
 *     onComplete = { ... },
 * )
 * ```
 *
 * @property filledColor The color of reached steps. `Color.Unspecified`
 *   takes [ProgressBarDefaults.filledColor] from the theme.
 * @property emptyColor The color of steps not reached yet. `Color.Unspecified`
 *   takes [ProgressBarDefaults.emptyColor] from the theme.
 */
@Immutable
public class SegmentedProgressBarStyle(
    public val filledColor: Color = Color.Unspecified,
    public val emptyColor: Color = Color.Unspecified,
) : ProgressBarStyle {

    @Composable
    override fun Body(configuration: ProgressBarConfiguration) {
        val filled = filledColor.takeOrElse { ProgressBarDefaults.filledColor }
        val empty = emptyColor.takeOrElse { ProgressBarDefaults.emptyColor }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .progressSemantics(configuration.fraction),
            horizontalArrangement = Arrangement.spacedBy(SegmentSpacing),
        ) {
            repeat(configuration.total) { index ->
                // Starts at the step's own color, and animates when the same bar gets a new step.
                val color by animateColorAsState(
                    targetValue = if (isFilled(index, configuration)) filled else empty,
                    label = "OnboardingSegment",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(ProgressBarDefaults.height)
                        .background(
                            color = color,
                            shape = CircleShape,
                        ),
                )
            }
        }
    }

    /**
     * The color of one segment as the style holds it: filled if the step is
     * reached, empty otherwise. `Color.Unspecified` when the theme decides.
     */
    internal fun color(forSegment: Int, configuration: ProgressBarConfiguration): Color =
        if (isFilled(forSegment, configuration)) filledColor else emptyColor

    private fun isFilled(segment: Int, configuration: ProgressBarConfiguration): Boolean =
        segment < configuration.completedCount

    override fun equals(other: Any?): Boolean =
        other is SegmentedProgressBarStyle && filledColor == other.filledColor && emptyColor == other.emptyColor

    override fun hashCode(): Int = 31 * filledColor.hashCode() + emptyColor.hashCode()

    private companion object {
        val SegmentSpacing = 4.dp
    }
}

/** A progress bar made of one segment per step, in the default colors. */
public val ProgressBarStyle.Companion.Segmented: SegmentedProgressBarStyle
    get() = SegmentedProgressBarStyle()

/**
 * A progress bar made of one segment per step, in the given colors.
 *
 * @param filledColor The color of reached steps. Defaults to the theme's primary color.
 * @param emptyColor The color of steps not reached yet.
 */
public fun ProgressBarStyle.Companion.segmented(
    filledColor: Color = Color.Unspecified,
    emptyColor: Color = Color.Unspecified,
): SegmentedProgressBarStyle = SegmentedProgressBarStyle(filledColor = filledColor, emptyColor = emptyColor)

@Preview(name = "Default colors", showBackground = true)
@Composable
private fun SegmentedDefaultColorsPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ProgressBarStyle.Segmented.Body(ProgressBarConfiguration(currentIndex = 3, total = 10))
        }
    }
}

@Preview(name = "Custom colors", showBackground = true)
@Composable
private fun SegmentedCustomColorsPreview() {
    val purple = Color(0xFF9C27B0)
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ProgressBarStyle.segmented(filledColor = purple, emptyColor = purple.copy(alpha = 0.2f))
                .Body(ProgressBarConfiguration(currentIndex = 3, total = 10))
        }
    }
}
