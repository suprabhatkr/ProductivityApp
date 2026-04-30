package com.example.productivityapp.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.UiStateStore
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.RunPointEntity
import com.example.productivityapp.data.repository.RunRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RunViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var appContext: Context
    private lateinit var uiStateStore: UiStateStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        appContext = ApplicationProvider.getApplicationContext()
        appContext.getSharedPreferences("ui_state_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        uiStateStore = UiStateStore(appContext)
    }

    @After
    fun tearDown() {
        if (::appContext.isInitialized) {
            appContext.getSharedPreferences("ui_state_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun incompleteLatestRun_buildsPausedActiveSessionWhenUiIsNotRunning() = runTest(dispatcher) {
        val latestRun = activeRun(id = 11L, distanceMeters = 1800.0, durationSec = 540L, avgSpeedMps = 3.33)
        val repo = FakeRunRepository(runs = listOf(latestRun), latestRun = latestRun)

        val vm = RunViewModel(repo, uiStateStore)
        runCurrent()

        val activeSession = vm.activeRunSession.value
        assertNotNull(activeSession)
        assertEquals(11L, activeSession?.runId)
        assertTrue(activeSession?.isPaused == true)
        assertEquals(1800.0, activeSession?.distanceMeters ?: 0.0, 0.0)
        assertFalse(vm.uiRunning.value)
    }

    @Test
    fun setUiRunning_updatesPersistedFlagAndActiveSessionPauseState() = runTest(dispatcher) {
        val latestRun = activeRun(id = 21L, distanceMeters = 2400.0, durationSec = 720L, avgSpeedMps = 3.5)
        val repo = FakeRunRepository(runs = listOf(latestRun), latestRun = latestRun)
        val vm = RunViewModel(repo, uiStateStore)
        runCurrent()

        vm.setUiRunning(true)

        assertTrue(vm.uiRunning.value)
        assertTrue(uiStateStore.isRunUiRunning())
        assertFalse(vm.activeRunSession.value?.isPaused ?: true)
    }

    @Test
    fun completedLatestRun_clearsActiveSession() = runTest(dispatcher) {
        val activeRun = activeRun(id = 31L)
        val completedRun = activeRun.copy(endTime = activeRun.startTime + 1_000L)
        val repo = FakeRunRepository(runs = listOf(activeRun), latestRun = activeRun)
        val vm = RunViewModel(repo, uiStateStore)
        runCurrent()
        assertNotNull(vm.activeRunSession.value)

        repo.latestRunFlow.value = completedRun
        repo.runsFlow.value = listOf(completedRun)
        runCurrent()

        assertNull(vm.activeRunSession.value)
        assertEquals(completedRun, vm.latestRun.value)
    }

    private fun activeRun(
        id: Long,
        distanceMeters: Double = 1200.0,
        durationSec: Long = 360L,
        avgSpeedMps: Double = 3.0,
    ): RunEntity = RunEntity(
        id = id,
        startTime = 1_700_000_000_000L + id,
        endTime = null,
        distanceMeters = distanceMeters,
        durationSec = durationSec,
        avgSpeedMps = avgSpeedMps,
        calories = 90.0,
        polyline = "",
    )
}

private class FakeRunRepository(
    runs: List<RunEntity> = emptyList(),
    latestRun: RunEntity? = runs.firstOrNull(),
) : RunRepository {
    val runsFlow = MutableStateFlow(runs)
    val latestRunFlow = MutableStateFlow(latestRun)

    override fun observeRuns(): Flow<List<RunEntity>> = runsFlow

    override fun observeLatestRun(): Flow<RunEntity?> = latestRunFlow

    override fun observeRun(id: Long): Flow<RunEntity?> = flowOf(runsFlow.value.firstOrNull { it.id == id })

    override fun observeRunPoints(runId: Long): Flow<List<RunPointEntity>> = flowOf(emptyList())

    override suspend fun getRunById(id: Long): RunEntity? = runsFlow.value.firstOrNull { it.id == id }

    override suspend fun getRunPoints(runId: Long): List<RunPointEntity> = emptyList()

    override suspend fun startRun(run: RunEntity): Long {
        runsFlow.value = listOf(run) + runsFlow.value
        latestRunFlow.value = run
        return run.id
    }

    override suspend fun updateRun(run: RunEntity) {
        runsFlow.value = runsFlow.value.map { existing -> if (existing.id == run.id) run else existing }
        if (latestRunFlow.value?.id == run.id) {
            latestRunFlow.value = run
        }
    }

    override suspend fun finishRun(runId: Long) = Unit

    override suspend fun addLocationPoint(point: RunPointEntity) = Unit
}
