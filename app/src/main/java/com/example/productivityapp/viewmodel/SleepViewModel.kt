package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepReviewState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SleepViewModel(private val repo: SleepRepository) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SleepEntity>>(emptyList())
    val sessions: StateFlow<List<SleepEntity>> = _sessions

    private val _weeklySessions = MutableStateFlow<List<SleepEntity>>(emptyList())
    val weeklySessions: StateFlow<List<SleepEntity>> = _weeklySessions

    private val _activeSession = MutableStateFlow<SleepEntity?>(null)
    val activeSession: StateFlow<SleepEntity?> = _activeSession

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _pendingReviewSession = MutableStateFlow<SleepEntity?>(null)
    val pendingReviewSession: StateFlow<SleepEntity?> = _pendingReviewSession

    private val _pendingDetectedReviewSession = MutableStateFlow<SleepEntity?>(null)
    val pendingDetectedReviewSession: StateFlow<SleepEntity?> = _pendingDetectedReviewSession

    private val _weeklySummary = MutableStateFlow<List<SleepDaySummary>>(emptyList())
    val weeklySummary: StateFlow<List<SleepDaySummary>> = _weeklySummary

    private var timerJob: Job? = null
    private var pausedAccumulatedMs: Long = 0L
    private var pauseStartedMs: Long = 0L

    private val today: String
        get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private val weekStart: String
        get() = LocalDate.now().minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        viewModelScope.launch {
            repo.observeSleepForDate(today).collectLatest { list -> _sessions.value = list }
        }
        viewModelScope.launch {
            repo.observeSleepForRange(weekStart, today).collectLatest { list ->
                _weeklySessions.value = list
                _weeklySummary.value = buildWeeklySummary(list)
                refreshDetectedReviewCandidate(list)
            }
        }
        viewModelScope.launch {
            val active = repo.getActiveSleepSession()
            _activeSession.value = active
            if (active != null) startTimer(active.startTimestamp)
        }
    }

    fun startSleep() {
        if (_activeSession.value != null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = SleepEntity(
                date = today,
                startTimestamp = now,
                endTimestamp = 0L,
                durationSec = 0L,
                sleepQuality = null,
                notes = null,
                detectionSource = SleepDetectionSource.MANUAL.storageValue,
                confidenceScore = 1.0,
                inferredStartTimestamp = null,
                inferredEndTimestamp = null,
                reviewState = SleepReviewState.CONFIRMED.storageValue,
                tagsCsv = null,
            )
            val id = repo.startSleep(session)
            val persisted = repo.getSleepById(id) ?: session.copy(id = id)
            pausedAccumulatedMs = 0L
            pauseStartedMs = 0L
            _isPaused.value = false
            _activeSession.value = persisted
            startTimer(persisted.startTimestamp)
        }
    }

    fun startNapTimer() {
        if (_activeSession.value != null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = SleepEntity(
                date = today,
                startTimestamp = now,
                endTimestamp = 0L,
                durationSec = 0L,
                sleepQuality = null,
                notes = null,
                detectionSource = SleepDetectionSource.NAP.storageValue,
                confidenceScore = 1.0,
                inferredStartTimestamp = now,
                inferredEndTimestamp = null,
                reviewState = SleepReviewState.CONFIRMED.storageValue,
                tagsCsv = "nap",
            )
            val id = repo.startSleep(session)
            val persisted = repo.getSleepById(id) ?: session.copy(id = id)
            pausedAccumulatedMs = 0L
            pauseStartedMs = 0L
            _isPaused.value = false
            _activeSession.value = persisted
            startTimer(persisted.startTimestamp)
        }
    }

    fun pauseSleep() {
        if (_activeSession.value == null || _isPaused.value) return
        pauseStartedMs = System.currentTimeMillis()
        _isPaused.value = true
        timerJob?.cancel()
    }

    fun resumeSleep() {
        val active = _activeSession.value ?: return
        if (!_isPaused.value) return
        pausedAccumulatedMs += (System.currentTimeMillis() - pauseStartedMs).coerceAtLeast(0L)
        pauseStartedMs = 0L
        _isPaused.value = false
        startTimer(active.startTimestamp)
    }

    fun stopSleep() {
        val active = _activeSession.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val pausePenaltyMs = pausedAccumulatedMs + if (_isPaused.value) {
                (now - pauseStartedMs).coerceAtLeast(0L)
            } else 0L
            val durationSec = ((now - active.startTimestamp - pausePenaltyMs).coerceAtLeast(0L) / 1000L)
            val stopped = active.copy(
                endTimestamp = now,
                durationSec = durationSec,
                reviewState = SleepReviewState.NEEDS_REVIEW.storageValue,
            )
            val persisted = repo.stopSleep(stopped)
            _activeSession.value = null
            _isPaused.value = false
            pausedAccumulatedMs = 0L
            pauseStartedMs = 0L
            timerJob?.cancel()
            _elapsedSeconds.value = 0L
            _pendingReviewSession.value = persisted
        }
    }

    fun submitSleepReview(quality: Int, notes: String) {
        val session = _pendingReviewSession.value ?: return
        viewModelScope.launch {
            val updated = session.copy(
                sleepQuality = quality,
                notes = notes.ifBlank { null },
                reviewState = SleepReviewState.CONFIRMED.storageValue,
            )
            repo.updateSleep(updated)
            _pendingReviewSession.value = null
        }
    }

    fun dismissSleepReview() {
        _pendingReviewSession.value = null
    }

    fun acceptDetectedSleepReview() {
        val session = _pendingDetectedReviewSession.value ?: return
        viewModelScope.launch {
            repo.updateSleep(
                session.copy(
                    reviewState = SleepReviewState.CONFIRMED.storageValue,
                )
            )
            _pendingDetectedReviewSession.value = null
        }
    }

    fun adjustDetectedSleepReview(durationMinutes: Int, quality: Int?, notes: String) {
        val session = _pendingDetectedReviewSession.value ?: return
        if (durationMinutes <= 0) return
        viewModelScope.launch {
            val endTimestamp = session.startTimestamp + durationMinutes * 60_000L
            val updated = session.copy(
                endTimestamp = endTimestamp,
                durationSec = durationMinutes * 60L,
                sleepQuality = quality,
                notes = notes.ifBlank { null },
                reviewState = SleepReviewState.CONFIRMED.storageValue,
            )
            repo.updateSleep(updated)
            _pendingDetectedReviewSession.value = null
        }
    }

    fun mergeDetectedSleepReviewWithPrevious() {
        val session = _pendingDetectedReviewSession.value ?: return
        viewModelScope.launch {
            val prior = _weeklySessions.value
                .filter { it.id != session.id }
                .filter { it.date == session.date }
                .filter { it.startTimestamp < session.startTimestamp }
                .maxByOrNull { it.startTimestamp }

            if (prior == null) return@launch

            val merged = prior.copy(
                endTimestamp = maxOf(prior.endTimestamp, session.endTimestamp),
                durationSec = ((maxOf(prior.endTimestamp, session.endTimestamp) - prior.startTimestamp).coerceAtLeast(0L) / 1000L),
                sleepQuality = listOfNotNull(prior.sleepQuality, session.sleepQuality).maxOrNull(),
                notes = mergeNotes(prior.notes, session.notes),
                reviewState = SleepReviewState.CONFIRMED.storageValue,
            )
            repo.updateSleep(merged)
            repo.deleteSleep(session.id)
            _pendingDetectedReviewSession.value = null
        }
    }

    fun dismissDetectedSleepReview() {
        val session = _pendingDetectedReviewSession.value ?: return
        viewModelScope.launch {
            repo.deleteSleep(session.id)
            _pendingDetectedReviewSession.value = null
        }
    }

    private fun startTimer(startTimestamp: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                val pausedMs = if (_isPaused.value) {
                    pausedAccumulatedMs + (now - pauseStartedMs).coerceAtLeast(0L)
                } else {
                    pausedAccumulatedMs
                }
                _elapsedSeconds.value = ((now - startTimestamp - pausedMs).coerceAtLeast(0L) / 1000L)
                delay(1000L)
            }
        }
    }

    private fun buildWeeklySummary(sessions: List<SleepEntity>): List<SleepDaySummary> {
        val byDate = sessions.groupBy { it.date }
        return (6 downTo 0).map { offset ->
            val date = LocalDate.now().minusDays(offset.toLong())
            val key = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val entries = byDate[key].orEmpty()
            val totalDuration = entries.sumOf { it.durationSec }
            val rated = entries.mapNotNull { it.sleepQuality }
            SleepDaySummary(
                date = key,
                label = date.dayOfWeek.name.take(3),
                totalDurationSec = totalDuration,
                averageQuality = if (rated.isNotEmpty()) rated.average() else null,
            )
        }
    }

    private fun refreshDetectedReviewCandidate(allSessions: List<SleepEntity>) {
        val currentId = _pendingDetectedReviewSession.value?.id
        val matchingCurrent = currentId?.let { id -> allSessions.firstOrNull { it.id == id } }
        if (matchingCurrent != null && isDetectedReviewable(matchingCurrent)) {
            _pendingDetectedReviewSession.value = matchingCurrent
            return
        }
        _pendingDetectedReviewSession.value = allSessions
            .filter { isDetectedReviewable(it) }
            .maxByOrNull { it.startTimestamp }
    }

    private fun isDetectedReviewable(session: SleepEntity): Boolean {
        return session.detectionSource == SleepDetectionSource.AUTO.storageValue &&
            session.reviewState != SleepReviewState.CONFIRMED.storageValue &&
            session.reviewState != SleepReviewState.DISMISSED.storageValue
    }

    private fun mergeNotes(first: String?, second: String?): String? {
        val pieces = listOfNotNull(
            first?.trim()?.takeIf { it.isNotBlank() },
            second?.trim()?.takeIf { it.isNotBlank() },
        )
        return pieces.takeIf { it.isNotEmpty() }?.joinToString(" | ")
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}

data class SleepDaySummary(
    val date: String,
    val label: String,
    val totalDurationSec: Long,
    val averageQuality: Double?,
)

class SleepViewModelFactory(private val repo: SleepRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SleepViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
