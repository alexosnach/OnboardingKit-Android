package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A custom style that counts how often it was asked to draw. */
private class RecordingLoadingStyle : OnboardingLoadingStyle {
    var bodiesMade = 0
        private set

    @Composable
    override fun Body(configuration: OnboardingLoadingConfiguration) {
        bodiesMade++
        Text("Preparing")
    }
}

@RunWith(AndroidJUnit4::class)
class OnboardingLoadingStyleTest {
    @get:Rule
    val compose = createComposeRule()

    /** Creating a style does not draw yet: it draws only when the screen is shown. */
    @Test
    fun creatingAStyleDoesNotDrawYet() {
        val style = RecordingLoadingStyle()

        assertEquals(0, style.bodiesMade)
    }

    /** Without any setup the composition holds a usable default. */
    @Test
    fun localDefaultIsTheAutomaticStyle() {
        lateinit var style: OnboardingLoadingStyle
        compose.setContent { style = LocalOnboardingLoadingStyle.current }

        assertSame(DefaultOnboardingLoadingStyle, style)
    }

    /** The default screen shows an indicator that TalkBack reads as "Loading...". */
    @Test
    fun defaultStyleShowsTheIndicator() {
        compose.setContent {
            MaterialTheme { OnboardingLoadingStyle.Automatic.Body(OnboardingLoadingConfiguration()) }
        }

        compose.onNodeWithContentDescription("Loading...").assertExists()
    }

    @Test
    fun automaticShorthandIsTheDefaultStyle() {
        val style: OnboardingLoadingStyle = OnboardingLoadingStyle.Automatic

        assertTrue(style is DefaultOnboardingLoadingStyle)
    }

    /** Providing the app's style puts it in place of the default. */
    @Test
    fun providedStyleReplacesTheDefault() {
        val style = RecordingLoadingStyle()
        compose.setContent {
            CompositionLocalProvider(LocalOnboardingLoadingStyle provides style) {
                LocalOnboardingLoadingStyle.current.Body(OnboardingLoadingConfiguration())
            }
        }

        compose.onNodeWithText("Preparing").assertExists()
        assertEquals(1, style.bodiesMade)
    }
}
