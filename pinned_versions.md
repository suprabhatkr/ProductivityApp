# Pinned version matrix — ProductivityApp

Generated: 2026-04-11

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

---

# Concrete implementation design — migrate away from `EncryptedSharedPreferences`

Purpose
- Define the implementation-ready design for replacing deprecated `EncryptedSharedPreferences` usage in `app/src/main/java/com/example/productivityapp/datastore/UserDataStore.kt`.
- Preserve the current `UserProfileRepository` contract and existing on-device user data during rollout.
- Keep the migration rollback-safe and incremental.

Scope
- In scope:
  - User profile storage currently backing `UserProfile`
  - Migration of legacy encrypted profile keys
  - Repository/storage abstraction changes required to support the new secure store
- Out of scope for this migration:
  - Water entry JSON storage in `WaterRepository.kt`
  - Step/run/sleep Room entities
  - General export/delete UX beyond repository/storage readiness

Current implementation summary
- Current profile model:
  - `displayName: String?`
  - `weightKg: Double?`
  - `heightCm: Int?`
  - `strideLengthMeters: Double = 0.78`
  - `preferredUnits: String = "metric"`
  - `dailyStepGoal: Int = 10000`
  - `dailyWaterGoalMl: Int = 2000`
- Current repository boundary:
  - `UserProfileRepository.observeUserProfile(): Flow<UserProfile>`
  - `UserProfileRepository.updateUserProfile(profile: UserProfile)`
- Current legacy storage keys in `UserDataStore.kt`:
  - `profile_name`
  - `profile_weight`
  - `profile_height`
  - `profile_stride`
  - `profile_units`
  - `profile_step_goal`
  - `profile_water_goal`
- Current change propagation mechanism:
  - `profile_version` in Preferences DataStore is bumped after secure profile writes so observers emit again.

Recommended target architecture
- Recommended choice: encrypted typed profile store using Proto DataStore + Android Keystore-backed encryption at the storage boundary.
- Rationale:
  - typed schema and clearer defaults/nullability
  - atomic writes and versionable structure
  - easier migration metadata than ad-hoc key/value encrypted prefs
  - repository API can remain stable

Architecture overview
- Keep `UserProfileRepository` as the stable API used by the rest of the app.
- Introduce a new internal storage layer, for example:
  - `SecureUserProfileStore` — typed read/write API
  - `ProtoUserProfileSerializer` — Proto/DataStore serialization layer
  - `ProfileCrypto` or `SecureProfileCipher` — keystore-backed encrypt/decrypt helper
  - `LegacyProfileReader` — reads old `user_profile_encrypted` values only
  - `UserProfileMigrationCoordinator` — orchestrates one-time migration
- Keep `UserDataStore` temporarily as a façade while migration is active, then shrink or split it once legacy profile storage is retired.

Proposed data schema for the new secure store
- Persisted profile payload fields:
  - `display_name: optional string`
  - `weight_kg: optional double`
  - `height_cm: optional int32`
  - `stride_length_meters: double`
  - `preferred_units: string`
  - `daily_step_goal: int32`
  - `daily_water_goal_ml: int32`
- Persisted migration metadata:
  - `schema_version: int32`
  - `migration_state: enum { NONE, MIGRATING, COMPLETE, FAILED }`
  - `migrated_at_epoch_ms: int64`
  - `last_write_epoch_ms: int64`
- Validation/default rules:
  - preserve nulls for `displayName`, `weightKg`, `heightCm`
  - default `strideLengthMeters = 0.78` if absent/invalid
  - default `preferredUnits = "metric"`
  - default `dailyStepGoal = 10000`
  - default `dailyWaterGoalMl = 2000`
  - reject or coerce malformed numeric legacy values instead of crashing

Compatibility contract
- `UserProfileRepository` must not change during the migration slice.
- `SettingsViewModel`, `StepViewModel`, home/water goal readers, and tests should continue consuming `UserProfile` exactly as they do now.
- Blocking reads used today should keep working, either through a synchronous cache, a bootstrap snapshot, or a narrowly-scoped blocking helper built on top of the new store.

Migration flow

Phase A — parallel store introduction
- Add the new secure store implementation without enabling destructive migration.
- New code can read/write the new store in isolation during tests/dev validation.
- Legacy encrypted prefs remain untouched.

Phase B — idempotent one-time migration
- Trigger migration on first profile access after upgrade.
- Read legacy encrypted prefs.
- Map and validate all fields.
- Write the full payload into the new secure store atomically.
- Re-read the new store to verify successful persistence.
- Only then mark `migration_state = COMPLETE`.
- If anything fails, keep legacy data untouched and fall back safely.

Phase C — cutover with fallback
- Reads become new-store-first.
- If migration is incomplete or the new store is unreadable, fall back to legacy profile reads.
- Writes go to the new store once migration succeeds.
- Optional: retain dual-write for one release if additional rollback assurance is desired.

Phase D — legacy retirement
- After at least one stable release, remove legacy write paths.
- Only delete legacy `user_profile_encrypted` keys when:
  - migration marker is complete
  - new store re-reads successfully
  - rollback window is intentionally closed

Rollback strategy
- Do not delete legacy encrypted prefs during the initial migration release.
- If the new store fails validation, decrypt, or readback, use legacy values.
- If an app rollback/downgrade happens, the previous app version should still find its original legacy encrypted data intact.
- Treat the first migration release as additive, not replacement-only.

Error handling and recovery
- Corruption, keystore exceptions, serializer failures, or malformed legacy values must not crash app startup.
- Recovery order:
  1. try new secure store
  2. if unavailable, try legacy encrypted prefs
  3. if both fail, emit safe defaults
- Do not log sensitive values; only log coarse-grained migration state or error category in debug-safe form.

Testing strategy
- Unit tests:
  - mapper tests for every profile field
  - defaults/nullability preservation
  - malformed legacy value handling
  - idempotent migration rerun
- Repository tests:
  - observe/update behavior unchanged across migration boundary
  - fallback from new-store failure to legacy reader
  - reset/profile update regression coverage
- Integration/manual tests:
  - install old build, save profile, upgrade, verify profile retained
  - migrate once, reopen app, confirm migration is not re-run destructively
  - simulated corruption/keystore failure fallback
  - downgrade/rollback behavior if still supported

Observability / migration signals
- Track internal migration states via stored metadata rather than analytics-only assumptions.
- Useful debug-visible markers:
  - schema version
  - migration state
  - last migration/write time
- Avoid exposing raw sensitive field values in logs or diagnostics.

Recommended implementation slices
- Slice 1: introduce new schema/store + pure tests, no runtime migration enabled
- Slice 2: add migration coordinator + idempotency/fallback tests
- Slice 3: switch repository reads/writes to new-store-first with legacy fallback
- Slice 4: legacy cleanup after one stable release

Acceptance criteria for final migration completion
- `UserProfileRepository` callers require no behavior changes.
- Existing users retain profile data across upgrade.
- New-store corruption does not crash the app and legacy fallback works during the rollout window.
- Deprecated `EncryptedSharedPreferences` usage is removed from production code once the retirement slice is complete.

Cross-reference
- Execution tracker and task statuses live in `plan.md` under the security/migration sections.


