plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "appteam.onboardingkit"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    // Keeps the kit's resources from clashing with the app's.
    resourcePrefix = "onboardingkit_"

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        // What apps get: the release build, with sources for documentation in the IDE.
        singleVariant("release") {
            withSourcesJar()
        }
    }

    testOptions {
        unitTests {
            // Robolectric reads the kit's strings and the test manifest.
            isIncludeAndroidResources = true
            all {
                // Robolectric reaches into JDK internals that newer JDKs no longer export.
                it.jvmArgs(
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--enable-native-access=ALL-UNNAMED",
                )
            }
        }
    }
}

kotlin {
    // Every declaration states `public` or `internal`, as the Swift sources do.
    explicitApi()
}

publishing {
    publications {
        register<MavenPublication>("release") {
            // JitPack serves it under the GitHub repository's own group; these apply to local use.
            groupId = "appteam"
            artifactId = "onboardingkit"
            version = providers.gradleProperty("onboardingkit.version").get()

            afterEvaluate { from(components["release"]) }
        }
    }
}

dependencies {
    // `api`: Compose types appear in the kit's public API.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)

    // Keeps the loaded flow across activity recreation (rotation, folding).
    implementation(libs.androidx.compose.runtime.retain)
    // The system back button and gesture.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // Newer than the one Compose's test library brings: that one fails on current SDKs.
    testImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
