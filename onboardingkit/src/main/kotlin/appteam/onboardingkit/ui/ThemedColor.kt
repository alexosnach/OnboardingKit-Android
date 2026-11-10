package appteam.onboardingkit.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * A color that switches between two colors with the light and dark appearance.
 *
 * Follows the system's dark theme: when it changes, the composable that called
 * this function is drawn again with the matching color.
 *
 * Works with every onboarding style, built-in or custom, because a style
 * only receives a `Color`:
 *
 * ```kotlin
 * OnboardingView(
 *     provider = provider,
 *     progressBarStyle = ProgressBarStyle.line(
 *         filledColor = themedColor(light = Indigo, dark = Mint),
 *         emptyColor = themedColor(light = Color.Black.copy(alpha = 0.1f), dark = Color.White.copy(alpha = 0.2f)),
 *     ),
 *     onComplete = { ... },
 * )
 * ```
 *
 * For colors of the app's theme, read `MaterialTheme.colorScheme` instead;
 * they adapt the same way. An app with its own theme switch, independent of
 * the system, picks the color from its own setting.
 *
 * @param light The color used with the light appearance.
 * @param dark The color used with the dark appearance.
 */
@Composable
@ReadOnlyComposable
public fun themedColor(light: Color, dark: Color): Color = if (isSystemInDarkTheme()) dark else light
