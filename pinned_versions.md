# Pinned version matrix — ProductivityApp

Generated: 2026-04-07

Purpose: pin critical toolchain/library versions to ensure CI reproducible builds. This file should be updated whenever you intentionally change core tooling versions.

Core
- Android Gradle Plugin (AGP): 9.1.0
- Gradle Wrapper: use the Gradle version compatible with AGP 9.1.0 (check AGP release notes). Pin in `gradle/wrapper/gradle-wrapper.properties`.
- Kotlin: 2.3.20
- KSP (Kotlin Symbol Processing): 2.3.6

Room / Persistence
- Room runtime/ktx: 2.8.4
- Room compiler (KSP): 2.8.4
- DataStore Preferences: 1.1.1
- AndroidX Security Crypto: 1.1.0

Compose
- Compose BOM: 2026.02.01 (use BOM to align compose artifacts)
- Activity Compose: 1.8.0

Coroutines & Testing
- Kotlinx Coroutines: 1.7.3
- kotlinx-coroutines-test: 1.7.3
- JUnit: 4.13.2
- Robolectric: 4.11
- AndroidX Test core: 1.5.0

Maps & Location
- OSMdroid: 6.1.18
- Play Services Location: 21.0.1

Work & Permissions
- WorkManager: 2.9.1
- Accompanist Permissions: 0.34.0

Notes and guidance
- KSP decoupling (2026): although KSP is no longer strictly tied to the Kotlin compiler version, prefer to pin a KSP version that is known compatible with your Kotlin version. We pin KSP `2.3.6` alongside Kotlin `2.3.20`.
- Keep Room runtime and Room compiler on the same major/minor version (we pin both to 2.8.4).
- When upgrading AGP/Kotlin, update this file and run a full CI build (including Robolectric tests) before merging.

Suggested CI checks
- Gradle sync + build (`./gradlew assembleDebug`)
- Run JVM unit tests (`./gradlew :app:testDebugUnitTest`)
- Run instrumented tests on emulator (`./gradlew :app:connectedAndroidTest`) — optional
- Validate generated KSP sources are present under `build/generated/ksp/` and included in classpath for tests.

How to update
- Bump the version here and in `gradle/libs.versions.toml` together, then run CI.


