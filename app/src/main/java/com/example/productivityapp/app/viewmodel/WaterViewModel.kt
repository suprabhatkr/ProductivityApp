package com.example.productivityapp.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.data.repository.WaterRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModel(
    private val repository: WaterRepository,
    private val currentDateProvider: () -> LocalDate = { LocalDate.now() },
    private val currentTimeProvider: () -> LocalTime = { LocalTime.now() },
) : ViewModel() {

    private val selectedDate = MutableStateFlow(currentDateString())

    val todayData: StateFlow<WaterDayData> = selectedDate
        .flatMapLatest(repository::observeDay)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = WaterDayData(date = currentDateString()),
        )

    val uiState: StateFlow<WaterUiState> = todayData
        .map { buildWaterUiState(it, currentTimeProvider()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildWaterUiState(WaterDayData(date = currentDateString()), currentTimeProvider()),
        )

    fun refresh() {
        val today = currentDateString()
        selectedDate.update { today }
    }

    fun addWater(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            repository.addEntry(selectedDate.value, amountMl)
        }
    }

    suspend fun addWaterAndGetId(amountMl: Int): Long {
        if (amountMl <= 0) return -1L
        return repository.addEntry(selectedDate.value, amountMl)
    }

    fun removeEntry(id: Long) {
        viewModelScope.launch {
            repository.removeEntry(selectedDate.value, id)
        }
    }

    private fun currentDateString(): String {
        return currentDateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

class WaterViewModelFactory(
    private val repository: WaterRepository,
    private val currentDateProvider: () -> LocalDate = { LocalDate.now() },
    private val currentTimeProvider: () -> LocalTime = { LocalTime.now() },
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(repository, currentDateProvider, currentTimeProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class WaterUiState(
    val dayData: WaterDayData,
    val remainingMl: Int,
    val completionText: String,
    val paceTitle: String,
    val paceMessage: String,
    val entriesSummary: String,
    val latestIntakeSummary: String,
    val streakTitle: String,
    val streakMessage: String,
)

internal fun buildWaterUiState(
    dayData: WaterDayData,
    currentTime: LocalTime,
): WaterUiState {
    val remainingMl = (dayData.goalMl - dayData.totalMl).coerceAtLeast(0)
    val expectedByNow = expectedHydrationByTime(dayData.goalMl, currentTime)
    val mlDelta = dayData.totalMl - expectedByNow
    val completionText = if (dayData.goalMl > 0) {
        "${dayData.progressPercent}% of ${dayData.goalMl} ml goal"
    } else {
        "${dayData.totalMl} ml logged"
    }

    val latestEntry = dayData.entries.maxByOrNull(WaterEntry::timestamp)
    val latestIntakeSummary = latestEntry?.let {
        "Latest ${it.amountMl} ml at ${it.timestamp.format(DateTimeFormatter.ofPattern("h:mm a"))}"
    } ?: "No water logged yet today"

    val entriesSummary = when (dayData.entries.size) {
        0 -> "No entries yet"
        1 -> "1 drink logged today"
        else -> "${dayData.entries.size} drinks logged today"
    }

    val (paceTitle, paceMessage) = when {
        dayData.totalMl >= dayData.goalMl && dayData.goalMl > 0 -> {
            "Goal reached" to "Nice work. Keep sipping through the evening to maintain your rhythm."
        }
        dayData.entries.isEmpty() && currentTime.hour >= 11 -> {
            "Start hydrating now" to "A glass of water this hour will help you catch up before the evening."
        }
        mlDelta >= 250 -> {
            "Ahead of today's pace" to "You are about ${mlDelta} ml ahead of schedule. A steady pace is working well."
        }
        mlDelta <= -250 -> {
            "Behind today's pace" to "Try another ${(remainingMl / 3).coerceAtLeast(150)} ml soon to close the gap gently."
        }
        else -> {
            "On a steady pace" to "You are tracking close to your goal pace. Keep spacing drinks across the day."
        }
    }

    val minutesSinceLatest = latestEntry?.let { Duration.between(it.timestamp, LocalDateTime.of(LocalDate.now(), currentTime)).toMinutes() }
    val (streakTitle, streakMessage) = when {
        latestEntry == null -> {
            "Hydration tip" to "Aim for a drink every couple of hours instead of waiting until you feel thirsty."
        }
        minutesSinceLatest != null && minutesSinceLatest <= 90 -> {
            "Nice rhythm" to "You logged water recently. Keeping drinks evenly spaced usually feels easier than catching up late."
        }
        currentTime.hour >= 18 && remainingMl > 600 -> {
            "Evening catch-up" to "You still have $remainingMl ml left. Split it into smaller drinks for the rest of the evening."
        }
        else -> {
            "Keep the habit going" to latestIntakeSummary
        }
    }

    return WaterUiState(
        dayData = dayData,
        remainingMl = remainingMl,
        completionText = completionText,
        paceTitle = paceTitle,
        paceMessage = paceMessage,
        entriesSummary = entriesSummary,
        latestIntakeSummary = latestIntakeSummary,
        streakTitle = streakTitle,
        streakMessage = streakMessage,
    )
}

private fun expectedHydrationByTime(goalMl: Int, currentTime: LocalTime): Int {
    val startHour = 7
    val endHour = 22
    val activeMinutes = ((endHour - startHour) * 60).coerceAtLeast(1)
    val elapsed = ((currentTime.hour - startHour) * 60 + currentTime.minute).coerceIn(0, activeMinutes)
    return (goalMl * (elapsed / activeMinutes.toFloat())).toInt()
}
