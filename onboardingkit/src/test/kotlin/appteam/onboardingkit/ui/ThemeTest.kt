package appteam.onboardingkit.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A color as a screen in the light theme and a screen in the dark theme get it. */
private class Resolved(val light: Color, val dark: Color)

/** Shows the content as the system does with the dark theme on or off. */
@Composable
private fun SystemTheme(isDark: Boolean, content: @Composable () -> Unit) {
    val configuration = Configuration(LocalConfiguration.current).apply {
        val night = if (isDark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
    }
    CompositionLocalProvider(LocalConfiguration provides configuration) {
        MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme(), content = content)
    }
}

@RunWith(AndroidJUnit4::class)
class ThemeTest {
    @get:Rule
    val compose = createComposeRule()

    /** Resolves a color the way Compose does for a screen in each theme. */
    private fun resolved(color: @Composable () -> Color): Resolved {
        var light = Color.Unspecified
        var dark = Color.Unspecified
        compose.setContent {
            SystemTheme(isDark = false) { light = color() }
            SystemTheme(isDark = true) { dark = color() }
        }
        compose.waitForIdle()
        return Resolved(light, dark)
    }

    @Test
    fun themedColorUsesEachVariantInItsTheme() {
        val color = resolved { themedColor(light = Color.Red, dark = Color.Blue) }

        assertEquals(Color.Red, color.light)
        assertEquals(Color.Blue, color.dark)
    }

    @Test
    fun themedColorDiffersBetweenThemes() {
        val color = resolved { themedColor(light = Color.Black, dark = Color.White) }

        assertNotEquals(color.light, color.dark)
    }

    /** Same fixed color for both themes behaves like a plain color. */
    @Test
    fun sameVariantsGiveTheSameColor() {
        val brand = Color(red = 0.1f, green = 0.6f, blue = 0.3f)
        val color = resolved { themedColor(light = brand, dark = brand) }

        assertEquals(color.light, color.dark)
    }

    @Test
    fun opacityIsKeptPerTheme() {
        val color = resolved {
            themedColor(light = Color.Black.copy(alpha = 0.1f), dark = Color.White.copy(alpha = 0.2f))
        }

        assertEquals(0.1f, color.light.alpha, 0.01f)
        assertEquals(0.2f, color.dark.alpha, 0.01f)
    }

    // Built-in styles

    /** The default colors come from the app's theme, so they follow its light and dark schemes. */
    @Test
    fun defaultFilledColorComesFromTheTheme() {
        val filled = resolved { ProgressBarDefaults.filledColor }

        assertEquals(lightColorScheme().primary, filled.light)
        assertEquals(darkColorScheme().primary, filled.dark)
    }

    /** The default empty track must stay visible on both backgrounds. */
    @Test
    fun defaultEmptyColorAdaptsToTheme() {
        val empty = resolved { ProgressBarDefaults.emptyColor }

        assertNotEquals(empty.light, empty.dark)
    }

    @Test
    fun lineStyleKeepsThemedColors() {
        val color = resolved {
            ProgressBarStyle.line(filledColor = themedColor(light = Color.Blue, dark = Color.Green)).filledColor
        }

        assertEquals(Color.Blue, color.light)
        assertEquals(Color.Green, color.dark)
    }

    /** A style left at its defaults resolves them from the theme of the screen it is drawn on. */
    @Test
    fun unspecifiedStyleColorResolvesPerTheme() {
        val color = resolved {
            ProgressBarStyle.Line.filledColor.takeOrElse { ProgressBarDefaults.filledColor }
        }

        assertEquals(lightColorScheme().primary, color.light)
        assertEquals(darkColorScheme().primary, color.dark)
    }

    @Test
    fun segmentedStyleColorsSegmentsPerTheme() {
        val configuration = ProgressBarConfiguration(currentIndex = 0, total = 2)
        val filled = resolved {
            ProgressBarStyle.segmented(
                filledColor = themedColor(light = Color.Red, dark = Color.Yellow),
                emptyColor = themedColor(light = Color.Gray, dark = Color.White),
            ).color(forSegment = 0, configuration)
        }

        assertEquals(Color.Red, filled.light)
        assertEquals(Color.Yellow, filled.dark)
    }

    /** Custom styles read themed colors directly: their `Body` is composable. */
    @Test
    fun customStyleCanUseThemedColors() {
        var drawn = Color.Unspecified
        val style = object : SkipButtonStyle {
            @Composable
            override fun Body(configuration: SkipButtonConfiguration) {
                val color = themedColor(light = Color.Magenta, dark = Color.Yellow)
                drawn = color
                TextButton(onClick = configuration.skip) { Text("Skip", color = color) }
            }
        }
        compose.setContent {
            SystemTheme(isDark = true) { style.Body(SkipButtonConfiguration(skip = {})) }
        }

        compose.onNodeWithText("Skip").assertExists()
        assertEquals(Color.Yellow, drawn)
    }
}
