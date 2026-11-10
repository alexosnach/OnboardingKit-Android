package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import appteam.onboardingkit.flow.OnboardingCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A custom style: a button titled "Not now" that skips through the given action. */
private object NotNowSkipButtonStyle : SkipButtonStyle {
    @Composable
    override fun Body(configuration: SkipButtonConfiguration) {
        TextButton(onClick = configuration.skip) { Text("Not now") }
    }
}

@RunWith(AndroidJUnit4::class)
class SkipButtonStyleTest {
    @get:Rule
    val compose = createComposeRule()

    /** Without any setup the composition holds the default style. */
    @Test
    fun localDefaultIsTheAutomaticStyle() {
        lateinit var style: SkipButtonStyle
        compose.setContent { style = LocalSkipButtonStyle.current }

        assertSame(DefaultSkipButtonStyle, style)
    }

    /** Providing a style replaces the default for everything below. */
    @Test
    fun providedStyleReplacesTheDefault() {
        compose.setContent {
            CompositionLocalProvider(LocalSkipButtonStyle provides NotNowSkipButtonStyle) {
                MaterialTheme { LocalSkipButtonStyle.current.Body(SkipButtonConfiguration(skip = {})) }
            }
        }

        compose.onNodeWithText("Not now").assertExists()
    }

    /** The default button is titled "Skip" and skips through the provided action. */
    @Test
    fun defaultStyleCallsTheSkipAction() {
        var skipped = 0
        compose.setContent {
            MaterialTheme { SkipButtonStyle.Automatic.Body(SkipButtonConfiguration(skip = { skipped++ })) }
        }

        compose.onNodeWithText("Skip").performClick()

        assertEquals(1, skipped)
    }

    /**
     * A custom style triggers skipping only through the provided action,
     * and that action drives the coordinator exactly like `skip()`.
     */
    @Test
    fun styleActionSkipsCurrentStep() {
        val coordinator = OnboardingCoordinator(steps = listOf(StubStep("a"), StubStep("b"), StubStep("c")))
        compose.setContent {
            MaterialTheme { NotNowSkipButtonStyle.Body(SkipButtonConfiguration(skip = { coordinator.skip() })) }
        }

        compose.onNodeWithText("Not now").performClick()

        assertEquals(1, coordinator.currentIndex)
    }

    @Test
    fun automaticShorthandIsTheDefaultStyle() {
        val style: SkipButtonStyle = SkipButtonStyle.Automatic

        assertTrue(style is DefaultSkipButtonStyle)
    }
}
