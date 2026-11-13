plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
}

// Read the OpenAI API key from local.properties (gitignored) so it never lands
// in version control. Absent key -> empty string; the app detects this and
// treats AI scene analysis as unavailable. Parsed by hand to avoid java.util
// resolution quirks in the Gradle Kotlin DSL classpath.
val openAiApiKey: String = run {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return@run ""
    f.readLines()
        .firstOrNull { it.trimStart().startsWith("OPENAI_API_KEY") && it.contains("=") }
        ?.substringAfter("=")
        ?.trim()
        ?: ""
}

android {
    namespace = "com.frame.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.frame.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Exposed to code as BuildConfig.OPENAI_API_KEY. Quoted string literal.
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Several test/library jars ship their own copies of these metadata
            // files; the packager can't merge duplicates, so we drop them.
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/DEPENDENCIES",
                "/META-INF/*.kotlin_module",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose (BOM-managed versions)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Pose Detection (wired in Phase 3; declared now for the seam)
    implementation(libs.mlkit.pose.detection)

    // Room (scaffold only in Phase 1)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // DataStore (overlay/mode preferences)
    implementation(libs.androidx.datastore.preferences)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Networking + JSON (OpenAI scene analyzer)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // Unit testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android) {
        // mockk-android drags in JUnit 5 (jupiter) transitively, which collides
        // on the androidTest classpath (our tests use JUnit 4). Keep it out.
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
    }
    androidTestImplementation(libs.androidx.room.testing)
}
