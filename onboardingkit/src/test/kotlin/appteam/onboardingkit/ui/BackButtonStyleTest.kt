package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

/** A custom style: a button titled "Return" that goes back through the given action. */
private object ReturnBackButtonStyle : BackButtonStyle {
    @Composable
    override fun Body(configuration: BackButtonConfiguration) {
        TextButton(onClick = configuration.back) { Text("Return") }
    }
}

@RunWith(AndroidJUnit4::class)
class BackButtonStyleTest {
    @get:Rule
    val compose = createComposeRule()

    /** Without any setup the composition holds the default style. */
    @Test
    fun localDefaultIsTheAutomaticStyle() {
        lateinit var style: BackButtonStyle
        compose.setContent { style = LocalBackButtonStyle.current }

        assertSame(DefaultBackButtonStyle, style)
    }

    /** Providing a style replaces the default for everything below. */
    @Test
    fun providedStyleReplacesTheDefault() {
        compose.setContent {
            CompositionLocalProvider(LocalBackButtonStyle provides ReturnBackButtonStyle) {
                MaterialTheme { LocalBackButtonStyle.current.Body(BackButtonConfiguration(back = {})) }
            }
        }

        compose.onNodeWithText("Return").assertExists()
    }

    /** The default button is read as "Back" and goes back through the provided action. */
    @Test
    fun defaultStyleCallsTheBackAction() {
        var calls = 0
        compose.setContent {
            MaterialTheme { BackButtonStyle.Automatic.Body(BackButtonConfiguration(back = { calls++ })) }
        }

        compose.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, calls)
    }

    /**
     * A custom button goes back only through the provided action,
     * and that action drives the coordinator exactly like `back()`.
     */
    @Test
    fun styleActionReturnsToPreviousStep() {
        val coordinator = OnboardingCoordinator(steps = listOf(StubStep("a"), StubStep("b"), StubStep("c")))
        coordinator.next()
        coordinator.next()
        compose.setContent {
            MaterialTheme { ReturnBackButtonStyle.Body(BackButtonConfiguration(back = { coordinator.back() })) }
        }

        compose.onNodeWithText("Return").performClick()

        assertEquals(1, coordinator.currentIndex)
    }

    @Test
    fun automaticShorthandIsTheDefaultStyle() {
        val style: BackButtonStyle = BackButtonStyle.Automatic

        assertTrue(style is DefaultBackButtonStyle)
    }
}
