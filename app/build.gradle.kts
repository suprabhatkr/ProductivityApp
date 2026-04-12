plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.productivityapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.productivityapp"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.datastore:datastore-core:${libs.versions.datastorePreferences.get()}")
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation("com.google.protobuf:protobuf-javalite:${libs.versions.protobuf.get()}")
    ksp(libs.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.osmdroid)
    implementation(libs.play.services.location)
    implementation(libs.security.crypto)
    implementation(libs.accompanist.permissions)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Compose UI test APIs for JVM unit tests
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // (Optional) Paparazzi could be added here if desired and available in repositories.
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}


// Simple verification task to prevent accidental hard-coded route string usage.
// Developers should use `com.example.productivityapp.navigation.AppRoutes` instead of
// calling `navController.navigate("home")` or `composable("home")` with raw strings.
tasks.register("verifyNoHardcodedRoutes") {
    group = "verification"
    description = "Fail build if hard-coded navigation route string literals are present (use AppRoutes)."
    doLast {
        val projectDir = project.projectDir
        val srcFiles = fileTree("src") { include("**/*.kt") }
        val pattern = Regex("\\b(navController\\.navigate|composable)\\(\\s*\"(home|steps|run|sleep|water|settings)\"\\s*\\)")
        val violations = mutableListOf<String>()
        srcFiles.files.forEach { f ->
            val path = f.absolutePath
            // skip our AppRoutes and legacy markers
            if (path.contains("${File.separator}navigation${File.separator}AppRoutes.kt") || path.contains("${File.separator}app${File.separator}ui${File.separator}legacy")) return@forEach
            val text = f.readText()
            pattern.findAll(text).forEach { m ->
                violations.add("${f.relativeTo(projectDir)}: ${m.value}")
            }
        }
        if (violations.isNotEmpty()) {
            throw org.gradle.api.GradleException("Hard-coded navigation routes found:\n" + violations.joinToString("\n"))
        }
    }
}

// Run the verification before any Kotlin compilation tasks to catch issues early.
tasks.matching { it.name.endsWith("Kotlin") && it.name.startsWith("compile") }.configureEach {
    dependsOn(tasks.named("verifyNoHardcodedRoutes"))
}


