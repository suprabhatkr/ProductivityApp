package com.example.productivityapp.app.viewmodel

import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.data.repository.WaterRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_buildsCatchUpPromptWhenLateAndEmpty() = runTest(dispatcher) {
        val today = LocalDate.of(2026, 4, 30)
        val repo = FakeWaterRepository(
            mutableMapOf(
                today.toString() to MutableStateFlow(WaterDayData(date = today.toString(), goalMl = 2200))
            )
        )

        val vm = WaterViewModel(
            repository = repo,
            currentDateProvider = { today },
            currentTimeProvider = { LocalTime.of(13, 0) },
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Start hydrating now", state.paceTitle)
        assertTrue(state.paceMessage.contains("glass of water", ignoreCase = true))
        assertEquals("No entries yet", state.entriesSummary)
    }

    @Test
    fun uiState_reportsGoalReachedWhenGoalMet() = runTest(dispatcher) {
        val today = LocalDate.of(2026, 4, 30)
        val entry = WaterEntry(id = 1L, amountMl = 2100, timestamp = LocalDateTime.of(today, LocalTime.of(9, 15)))
        val repo = FakeWaterRepository(
            mutableMapOf(
                today.toString() to MutableStateFlow(
                    WaterDayData(
                        date = today.toString(),
                        entries = listOf(entry),
                        goalMl = 2000,
                    )
                )
            )
        )

        val vm = WaterViewModel(
            repository = repo,
            currentDateProvider = { today },
            currentTimeProvider = { LocalTime.of(18, 30) },
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Goal reached", state.paceTitle)
        assertEquals(0, state.remainingMl)
        assertTrue(state.completionText.contains("2000 ml goal"))
    }

    @Test
    fun addWaterAndRemoveEntry_delegateToRepositoryForSelectedDate() = runTest(dispatcher) {
        val today = LocalDate.of(2026, 4, 30)
        val repo = FakeWaterRepository(
            mutableMapOf(today.toString() to MutableStateFlow(WaterDayData(date = today.toString())))
        )
        val vm = WaterViewModel(
            repository = repo,
            currentDateProvider = { today },
            currentTimeProvider = { LocalTime.NOON },
        )
        advanceUntilIdle()

        val id = vm.addWaterAndGetId(350)
        vm.removeEntry(id)
        advanceUntilIdle()

        assertEquals(listOf(today.toString() to 350), repo.addedEntries)
        assertEquals(listOf(today.toString() to id), repo.removedEntries)
    }

    @Test
    fun refresh_switchesObservationToNewDate() = runTest(dispatcher) {
        var currentDate = LocalDate.of(2026, 4, 30)
        val todayFlow = MutableStateFlow(WaterDayData(date = currentDate.toString(), goalMl = 2000))
        val tomorrowFlow = MutableStateFlow(WaterDayData(date = "2026-05-01", goalMl = 2600))
        val repo = FakeWaterRepository(
            mutableMapOf(
                currentDate.toString() to todayFlow,
                "2026-05-01" to tomorrowFlow,
            )
        )

        val vm = WaterViewModel(
            repository = repo,
            currentDateProvider = { currentDate },
            currentTimeProvider = { LocalTime.of(10, 0) },
        )
        advanceUntilIdle()
        assertEquals(currentDate.toString(), vm.todayData.value.date)

        currentDate = LocalDate.of(2026, 5, 1)
        vm.refresh()
        advanceUntilIdle()

        assertEquals("2026-05-01", vm.todayData.value.date)
        assertEquals(2600, vm.todayData.value.goalMl)
    }
}

private class FakeWaterRepository(
    private val days: MutableMap<String, MutableStateFlow<WaterDayData>>,
) : WaterRepository {
    val addedEntries = mutableListOf<Pair<String, Int>>()
    val removedEntries = mutableListOf<Pair<String, Long>>()
    private var nextId = 100L

    override fun observeDay(date: String): Flow<WaterDayData> {
        return days.getOrPut(date) { MutableStateFlow(WaterDayData(date = date)) }
    }

    override suspend fun getDay(date: String): WaterDayData {
        return days.getOrPut(date) { MutableStateFlow(WaterDayData(date = date)) }.value
    }

    override suspend fun addEntry(date: String, amountMl: Int): Long {
        addedEntries += date to amountMl
        val id = nextId++
        val entry = WaterEntry(
            id = id,
            amountMl = amountMl,
            timestamp = LocalDateTime.of(LocalDate.parse(date), LocalTime.NOON),
        )
        val current = days.getOrPut(date) { MutableStateFlow(WaterDayData(date = date)) }
        current.value = current.value.copy(entries = current.value.entries + entry)
        return id
    }

    override suspend fun removeEntry(date: String, id: Long) {
        removedEntries += date to id
        val current = days.getOrPut(date) { MutableStateFlow(WaterDayData(date = date)) }
        current.value = current.value.copy(entries = current.value.entries.filterNot { it.id == id })
    }
}
