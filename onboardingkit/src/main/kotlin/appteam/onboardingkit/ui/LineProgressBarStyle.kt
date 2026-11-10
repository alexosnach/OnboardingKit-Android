package appteam.onboardingkit.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A progress bar drawn as one continuous line. The default style.
 *
 * The filled part takes `configuration.fraction` of the line: on step N of M
 * it covers N/M, so the first step already shows progress and the last step
 * shows a full line. It grows from the start edge, so it mirrors
 * automatically in right-to-left languages. With `Pinned` chrome the fill
 * animates when the step changes. With `PerStep` each step's bar slides in
 * with its screen, already at that step's progress.
 *
 * Use the shorthand `ProgressBarStyle.Line` for the default colors, or
 * `ProgressBarStyle.line(filledColor, emptyColor)` to set your own:
 *
 * ```kotlin
 * OnboardingView(
 *     provider = provider,
 *     progressBarStyle = ProgressBarStyle.line(filledColor = Purple, emptyColor = Purple.copy(alpha = 0.2f)),
 *     onComplete = { ... },
 * )
 * ```
 *
 * @property filledColor The color of the completed part. `Color.Unspecified`
 *   takes [ProgressBarDefaults.filledColor] from the theme.
 * @property emptyColor The color of the remaining part. `Color.Unspecified`
 *   takes [ProgressBarDefaults.emptyColor] from the theme.
 */
@Immutable
public class LineProgressBarStyle(
    public val filledColor: Color = Color.Unspecified,
    public val emptyColor: Color = Color.Unspecified,
) : ProgressBarStyle {

    @Composable
    override fun Body(configuration: ProgressBarConfiguration) {
        val filled = filledColor.takeOrElse { ProgressBarDefaults.filledColor }
        val empty = emptyColor.takeOrElse { ProgressBarDefaults.emptyColor }
        // Starts at the step's own progress, and animates when the same bar gets a new step.
        val fraction by animateFloatAsState(configuration.fraction, label = "OnboardingProgress")

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(ProgressBarDefaults.height)
                .progressSemantics(configuration.fraction)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(color = empty, cornerRadius = radius)

                    val width = size.width * fraction.coerceIn(0f, 1f)
                    // Grows from the start edge: from the right in right-to-left languages.
                    val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                    drawRoundRect(
                        color = filled,
                        topLeft = Offset(left, 0f),
                        size = Size(width, size.height),
                        cornerRadius = radius,
                    )
                },
        )
    }

    override fun equals(other: Any?): Boolean =
        other is LineProgressBarStyle && filledColor == other.filledColor && emptyColor == other.emptyColor

    override fun hashCode(): Int = 31 * filledColor.hashCode() + emptyColor.hashCode()
}

/** A continuous progress line in the default colors. */
public val ProgressBarStyle.Companion.Line: LineProgressBarStyle
    get() = LineProgressBarStyle()

/**
 * A continuous progress line in the given colors.
 *
 * @param filledColor The color of the completed part. Defaults to the theme's primary color.
 * @param emptyColor The color of the remaining part.
 */
public fun ProgressBarStyle.Companion.line(
    filledColor: Color = Color.Unspecified,
    emptyColor: Color = Color.Unspecified,
): LineProgressBarStyle = LineProgressBarStyle(filledColor = filledColor, emptyColor = emptyColor)

@Preview(name = "Default colors", showBackground = true)
@Composable
private fun LineDefaultColorsPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ProgressBarStyle.Line.Body(ProgressBarConfiguration(currentIndex = 3, total = 10))
        }
    }
}

@Preview(name = "Custom colors", showBackground = true)
@Composable
private fun LineCustomColorsPreview() {
    val purple = Color(0xFF9C27B0)
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ProgressBarStyle.line(filledColor = purple, emptyColor = purple.copy(alpha = 0.2f))
                .Body(ProgressBarConfiguration(currentIndex = 3, total = 10))
        }
    }
}
