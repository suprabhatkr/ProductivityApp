
# ARCHIVED — implemented (2026-04-07)

This auto-issue was archived because the JVM/Robolectric repository tests were implemented and a local test run succeeded. A copy has been moved to `.github/auto-issues-archived/` for audit.

Original content below for reference:

# Add JVM / Robolectric unit tests for repositories & DataStore (CI-friendly)

Context

There are existing androidTest repository tests. Add Robolectric/JVM tests so CI can run without an emulator.

Checklist

- [ ] Convert repo tests into Robolectric/JVM tests under `app/src/test`.
- [ ] Add `DataStoreUnitTest` using `kotlinx-coroutines-test` with a temporary directory.
- [ ] Ensure tests run with `./gradlew :app:testDebugUnitTest` in CI.
- [ ] Document how to run tests locally and in CI.

Suggested test files

- `app/src/test/java/.../StepRepositoryUnitTest.kt`
- `app/src/test/java/.../RunRepositoryUnitTest.kt`
- `app/src/test/java/.../SleepRepositoryUnitTest.kt`
- `app/src/test/java/.../DataStoreUnitTest.kt`

