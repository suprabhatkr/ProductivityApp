package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        assertNotNull(vm.activeSession.value)

        vm.pauseSleep()
        assertTrue(vm.isPaused.value)

        vm.resumeSleep()
        assertFalse(vm.isPaused.value)

        vm.stopSleep()
        runCurrent()
        assertNull(vm.activeSession.value)
        assertNotNull(vm.pendingReviewSession.value)

        vm.submitSleepReview(5, "Slept well")
        runCurrent()
        assertNull(vm.pendingReviewSession.value)
        assertEquals(5, repo.allSessions.value.first().sleepQuality)
        assertEquals("Slept well", repo.allSessions.value.first().notes)
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
                ),
                SleepEntity(
                    id = 2,
                    date = today.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                    startTimestamp = 3_000L,
                    endTimestamp = 4_000L,
                    durationSec = 7200,
                    sleepQuality = 5,
                    notes = null,
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
}



