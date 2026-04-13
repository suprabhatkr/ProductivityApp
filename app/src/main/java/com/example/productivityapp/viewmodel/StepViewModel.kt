package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
// ...existing imports...
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay

class StepViewModel(
    private val repo: StepRepository,
    private val userProfileRepository: UserProfileRepository,
    private val uiStateStore: com.example.productivityapp.data.UiStateStore? = null,
) : ViewModel() {
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters

    private val _dailyGoal = MutableStateFlow(10000)
    val dailyGoal: StateFlow<Int> = _dailyGoal

    private val _weeklySteps = MutableStateFlow<List<Int>>(emptyList())
    val weeklySteps: StateFlow<List<Int>> = _weeklySteps

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning

    // Today's intra-day segments (Morning, Afternoon, Evening, Night)
    private val _todaySegments = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val todaySegments: StateFlow<List<Pair<String, Int>>> = _todaySegments

    init {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repo.observeStepsForDate(today).collectLatest { entity ->
                if (entity != null) {
                    _steps.value = entity.steps
                    _distanceMeters.value = entity.distanceMeters
                } else {
                    _steps.value = 0
                    _distanceMeters.value = 0.0
                }
            }
        }
        viewModelScope.launch {
            userProfileRepository.observeUserProfile().collectLatest { profile ->
                _dailyGoal.value = profile.dailyStepGoal
            }
        }
        // observe last 7 days steps so the UI updates live when DB changes
        viewModelScope.launch {
            try {
                val todayDate = LocalDate.now()
                val end = todayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val start = todayDate.minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE)
                repo.observeStepsForRange(start, end).collectLatest { list ->
                    val map = list.associateBy { it.date }
                    val values = (0..6).map { i ->
                        val d = todayDate.minusDays((6 - i).toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        map[d]?.steps ?: 0
                    }
                    _weeklySteps.value = values
                }
            } catch (_: Throwable) {
                // if observe fails, keep empty list
                _weeklySteps.value = emptyList()
            }
        }
        // restore persisted UI-running flag
        try {
            val stored = uiStateStore?.isStepUiRunning() ?: false
            _serviceRunning.value = stored
        } catch (_: Throwable) {}

        // observe timestamped samples to compute exact per-segment aggregates
        viewModelScope.launch {
            try {
                repo.observeSamplesForDate(today).collectLatest { samples ->
                    if (samples.isEmpty()) {
                        // fallback to proportionally distributing total steps if no samples
                        _todaySegments.value = computeFallbackSegments(_steps.value)
                    } else {
                        _todaySegments.value = computeSegmentsFromSamples(samples)
                    }
                }
            } catch (_: Throwable) {
                // on error, fallback to heuristic distribution
                _todaySegments.value = computeFallbackSegments(_steps.value)
            }
        }
    }

    private fun computeFallbackSegments(totalSteps: Int): List<Pair<String, Int>> {
        // old heuristic distribution used when no samples exist
        // Define segments: Morning 6-12, Afternoon 12-18, Evening 18-22, Night 22-6
        data class Seg(val label: String, val start: Int, val end: Int)
        val segments = listOf(
            Seg("Morning", 6 * 60, 12 * 60),
            Seg("Afternoon", 12 * 60, 18 * 60),
            Seg("Evening", 18 * 60, 22 * 60),
            Seg("Night", 22 * 60, 6 * 60)
        )

        val now = LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute

        fun coveredIn(seg: Seg): Int {
            val s = seg.start
            val e = seg.end
            return if (e > s) {
                val covered = (minOf(nowMinutes, e) - s).coerceAtLeast(0)
                covered
            } else {
                // wraps past midnight
                val part1 = (minOf(nowMinutes, 1440) - s).coerceAtLeast(0)
                val part2 = (minOf(nowMinutes, e) - 0).coerceAtLeast(0)
                (part1 + part2)
            }
        }

        val coveredList = segments.map { coveredIn(it).coerceAtLeast(0) }
        val totalCovered = coveredList.sum().coerceAtLeast(1) // avoid div by zero

        val rawAllocation = coveredList.map { c -> c.toDouble() / totalCovered.toDouble() }
        val ints = mutableListOf<Int>()
        var acc = 0
        for (i in rawAllocation.indices) {
            val v = kotlin.math.round(totalSteps * rawAllocation[i]).toInt()
            ints.add(v)
            acc += v
        }
        // adjust rounding error to match totalSteps
        if (acc != totalSteps) {
            val diff = totalSteps - acc
            ints[ints.lastIndex] = (ints.last() + diff).coerceAtLeast(0)
        }

        return segments.mapIndexed { idx, s -> s.label to ints[idx] }
    }

    private fun computeSegmentsFromSamples(samples: List<com.example.productivityapp.data.entities.StepSampleEntity>): List<Pair<String, Int>> {
        // Map samples to segments using their tsMs in system default timezone
        val zone = java.time.ZoneId.systemDefault()
        data class Seg(val label: String, val start: Int, val end: Int)
        val segments = listOf(
            Seg("Morning", 6 * 60, 12 * 60),
            Seg("Afternoon", 12 * 60, 18 * 60),
            Seg("Evening", 18 * 60, 22 * 60),
            Seg("Night", 22 * 60, 6 * 60)
        )

        val sums = IntArray(segments.size)
        for (s in samples) {
            try {
                val instant = java.time.Instant.ofEpochMilli(s.tsMs)
                val lt = instant.atZone(zone).toLocalTime()
                val minuteOfDay = lt.hour * 60 + lt.minute
                // find segment index
                val idx = segments.indexOfFirst { seg ->
                    val sMin = seg.start
                    val eMin = seg.end
                    if (eMin > sMin) {
                        minuteOfDay in sMin until eMin
                    } else {
                        // wrap
                        minuteOfDay >= sMin || minuteOfDay < eMin
                    }
                }.coerceAtLeast(0)
                if (idx >= 0 && idx < sums.size) sums[idx] += s.delta
            } catch (_: Throwable) {}
        }

        // Ensure total matches known _steps where possible by adjusting any rounding
        val totalSamples = sums.sum()
        val totalKnown = _steps.value
        if (totalKnown > totalSamples) {
            // attribute the difference to the last segment
            sums[sums.lastIndex] += (totalKnown - totalSamples)
        }

        return segments.mapIndexed { i, seg -> seg.label to sums[i] }
    }

    fun addManualSteps(delta: Int) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repo.incrementSteps(today, delta, "manual")
        }
    }

    fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
        try {
            uiStateStore?.setStepUiRunning(running)
        } catch (_: Throwable) {}
    }
}

class StepViewModelFactory(
    private val repo: StepRepository,
    private val userProfileRepository: UserProfileRepository,
    private val uiStateStore: com.example.productivityapp.data.UiStateStore,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepViewModel(repo, userProfileRepository, uiStateStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

