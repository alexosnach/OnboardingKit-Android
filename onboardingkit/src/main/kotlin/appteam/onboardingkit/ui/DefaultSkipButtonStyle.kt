package appteam.onboardingkit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import appteam.onboardingkit.R

/**
 * The default Skip button: a standard text button.
 *
 * Uses no custom colors or fonts, so it looks like any toolbar button
 * and follows the app's theme.
 *
 * The title is the string `onboardingkit_skip` ("Skip"); declare a string with
 * the same name in the app to translate it. For a different look, use a custom style.
 *
 * Use the shorthand `SkipButtonStyle.Automatic` to apply it.
 */
public object DefaultSkipButtonStyle : SkipButtonStyle {
    @Composable
    override fun Body(configuration: SkipButtonConfiguration) {
        TextButton(onClick = configuration.skip) {
            Text(stringResource(R.string.onboardingkit_skip))
        }
    }
}

/** The default Skip button: a standard text button. */
public val SkipButtonStyle.Companion.Automatic: DefaultSkipButtonStyle
    get() = DefaultSkipButtonStyle

@Preview(showBackground = true)
@Composable
private fun DefaultSkipButtonPreview() {
    MaterialTheme {
        SkipButtonStyle.Automatic.Body(SkipButtonConfiguration(skip = {}))
    }
}
