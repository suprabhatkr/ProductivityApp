# ProductivityApp Copilot Instructions

## Build, test, and lint commands

- `./gradlew :app:assembleDebug` builds the main debug APK.
- `./gradlew :app:compileDebugKotlin` is the fastest compile-only verification path and also triggers the route-string guard.
- `./gradlew :app:lintDebug` runs Android lint for the debug variant.
- `./gradlew :app:testDebugUnitTest --no-daemon --console=plain` runs the full JVM/Robolectric suite.
- `./gradlew :app:testDebugUnitTest --tests "com.example.productivityapp.data.repository.impl.StepRepositoryUnitTest" --no-daemon` runs a single JVM test class.
- `./gradlew :app:testDebugUnitTest --tests "com.example.productivityapp.data.repository.impl.StepRepositoryUnitTest.testIncrementAndReset" --no-daemon` runs a single JVM test method.
- `./gradlew :app:connectedAndroidTest` runs instrumentation tests on a connected device or emulator.
- `./gradlew :app:connectedAndroidTest --tests "com.example.productivityapp.data.repository.StepRepositoryTest"` runs a single instrumentation test.

Gradle uses the wrapper in this repo and the daemon JVM is pinned to Java 21 in `gradle/gradle-daemon-jvm.properties`.

## High-level architecture

- This is a single-module, single-activity Android app. `MainActivity` schedules `MidnightResetWorker` and `SleepMaintenanceWorker`, then hosts one Compose `NavHost` whose route contract lives in `navigation/AppRoutes.kt`.
- The UI currently spans two trees. `com.example.productivityapp.app.ui.*` contains the newer app shell pieces such as the dashboard and water screen, while `com.example.productivityapp.ui.*` contains the live feature screens for steps, run, sleep, settings, and debug overlays. `AppRoutes.STEPS` points at the newer ring-style steps UI, while `AppRoutes.STEPS_LEGACY` keeps the older step screen reachable.
- Room is the persistence backbone for activity data. `AppDatabase` stores steps, timestamped step samples, runs, normalized run points, and sleep sessions. `DatabaseProvider` owns schema migrations and also performs a one-time runtime backfill from legacy `runs.polyline` data into `run_points`.
- `RepositoryProvider` is the central wiring point for the app. Screens, services, and workers ask it for repositories and helpers instead of using a DI framework. It also assembles the secure profile migration stack and the run replay exporter.
- Background components are part of normal feature flow, not edge utilities. `StepCounterService` writes batched sensor deltas into the step repository, `RunTrackingService` records location points and updates the active run, `SleepMaintenanceWorker` auto-detects and finalizes sleep around user profile boundaries, and `BootCompleteReceiver` re-schedules the reset/sleep workers after reboot.
- Water and profile data are handled separately from Room. `UserDataStore` manages per-day water entries in Proto DataStore, while user profile reads and writes go through `UserProfileRepository`; the concrete implementation is a hybrid `SecureAwareUserProfileRepository` that mirrors legacy storage and the encrypted proto-backed store during migration.

## Key conventions

- Never hard-code route names in `navController.navigate(...)` or `composable(...)`. Add or change routes in `AppRoutes` and use those constants everywhere. The custom `verifyNoHardcodedRoutes` Gradle task runs before Kotlin compilation and fails the build on raw route strings.
- Follow the existing lightweight dependency pattern: Compose screens usually call `RepositoryProvider.provide...(...)` and create their view models with explicit `*ViewModelFactory` classes. Keep new feature wiring consistent with that pattern instead of introducing a second DI style.
- Preserve the UI-to-service handshake for long-running tracking features. `UiStateStore` stores whether the steps or run UI is actively tracking so the UI can reconnect cleanly after process recreation.
- For service and repository tests, reuse the existing test seams: JVM/Robolectric tests commonly use `Room.inMemoryDatabaseBuilder(...)` plus `DatabaseProvider.setTestInstance(...)`, and services expose `@VisibleForTesting` hooks instead of relying on reflection-heavy tests.
- Compose UI tests often target extracted `*ScreenContent(...)` composables instead of the full permission/service stack. Keep that split when adding tests so UI coverage stays deterministic and fast.
- Treat `UserProfileRepository` as the stable boundary for profile changes. Avoid reading or writing profile data directly through `UserDataStore`, or you can bypass the secure-store migration and fallback logic.

## Where to look for more context

- TESTING.md (root) — detailed test commands, troubleshooting, and CI snippets.
- UI_WORKFLOW_ARCHITECTURE_AUDIT.md — design notes for UI components and workflow.
- plan.md / run_plan.md — project plans and migration notes (useful for understanding recent large changes).

## AI assistant and config files

- No additional AI assistant config files (e.g., CLAUDE.md, AGENTS.md, .cursorrules, .windsurfrules) were found during inspection. If such files are added, include their relevant guidance here so Copilot sessions can incorporate that input.

## If you want

- Add a recommended GitHub Actions workflow for JVM tests: .github/workflows/android-unit-tests.yml (I can add it).
- Add short command snippets for common developer tasks (DB backfill, running a single service test, etc.).

--
Generated for repository: suprabhatkr/ProductivityApp
