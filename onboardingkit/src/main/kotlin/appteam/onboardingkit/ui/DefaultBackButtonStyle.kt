package appteam.onboardingkit.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import appteam.onboardingkit.R

/**
 * The default back button: a standard icon button with a back arrow.
 *
 * Uses no custom colors, so it looks like any toolbar button and follows the
 * app's theme. The arrow mirrors in right-to-left languages. TalkBack reads
 * the string `onboardingkit_back` ("Back"); declare a string with the same
 * name in the app to translate it.
 *
 * Use the shorthand `BackButtonStyle.Automatic` to apply it.
 */
public object DefaultBackButtonStyle : BackButtonStyle {
    @Composable
    override fun Body(configuration: BackButtonConfiguration) {
        IconButton(onClick = configuration.back) {
            Icon(
                painter = painterResource(R.drawable.onboardingkit_ic_arrow_back),
                contentDescription = stringResource(R.string.onboardingkit_back),
            )
        }
    }
}

/** The default back button: a back arrow. */
public val BackButtonStyle.Companion.Automatic: DefaultBackButtonStyle
    get() = DefaultBackButtonStyle

@Preview(showBackground = true)
@Composable
private fun DefaultBackButtonPreview() {
    MaterialTheme {
        BackButtonStyle.Automatic.Body(BackButtonConfiguration(back = {}))
    }
}
