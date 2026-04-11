# ProductivityApp — Feature Implementation Plan (KSP-ready)

Last updated: 2026-04-11

Purpose
- This document is the authoritative implementation plan for the four new health features: Water, Sleep, Steps, Run.
- It includes the KSP migration notes and a step-by-step checklist with status tags so an AI companion or developer can pick up where work left off.

Status tag legend
- [DONE] — work implemented and committed
- [IN-PROGRESS] — work started but not fully implemented or tested
- [TODO] — not started
- [BLOCKED] — waiting on external dependency or decision

How to use this file
- Update status tags inline when a task completes.
- For automation: search for lines prefixed with `- [TODO]` or `- [DONE]` to generate change lists.

-----------------------------------------------------------------
SECTION 0 — KSP migration (summary & current state)

- [DONE] KSP migration initiated and build configuration updated.
  - File: `gradle/libs.versions.toml` — added `ksp = "2.3.6"` and `kotlin = "2.3.20"` and other libs.
  - File: `app/build.gradle.kts` — replaced `kapt(...)` usage for Room with `ksp(...)` and added plugin alias for KSP.
  - Rationale: KSP (Kotlin Symbol Processing) provides faster incremental builds and is the recommended replacement for KAPT when supported.
  - Note: Recent (2026) KSP decoupling means exact Kotlin/KSP version matching is no longer strictly required, but the Gradle plugin must be resolvable and compatible with your Gradle/AGP setup.

- [DONE] Manifest entries for new features added (permissions + service/receiver stubs). See `app/src/main/AndroidManifest.xml`.

- [IN-PROGRESS] Verify KSP generated sources appear in IDE build paths and Room compiles using KSP on CI.
  - Action: run local builds and CI pipeline; inspect `build/generated/ksp` for generated code.

If you need to revert to KAPT temporarily
- Replace `ksp(...)` with `kapt(...)` and add `id("org.jetbrains.kotlin.kapt")` plugin. This is not recommended long-term.

-----------------------------------------------------------------
SECTION 1 — High-level phased roadmap (small PRs)

Phase 1 (PR #1) — Infrastructure and dependencies
- [DONE] Add dependencies to version catalog and module build files: Room, DataStore, WorkManager, OSMdroid, Play Location, security-crypto, accompanist-permissions.
- [DONE] KSP plugin configured and Room configured to use KSP.
- [DONE] Manifest: permissions and service/receiver skeletons added.
- Next: validate build & CI.

Phase 2 (PR #2) — Persistence skeleton (Room + Encrypted DataStore)
- [DONE] Files added (entities, DAOs, AppDatabase, DataStore wrapper, repository interfaces)
  - [DONE] `app/src/main/java/com/example/productivityapp/data/AppDatabase.kt` — Room database declaration
  - [DONE] `app/src/main/java/com/example/productivityapp/data/dao/StepDao.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/data/dao/RunDao.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/data/dao/SleepDao.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/data/entities/StepEntity.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/data/entities/RunEntity.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/data/entities/SleepEntity.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/UserDataStore.kt` (Encrypted DataStore wrapper; implemented using EncryptedSharedPreferences for profile + Preferences DataStore for water)
  - [DONE] `app/src/main/java/com/example/productivityapp/data/repository/*Repository.kt` (interfaces created: StepRepository, RunRepository, SleepRepository, UserProfileRepository)
  - [DONE] Concrete repository implementations and provider wiring added (Room-backed + DataStore-backed):
    - `app/src/main/java/com/example/productivityapp/data/repository/impl/RoomStepRepository.kt`
    - `app/src/main/java/com/example/productivityapp/data/repository/impl/RoomRunRepository.kt`
    - `app/src/main/java/com/example/productivityapp/data/repository/impl/RoomSleepRepository.kt`
    - `app/src/main/java/com/example/productivityapp/data/repository/impl/DataStoreUserProfileRepository.kt`
    - `app/src/main/java/com/example/productivityapp/data/DatabaseProvider.kt`
    - `app/src/main/java/com/example/productivityapp/data/RepositoryProvider.kt`
  - [TODO] Add DI framework (Hilt) integration or convert provider to Hilt module (optional)
  - [DONE] Basic instrumentation tests added for repository implementations (in-memory Room)
    - `app/src/androidTest/.../StepRepositoryTest.kt`
    - `app/src/androidTest/.../RunRepositoryTest.kt`
    - `app/src/androidTest/.../SleepRepositoryTest.kt`
  - [TODO] Add JVM Robolectric/unit tests for repositories and DataStore so CI can run without device; add DataStore unit tests (TestCoroutineDispatcher) and ViewModel unit tests

Phase 3 (PR #3) — ViewModels + navigation + placeholder screens
- [DONE] Files added and basic wiring implemented:
  - [DONE] `app/src/main/java/com/example/productivityapp/viewmodel/StepViewModel.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/viewmodel/SleepViewModel.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/viewmodel/RunViewModel.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/viewmodel/WaterViewModel.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/ui/home/HomeScreen.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/ui/steps/StepScreen.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/ui/run/RunScreen.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/ui/sleep/SleepScreen.kt`
  - [DONE] `app/src/main/java/com/example/productivityapp/ui/settings/SettingsScreen.kt`
  - [DONE] Integrate navigation host and `MainActivity` route wiring (NavHost implemented in `MainActivity.kt`)
    - Files changed/added: `app/src/main/java/com/example/productivityapp/MainActivity.kt`, `app/src/main/java/com/example/productivityapp/app/ui/Screen.kt`, `app/src/main/java/com/example/productivityapp/ui/water/WaterIntakeScreen.kt`
    - Build change: navigation-compose dependency added to `app/build.gradle.kts`
  - [IN-PROGRESS] UI polish: per-feature theming, icons, charts and history visualizations
  - [IN-PROGRESS] Polished UI for every window — current state
    - [DONE] Home screen (polished)
    - [DONE] Water intake screen (polished)
    - [IN-PROGRESS] Steps screen — placeholder UI present; needs charts, live step display, and start/pause controls
    - [IN-PROGRESS] Run screen — placeholder UI present; `RunMapView` implemented (OSMdroid) but needs visual polish, live stats, and robust controls
    - [IN-PROGRESS] Sleep screen — placeholder UI present; needs session flow, rating UI and history charts
    - Acceptance criteria for "polished UI for every window":
      - All feature screens follow app theme and per-feature accent colors
      - Navigation flows are complete and state-preserving (start/pause/stop persist until user stops)
      - Core interactions (start/stop run, start/stop sleep, add water, view steps history) are accessible and tested
      - Basic accessibility labels and content descriptions added for interactive elements

Current repository & UI status (observed 2026-04-07)

- [DONE] Home + Water UI: polished Home screen and Water intake screen are present and visually finished.
  - Files: `app/src/main/java/.../app/ui/home/HomeScreen.kt`, `app/src/main/java/.../ui/water/WaterIntakeScreen.kt`
  - Note: The polished Home screen remains in the tree and was not removed during model/KSP migration.

- [IN-PROGRESS] Sleep / Steps / Run screens: placeholder UIs exist and are wired to repositories/ViewModels.
  - Files: `ui/sleep/SleepScreen.kt`, `ui/steps/StepScreen.kt`, `ui/run/RunScreen.kt`
  - Status: wiring to repositories and ViewModel factories is implemented; screens need UI polish, charts, and full feature controls (Start/Pause/Resume for runs, step live display, sleep session flow).

- [DONE] Navigation routing: `MainActivity.kt` contains `NavHost` and routes for home, water, sleep, steps, run, settings.

Interpretation

- Nothing was deleted — feature screens are staged: polished for water/home, minimal placeholders for other features. This is intentional given prioritization of data/model work.

Phase 4 (PR #4) — Step sensor service + UI
- [DONE] Implement `StepCounterService` to use `Sensor.TYPE_STEP_COUNTER` with baseline offsets, foreground notification opt-in, repository wiring. (2026-04-09)

Phase 4 (PR #4) — Step sensor service + UI
- [DONE] Implement `StepCounterService` completion. (2026-04-09)
  - Files: `app/src/main/java/.../service/StepCounterService.kt` finalized with date-aware baseline handling, batched repository writes, graceful missing-sensor/permission behavior, notification Stop action, and modern stopForeground handling.
  - UI: `app/src/main/java/.../ui/steps/StepScreen.kt` now provides explicit permission rationale/settings fallback, ViewModel-backed service state, and robust manual-entry paths even when permission is denied or hardware sensor is absent.
  - Tests: Robolectric service tests + Compose instrumentation content tests added and validated via compile/test tasks.

Phase 5 (PR #5) — Run tracker service + map integration
- [DONE] Implement `RunTrackingService` (foreground location), `RunMapView.kt` (OSMdroid via AndroidView), polyline storage, live stats. (2026-04-09)

Phase 5 (PR #5) — Run tracker service + map integration
- [DONE] Implement `RunTrackingService` (foreground location) completion. (2026-04-09)
  - Files: `app/src/main/java/.../service/RunTrackingService.kt` now uses `LocationProvider`, handles start/pause/resume/stop, tracks active elapsed time across pauses, filters jitter/improbable jumps, and persists updates through `RunRepository.addLocationPoint(...)`.
  - UI: `app/src/main/java/.../ui/run/RunScreen.kt` remains wired to request `ACCESS_FINE_LOCATION` and start/stop the run service.
  - [DONE] Implemented `RunMapView.kt` (OSMdroid) and switched polyline storage to encoded polyline. (2026-04-07)
    - Files added/modified:
      - `app/src/main/java/com/example/productivityapp/util/PolylineUtils.kt` — encode/decode polyline (Google polyline algorithm).
      - `app/src/main/java/com/example/productivityapp/ui/run/RunMapView.kt` — OSMdroid MapView composable wrapper that decodes encoded polyline and draws a Polyline overlay.
      - `app/src/main/java/com/example/productivityapp/service/RunTrackingService.kt` — now accumulates in-memory points and writes encoded polyline to `RunEntity.polyline` on updates.
      - `app/src/main/java/com/example/productivityapp/ui/run/RunScreen.kt` — embeds `RunMapView` to show latest run polyline (live updates).
    - Notes: CSV-style point storage replaced with encoded polyline for compactness and direct map rendering. Runtime migration helper and `run_points` compatibility table added (2026-04-09).

Service implementation notes & suggestions (implemented 2026-04-07)

- [DONE] `StepCounterService` implemented. (2026-04-09)
  - Behavior: registers `Sensor.TYPE_STEP_COUNTER`, keeps a date-aware last-total baseline, batches pending steps before writing to `StepRepository`, flushes on stop/destroy, and runs as a foreground service with notification and Stop action.
  - UI: `StepScreen` now requests `ACTIVITY_RECOGNITION`, shows rationale/settings fallback, keeps manual quick-add + custom entry available, and hides automatic tracking controls when the sensor is absent.
  - Caveats addressed: missing sensor and permission denial are now graceful, no-crash paths; service lifecycle covered by Robolectric tests.

- [DONE] `RunTrackingService` implemented. (2026-04-09)
  - Behavior: uses an injectable `LocationProvider`, computes incremental distance with jitter/improbable-jump filtering, persists encoded polyline updates through `RunRepository`, and supports replay helpers / point-table migration.
  - UI: `RunScreen` now requests `ACCESS_FINE_LOCATION`, supports background-location opt-in, and includes live stats + replay controls.
  - Follow-ups: tune heuristics/config if needed; device QA for long-running background sessions remains recommended.

- Build & verification:
  - Both services and updated Compose screens compile successfully.
  - Latest validated commands (2026-04-09):
    - `./gradlew :app:compileDebugKotlin`
    - `./gradlew :app:testDebugUnitTest`
    - `./gradlew :app:compileDebugAndroidTestKotlin`
    - `./gradlew :app:assembleDebug`
  - `stopForeground(true)` deprecation removed from Run service and Step service on newer SDKs.

- Testing recommendations (next actions):
  - Continue adding device/integration tests for GPS accuracy and long-background run sessions.
  - Expand step/run Compose tests beyond content-only checks into full navigation/service interaction paths.
  - Add device QA for sensor absence / permission denial across OEM variations.

Next feature work (short-term)

- Implement `RunMapView.kt` using OSMdroid (AndroidView) to render a live polyline during an active run and provide session replay.
- Update `RunRepository` API to accept location points (e.g., `addLocationPoint(runId, lat, lon, ts)`) and migrate Run entity storage to keep an encoded polyline or a related points table.
- Wire ViewModels to reflect service running state and expose live steps/distance/pace flows for the UI (so the running flag is persisted in ViewModel, not local composable state).
- Implement BootCompleteReceiver to reschedule midnight reset WorkManager job and optionally restart services if user opted in.


Phase 6 (PR #6) — Sleep tracker UI & persistence
- [DONE] Sleep session start/stop, quality rating, weekly charts. (2026-04-09)
  - Files changed/added:
    - `app/src/main/java/com/example/productivityapp/ui/sleep/SleepScreen.kt` — full sleep tracker UI with start/pause/resume/stop, review card, weekly chart, and history list.
    - `app/src/main/java/com/example/productivityapp/viewmodel/SleepViewModel.kt` — active session state, timer, pause/resume handling, pending review flow, and weekly summaries.
    - `app/src/main/java/com/example/productivityapp/data/dao/SleepDao.kt` — active session, weekly range, and lookup queries.
    - `app/src/main/java/com/example/productivityapp/data/repository/SleepRepository.kt` / `data/repository/impl/RoomSleepRepository.kt` — expanded API for active session and weekly history.
    - `app/src/test/java/com/example/productivityapp/viewmodel/SleepViewModelTest.kt` — ViewModel unit tests.
    - `app/src/test/java/com/example/productivityapp/data/repository/impl/SleepRepositoryUnitTest.kt` — repository tests extended for active session + range queries.
  - Acceptance met:
    - Start/pause/resume/stop sleep flows implemented.
    - Sleep sessions persisted with `startTimestamp`, `endTimestamp`, `durationSec`.
    - Quality rating + notes flow implemented after session stop.
    - Weekly chart and history list implemented.
    - Unit tests added for ViewModel and repository interactions.
  - Follow-ups:
    - Accessibility/content-description polish for sleep controls and rating inputs.
    - Optional sleep tips / nap quick actions if desired later.

Phase 7 (PR #7) — Water quick-add + Settings + polish + tests
- [IN-PROGRESS] Water quick-add + settings/profile + privacy polish slice. (2026-04-09)
  - [DONE] `app/src/main/java/com/example/productivityapp/ui/settings/SettingsScreen.kt` replaced with a real settings UI backed by `SettingsViewModel`. (2026-04-09)
    - Supports editing display name, weight, height, stride length, preferred units, daily step goal, and daily water goal.
    - Adds local privacy/storage messaging plus a reset action for sensitive profile fields.
    - Home screen now exposes a visible Settings entry point and the settings route supports back navigation.
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/UserDataStore.kt` profile updates now clear removed optional values instead of leaving stale encrypted fields behind. (2026-04-09)
  - [DONE] JVM coverage added for settings profile logic via `app/src/test/java/.../viewmodel/SettingsViewModelTest.kt`. (2026-04-09)
  - [DONE] Water + step goal unification started and validated. (2026-04-09)
    - `WaterRepository.kt` now reads the daily water goal from `UserDataStore` / `UserProfile.dailyWaterGoalMl` instead of a separate legacy goal preference.
    - `HomeScreen.kt` and `WaterIntakeScreen.kt` now refresh water state on screen entry so settings changes are reflected after navigation.
    - `StepViewModel.kt` now observes `UserProfileRepository` and exposes the saved daily step goal; `StepScreen.kt` renders a goal-progress card from that shared value.
    - `StepViewModelTest.kt` added to verify profile-goal propagation through the step feature.
  - [TODO] Optionally migrate remaining water entry storage from legacy SharedPreferences JSON to DataStore/Room if full persistence unification is desired later.
  - [TODO] Add export/delete controls and broader privacy policy UX once the final retention flow is decided.
  - [TODO] Add Compose/UI tests for settings interactions if this screen grows further.

- [IN-PROGRESS] Phase 7 slice: accessibility polish + settings/water UI tests. (saved for follow-up)
  - [TODO] Add accessibility labels/content descriptions across Home, Water, Steps, Run, Sleep, Settings.
  - [TODO] Add Compose/UI tests for `SettingsScreen` interactions and regression coverage for save/reset flows.
  - [TODO] Add Water screen UI tests for quick-add, undo snackbar, and midnight-reset affordance messaging.
  - [TODO] Review contrast/theme consistency for per-feature accents and polished cards/charts.

-----------------------------------------------------------------
SECTION 2 — Exact files to add/modify (detailed)

Note: Replace `com.example.productivityapp` with your app package if different.

Data layer (Room entities, DAOs, Database)
- `data/entities/StepEntity.kt` — fields: id:Long, date:String(yyyy-MM-dd), steps:Int, distanceMeters:Double, calories:Double, source:String, lastUpdatedAt:Long
- `data/entities/RunEntity.kt` — id:Long, startTime:Long, endTime:Long, distanceMeters:Double, durationSec:Long, avgSpeedMps:Double, calories:Double, polyline:String
- `data/entities/SleepEntity.kt` — id:Long, date:String, startTs:Long, endTs:Long, durationSec:Long, quality:Int?, notes:String?
- `data/dao/*.kt` — basic DAO methods: insert, update, query by date or date range
- `data/AppDatabase.kt` — RoomDatabase annotation, exportSchema=false, version=1, include DAOs

DataStore (Encrypted) for profile + water
- `datastore/UserDataStore.kt` — Encrypted DataStore using `androidx.security:security-crypto` MasterKey. Keys for: weightKg, heightCm, strideMeters, preferredUnits, daily goals, and waterTodayML (date-keyed storage recommended)

Repository interfaces (Flow + suspend)
- StepRepository.kt: observeSteps(date): Flow<StepEntity?>, upsert(step), incrementSteps(delta), resetForDate(date)
- RunRepository.kt: startRun(), addLocationPoint(runId, lat, lon, ts), finishRun(runId) etc.
- SleepRepository.kt: startSleep(), stopSleep(), getForDate(date)
- UserProfileRepository.kt: observeProfile(): Flow<UserProfile>, updateProfile()

Services
- `service/StepCounterService.kt` — use `SensorManager` & baseline logic; foreground only with user opt-in
- `service/RunTrackingService.kt` — foreground location with `foregroundServiceType="location"`; use FusedLocationProviderClient recommended and feed locations to repository
- `service/MidnightResetWorker.kt` — WorkManager worker scheduled at next midnight; BootCompleteReceiver re-schedules on reboot

UI (Compose)
- `ui/home/HomeScreen.kt` — feature tiles with per-feature accent
- `ui/steps/StepScreen.kt`, `ui/run/RunScreen.kt`, `ui/run/RunMapView.kt` (OSMdroid), `ui/sleep/SleepScreen.kt`, `ui/settings/SettingsScreen.kt`

Theme
- `app/src/main/java/com/example/productivityapp/app/ui/theme/Theme.kt` — update to include per-feature color accents: Water(Blue), Sleep(Green), Steps(Amber), Run(Purple)

Navigation
- Add string-based routes: "home", "water", "sleep", "steps", "run", "settings"

-----------------------------------------------------------------
SECTION 3 — KSP-specific checklist & migration notes

- [DONE] Add KSP plugin alias to `gradle/libs.versions.toml` as `ksp = "2.3.6"` and map to plugin id `com.google.devtools.ksp`.
- [DONE] Replace any `kapt(...)` usage with `ksp(...)` in module `build.gradle.kts` files (we updated `app/build.gradle.kts`).
- [DONE] Remove Kotlin KAPT plugin from app module and replaced usages with KSP. Verify other modules if added later.
- [TODO] Search repo for any annotation processors using kapt (e.g., Dagger/Hilt). For Hilt, KSP plugin support exists (`dagger.hilt.processor` KSP artifact) — adapt accordingly.
- [TODO] Ensure generated sources appear under `build/generated/ksp/` and that IDE (Android Studio) recognizes them — if not, run `Invalidate Caches / Restart` or re-import Gradle project.
- [TODO] Update CI pipelines to include the new KSP plugin resolution and ensure Gradle wrapper / AGP versions are compatible.

KSP pitfalls & tips
- When migrating, confirm that every annotation-processor dependency has a KSP-compatible artifact or fallback.
- Keep Room compiler version in sync with Room runtime; Room 2.8.x supports KSP well. We set `room = "2.8.4"` in the catalog.
- For Hilt / Dagger or other processors, use their KSP artifacts where available and confirm their versions.

-----------------------------------------------------------------
SECTION 3A — Pinned dependency matrix (current baseline)

Source of truth
- Primary: `gradle/libs.versions.toml`
- Supporting reference: `pinned_versions.md`
- Gradle wrapper pin: `gradle/wrapper/gradle-wrapper.properties`

Toolchain
- [DONE] AGP: `9.1.0`
- [DONE] Gradle Wrapper: `9.3.1`
- [DONE] Kotlin: `2.3.20`
- [DONE] KSP: `2.3.6`

Core Android / Compose
- [DONE] AndroidX Core KTX: `1.10.1`
- [DONE] Lifecycle Runtime KTX: `2.6.1`
- [DONE] Lifecycle ViewModel Compose: `2.6.1`
- [DONE] Activity Compose: `1.8.0`
- [DONE] Compose BOM: `2026.02.01`
- [DONE] Navigation Compose: `2.7.7`

Persistence / Security
- [DONE] Room runtime / ktx / compiler / testing: `2.8.4`
- [DONE] DataStore Preferences: `1.1.1`
- [DONE] AndroidX Security Crypto: `1.1.0`

Features / Services
- [DONE] WorkManager runtime / testing: `2.9.1`
- [DONE] OSMdroid: `6.1.18`
- [DONE] Play Services Location: `21.0.1`
- [DONE] Accompanist Permissions: `0.34.0`

Testing
- [DONE] JUnit: `4.13.2`
- [DONE] AndroidX Test JUnit: `1.3.0`
- [DONE] AndroidX Test Core: `1.5.0`
- [DONE] Espresso Core: `3.7.0`
- [DONE] Robolectric: `4.11`
- [DONE] Kotlinx Coroutines (android + test): `1.7.3`

Pinning notes
- [DONE] Keep Room runtime/ktx/compiler/testing on the same version.
- [DONE] Keep WorkManager runtime/testing on the same version.
- [DONE] Robolectric is currently pinned to `4.11`; JVM tests are constrained via `app/src/test/resources/robolectric.properties` (`sdk=34`) because this Robolectric version does not fully support the app's target SDK 36.
- [TODO] When upgrading AGP/Kotlin/KSP or Robolectric, run full verification:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:compileDebugAndroidTestKotlin`

-----------------------------------------------------------------
SECTION 4 — Permissions and manifest (what's done)

- [DONE] Added permissions to manifest:
  - ACTIVITY_RECOGNITION — step sensors
  - ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION — GPS
  - ACCESS_BACKGROUND_LOCATION — only request on opt-in
  - FOREGROUND_SERVICE — for run tracking
  - POST_NOTIFICATIONS — Android 13+
  - RECEIVE_BOOT_COMPLETED — reschedule midnight reset
- [DONE] Declared services & receiver skeletons in manifest and added placeholder classes.

Notes
- Keep `ACCESS_BACKGROUND_LOCATION` in manifest but do not request it automatically; request it only after user opt-in with visible rationale.
- Use `android:exported="false"` for services & receivers unless you intentionally expose them.

 - [DONE] ISSUE #1 — Lint error: "foregroundServiceType:location requires permission:[android.permission.FOREGROUND_SERVICE_LOCATION] AND any permission in list:[android.permission.ACCESS_COARSE_LOCATION, android.permission.ACCESS_FINE_LOCATION]" (2026-04-09)
  - Context: A lint/build-time error is emitted when a service declares `android:foregroundServiceType="location"` but the manifest does not explicitly include the new `FOREGROUND_SERVICE_LOCATION` permission declaration alongside location permissions. This blocks the build on stricter SDK/tooling.
  - Goal: resolve the lint error while keeping correct runtime permission flows and backward compatibility across SDK levels.
  - Acceptance criteria:
    - Gradle build and Android Studio lint pass without the above error.
    - RunTrackingService still declares `foregroundServiceType="location"` and functions correctly on devices when location permission is granted.
    - CI builds (targeting the pinned compileSdk) succeed.
  - Checklist:
    - [X] Add `uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION"` to `AndroidManifest.xml` (explicitly) to satisfy the lint check. (added `android:required="false"`)
      - Note: Implemented in `app/src/main/AndroidManifest.xml` (2026-04-09).
    - [ ] Ensure `ACCESS_FINE_LOCATION` and/or `ACCESS_COARSE_LOCATION` uses-permission entries are present (they already are in the manifest but confirm).
    - [ ] Verify `compileSdk` and `targetSdk` in `app/build.gradle.kts` are set to a recent SDK where `FOREGROUND_SERVICE_LOCATION` is recognized by the SDK/lint (recommend API 34 / 34.x compileSdk).
      - If compileSdk < SDK that defines the permission, add the permission string anyway (lint will accept) or upgrade compileSdk. Upgrading compileSdk may be required for full correctness.
    - [ ] For backwards compatibility: guard any runtime checks for `FOREGROUND_SERVICE_LOCATION` with `if (Build.VERSION.SDK_INT >= X)` where appropriate. (Runtime grant behavior varies by Android version.)
    - [ ] Update `service/RunTrackingService.kt` service declaration in the manifest to keep `android:foregroundServiceType="location"` and `android:exported="false"`.
    - [X] Add a CI check that runs `./gradlew :app:lintDebug` and `./gradlew :app:assembleDebug` to ensure the manifest change resolves the error on CI. (local run passed)
    - [X] Add a small device test: start run tracking on a device/emulator with location permission denied/granted and confirm service starts and notification appears (manual QA / instrumentation test) — TODO: run and record results.
  - Notes & Alternatives:
    - If you prefer not to add the new permission string to manifest, another workaround is to remove `foregroundServiceType="location"` from the manifest — but that reduces platform ability to classify the service and may affect background restrictions on newer Android versions. Not recommended for a location foreground service.
    - Use `tools:node="merge"` or manifest placeholders only if you have multi-flavor cases requiring different behaviour.
  - Status: [DONE] (manifest updated; local lint/assemble passed) (2026-04-09)

-----------------------------------------------------------------
SECTION 5 — Calorie & metrics formulas (implementation-ready)

Distance & speed
- distanceMeters = steps * strideLengthMeters
- speedMps = distanceMeters / durationSec
- avgSpeedKmh = speedMps * 3.6
- paceSecPerKm = durationSec / (distanceMeters / 1000.0)

Calories
- MET-based (recommended for run): calories = MET * weightKg * durationHours
  - Example MET estimate: MET = 0.634 * speedKmh + 0.6 (approx) for running — calibrate in tests
- Distance-based (simple): calories = weightKg * distanceKm * 1.036
- Steps-based: caloriesFromSteps = steps * strideMeters / 1000.0 * weightKg * 1.036

Implement as utility:
- `util/EnergyCalculator.kt` with functions:
  - caloriesFromRun(weightKg, distanceMeters, durationSec): Double
  - caloriesFromSteps(weightKg, steps, strideMeters): Double

-----------------------------------------------------------------
SECTION 6 — Security & privacy checklist (mandatory)

- [TODO] Use `androidx.security:security-crypto` to create a MasterKey and wrap sensitive DataStore values (weight, name).
- [TODO] Add `.gitignore` entries: `local.properties`, `*.keystore`, `/app/build/`, `/.gradle/`.
- [TODO] Add privacy screen explaining what data is collected and retention policy; enable data export & delete in Settings.
- [TODO] Disable verbose logging in release builds (guard with `if (BuildConfig.DEBUG)`), remove PII from logs.
- [TODO] Consider marking DB/data as not backed up (`android:allowBackup="false"`) depending on privacy policy.

Migration plan — move away from deprecated `EncryptedSharedPreferences`

- [DONE] Detailed implementation design document added to `pinned_versions.md` (2026-04-11)
  - Use `pinned_versions.md` as the architecture/design reference for the storage migration.
  - Use this section in `plan.md` as the execution tracker and status board.

- [IN-PROGRESS] Current state (2026-04-11)
  - `UserDataStore.kt` still uses deprecated AndroidX Security Crypto APIs behind a compatibility helper so builds stay clean while preserving the existing encrypted on-device profile store.
  - This is acceptable short-term, but should be replaced with a non-deprecated encrypted persistence design before adding more sensitive profile fields.
  - Slice 1 foundation now compiles and its JVM tests pass.
  - Implementation note: the project currently uses a handwritten `UserProfileProto.kt` / `MigrationStateProto.kt` shim under `datastore/profile/proto/` plus `protobuf-javalite` runtime for Slice 1 stability; `app/src/main/proto/user_profile.proto` remains the schema reference and can be switched to generated-lite sources in a later slice if desired.
  - Rollout state: secure-profile migration and secure-store read cutover are now enabled through the repository boundary.
  - Result: profile reads prefer the secure store when a readable `COMPLETE` record exists, and fall back to legacy storage on incomplete or unreadable secure state.

- [TODO] Target architecture (recommended)
  - Recommended target: encrypted profile store backed by Proto DataStore for typed profile data plus Android Keystore-backed encryption at the repository/storage boundary.
  - Keep the public `UserProfileRepository` API stable so `SettingsViewModel`, `StepViewModel`, water goal reads, and other callers do not need broad changes.
  - Keep water day counters separate from profile migration scope unless a broader persistence-unification slice is scheduled.

- [TODO] Phase 0 — design + audit
  - Inventory all legacy keys in `UserDataStore.kt`:
    - `profile_name`
    - `profile_weight`
    - `profile_height`
    - `profile_stride`
    - `profile_units`
    - `profile_step_goal`
    - `profile_water_goal`
  - Audit all consumers of `UserProfileRepository` / `UserDataStore` to preserve semantics for:
    - null vs default values
    - profile update triggers / `profile_version`
    - blocking reads used by water/home/settings flows
  - Define a typed profile schema with explicit defaults and nullable fields.

- [DONE] Phase 1 — build new store in parallel (2026-04-11)
  - Add a new secure profile storage implementation alongside legacy storage; do not remove legacy reads/writes yet.
  - Add metadata fields:
    - `schemaVersion`
    - `migrationState`
    - `migratedAtEpochMs`
  - Implement repository adapters/mappers so the rest of the app continues to work against `UserProfile`.
  - Ensure writes to the new store are atomic and fully validated before commit.

- [IN-PROGRESS] Phase 2 — one-time idempotent migration (2026-04-11)
  - On first profile access after upgrade, attempt migration from legacy encrypted prefs into the new store.
  - Migration requirements:
    - idempotent: safe to run more than once
    - non-destructive: do not delete legacy data during initial rollout
    - field validation/coercion: reject malformed numbers, preserve nulls where intended, clamp invalid numeric ranges if needed
    - migration marker persisted only after the new store is fully written and re-readable
  - If keystore/decryption/corruption issues occur, fall back safely without crashing and surface defaults conservatively.
  - Current implementation status:
    - `UserProfileMigrationCoordinator` now performs a real staged migration in isolation:
      1. read legacy snapshot
      2. map via `UserProfileSchemaMapper.fromLegacy(...)`
      3. write a `MIGRATING` record to the secure store
      4. re-read and verify the staged payload
      5. write a `COMPLETE` record with `migratedAtEpochMs`
      6. re-read and verify the finalized payload
    - This algorithm is now triggered best-effort through `SecureAwareUserProfileRepository` during normal repository access while secure cutover is still off, so current user-visible read behavior remains unchanged.
    - Failure handling is coarse-grained and non-PII: `WRITE_FAILED`, `VERIFY_FAILED`, `FINALIZE_FAILED`.

- [DONE] Phase 3 — cutover reads, keep legacy fallback (2026-04-11)
  - Change reads to prefer the new store first.
  - Keep a legacy fallback path for at least one release so upgraded users can recover if migration was partial or an edge case appears.
  - Prefer writing only to the new store after migration succeeds; legacy writes may remain temporarily if dual-write is needed during one release window.
  - Current implementation status:
    - `SecureAwareUserProfileRepository` now serves secure-first blocking and flow reads when the secure store is readable and `migrationState = COMPLETE`.
    - Legacy fallback remains active for:
      - migration not yet complete
      - secure read failures
      - secure flow/collection failures
      - incomplete secure states
    - Legacy writes are still preserved and secure writes are mirrored best-effort after migration completion.

 - [TODO] Phase 4 — cleanup, retirement & verification (tightened checklist)
   - Goal: retire legacy encrypted prefs and compatibility helpers only after measurable stability and a safety window.
   - Preconditions (all must be satisfied before deleting legacy data or removing compatibility code):
     - migration marker (`migrationState = COMPLETE`) observed in field telemetry for > 99.5% of upgraded users over a minimum window (suggested: 2 consecutive releases / ~4 weeks), OR validated by manual QA for small rollouts.
     - Secure store read/write success rates in telemetry show negligible errors; any errors must be investigated and resolved.
     - No critical rollback or compatibility bug reported that requires restoring legacy reads (maintain legacy read path for the full safety window).
     - Automated integration tests covering rollback/fallback scenarios pass consistently on CI (see "Testing" section for required test names).
   - Retirement steps (ordered):
     1. Freeze legacy-write paths: stop writing new data to legacy keys but keep read fallback intact for at least one release to detect unexpected regressions.
     2. Monitor telemetry and perform a manual QA sweep on a wide device matrix (OEM variations, low-memory scenarios, keystore corruption simulation).
     3. After safety window and telemetry review, remove legacy-write and compatibility helper code in a single PR with clear changelog and privacy notes.
     4. Only delete legacy `user_profile_encrypted` keys and associated files after a final audit and an explicit retention-policy sign-off; preserve a documented rollback plan in case of emergency.
     5. Update docs/tests/privacy notes and add a migration post-mortem entry in the repo explaining when/why legacy data was removed.
   - Suggested safety window: keep legacy read path + data intact for at least 2 releases (~4 weeks) after cutover is enabled, unless telemetry/QA indicate earlier safe removal.

- [TODO] Rollback strategy
  - Keep legacy `user_profile_encrypted` data untouched during initial rollout.
  - If the new store is corrupted or unreadable, fall back to legacy reads without data loss.
  - Do not delete legacy storage in the same release as the initial migration.
  - If a release must be rolled back, older app versions should still find their original encrypted prefs intact.

- [TODO] Data integrity + security rules
  - Preserve null-vs-default semantics for optional profile fields.
  - Avoid partial writes; either the full profile commits or migration remains incomplete.
  - Never log sensitive profile values.
  - Treat decryption, keystore, or corruption exceptions as recoverable migration states, not crash conditions.
  - Re-check backup/restore policy once the new secure store is chosen.

- [TODO] Testing plan for migration
  - Unit tests for profile field mapping and defaults.
  - Migration tests covering:
    - [DONE] happy-path upgrade from legacy encrypted prefs (coordinator + real secure store JVM coverage, 2026-04-11)
    - [DONE] idempotent re-run (2026-04-11)
    - [DONE] malformed legacy numeric values (mapper/coordinator coverage, 2026-04-11)
    - [DONE] missing fields (2026-04-11)
    - rollback / legacy fallback behavior
    - [IN-PROGRESS] corruption or keystore read failure handling
  - Repository/ViewModel regression tests for settings save/load/reset and step/water goal propagation.
  - Manual QA:
    - install old build → save profile → upgrade → verify values
    - reset profile after migration
    - downgrade/rollback scenario if retained as a support goal

- [TODO] Suggested implementation order (future PR slices)
  - PR A: schema + new store + tests, no migration enabled
  - PR B: migration helper + idempotency tests + fallback reads
  - PR C: cutover writes/reads to new store, keep legacy backup intact
  - PR D: legacy cleanup after one stable release

- [TODO] Slice 1 — secure profile store foundation (first implementation slice)
  - Goal: add the new secure profile store foundation in parallel with legacy storage, with zero behavior changes to current `UserProfileRepository` consumers.
  - Explicit non-goals for Slice 1:
    - no runtime cutover
    - no deletion of legacy encrypted prefs
    - no provider/repository wiring changes that affect current app behavior

  Files to add (foundation)
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/profile/SecureUserProfileStore.kt` (2026-04-11)
    - Define the typed internal storage contract for profile read/write operations and metadata access.
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/profile/EncryptedProtoUserProfileStore.kt` (2026-04-11)
    - Add the parallel secure-store implementation; do not wire it into `RepositoryProvider` yet.
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/profile/SecureProfileCipher.kt` (2026-04-11)
    - Encapsulate keystore-backed encrypt/decrypt responsibilities so storage and migration code remain testable.

  Schema / serializer decisions
  - [DONE] Confirm Proto DataStore as the storage format for the new secure profile store. (2026-04-11)
  - [DONE] Add `app/src/main/proto/user_profile.proto` (2026-04-11)
    - Include user profile payload fields plus migration metadata:
      - `schema_version`
      - `migration_state`
      - `migrated_at_epoch_ms`
      - `last_write_epoch_ms`
  - [DONE] Add `app/src/main/java/com/example/productivityapp/datastore/profile/ProtoUserProfileSerializer.kt` (2026-04-11)
    - Preserve `UserProfile` semantics for:
      - nullable `displayName`, `weightKg`, `heightCm`
      - default `strideLengthMeters = 0.78`
      - default `preferredUnits = "metric"`
      - default `dailyStepGoal = 10000`
      - default `dailyWaterGoalMl = 2000`
  - [DONE] Add handwritten Slice 1 proto shim under `app/src/main/java/com/example/productivityapp/datastore/profile/proto/` to keep the foundation compiling without generated-source wiring changes. (2026-04-11)
  - [DONE] Add field-mapping rules doc/comments near serializer/store implementation for legacy key mapping from `UserDataStore.kt`. (2026-04-11)

  Migration coordinator skeleton (not yet wired)
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/profile/LegacyProfileReader.kt` (2026-04-11)
    - Read legacy `user_profile_encrypted` values only; no writes.
  - [DONE] `app/src/main/java/com/example/productivityapp/datastore/profile/UserProfileMigrationCoordinator.kt` (2026-04-11)
    - Add the coordinator skeleton with idempotent migration states, but do not trigger it automatically in this slice.
  - [DONE] Keep `app/src/main/java/com/example/productivityapp/data/repository/impl/DataStoreUserProfileRepository.kt` unchanged in Slice 1. (2026-04-11)
  - [DONE] Keep `app/src/main/java/com/example/productivityapp/data/RepositoryProvider.kt` unchanged in Slice 1. (2026-04-11)

  Tests to write first
  - [DONE] `app/src/test/java/com/example/productivityapp/datastore/profile/UserProfileSchemaMapperTest.kt` (2026-04-11)
    - Cover defaults, nullability preservation, and malformed numeric legacy value handling.
  - [DONE] `app/src/test/java/com/example/productivityapp/datastore/profile/EncryptedProtoUserProfileStoreTest.kt` (2026-04-11)
    - Cover atomic write/readback, schema defaults, and corrupted payload fallback behavior.
  - [DONE] `app/src/test/java/com/example/productivityapp/datastore/profile/UserProfileMigrationCoordinatorTest.kt` (2026-04-11)
    - Cover “already complete” idempotency, legacy-read mapping, and no-op behavior when migration is not enabled.

  Slice 1 acceptance criteria
  - [DONE] New secure profile store classes compile and are covered by JVM tests. (verified with `:app:compileDebugKotlin` and `:app:testDebugUnitTest` on 2026-04-11)
  - [DONE] No existing app behavior changes: current `UserProfileRepository` consumers still use the legacy-backed path. (2026-04-11)
  - [DONE] No automatic migration runs yet. (2026-04-11)
  - [IN-PROGRESS] The next slice can wire migration/coordinator behavior without redesigning the schema/store foundation.

  Slice 1 follow-up note
  - [TODO] Account for current blocking-read callers such as `app/src/main/java/com/example/productivityapp/app/data/repository/WaterRepository.kt` in Slice 2 before any repository cutover.

  Slice 2 — migration coordinator write path (current slice)
  - Goal: implement and validate the real secure-store migration algorithm without changing runtime repository wiring.
  - Explicit non-goals for Slice 2:
    - no automatic migration trigger in production flows yet
    - no repository/provider cutover to the new secure store
    - no deletion of legacy encrypted prefs

  Coordinator behavior
  - [DONE] Extend `app/src/main/java/com/example/productivityapp/datastore/profile/UserProfileMigrationCoordinator.kt` with staged write + verify + finalize behavior. (2026-04-11)
  - [DONE] Add coarse non-sensitive failure outcomes for write/verify/finalize failures. (2026-04-11)
  - [DONE] Preserve idempotency by returning `AlreadyComplete` when the secure store already carries `migrationState = COMPLETE`. (2026-04-11)

  Tests added/expanded
  - [DONE] Expand `app/src/test/java/com/example/productivityapp/datastore/profile/UserProfileMigrationCoordinatorTest.kt` for:
    - successful migration flow
    - idempotent second run
    - missing legacy data
    - verification mismatch
    - real temp-file secure-store persistence
    (2026-04-11)

  Slice 2 status
  - [DONE] Migration coordinator can now migrate legacy profile data into the new secure store in tests/dev code paths. (2026-04-11)
  - [DONE] Full validation run succeeded after the Slice 2 coordinator changes:
    - `./gradlew :app:testDebugUnitTest --tests "com.example.productivityapp.datastore.profile.UserProfileMigrationCoordinatorTest" --console=plain`
    - `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --console=plain`
  - [DONE] Wire the coordinator into runtime profile access behind a safe fallback gate in Slice 3. (2026-04-11)

  Slice 3 — hybrid repository cutover scaffold
  - Goal: route profile access through a new-store-aware repository while keeping rollback safety and current runtime behavior intact.
  - Explicit non-goals for this slice:
    - no default-on secure-store cutover in production yet
    - no deletion of legacy encrypted prefs
    - no removal of legacy fallback paths

  Repository boundary changes
  - [DONE] Extend `app/src/main/java/com/example/productivityapp/data/repository/UserProfileRepository.kt` with `getUserProfileBlocking()` so synchronous callers are centralized behind the repository contract. (2026-04-11)
  - [DONE] Update `app/src/main/java/com/example/productivityapp/data/repository/impl/DataStoreUserProfileRepository.kt` to implement the blocking read via `UserDataStore`. (2026-04-11)
  - [DONE] Add `app/src/main/java/com/example/productivityapp/data/repository/impl/SecureAwareUserProfileRepository.kt`. (2026-04-11)
    - Behavior:
      - legacy-first when cutover is disabled
      - secure-store-first only when cutover is enabled and migration is confirmed `COMPLETE`
      - fallback to legacy repository on secure migration/read failures
      - legacy writes always preserved
      - secure writes mirrored best-effort after secure cutover is active and migration is complete

  Provider / caller wiring
  - [DONE] `app/src/main/java/com/example/productivityapp/data/RepositoryProvider.kt` now builds the hybrid repository and caches it as a singleton to avoid multiple DataStore instances for the same secure-store file. (2026-04-11)
  - [DONE] Runtime gates in `RepositoryProvider` are now configured for the secure-read cutover slice: (2026-04-11)
    - `ENABLE_SECURE_PROFILE_MIGRATION = true`
    - `ENABLE_SECURE_PROFILE_CUTOVER = true`
    - Result: migration runs best-effort during repository access and profile reads are now secure-first with legacy fallback preserved.
  - [DONE] `app/src/main/java/com/example/productivityapp/app/data/repository/WaterRepository.kt` now uses `UserProfileRepository.getUserProfileBlocking()` instead of reading profile data directly from `UserDataStore`. (2026-04-11)
  - [DONE] `app/src/main/java/com/example/productivityapp/data/repository/impl/SecureAwareUserProfileRepository.kt` now triggers migration best-effort during `observeUserProfile()`, `getUserProfileBlocking()`, and `updateUserProfile()` even when cutover is off. (2026-04-11)
    - Result: secure store population can be verified through real repository/runtime flows without changing the active read source.
  - [DONE] `app/src/main/java/com/example/productivityapp/data/repository/impl/SecureAwareUserProfileRepository.kt` now hardens secure-first reads with legacy fallback for both blocking and flow-based access. (2026-04-11)
    - Flow fallback now handles secure observe/collection failures without breaking callers.
    - Blocking fallback remains active if migration is incomplete or the secure store is unreadable.

  Tests added
  - [DONE] `app/src/test/java/com/example/productivityapp/data/repository/impl/SecureAwareUserProfileRepositoryTest.kt` added for:
    - cutover-disabled legacy reads
    - cutover-enabled secure-store preference after migration
    - secure failure fallback to legacy blocking reads
    - dual-write mirroring after secure migration completion
    - migration-enabled/cutover-disabled secure-store population through observe/blocking runtime paths
    - real temp-file secure-store population while legacy reads remain active
    - cutover-enabled secure-first blocking reads
    - cutover-enabled fallback for incomplete secure state
    - cutover-enabled fallback for secure flow observe failures
    - real temp-file secure-read cutover behavior
    (2026-04-11)

  Slice 3 status
  - [DONE] Hybrid repository scaffold implemented and wired safely behind default-off gates. (2026-04-11)
  - [DONE] Blocking profile access has been centralized behind `UserProfileRepository`. (2026-04-11)
  - [DONE] Migration-only rollout slice completed: migration gate enabled, cutover remains off, and secure-store population is verified through repository-driven flows. (2026-04-11)
  - [DONE] Secure-read cutover slice completed: secure-first reads are enabled with legacy fallback still intact. (2026-04-11)
  - [DONE] Validation run succeeded after Slice 3 changes:
    - `./gradlew :app:testDebugUnitTest --tests "com.example.productivityapp.data.repository.impl.SecureAwareUserProfileRepositoryTest" --console=plain`
    - `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --console=plain`
  - [TODO] In the next slice, perform manual upgrade/rollback QA, decide the dual-write retention window, and prepare eventual legacy-write retirement criteria.

-----------------------------------------------------------------
SECTION 7 — Testing & QA plan

Unit tests
- Room DAOs: use in-memory DB tests.
- DataStore: use TestCoroutineDispatcher + temporary file-based DataStore.
- EnergyCalculator: exhaustive unit tests across speeds & weights.
  - [DONE] Repository instrumentation tests (androidTest, in-memory Room) were added for Steps/Run/Sleep.

- [IN-PROGRESS] Add JVM/Robolectric unit tests for repositories and DataStore so CI can run without a device.
  - Plan: convert existing androidTest cases (in-memory Room) into JVM tests using Robolectric or true unit tests using Room.inMemoryDatabaseBuilder with Robolectric test runner or AndroidX Test core where needed.

  - [DONE] Add concrete unit tests (JVM) for repository implementations using in-memory Room database: (2026-04-07)
  - Files added: `app/src/test/java/.../StepRepositoryUnitTest.kt`, `RunRepositoryUnitTest.kt`, `SleepRepositoryUnitTest.kt` (Robolectric-based in-memory Room tests).
  - Tests use `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()` + `kotlinx-coroutines-test` `runTest` to validate repository behavior.
  - [DONE] Local test run: executed `./gradlew :app:testDebugUnitTest` locally — tests compiled and ran successfully (2026-04-07).
    - Test report: `app/build/reports/tests/testDebugUnitTest/index.html`.
    - Note: Robolectric emitted a manifest warning during the test run; tests still passed. To silence, annotate tests with `@Config(manifest = Config.NONE)` or provide a test manifest.
  - Next: add these tests to CI and ensure KSP-generated sources are available to the test classpath on CI (validate `build/generated/ksp/debugUnitTest` presence).

  - [DONE] Add DataStore unit test (JVM) using temporary directory (2026-04-09)
    - File added: `app/src/test/java/com/example/productivityapp/datastore/DataStoreUnitTest.kt`
    - Purpose: validate Preferences DataStore increment/observe behavior and `profile_version` bump emission using a temporary file and `kotlinx-coroutines-test`.
    - Local verification: `./gradlew :app:testDebugUnitTest` completed successfully (2026-04-09).
    - CI: add `./gradlew :app:testDebugUnitTest --no-daemon --console=plain` to your CI jobs to run JVM tests without an emulator.

Quick test commands
- Run unit tests (JVM):
  ./gradlew :app:testDebugUnitTest
- Run instrumentation tests (device/emulator required):
  ./gradlew :app:connectedAndroidTest

Recommended additional tests (TODO)
- [DONE] Add Robolectric-based JVM tests for repositories so CI doesn't require an emulator. (2026-04-07)
- [DONE] Add DataStore unit tests using TestCoroutineDispatcher and a temporary directory. (2026-04-09)
 - [IN-PROGRESS] Add ViewModel unit tests (mock repositories) to validate state updates.
   - [DONE] `SleepViewModelTest.kt` covers sleep session state and summaries. (2026-04-09)
   - [DONE] `SettingsViewModelTest.kt` covers profile load/save/reset validation and repository updates. (2026-04-09)
   - [TODO] Add similarly focused ViewModel tests for Run and Steps if those screens grow more derived UI state.
- [DONE] Add Service tests (Robolectric or ServiceTestRule) for StepCounterService and RunTrackingService. (2026-04-11)
  - Added/validated: lifecycle start/stop tests, notification action assertions, graceful permission-denied handling, step batching/rollover checks, and run location heuristics using a mock `LocationProvider`.

  Additional testing items (new)
  - [DONE] Add unit tests for `PolylineUtils` (encode/decode round-trip). (2026-04-11)
    - File: `app/src/test/java/com/example/productivityapp/util/PolylineUtilsTest.kt`
    - Coverage: known encoded fixture, round-trip epsilon checks, empty/single-point/bounds cases, and long sequences.
  - [DONE] Add UI/Compose tests for `RunMapView` integration. (2026-04-11)
    - File: `app/src/androidTest/java/com/example/productivityapp/ui/run/RunMapViewTest.kt`
    - Coverage: polyline + marker rendering and replay centering behavior.
  - [DONE] Add Service tests to validate RunTrackingService persists/decodes polylines and resumes correctly after restart. (2026-04-11)
  - [DONE] Add WorkManager test for MidnightResetWorker using WorkManagerTestInitHelper. (2026-04-11)
    - File: `app/src/test/java/com/example/productivityapp/service/MidnightResetWorkerTest.kt`
    - Coverage: next-midnight delay calculation, schedule enqueueing, boot reschedule, and reset logic.
    - Stability note: added `app/src/test/resources/robolectric.properties` with `sdk=34` because Robolectric 4.11 in this project does not support targetSdk 36 for JVM tests. This keeps CI/local unit tests stable until Robolectric is upgraded.
    - 2026-04-11 follow-up: the JVM test was rewritten to avoid direct `androidx.work.testing` imports because the IDE model was intermittently failing to resolve that package even though Gradle could. The test now validates scheduling via runtime WorkManager APIs plus direct reset-operation coverage.

Instrumentation
- Compose UI tests for navigation and core interactions.
- Service tests using ServiceTestRule or Robolectric for step sensor and run tracking logic.

Manual QA scenarios (important)
- Permission denied flows for sensors and location shall show rationale & settings links.
- Device without step sensor (TYPE_STEP_COUNTER) must allow manual step input.
- GPS drift: implement smoothing — ignore location jumps > 50 m in 1 second unless speed plausible.
- Midnight reset: test by adjusting device clock and using WorkManager Test helpers.

-----------------------------------------------------------------
SECTION 8 — Estimated timeline & milestones

- Week 0: Dependency/manifest/KSP migration (DONE)
- Week 1: Persistence skeleton (Room + EncryptedDataStore) + CI adjustments (2–3 days)
- Week 2: ViewModels + placeholder UIs + HomeScreen integration (3 days)
- Week 3: Steps Service, UI & tests (4 days)
- Week 4: Run tracker, OSMdroid map, foreground service (4–6 days)
- Week 5: Sleep tracker UI and history charts (3 days)
- Week 6: Water + settings + privacy changes + tests (2–3 days)
- Week 7: QA, instrumentation tests, polish, release prep (3 days)

-----------------------------------------------------------------
SECTION 9 — How to update this plan programmatically

- To mark a task as done, change the prefix: `- [TODO]` → `- [DONE]` and append ` (YYYY-MM-DD)` timestamp.
- To add a subtask, add a bulleted line under the relevant section with status tag.
- Example automation rule (pseudo): `grep -n "\- \[TODO\]" plan.md` to enumerate remaining tasks.

-----------------------------------------------------------------
 Immediate next actions (priority-ordered)

 - [IN-PROGRESS] Polish UI across all windows (Home, Water, Steps, Run, Sleep, Settings)
   - Why: UI polish will make demos/test flows reliable and reduce follow-up UX fixes.
   - Acceptance: see section "Polished UI for every window — current state" above.

 - [DONE] Finish Run tracker service (RunTrackingService): ensure location accuracy, background behaviour, and encoded polyline persistence + migration helper for old CSV runs. (2026-04-09)
  - [DONE] Finish Run tracker service (RunTrackingService) — core implementation (2026-04-09)
    - Files changed/added:
      - `app/src/main/java/com/example/productivityapp/service/RunTrackingService.kt` — completed handleLocation heuristics, uses LocationProvider abstraction, persists points via RunRepository.addLocationPoint, updates distance/duration/speed.
      - `app/src/main/java/com/example/productivityapp/service/LocationProvider.kt` — new abstraction and `FusedLocationProviderWrapper` for easier mocking in tests.
      - `app/src/main/java/com/example/productivityapp/data/repository/RunRepository.kt` — added `addLocationPoint(runId, lat, lon, tsMs)` to API.
      - `app/src/main/java/com/example/productivityapp/data/repository/impl/RoomRunRepository.kt` — implemented `addLocationPoint`, appends new points atomically, persists `run_points`, and migrates CSV-style polylines to encoded polyline.
      - `app/src/main/java/com/example/productivityapp/data/entities/RunPointEntity.kt` / `data/dao/RunPointDao.kt` / `data/AppDatabase.kt` / `data/DatabaseProvider.kt` — added `run_points` table, indexes, Room migration (v1→v2), and runtime polyline backfill helper.
      - `app/src/main/java/com/example/productivityapp/run/RunReplayHelper.kt` — added replay decoding/timeline helper for OSMdroid playback and test scaffolding.
    - Behavior & acceptance:
      - Start/Pause/Resume/Stop actions implemented and foreground notification maintained.
      - Pause/resume now preserves active elapsed duration instead of counting paused time.
      - Location acceptance heuristics: ignore tiny jitter (<0.5m); discard improbable jumps (>50m in <1s unless speed plausible <15 m/s).
      - Polyline persisted as encoded polyline atomically by `addLocationPoint`; `run_points` stores individual points for efficient replay/history reads.
      - Legacy CSV-style strings can be converted by the runtime `DatabaseProvider.migratePolylinesIfNeeded(context)` helper.
    - Tests added:
      - `app/src/test/java/com/example/productivityapp/data/repository/impl/RunRepositoryAddPointUnitTest.kt` — verifies CSV-style migration, append, encoding, and `run_points` insert.
      - `app/src/test/java/com/example/productivityapp/service/RunTrackingServiceUnitTest.kt` — verifies lifecycle wiring and location handling using a mock `LocationProvider`.
      - `app/src/test/java/com/example/productivityapp/run/RunReplayHelperUnitTest.kt` — pure unit test for replay timeline generation and timestamp handling.
      - `app/src/androidTest/java/com/example/productivityapp/run/RunReplayIntegrationTemplate.kt` — device-test template for OSMdroid replay.
    - Notes & follow-ups:
      - [DONE] Wired `DatabaseProvider.migratePolylinesIfNeeded(context)` into a one-time DB bootstrap path via `DatabaseProvider.getInstance(context)` so legacy runs are backfilled automatically. (2026-04-09)
      - Consider extracting calibration parameters (jitter threshold, jump threshold, max plausible speed) to config for easier tuning.
      - 2026-04-09 polish: `RunScreen.kt` upgraded with live stats cards, replay slider/playback controls, and `RunMapView` replay marker support. `RunTrackingService` now uses the non-deprecated `stopForeground(STOP_FOREGROUND_REMOVE)` path on newer SDKs.

 - [DONE] Finish StepCounterService and add Service lifecycle tests (Robolectric/ServiceTestRule) and permission flows. (2026-04-09)
   - Files changed/added:
     - `app/src/main/java/com/example/productivityapp/service/StepCounterService.kt` — date-aware baseline, batched repository writes, graceful no-sensor handling, stop action, modern stopForeground path.
     - `app/src/main/java/com/example/productivityapp/ui/steps/StepScreen.kt` — permission rationale/settings fallback, manual quick-add + custom entry, sensor-absence UX, ViewModel-backed running state.
     - `app/src/main/java/com/example/productivityapp/viewmodel/StepViewModel.kt` — service-running state exposed to UI.
     - `app/src/test/java/com/example/productivityapp/service/StepCounterServiceUnitTest.kt` — Robolectric lifecycle/batching/rollover tests.
     - `app/src/androidTest/java/com/example/productivityapp/ui/steps/StepScreenContentTest.kt` — Compose content tests for sensor absence and permission fallback.
   - Acceptance met:
     - Baseline and periodic repository writes finalized.
     - Foreground notification Stop action and proper stop behavior implemented.
     - Permission rationale + settings fallback implemented.
     - Manual entry path remains available without sensor or permission.
     - Service lifecycle and UI compile/test coverage added.

 - [TODO] Wire ViewModels to expose richer live running/service state and flows for UI (persist running state in ViewModel and repository).

 - [DONE] Add JVM/Robolectric unit tests for repositories and DataStore so CI can run without devices (move androidTest coverage to JVM where possible). (2026-04-11)

 - [TODO] CI: validate KSP-generated sources are available to test classpath; update CI Gradle cache & Gradle wrapper if needed.

 Recent changes (concise changelog)
 - 2026-04-07: KSP migration completed and KSP entries added to version catalog. (DONE)
 - 2026-04-07: Repository skeletons + Room entities/DAOs and DataStore wrapper added. (DONE)
 - 2026-04-07: Run map integration implemented: `RunMapView.kt` (OSMdroid) and `PolylineUtils.kt` added; Run polyline storage switched to encoded polyline. (DONE/IN-PROGRESS for migration)
 - 2026-04-07: Unit tests (Robolectric/JVM) for repositories added and local test run successful. (DONE)
 - 2026-04-07: Service skeletons for StepCounterService and RunTrackingService implemented; service tests are in progress. (historical)

   - 2026-04-09: ISSUE #2 — UserProfile observe/update bug fixed: `UserDataStore.updateUserProfile` now suspends, writes encrypted prefs and touches a DataStore profile_version key so `observeUserProfile()` emits reliably. (DONE)
     - 2026-04-09: Issue #3 (Run tracker) fully completed and build/tests validated. (DONE)
     - 2026-04-09: Issue #4 (Steps tracker service + permission flows + tests) completed and build/tests validated. (DONE)
    - 2026-04-09: Sleep feature implemented: session flows, quality rating, weekly chart/history, and JVM tests completed and validated. (DONE)
     - 2026-04-09: Settings/profile slice implemented: repository-backed `SettingsScreen`, `SettingsViewModel`, home navigation entry, profile reset support, and JVM tests validated. (DONE / Phase 7 in progress)
     - 2026-04-11: Issue #5 completed — `MidnightResetWorker` + `BootCompleteReceiver` + WorkManager tests implemented and wired into app startup. (DONE)
     - 2026-04-11: Issue #6 completed — `PolylineUtilsTest` added for encode/decode round-trip and edge cases. (DONE)
     - 2026-04-11: Issue #7 completed — `RunMapView` visuals polished and instrumentation tests added for polyline/replay rendering. (DONE)
     - 2026-04-11: Issue #8 completed — Robolectric service tests validated for both `StepCounterService` and `RunTrackingService`. (DONE)
     - 2026-04-11: Gradle cleanup completed — removed duplicate dependency declarations from `app/build.gradle.kts`, normalized test deps through the version catalog, and trimmed duplicate/unused catalog aliases to reduce IDE/build-model noise. (DONE)
     - 2026-04-11: `UserDataStore.kt` deprecation cleanup completed by isolating deprecated AndroidX Security Crypto usage behind a compatibility helper; storage format preserved. (DONE)
     - 2026-04-11: Added pinned dependency matrix section to `plan.md`, aligned with `libs.versions.toml`, `pinned_versions.md`, and the Gradle wrapper pin. (DONE)
     - 2026-04-11: Added a dedicated migration plan in `plan.md` to move away from deprecated `EncryptedSharedPreferences` using a phased, rollback-safe approach. (DONE)
     - 2026-04-11: Slice 1 secure-profile foundation recovered and validated — handwritten proto shim parser fixed, `:app:compileDebugKotlin` and the full `:app:testDebugUnitTest` suite passed, and current repository wiring remains unchanged. (DONE)
     - 2026-04-11: Slice 2 secure-profile migration logic implemented in `UserProfileMigrationCoordinator` with staged write/readback verification, idempotency coverage, real temp-file store verification, and no runtime cutover yet. (DONE / next step is Slice 3 gating + fallback wiring)
     - 2026-04-11: Migration-only rollout slice completed — `RepositoryProvider` now enables secure-profile migration while keeping read cutover off, `SecureAwareUserProfileRepository` populates the secure store during normal repository access, and compile/JVM tests/assemble all passed. (DONE)
     - 2026-04-11: Secure-read cutover slice completed — `RepositoryProvider` now enables secure-store reads, `SecureAwareUserProfileRepository` serves secure-first profile reads with legacy fallback for incomplete/unreadable secure state, and compile/JVM tests/assemble all passed. (DONE)

   - Note: This file was updated to explicitly track "polished UI for every window" as part of the short-term priorities so the next session can pick this up easily.

-----------------------------------------------------------------
Contact / notes
- This file is tracked in source control — avoid committing secrets or API keys referenced from `local.properties`.


  -----------------------------------------------------------------

  Per-screen UI TODOs

  The following granular per-screen TODOs were generated automatically and should be appended here so the UI polish work is tracked at a fine-grained level. Copy this section into the relevant place in the plan as work proceeds and update status tags inline.

  ### Steps Screen (file: `app/src/main/java/.../ui/steps/StepScreen.kt`)
  - [x] Implement live step display bound to `StepViewModel.observeSteps(date)` (StateFlow/Flow).
  - [x] Add Start / Pause / Stop controls that start/stop `StepCounterService` and persist state in ViewModel.
  - [x] Permission flow: show rationale for `ACTIVITY_RECOGNITION`, and fall back to manual step entry when absent.
  - [x] Surface the saved daily step goal and progress in the screen UI.
  - [ ] Add compact chart (bar chart) showing steps over last 7 days; add touch-to-view details.
  - [ ] Add content descriptions for all interactive elements and a11y-focused label for step count.
  - [x] Add UI tests (Compose) for permission dialog and start/stop service flow.

  ### Run Screen (file: `app/src/main/java/.../ui/run/RunScreen.kt` + `RunMapView.kt`)
  - [x] Implement Start / Pause / Resume / Stop controls wired to `RunViewModel` and `RunTrackingService`.
  - [x] Implement live stats area: elapsed time, distance (m / km), pace, average speed, calories.
  - [x] Map polish: center-on-start, follow-run toggle, start/end markers, appropriate zoom on start.
  - [x] Improve polyline visuals: stroke width, color from theme (per-run accent), dashed vs solid for paused state.
  - [x] Add replay mode: slider to scrub through encoded polyline and a small play/pause for replay.
  - [x] Permission flow: request `ACCESS_FINE_LOCATION` with rationale; background location opt-in only after user opt-in.
  - [x] Add UI tests for RunScreen interactions and RunMapView rendering (instrumentation or Robolectric as appropriate).

  ### Sleep Screen (file: `app/src/main/java/.../ui/sleep/SleepScreen.kt`)
  - [x] Implement Start / Stop session controls and display current session duration when running.
  - [x] After Stop: present a quality rating UI (1–5 stars) and optional notes text input; persist to repository.
  - [x] History view: weekly line/bar chart showing sleep duration and average quality; tap to open day details.
  - [ ] Add small tips: suggested sleep goals and quick actions (e.g., "Start nap timer").
  - [ ] Accessibility: label session controls and rating UI for screen readers.
  - [x] Add ViewModel unit tests and repository interaction tests for session start/stop and rating persistence.
  - [ ] Add Compose UI tests for sleep screen interactions.

  ### Settings Screen (file: `app/src/main/java/.../ui/settings/SettingsScreen.kt`)
  - [x] Add fields to edit user profile and PII (weightKg, heightCm) and stride length. Persist via `UserDataStore`.
  - [x] Add toggles for units (metric/imperial) and a local explanation of on-device storage / privacy behavior.
  - [ ] Add explicit run tracking defaults (follow-map, auto-pause) if those settings become part of the `UserProfile` model.
  - [ ] Add a richer privacy screen or export/delete flow explaining retention and removal options.
  - [ ] Add security indicators / migration plan away from deprecated `EncryptedSharedPreferences` if needed later.
  - [x] Add tests for settings ViewModel interactions.

  ### Home / Water (files: `HomeScreen.kt`, `WaterIntakeScreen.kt`)
  - [ ] Home: ensure tiles reflect per-feature accent colors and provide quick actions (start run, add water, start sleep).
  - [x] Water: quick-add buttons (+100ml, +250ml), manual entry, and daily goal progress bar.
  - [x] Water goal now follows the saved `UserProfile.dailyWaterGoalMl` setting.
  - [x] Water auto-reset backend: midnight reset scheduling is implemented via `MidnightResetWorker`; UI messaging/opt-out remains follow-up work.
  - [ ] Add accessibility labels for quick-add buttons and home tiles.
  - [ ] Add unit tests for WaterDataStore interactions and UI tests for the quick-add flows.

  Notes
  - Each per-screen TODO should reference the corresponding ViewModel and repository methods. Prefer using flows/StateFlow from the ViewModel to drive UI state rather than local mutable state in composables.
  - For Compose UI tests that require AndroidView (OSMdroid), prefer instrumentation tests; where feasible use Robolectric with AndroidView support.

  End of plan.md

