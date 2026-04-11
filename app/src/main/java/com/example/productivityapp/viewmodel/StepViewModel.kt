package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
// ...existing imports...
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

