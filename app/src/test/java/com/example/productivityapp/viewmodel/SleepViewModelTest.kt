package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startStopPauseResumeAndReviewFlow_updatesRepositoryState() = runTest(dispatcher) {
        val repo = FakeSleepRepository()
        val vm = SleepViewModel(repo)
        runCurrent()

        vm.startSleep()
        runCurrent()
        val active = vm.activeSession.value
        assertNotNull(active)
        assertEquals(SleepDetectionSource.MANUAL.storageValue, active?.detectionSource)
        assertEquals(SleepReviewState.CONFIRMED.storageValue, active?.reviewState)
        assertEquals(1.0, active?.confidenceScore ?: 0.0, 0.0)

        vm.pauseSleep()
        assertTrue(vm.isPaused.value)

        vm.resumeSleep()
        assertFalse(vm.isPaused.value)

        vm.stopSleep()
        runCurrent()
        assertNull(vm.activeSession.value)
        val pending = vm.pendingReviewSession.value
        assertNotNull(pending)
        assertEquals(SleepReviewState.NEEDS_REVIEW.storageValue, pending?.reviewState)

        vm.submitSleepReview(5, "Slept well")
        runCurrent()
        assertNull(vm.pendingReviewSession.value)
        val saved = repo.allSessions.value.first()
        assertEquals(5, saved.sleepQuality)
        assertEquals("Slept well", saved.notes)
        assertEquals(SleepReviewState.CONFIRMED.storageValue, saved.reviewState)
    }

    @Test
    fun startNapTimer_createsNapSessionWithNapMetadata() = runTest(dispatcher) {
        val repo = FakeSleepRepository()
        val vm = SleepViewModel(repo)
        runCurrent()

        vm.startNapTimer()
        runCurrent()

        val nap = vm.activeSession.value
        assertNotNull(nap)
        assertEquals(SleepDetectionSource.NAP.storageValue, nap?.detectionSource)
        assertEquals(SleepReviewState.CONFIRMED.storageValue, nap?.reviewState)
        assertEquals("nap", nap?.tagsCsv)

        vm.stopSleep()
        runCurrent()
        assertNull(vm.activeSession.value)
    }

    @Test
    fun weeklySummary_aggregatesRecentHistory() = runTest(dispatcher) {
        val today = LocalDate.now()
        val repo = FakeSleepRepository(
            initial = listOf(
                SleepEntity(
                    id = 1,
                    date = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    startTimestamp = 1_000L,
                    endTimestamp = 2_000L,
                    durationSec = 3600,
                    sleepQuality = 4,
                    notes = null,
                    detectionSource = SleepDetectionSource.AUTO.storageValue,
                    confidenceScore = 0.82,
                    inferredStartTimestamp = 900L,
                    inferredEndTimestamp = 2100L,
                    reviewState = SleepReviewState.PROVISIONAL.storageValue,
                    tagsCsv = "overnight",
                ),
                SleepEntity(
                    id = 2,
                    date = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    startTimestamp = 3_000L,
                    endTimestamp = 4_000L,
                    durationSec = 7200,
                    sleepQuality = 5,
                    notes = null,
                    detectionSource = SleepDetectionSource.NAP.storageValue,
                    confidenceScore = 0.94,
                    inferredStartTimestamp = 2800L,
                    inferredEndTimestamp = 4300L,
                    reviewState = SleepReviewState.CONFIRMED.storageValue,
                    tagsCsv = "nap,weekend",
                ),
            )
        )

        val vm = SleepViewModel(repo)
        runCurrent()

        val summary = vm.weeklySummary.value
        assertEquals(7, summary.size)
        assertEquals(3600L, summary.last().totalDurationSec)
        assertEquals(7200L, summary[5].totalDurationSec)
    }

    @Test
    fun detectedReviewFlow_loadsProvisionalSessionAndSupportsAcceptAdjustDismiss() = runTest(dispatcher) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val repo = FakeSleepRepository(
            initial = listOf(
                SleepEntity(
                    id = 77L,
                    date = today,
                    startTimestamp = 1_000L,
                    endTimestamp = 9_000L,
                    durationSec = 8_000L,
                    sleepQuality = null,
                    notes = null,
                    detectionSource = SleepDetectionSource.AUTO.storageValue,
                    confidenceScore = 0.89,
                    inferredStartTimestamp = 1_000L,
                    inferredEndTimestamp = 9_000L,
                    reviewState = SleepReviewState.PROVISIONAL.storageValue,
                    tagsCsv = "overnight",
                )
            )
        )

        val vm = SleepViewModel(repo)
        runCurrent()

        val detected = vm.pendingDetectedReviewSession.value
        assertNotNull(detected)
        assertEquals(SleepReviewState.PROVISIONAL.storageValue, detected?.reviewState)

        vm.acceptDetectedSleepReview()
        runCurrent()
        assertEquals(SleepReviewState.CONFIRMED.storageValue, repo.allSessions.value.first().reviewState)

        repo.allSessions.value = listOf(
            SleepEntity(
                id = 78L,
                date = today,
                startTimestamp = 2_000L,
                endTimestamp = 12_000L,
                durationSec = 10_000L,
                sleepQuality = null,
                notes = null,
                detectionSource = SleepDetectionSource.AUTO.storageValue,
                confidenceScore = 0.82,
                inferredStartTimestamp = 2_000L,
                inferredEndTimestamp = 12_000L,
                reviewState = SleepReviewState.PROVISIONAL.storageValue,
                tagsCsv = "overnight",
            )
        )
        runCurrent()

        vm.adjustDetectedSleepReview(180, 4, "Adjusted")
        runCurrent()

        val adjusted = repo.allSessions.value.first()
        assertEquals(180 * 60L, adjusted.durationSec)
        assertEquals(4, adjusted.sleepQuality)
        assertEquals("Adjusted", adjusted.notes)

        repo.allSessions.value = listOf(
            SleepEntity(
                id = 79L,
                date = today,
                startTimestamp = 3_000L,
                endTimestamp = 13_000L,
                durationSec = 10_000L,
                sleepQuality = null,
                notes = null,
                detectionSource = SleepDetectionSource.AUTO.storageValue,
                confidenceScore = 0.75,
                inferredStartTimestamp = 3_000L,
                inferredEndTimestamp = 13_000L,
                reviewState = SleepReviewState.PROVISIONAL.storageValue,
                tagsCsv = "overnight",
            )
        )
        runCurrent()

        vm.dismissDetectedSleepReview()
        runCurrent()

        assertTrue(repo.allSessions.value.none { it.id == 79L })
    }
}

private class FakeSleepRepository(initial: List<SleepEntity> = emptyList()) : SleepRepository {
    val allSessions = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observeSleepForDate(date: String): Flow<List<SleepEntity>> =
        allSessions.map { list -> list.filter { it.date == date }.sortedByDescending { it.startTimestamp } }

    override fun observeSleepForRange(startDate: String, endDate: String): Flow<List<SleepEntity>> =
        allSessions.map { list ->
            list.filter { it.date >= startDate && it.date <= endDate }.sortedByDescending { it.startTimestamp }
        }

    override suspend fun getActiveSleepSession(): SleepEntity? =
        allSessions.value.firstOrNull { it.endTimestamp == 0L }

    override suspend fun getSleepById(id: Long): SleepEntity? =
        allSessions.value.firstOrNull { it.id == id }

    override suspend fun startSleep(session: SleepEntity): Long {
        val active = getActiveSleepSession()
        if (active != null) return active.id
        val persisted = session.copy(id = nextId++)
        allSessions.value = listOf(persisted) + allSessions.value
        return persisted.id
    }

    override suspend fun stopSleep(session: SleepEntity): SleepEntity {
        updateSleep(session)
        return session
    }

    override suspend fun updateSleep(session: SleepEntity) {
        allSessions.value = allSessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun deleteSleep(id: Long) {
        allSessions.value = allSessions.value.filterNot { it.id == id }
    }
}
