# Run feature redesign plan

## Problem

The current run feature already has a working tracking skeleton, Room persistence, replay helpers, and an osmdroid route map, but the experience is still basic:

- `RunScreen` is function-first, not polished like `SleepScreen`.
- The route map is serviceable, but not premium-looking.
- Replay is only an in-app slider/playback for the latest run and does not export a shareable asset.
- Analysis is limited to basic aggregate stats.
- Permission requests are too close to implementation concerns and not yet structured around best-practice user education.

The goal is to redesign Run so it feels as deliberate as Sleep while staying scrollable, adding a polished map, deeper analysis, and a 15-second shareable replay video with adaptive map framing.

## Progress

- **Completed:** `run-dashboard-shell`
  - `RunScreen` now uses a scrollable dashboard structure instead of the older utilitarian layout.
  - The hero ring is part of the page flow, not pinned above it.
  - The dashboard now includes: hero ring, run controls/status, permission guidance card, latest route card, summary metric grid, insights card, and history cards.
  - This phase intentionally keeps the existing run service and existing map stack in place; MapLibre migration, detailed analytics, and replay export remain pending.
- **Completed:** `run-details-flow`
  - Added a dedicated `RunDetailsScreen` and navigation route for individual runs.
  - The dashboard now sends users from the latest-route preview and history cards into the dedicated details surface.
  - Replay controls now live on the details screen instead of crowding the dashboard.
  - The details screen includes the phase-1 shell for route review, summary, replay placement, analysis placement, and replay-export placement.
- **Completed:** `run-permissions`
  - Extracted Run permission timing and card decisions into a dedicated permission coordinator.
  - The dashboard no longer surfaces background location too early; it now waits until there is an active or paused run that would benefit.
  - Notification prompting is now tied to entering foreground tracking instead of being treated like an always-visible dashboard ask.
  - `RunScreen` now delegates permission-card content and start-action decisions instead of embedding those rules inline.
- **Completed:** `run-maplibre-migration`
  - Replaced the run map dependency from osmdroid to MapLibre.
  - Added a dedicated `RunMapStyleProvider` backed by OpenFreeMap vector styles.
  - Rebuilt `RunMapView` on top of `org.maplibre.android.maps.MapView` with MapLibre camera updates, route polylines, and start/current-finish markers.
  - Updated the run map instrumentation test to compile against MapLibre and validate route annotations/camera behavior.
  - Current implementation uses MapLibre's built-in annotation APIs as the bridge step; that keeps phase 2 moving while adaptive camera and replay work land later.
  - Follow-up for later phases: move from the built-in annotation APIs to more advanced layering or plugin-based rendering when adaptive camera behavior and richer replay/export visuals are implemented.
- **Completed:** `run-data-modeling`
  - Extended the run repository contract with first-class flows for latest run, individual runs, and run points.
  - Added DAO support for observing a single run and observing route points by run id.
  - Added an `ActiveRunSession` model and exposed richer live run state from `RunViewModel`.
  - Switched `RunDetailsScreen` to consume repository-backed run/run-point flows instead of reconstructing everything from the runs list alone.
  - The encoded polyline remains in place as compatibility storage, but route points are now a first-class read path for later analytics and replay work.
- **Completed:** `run-analytics`
  - Added `RunAnalyticsCalculator` plus analytics models for speed samples, split summaries, and insight strings.
  - `RunDetailsScreen` now renders real analytics sections instead of placeholders: summary metrics, speed profile, split list, and insight cards.
  - Analytics are derived from timed `run_points`, with encoded polyline remaining as compatibility fallback for replay rendering.
  - Current analytics scope covers speed, pace, splits, and pacing insights; adaptive camera logic and export-specific visual rendering remain later tasks.
- **Completed:** `run-camera-bounds`
  - Added a reusable `RunMapViewportCalculator` so the map camera fits route bounds from visible points instead of relying on a fixed zoom fallback.
  - Very short runs now expand to a pleasant minimum visible area instead of over-zooming into a dot.
  - Route fitting now scales with covered distance, with padded bounds that grow more gracefully as the route expands.
  - Replay camera updates now animate through the computed bounds, which sets up later frame-based replay/export work without changing the temporary MapLibre annotation bridge yet.
- **Completed:** `run-replay-refactor`
  - Added a dedicated `RunReplayTimelineBuilder` plus `RunReplayFrame` models for interactive replay and future fixed-duration export timelines.
  - `RunDetailsScreen` now drives replay from timeline frames instead of a raw point-index loop, so playback uses frame metadata, elapsed progress, covered distance, and frame-aware camera state.
  - `RunMapView` can now consume a replay viewport override from the active frame, keeping replay camera behavior aligned with the new adaptive bounds work.
  - This keeps MapLibre's built-in annotation APIs as the temporary rendering bridge while the later MP4 export phase builds on the same frame model.
- **Completed:** `run-replay-export`
  - Added a dedicated export stack: `RunReplayExporter`, `RunMapSnapshotRenderer`, `RunReplayOverlayRenderer`, `RunReplayVideoEncoder`, and `RunShareFileWriter`.
  - The details screen can now render and share a deterministic 15-second MP4 replay with adaptive frame-based camera bounds, route progress overlays, and minimal distance/time/pace stat treatment.
  - Exported files are written to cache, exposed through a `FileProvider`, and launched through Android's share sheet from the run details route.
  - Export failures are surfaced as explicit replay-export exceptions and shown back in the details UI instead of failing silently.
- **Completed:** `run-phase6-performance-pass`
  - Reworked replay timeline generation so frame selection advances with a monotonic source index instead of rescanning the route for every frame.
  - Cached prefix bounds per replay point and reused them to build viewports without repeatedly slicing and remapping visible route prefixes.
  - Removed per-frame visible-route list copying in replay overlay rendering, which reduces allocation churn during long-route export.
- **Completed:** `run-phase6-permission-verification`
  - Run permission orchestration is now version-aware instead of treating all permission paths the same across Android releases.
  - Notification prompting is only considered on Android 13+; earlier versions treat notifications as already available for run-flow decisions.
  - Deferred background-location guidance now routes Android 11+ users to app settings, while Android 10 keeps the direct runtime-request path.
  - Added coordinator coverage for Android 13- and Android 11+ permission behavior so the flow stays stable as the run UI evolves.
- **Completed:** `run-phase6-test-expansion`
  - Added direct `RunViewModelTest` coverage for active-session construction, persisted UI-running state, and completed-run clearing behavior.
  - Added dashboard empty-state coverage and fixed a real bug where the Run dashboard could incorrectly report "Run paused" when there were no runs at all.
  - Added pure replay/export UI-state coverage for the run details screen and unified paused-run detection to prevent dashboard/permission drift.
  - Emulator-backed instrumentation validation now passes after fixing first-frame replay marker behavior and surfacing step permission/manual-entry actions earlier on the Steps screen.
  - Replaced the fragile `RunScreenPaparazziTest` placeholder with stable `androidTest` coverage for `RunScreenContent` and `RunDetailsScreenContent`.

## Current state summary

### Existing foundations already in the app

- `RunTrackingService` records points, distance, duration, and average speed.
- `RunEntity` stores summary metrics and an encoded polyline.
- `RunPointEntity` stores individual route points with timestamps.
- `RunReplayHelper` supports replay point normalization, `RunReplayTimelineBuilder` builds frame-based replay timelines, and `RunReplayExporter` now turns those frames into a shareable MP4.
- `RunMapView` renders the route with MapLibre and adaptive bounds fitting.
- Unit and instrumentation tests already exist for service, replay helper, and map rendering.

### Remaining gaps / stabilization focus

- Phase 6 stabilization is complete across performance tuning, permission verification, broader durable coverage, and emulator-backed validation.
- Fragile run placeholder snapshot coverage has been removed in favor of stable behavior tests for `RunScreenContent` and `RunDetailsScreenContent`.
- Post-Phase-6 follow-up work now covers replay camera progression and short-route minimum-area handling, including high-latitude minimum-width framing.
- The dashboard latest-route card now keeps its details CTA visible before the map and preserves that entry point even before route points are available.
- Remaining run work is no longer a Phase 6 blocker; future additions are incremental coverage polish rather than known functional gaps.
- Device validation has now been exercised on the emulator, and the current `connectedDebugAndroidTest` suite passes.

## Product/UX direction

### 1. Main run screen

Use the Sleep screen as the structural reference, but do **not** pin the ring above the scroll container.

Recommended layout:

1. `TopAppBar`
2. `LazyColumn`
3. First item = large `RunHeroRingCard`
4. Next item = run action/status card
5. Next item = permission education/status card when needed
6. Next item = latest run map card
7. Next items = analysis cards/charts
8. Next items = run history list

This keeps the ring visually prominent like Sleep while remaining part of page flow and fully scrollable.

### 2. Ring behavior

The ring should communicate one clear progress model. Recommended default:

- **Today’s distance vs daily goal** if the app already stores/introduces a run distance goal.
- If no goal exists yet, phase 1 can use **weekly target distance** or **latest run completion summary**, but the preferred product direction is a configurable distance goal in profile/settings later.

Ring content:

- primary number: today/this week distance
- secondary label: goal distance
- tertiary label: run streak / active status / "Ready to run"

### 3. Map card

Replace the current osmdroid presentation with a MapLibre-based map using OpenStreetMap-backed vector styling.

Design goals:

- rounded card with generous padding
- light/dark map style matched to app theme
- thicker route line with subtle glow
- clear start/end markers
- optional progress marker during replay
- smooth camera transitions
- proper attribution handling

### 4. Analysis area

Recommended analysis blocks:

- summary grid: distance, moving time, elapsed time, avg pace, avg speed, max speed, calories
- speed/pace chart across run duration
- split list or split chart (per km)
- elevation card only if altitude is captured reliably
- run quality insights: steady pace, strong finish, large pauses, best segment

### 5. Replay and share

Add two related but distinct experiences:

- **Interactive replay** inside the run details UI
- **15-second MP4 replay export** for sharing

The export should not simply screen-record the interactive map. It should render a clean, deterministic video with:

- fixed 15 second duration
- adaptive camera bounds
- route progress animation
- optional stat overlays: distance, elapsed time, average pace
- consistent padding and map framing across devices

## Permission strategy and timing

Based on current Android guidance, permissions should be requested in context, only when the user starts a feature that requires them, and background access should be deferred until the user explicitly benefits from it.

### Recommended flow

#### On first opening Run

- Do **not** immediately show the system permission dialog.
- Show a small educational card that explains what the feature can do:
  - track outdoor runs
  - keep the route accurate
  - optionally continue tracking when the app is backgrounded

#### When user taps "Start Run"

1. Show an in-app rationale sheet/card if location is not granted.
2. Request `ACCESS_FINE_LOCATION`.
3. If granted, start tracking.
4. On API 33+, request `POST_NOTIFICATIONS` only after location is granted and just before/after foreground tracking starts so the persistent tracking notification is understandable.

#### When to ask for background location

Do **not** ask for `ACCESS_BACKGROUND_LOCATION` on first entry or before the user has started a run.

Ask only when one of these happens:

- the user explicitly enables "Track while app is in background"
- the user starts a run and then attempts to leave the app while tracking
- the user taps a dedicated "Improve tracking outside the app" affordance

For Android 11+, use a rationale screen and then deep-link to app settings for background location. The UI should clearly explain why background access helps and that the user can continue using the app without it.

### Permission denial behavior

- If fine location is denied: keep history and analysis visible, disable live tracking CTA, show retry/settings affordance.
- If notifications are denied: tracking still works, but explain that persistent run status visibility is limited.
- If background location is denied: continue foreground tracking and prompt later only on explicit user action.

## Recommended architecture

## UI architecture

### Screen split

Recommended structure:

- `RunScreen` = run home/dashboard
- `RunDetailsScreen` = a selected run’s full map, replay, and analysis

Why:

- avoids overloading the main screen
- keeps replay export tied to a concrete completed run
- mirrors how polished fitness apps separate "overview" from "activity detail"

### Compose components

#### Main screen

- `RunScreen`
- `RunHeroRingCard`
- `RunActionCard`
- `RunPermissionCard`
- `RunLatestMapCard`
- `RunSummaryGrid`
- `RunInsightsCard`
- `RunHistorySection`
- `RunHistoryCard`

#### Details screen

- `RunDetailsScreen`
- `RunDetailsHeader`
- `RunReplayCard`
- `RunAnalysisChartCard`
- `RunSplitListCard`
- `RunShareReplayButton`
- `RunReplayExportProgressDialog`

### UI state models

```kotlin
data class RunUiState(
    val activeRun: ActiveRunUiModel?,
    val latestRun: RunSummaryUiModel?,
    val history: List<RunSummaryUiModel>,
    val hero: RunHeroUiModel,
    val permissionState: RunPermissionUiState,
    val isTracking: Boolean,
    val isBackgroundTrackingEnabled: Boolean,
)

data class RunDetailsUiState(
    val run: RunSummaryUiModel,
    val map: RunMapUiModel,
    val analysis: RunAnalysisUiModel,
    val replay: RunReplayUiState,
    val exportState: ReplayExportUiState,
)
```

## ViewModel layer

### Recommended ViewModels

- keep `RunViewModel`, but expand it into dashboard state + live run actions
- add `RunDetailsViewModel`

```kotlin
class RunViewModel(
    private val repo: RunRepository,
    private val uiStateStore: UiStateStore,
    private val analyticsCalculator: RunAnalyticsCalculator,
    private val permissionCoordinator: RunPermissionCoordinator,
) : ViewModel()

class RunDetailsViewModel(
    private val repo: RunRepository,
    private val analyticsCalculator: RunAnalyticsCalculator,
    private val replayTimelineBuilder: RunReplayTimelineBuilder,
    private val replayExporter: RunReplayExporter,
) : ViewModel()
```

### ViewModel responsibilities

`RunViewModel`

- observe active/latest/history runs
- expose hero ring model
- coordinate start/pause/resume/stop
- expose permission guidance state

`RunDetailsViewModel`

- load run + route points
- compute charts/splits/insights
- drive interactive replay
- trigger MP4 export

## Data and persistence

### Keep existing entities, but extend them

The current schema is a good starting point. For the richer feature set, plan a Room migration to add analytics-friendly metadata.

#### `RunEntity` recommended shape

```kotlin
@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val status: String, // active, paused, completed, cancelled
    val distanceMeters: Double,
    val movingDurationSec: Long,
    val elapsedDurationSec: Long,
    val avgSpeedMps: Double,
    val maxSpeedMps: Double,
    val calories: Double,
    val elevationGainMeters: Double?,
    val polyline: String,
)
```

#### `RunPointEntity` recommended shape

```kotlin
@Entity(tableName = "run_points")
data class RunPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val lat: Double,
    val lon: Double,
    val tsMs: Long,
    val altitudeMeters: Double?,
    val accuracyMeters: Float?,
    val speedMps: Float?,
    val segmentIndex: Int,
)
```

#### Optional later entity

If chart computation becomes expensive, add:

```kotlin
@Entity(tableName = "run_splits")
data class RunSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val splitIndex: Int,
    val distanceMeters: Double,
    val durationSec: Long,
    val paceSecPerKm: Double,
)
```

### Repository contract

`RunRepository` should evolve beyond the current aggregate-only API:

```kotlin
interface RunRepository {
    fun observeRuns(): Flow<List<RunEntity>>
    fun observeRun(runId: Long): Flow<RunEntity?>
    fun observeLatestRun(): Flow<RunEntity?>
    suspend fun getRunById(id: Long): RunEntity?
    suspend fun getRunPoints(runId: Long): List<RunPointEntity>
    suspend fun startRun(run: RunEntity): Long
    suspend fun updateRun(run: RunEntity)
    suspend fun addLocationPoint(point: RunPointEntity)
}
```

## Domain / helper classes

### 1. Analytics

```kotlin
class RunAnalyticsCalculator {
    fun buildSnapshot(
        run: RunEntity,
        points: List<RunPointEntity>,
    ): RunAnalysisSnapshot
}
```

Outputs:

- splits
- speed samples
- pace samples
- steady-state insights
- map bounds metadata

### 2. Adaptive camera logic

```kotlin
data class RunCameraBounds(
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
)

class RunCameraBoundsCalculator {
    fun forFullRoute(points: List<RunPointEntity>): RunCameraBounds
    fun forReplayProgress(
        points: List<RunPointEntity>,
        visibleCount: Int,
        minimumSpanMeters: Double,
        paddingPercent: Double,
    ): RunCameraBounds
}
```

### 3. Replay timeline

```kotlin
class RunReplayTimelineBuilder {
    fun buildInteractiveTimeline(
        points: List<RunPointEntity>,
    ): List<ReplayFrame>

    fun buildFixedDurationTimeline(
        points: List<RunPointEntity>,
        durationMs: Long = 15_000L,
        fps: Int = 30,
    ): List<ReplayFrame>
}
```

`ReplayFrame` should include:

- visible point count
- normalized progress
- display timestamp/distance
- camera bounds for that frame

### 4. Replay export

```kotlin
class RunReplayExporter(
    private val snapshotRenderer: RunMapSnapshotRenderer,
    private val videoEncoder: RunReplayVideoEncoder,
    private val shareFileWriter: RunShareFileWriter,
) {
    suspend fun exportRunReplay(runId: Long): Uri
}
```

Supporting classes:

- `RunMapSnapshotRenderer`
- `RunReplayVideoEncoder`
- `RunShareFileWriter`
- `RunReplayOverlayRenderer`

### 5. Permission orchestration

```kotlin
class RunPermissionCoordinator {
    fun shouldPromptFineLocation(...)
    fun shouldPromptNotifications(...)
    fun shouldPromptBackgroundLocation(...)
    fun buildRationale(...)
}
```

This keeps permission timing decisions out of `RunScreen`.

## Map implementation plan

## Why move away from osmdroid

osmdroid works for basic plotting, but for the target UX it is the wrong long-term fit:

- weaker styling/theming story
- less polished vector presentation
- less suitable for premium replay/share visuals
- harder to make feel like modern fitness apps

## Recommended map stack

- `org.maplibre.gl:android-sdk`
- OpenStreetMap-backed vector style JSON
- style URL/configurable provider in app code

Important note:

- do not rely on demo tiles in production
- use a configurable style source and confirm attribution/tile usage policy before release

## Map classes

```kotlin
class RunMapStyleProvider {
    fun styleUrl(isDarkTheme: Boolean): String
}

data class RunMapViewportSpec(
    val bounds: RunCameraBounds,
    val paddingPx: Int,
    val animate: Boolean,
)
```

Refactor `RunMapView` to render:

- full route
- replay-progress route
- start/end/progress markers
- theme-aware style
- camera updates from `RunMapViewportSpec`

## Adaptive map scaling algorithm

This is a core requirement and should be implemented explicitly, not as a side effect of whatever the map decides.

### Desired behavior

- very short runs should still show a pleasant minimum area instead of an over-zoomed dot
- as the route grows, the camera should expand to fit the covered area
- during replay/export, the visible map should be based on the route covered **so far**, not only the full route bounds

### Recommended algorithm

1. Build bounds from the visible route points.
2. Compute center point.
3. Expand the bounds to a minimum square/rectangular area if the current route is too small.
4. Add padding that scales with content but never drops below a fixed minimum.
5. Smooth camera updates so the viewport does not jitter every frame.

### Recommended defaults

- minimum visible span: `250m - 400m`
- camera padding: `10% - 14%` of bounds span
- lower bound on padding in pixels for overlays
- use eased interpolation between successive replay frames

### Important export behavior

For the 15-second MP4 export:

- the viewport should **mostly expand** as the route expands
- avoid aggressive zoom-in/zoom-out oscillation
- prefer stability over maximum tightness

That means replay/export camera logic should likely use:

- monotonic or lightly-smoothed expansion
- optional limited recentering

instead of recomputing a fully tight box every frame.

## Replay export design

## Functional requirement

Export a deterministic 15-second MP4 showing the entire run replay with route growth and adaptive map framing.

## Implementation approach

### Step 1: Normalize the run

- load ordered `RunPointEntity` values
- remove unusable/jitter points based on accuracy and distance thresholds
- compute cumulative distance

### Step 2: Build a fixed output timeline

- output length: `15_000 ms`
- frame rate: `30 fps`
- total frames: `450`

Each frame maps to a route progress position. Use actual timestamps when present, but normalize the full run to the 15-second export duration.

### Step 3: Render frames

For each frame:

- determine visible route subset
- determine camera bounds with minimum area rules
- render base map snapshot
- draw route progress line and progress marker
- draw overlays for distance/time/pace if desired

### Step 4: Encode video

- encode to MP4 using `MediaCodec` + `MediaMuxer`
- write to cache/external cache
- expose via `FileProvider`
- launch Android share sheet

## Recommendation on overlay density

Keep overlays minimal:

- distance
- elapsed time
- pace

Avoid packing too many stats into the share asset; the map and motion should remain the hero.

## Run analysis plan

### Phase 1 analysis

- total distance
- moving time
- elapsed time
- avg speed
- avg pace
- max speed
- calories
- split list (per km)

### Phase 2 analysis

- speed chart by time
- pace chart by distance
- pause markers
- best split / slowest split
- consistency insight

### Phase 3 analysis if altitude is available

- elevation gain/loss
- grade-aware insights

## Service and tracking upgrades

`RunTrackingService` should remain the recorder, but it needs more precise state handling.

Recommended improvements:

- maintain explicit run status: active/paused/completed
- separate moving time from elapsed time
- store accuracy/speed/altitude if available
- write pause/resume segment boundaries
- expose more stable live state for UI observation

Potential supporting model:

```kotlin
data class ActiveRunSession(
    val runId: Long,
    val isPaused: Boolean,
    val distanceMeters: Double,
    val movingDurationSec: Long,
    val elapsedDurationSec: Long,
    val currentSpeedMps: Double,
)
```

## Navigation plan

Keep `AppRoutes.RUN` for the dashboard and add a details route:

```kotlin
object AppRoutes {
    const val RUN = "run"
    const val RUN_DETAILS = "run/{runId}"
}
```

History cards and latest-run map should navigate to the details screen.

## Testing plan

### Unit tests

- `RunAnalyticsCalculatorTest`
- `RunCameraBoundsCalculatorTest`
- `RunReplayTimelineBuilderTest`
- `RunReplayExporterTest` (timeline + encoder coordination with fakes)
- `RunPermissionCoordinatorTest`

### UI tests

- `RunScreenContentTest`
- `RunDetailsScreenTest`
- permission card visibility / CTA states
- ring rendering semantics
- replay controls semantics

### Map/instrumentation tests

- `RunMapViewTest` for MapLibre overlay/camera behavior
- replay camera bounds progression
- minimum-area handling for short routes

### Existing baseline note

The old `RunScreenPaparazziTest` placeholder was removed after replacing it with stable instrumentation coverage of actual `RunScreenContent` / `RunDetailsScreenContent` behavior.

## Execution phases

### Phase 1: foundation and UX shell

- introduce new run UI structure **(completed)**
- keep ring scrollable in the list **(completed)**
- create details screen shell **(completed)**
- separate permission logic into coordinator **(completed)**

### Phase 2: map modernization

- add MapLibre dependency **(completed)**
- replace osmdroid-based `RunMapView` **(completed)**
- implement themed vector style, route line, markers, and bounds control **(completed)**

### Phase 3: analytics

- extend repository access to run points **(completed)**
- add analytics calculator **(completed)**
- add splits and speed/pace charts **(completed)**

### Phase 4: replay

- refactor interactive replay around timeline frames and viewport specs **(completed)**
- add adaptive camera scaling **(completed)**

### Phase 5: export/share

- render fixed 15-second replay **(completed)**
- encode MP4 **(completed)**
- expose share sheet **(completed)**

### Phase 6: stabilization

- add/repair tests **(completed)**
- performance tuning for long routes **(completed)**
- verify permission flows on Android 13+ and Android 11+ background-location path **(completed)**

## Todos

1. Audit and redesign the Run dashboard structure to match Sleep’s premium feel while keeping the hero ring scrollable. **(completed)**
2. Introduce a proper run details flow so replay, map, and analysis do not overload the main screen. **(completed)**
3. Replace osmdroid with MapLibre plus an OpenStreetMap-backed vector style provider and attribution-safe configuration. **(completed)**
4. Refactor run data access so run points and richer live state are first-class repository/viewmodel inputs. **(completed)**
5. Implement analytics models and calculators for splits, pace/speed charts, and run insights. **(completed)**
6. Add a permission coordinator so fine location, notifications, and background location are requested only at the right moments. **(completed)**
7. Build adaptive camera bounds logic with minimum visible area and smooth replay/export scaling. **(completed)**
8. Rebuild interactive replay on top of a frame/timeline model rather than the current simple point index loop. **(completed)**
9. Implement 15-second MP4 replay export using map snapshots, overlay rendering, and video encoding. **(completed)**
10. Expand and stabilize tests around map behavior, analytics, replay, permission flows, and the existing run snapshot baseline. **(completed)**
11. Review long-route rendering/export performance and tune hotspots in map, replay, and export paths. **(completed)**
12. Verify Android 13+ notification prompting and Android 11+ deferred background-location behavior for run tracking. **(completed)**

## Notes / important decisions

- Map stack confirmed: **MapLibre / OpenStreetMap**
- Share format confirmed: **MP4 video**
- Best-practice permission timing: **ask in context, defer background location until the user clearly benefits**
- Recommended product direction: **dashboard + details screen**, not one giant run page
- Recommended technical direction: **MapLibre migration first, replay/export after adaptive bounds and analytics foundations exist**
- Phase 1 status: **completed**
- Current focus: **Phase 6 complete; latest-route CTA polish is done, with only small incremental run polish remaining**
