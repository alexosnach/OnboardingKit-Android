package appteam.onboardingkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import appteam.onboardingkit.R

/**
 * The default loading screen: an activity indicator in the center,
 * on the theme's background, edge to edge.
 *
 * TalkBack reads the string `onboardingkit_loading` ("Loading..."); declare a
 * string with the same name in the app to translate it.
 *
 * Use the shorthand `OnboardingLoadingStyle.Automatic` to apply it.
 */
public object DefaultOnboardingLoadingStyle : OnboardingLoadingStyle {
    @Composable
    override fun Body(configuration: OnboardingLoadingConfiguration) {
        val description = stringResource(R.string.onboardingkit_loading)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(Modifier.semantics { contentDescription = description })
        }
    }
}

/** The default loading screen: an activity indicator on the theme's background. */
public val OnboardingLoadingStyle.Companion.Automatic: DefaultOnboardingLoadingStyle
    get() = DefaultOnboardingLoadingStyle

@Preview
@Composable
private fun DefaultLoadingPreview() {
    MaterialTheme {
        OnboardingLoadingStyle.Automatic.Body(OnboardingLoadingConfiguration())
    }
}
