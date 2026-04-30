package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.entities.RunPointEntity
import com.example.productivityapp.data.repository.RunRepository
import com.example.productivityapp.data.model.ActiveRunSession
import com.example.productivityapp.data.entities.RunEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RunViewModel(private val repo: RunRepository, private val uiStateStore: com.example.productivityapp.data.UiStateStore) : ViewModel() {
    private val _runs = MutableStateFlow<List<RunEntity>>(emptyList())
    val runs: StateFlow<List<RunEntity>> = _runs
    private val _latestRun = MutableStateFlow<RunEntity?>(null)
    val latestRun: StateFlow<RunEntity?> = _latestRun
    private val _uiRunning = MutableStateFlow(false)
    val uiRunning: StateFlow<Boolean> = _uiRunning
    private val _activeRunSession = MutableStateFlow<ActiveRunSession?>(null)
    val activeRunSession: StateFlow<ActiveRunSession?> = _activeRunSession

    init {
        viewModelScope.launch {
            repo.observeRuns().collectLatest { list -> _runs.value = list }
        }
        viewModelScope.launch {
            repo.observeLatestRun().collectLatest { run ->
                _latestRun.value = run
                _activeRunSession.value = buildActiveRunSession(run, _uiRunning.value)
            }
        }
        try {
            _uiRunning.value = uiStateStore.isRunUiRunning()
            _activeRunSession.value = buildActiveRunSession(_latestRun.value, _uiRunning.value)
        } catch (_: Throwable) {}
    }

    suspend fun startRun(run: RunEntity): Long = repo.startRun(run)
    suspend fun updateRun(run: RunEntity) = repo.updateRun(run)
    fun observeRun(runId: Long): Flow<RunEntity?> = repo.observeRun(runId)
    fun observeRunPoints(runId: Long): Flow<List<RunPointEntity>> = repo.observeRunPoints(runId)

    fun setUiRunning(running: Boolean) {
        _uiRunning.value = running
        _activeRunSession.value = buildActiveRunSession(_latestRun.value, running)
        try { uiStateStore.setRunUiRunning(running) } catch (_: Throwable) {}
    }

    private fun buildActiveRunSession(run: RunEntity?, uiRunning: Boolean): ActiveRunSession? {
        if (run == null || run.endTime != null) return null
        return ActiveRunSession(
            runId = run.id,
            isPaused = !uiRunning,
            distanceMeters = run.distanceMeters,
            durationSec = run.durationSec,
            avgSpeedMps = run.avgSpeedMps,
        )
    }
}

class RunViewModelFactory(private val repo: RunRepository, private val uiStateStore: com.example.productivityapp.data.UiStateStore) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RunViewModel(repo, uiStateStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
