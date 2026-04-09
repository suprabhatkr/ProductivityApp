# ProductivityApp — Test Guide

This file documents how to run the project's test suite locally and in CI, what tests are present, and troubleshooting tips.

Status (2026-04-09)
- Unit / Robolectric JVM tests for repositories: present under `app/src/test/java/.../data/repository/impl/` (Step/Run/Sleep).
- Preferences DataStore unit test: `app/src/test/java/.../datastore/DataStoreUnitTest.kt` (temporary-file based).
- Instrumentation tests (androidTest) exist for some repository integrations (in-memory Room).
- Step feature tests added:
  - `app/src/test/java/.../service/StepCounterServiceUnitTest.kt` — lifecycle, batching, rollover, graceful no-sensor behavior.
  - `app/src/androidTest/java/.../ui/steps/StepScreenContentTest.kt` — content-level Compose checks for sensor absence and permission fallback UI.
- Run feature replay test added:
  - `app/src/test/java/.../run/RunReplayHelperUnitTest.kt` — replay timeline generation and timestamp behavior.
- Sleep feature tests added:
  - `app/src/test/java/.../viewmodel/SleepViewModelTest.kt` — session flow, pause/resume/stop, review persistence, weekly summary aggregation.
  - `app/src/test/java/.../data/repository/impl/SleepRepositoryUnitTest.kt` — active-session and weekly-range repository behavior.
- Settings/profile tests added:
  - `app/src/test/java/.../viewmodel/SettingsViewModelTest.kt` — profile load/save/reset validation against a fake `UserProfileRepository`.
- Step goal propagation test added:
  - `app/src/test/java/.../viewmodel/StepViewModelTest.kt` — verifies the saved daily step goal flows from `UserProfileRepository` into step UI state.

Table of contents
- Running tests locally
- Running instrumentation tests (device/emulator)
- Interpreting test reports
- CI recommendations (GitHub Actions snippet)
- Troubleshooting & common fixes

- Migration: After upgrading the DB schema (v1 -> v2) we add a `run_points` table. A runtime helper
  `DatabaseProvider.migratePolylinesIfNeeded(context)` is available to populate `run_points` from
  existing encoded or CSV-style polyline strings. This is now automatically bootstrapped one time from
  `DatabaseProvider.getInstance(context)`, so legacy runs are backfilled without manual calls.

Running tests locally (JVM unit tests)

Run all JVM unit tests (fast, CI-friendly):

```bash
./gradlew :app:testDebugUnitTest --no-daemon --console=plain
```

Run a single test class (example):

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.productivityapp.data.repository.impl.StepRepositoryUnitTest" --no-daemon
```

Run a single test method (example):

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.productivityapp.data.repository.impl.StepRepositoryUnitTest.testIncrementAndReset" --no-daemon
```

Notes about the DataStore unit test
- The DataStore unit test uses a temporary file and creates its own CoroutineScope for PreferenceDataStoreFactory. The test cancels the scope/job at the end to avoid leaking coroutines (this is important when using `kotlinx.coroutines.test.runTest`).
- If you add more DataStore tests, use a temporary directory (`@Rule TemporaryFolder`) or JUnit `@TempDir` to avoid interfering with real user files.

Running instrumentation tests (device or emulator)

These require an Android device or emulator connected.

```bash
./gradlew :app:connectedAndroidTest
```

Or run a specific instrumentation test:

```bash
./gradlew :app:connectedAndroidTest --tests "com.example.productivityapp.data.repository.StepRepositoryTest"
```

Interpreting test reports

- JVM test HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`
- JVM test raw results: `app/build/test-results/testDebugUnitTest/`
- Lint report (useful for manifest/permission issues): `app/build/reports/lint-results-debug.html`

CI recommendations (example GitHub Actions job)

Create `.github/workflows/android-unit-tests.yml` and add a job that:
- checks out the repo
- sets up JDK (11)
- caches Gradle and dependency artifacts
- runs `./gradlew :app:testDebugUnitTest --no-daemon --console=plain`

Minimal workflow snippet:

```yaml
name: Android JVM Unit Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 11
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '11'
      - name: Run JVM tests
        run: ./gradlew :app:testDebugUnitTest --no-daemon --console=plain
```

Troubleshooting & common fixes

- Robolectric manifest warnings: Robolectric may warn about the Android manifest. Add `@Config(manifest = Config.NONE)` to tests that do not require manifest features, or provide a test manifest if needed.
- KSP-generated sources in tests: ensure KSP's generated sources are available to the test classpath. CI should run the same Gradle command as locally so the `ksp` tasks run and generate sources under `build/generated/ksp/debugUnitTest`.
- DataStore tests: if you see `UncompletedCoroutinesError`, ensure any custom DataStore CoroutineScope is cancelled at the end of the test and use `kotlinx.coroutines.test.runTest` correctly.
- EncryptedSharedPreferences / MasterKey warnings: `androidx.security` APIs may show deprecation warnings depending on the version of the library; these are warnings only. Consider migrating to encrypted Proto DataStore for profile storage for stronger typing and testability.
- Lint errors (example: foregroundServiceType=location): follow plan notes to add `uses-permission android.permission.FOREGROUND_SERVICE_LOCATION` in the manifest; run `./gradlew :app:lintDebug` to reproduce.
- Step sensor tests: `StepCounterServiceUnitTest` uses Robolectric + an in-memory Room DB via `DatabaseProvider.setTestInstance(...)`. If a batching assertion fails, check pending-step thresholds and flush timing in `StepCounterService`.
- Compose content tests: `StepScreenContentTest` intentionally tests the extracted `StepScreenContent(...)` composable rather than the full permission API wiring to keep tests deterministic and emulator-free during compilation.

Adding more tests

- Repository tests: add more unit tests under `app/src/test/java/...` and follow the same Robolectric + Room.inMemoryDatabaseBuilder pattern.
- DataStore tests: use temporary directories and cancel background scopes.
- ViewModel tests: mock repository interfaces (use `mockito-kotlin` or `mockk`) and test state flows using `kotlinx-coroutines-test`.
- Settings ViewModel tests: validate form hydration, numeric-field validation, save behavior, and reset behavior without needing Android framework dependencies.
- Service tests: prefer small deterministic hooks (`@VisibleForTesting`) over reflection when asserting lifecycle state or notification contents.
- Sleep ViewModel tests: avoid `advanceUntilIdle()` on repeating timers; prefer `runCurrent()` / controlled scheduler advancement when the ViewModel owns a perpetual timer job.

If you want, I can add the GitHub Actions workflow file now or add more unit tests (for `UserDataStore.observeUserProfile` + `updateUserProfile`) — tell me which you'd prefer.

End of TESTING.md

