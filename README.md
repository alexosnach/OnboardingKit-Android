# OnboardingKit for Android

A Jetpack Compose library for step-by-step onboarding flows on Android. You
write the screens; OnboardingKit handles the navigation, the back button, the
progress bar, Skip, analytics events and the loading state.

It is the Android version of [OnboardingKit for iOS](https://github.com/alexosnach/OnboardingKit),
with the same concepts, names and behavior.

- **Any screen is a step.** A step is a small class with a composable `Content()`.
- **System back.** The back button, the back gesture and the predictive back animation work out of the box.
- **Any device.** The flow keeps its place through rotation, folding and window resizing; the bar keeps clear of the status bar and display cutouts; right-to-left layouts are mirrored.
- **Progress bar.** A line or a segment per step, or your own style. Colors are configurable.
- **Skip.** On the end side, shown only on the steps you allow.
- **Per-step chrome.** Every step decides whether it shows the back button, the progress bar and Skip.
- **Flow providers.** The order of steps can come from code or from remote config, with a loading screen while it loads.
- **Analytics funnel.** Start, step shown, continued, skipped, back and completed, sent to your analytics.
- **Lifecycle hooks.** A step can know when it becomes current and when it leaves.

## Requirements

- Android 7.0 (API 24) or later
- Jetpack Compose with Material 3

## Installation

The library is published from GitHub through [JitPack](https://jitpack.io).

Add the JitPack repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependency to your app module:

```kotlin
dependencies {
    implementation("com.github.alexosnach.OnboardingKit-Android:onboardingkit:0.1.0")
}
```

## Quick start

### 1. Write the steps

A step has an `id` (used in analytics), an optional `configuration` and the
`Content()` to show. The step moves on by calling the `LocalOnboardingNext` action.

```kotlin
class WelcomeStep : OnboardingStep {
    override val id = "welcome"

    @Composable
    override fun Content() {
        val next = LocalOnboardingNext.current

        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Welcome!", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = { next() }) { Text("Continue") }
        }
    }
}

class NameStep : OnboardingStep {
    override val id = "name"
    override val configuration = StepConfiguration(isSkipButtonShown = true)

    @Composable
    override fun Content() {
        NameScreen()
    }
}
```

`StepConfiguration` defaults: back button shown, progress bar shown, Skip hidden.
Set only what differs.

### 2. Describe the flow

A provider returns the steps in the order they are shown. `flowId` names the
flow in analytics, for example an A/B test variant.

```kotlin
class AppFlowProvider : OnboardingFlowProvider {
    override suspend fun makeFlow() = OnboardingFlow(
        steps = listOf(WelcomeStep(), NameStep()),
        flowId = "main_v1",
    )
}
```

### 3. Show the onboarding

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                var isOnboardingCompleted by remember { mutableStateOf(loadCompleted()) }

                if (isOnboardingCompleted) {
                    MainScreen()
                } else {
                    OnboardingView(
                        provider = remember { AppFlowProvider() },
                        onComplete = {
                            saveCompleted()
                            isOnboardingCompleted = true
                        },
                    )
                }
            }
        }
    }
}
```

`onComplete` is called once, when the user moves forward from the last step.
OnboardingKit stores nothing: remembering that the onboarding is done is up to
the app.

## Customization

Every part the onboarding draws has a style, set with a parameter of
`OnboardingView`. Anything not set keeps its default.

### Progress bar

```kotlin
OnboardingView(
    provider = provider,
    onComplete = { /* ... */ },
    progressBarStyle = ProgressBarStyle.Segmented, // one segment per step; the default is Line
)

// Your own colors:
progressBarStyle = ProgressBarStyle.line(filledColor = Purple, emptyColor = Purple.copy(alpha = 0.2f))
```

Your own style:

```kotlin
object DotsProgressBarStyle : ProgressBarStyle {
    @Composable
    override fun Body(configuration: ProgressBarConfiguration) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(configuration.total) { index ->
                val color = if (index < configuration.completedCount) {
                    ProgressBarDefaults.filledColor
                } else {
                    ProgressBarDefaults.emptyColor
                }
                Box(Modifier.size(8.dp).background(color, CircleShape))
            }
        }
    }
}
```

`ProgressBarConfiguration` gives `currentIndex`, `total`, `completedCount` and `fraction`.

### Skip and back buttons

```kotlin
object TextSkipButtonStyle : SkipButtonStyle {
    @Composable
    override fun Body(configuration: SkipButtonConfiguration) {
        TextButton(onClick = configuration.skip) { Text("Not now") }
    }
}

object TextBackButtonStyle : BackButtonStyle {
    @Composable
    override fun Body(configuration: BackButtonConfiguration) {
        TextButton(onClick = configuration.back) { Text("Back") }
    }
}

OnboardingView(
    provider = provider,
    onComplete = { /* ... */ },
    skipButtonStyle = TextSkipButtonStyle,
    backButtonStyle = TextBackButtonStyle,
)
```

### Chrome placement

```kotlin
chromePlacement = OnboardingChromePlacement.PerStep // default: each step has its own bar and slides with it
chromePlacement = OnboardingChromePlacement.Pinned  // one bar for the whole flow; only the content slides
```

### Loading screen

Shown while `makeFlow()` runs.

```kotlin
object BrandLoadingStyle : OnboardingLoadingStyle {
    @Composable
    override fun Body(configuration: OnboardingLoadingConfiguration) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.logo), contentDescription = null)
        }
    }
}

loadingStyle = BrandLoadingStyle
```

### One style for every onboarding

Each parameter defaults to a `CompositionLocal`, so a style can also be set
once, above the onboarding:

```kotlin
CompositionLocalProvider(LocalProgressBarStyle provides ProgressBarStyle.Segmented) {
    OnboardingView(provider = provider, onComplete = { /* ... */ })
}
```

### Light and dark appearance

The built-in styles take their colors from `MaterialTheme`. For a pair of fixed
colors, use `themedColor`:

```kotlin
progressBarStyle = ProgressBarStyle.line(
    filledColor = themedColor(light = Indigo, dark = Mint),
)
```

`themedColor` follows the system's appearance (`isSystemInDarkTheme()`). An app
with its own light/dark switch picks the color from its own theme instead.

## System back

The system back, button and gesture, follows the back button of the bar:

| Step | System back |
|---|---|
| A later step with the back button shown | Returns to the previous step, with the predictive back animation on Android 14 and later. |
| A later step with `isBackButtonShown = false` | Does nothing: the step turned going back off. |
| The first step | Not handled by the onboarding: the system does what it does without it. |
| After `onComplete`, while the onboarding is still on screen | Does nothing on a later step. |

The predictive back animation needs the app to opt in when it targets Android 15
(API 35) or lower: set `android:enableOnBackInvokedCallback="true"` on the
`<application>` in the manifest. Without it back still works, without the preview.

While one step replaces another, taps are ignored until the transition ends,
so a double tap on Continue moves one step.

## Rotation, folding and resizing

When the activity is recreated, the onboarding stays where it was: the same
flow and the same step. The provider is not called again, and analytics gets no
second `Started`.

After the app's process was killed in the background, the onboarding starts
again from the first step.

## Window insets

The bar keeps clear of the status bar and of display cutouts at the top and on
the sides. A step's content gets all the room below the bar, up to the
window's edges. The bottom and side insets are the content's own to handle:

```kotlin
Column(Modifier.fillMaxSize().safeDrawingPadding()) { /* ... */ }
```

The top inset is already taken by the bar, so `safeDrawingPadding()` adds nothing at the top.

## Analytics

Implement `OnboardingAnalytics` and pass it to `OnboardingView`. Every event has
a ready `name` and `parameters` for your analytics SDK.

```kotlin
class AppAnalytics : OnboardingAnalytics {
    override fun track(event: OnboardingAnalyticsEvent) {
        Analytics.log(event.name, event.parameters)
    }
}

OnboardingView(provider = provider, onComplete = { /* ... */ }, analytics = AppAnalytics())
```

| Event | `name` |
|---|---|
| `OnboardingEvent.Started` | `onboarding_started` |
| `OnboardingEvent.StepShown` | `onboarding_step_shown` |
| `OnboardingEvent.StepContinued` | `onboarding_step_continued` |
| `OnboardingEvent.StepSkipped` | `onboarding_step_skipped` |
| `OnboardingEvent.StepBack` | `onboarding_step_back` |
| `OnboardingEvent.Completed` | `onboarding_completed` |

Step events carry `step_id`, `step_index` and `step_count`; every event carries
`flow_id` when the flow has one. Use `event.kind` to switch over the events
yourself. The names and parameters are the same as on iOS, so both funnels compare.

## Remote flows

`makeFlow()` is a suspending function, so the flow can come from remote config.
The provider must always return a flow: wait with your own timeout and fall
back to a built-in flow. Use `withTimeoutOrNull`, not `withTimeout`: a provider
that is cancelled from inside returns no flow, and the onboarding then completes
at once without showing a step.

```kotlin
class RemoteFlowProvider(private val config: RemoteConfig) : OnboardingFlowProvider {
    override suspend fun makeFlow(): OnboardingFlow {
        val variant = withTimeoutOrNull(2.seconds) { config.onboardingVariant() }
            ?: return defaultFlow
        val steps = variant.stepIds.mapNotNull(::makeStep) // drop unknown ids
        return if (steps.isEmpty()) defaultFlow else OnboardingFlow(steps, flowId = variant.name)
    }
}
```

## Step lifecycle

Implement `OnboardingStepLifecycle` when a step needs to react to becoming
current, for example to start a timer.

```kotlin
class PaywallStep(private val model: PaywallModel) : OnboardingStep, OnboardingStepLifecycle {
    override val id = "paywall"

    @Composable
    override fun Content() { PaywallScreen(model) }

    override fun didBecomeCurrent() { model.startTimer() }
    override fun didResignCurrent() { model.stopTimer() }
}
```

## Values for steps

| Value | What it is |
|---|---|
| `LocalOnboardingNext` | Moves to the next step (completes the flow on the last one). |
| `LocalOnboardingSkip` | Skips the step; moves like Next, reported as `StepSkipped`. |
| `LocalOnboardingStepInfo` | The step's `id`, `index` and `total`. |
| `LocalOnboardingFlowId` | The `flowId` of the current flow. |

## Localization

The built-in texts are string resources, in English:

| Resource | Text |
|---|---|
| `onboardingkit_skip` | Skip |
| `onboardingkit_back` | Back |
| `onboardingkit_loading` | Loading... |

To translate them, declare strings with the same names in your app's
`values-xx/strings.xml`. For other wording, provide your own style.

## Differences from the iOS version

| iOS | Android |
|---|---|
| View modifiers: `.progressBarStyle(...)` | Parameters of `OnboardingView`, or `Local…` providers |
| `.line`, `.segmented`, `.automatic` | `ProgressBarStyle.Line`, `ProgressBarStyle.Segmented`, `SkipButtonStyle.Automatic` |
| `Color(light:dark:)` | `themedColor(light, dark)` |
| `flowID` | `flowId` |
| Native back button and swipe back | System back button and gesture |
| `skipButtonPrefersSideBar()` for iPhone Duo | Not available: Android has no system side bar |
| A step's content extends under the navigation bar | A step's content starts below the bar |
| Built-in texts cannot be translated without a custom style | Built-in texts are string resources the app can override |

## License

MIT. See [LICENSE](LICENSE).
