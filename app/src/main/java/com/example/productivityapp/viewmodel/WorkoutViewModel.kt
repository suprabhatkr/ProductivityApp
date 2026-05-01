package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.entities.WorkoutEntity
import com.example.productivityapp.data.entities.type
import com.example.productivityapp.data.repository.WorkoutRepository
import com.example.productivityapp.data.model.WorkoutType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: WorkoutRepository,
) : ViewModel() {
    private val _workouts = MutableStateFlow<List<WorkoutEntity>>(emptyList())
    val workouts: StateFlow<List<WorkoutEntity>> = _workouts

    private val _activeWorkout = MutableStateFlow<WorkoutEntity?>(null)
    val activeWorkout: StateFlow<WorkoutEntity?> = _activeWorkout

    private val _selectedWorkoutType = MutableStateFlow(WorkoutType.INDOOR)
    val selectedWorkoutType: StateFlow<WorkoutType> = _selectedWorkoutType

    init {
        viewModelScope.launch {
            repository.observeWorkouts().collectLatest { workouts ->
                _workouts.value = workouts
            }
        }
        viewModelScope.launch {
            repository.observeActiveWorkout().collectLatest { workout ->
                _activeWorkout.value = workout
                if (workout != null) {
                    _selectedWorkoutType.value = workout.type
                }
            }
        }
    }

    fun selectWorkoutType(type: WorkoutType) {
        _selectedWorkoutType.value = type
    }

    fun startWorkout() {
        if (_activeWorkout.value != null) return

        viewModelScope.launch {
            repository.startWorkout(
                WorkoutEntity(
                    workoutType = _selectedWorkoutType.value.storageValue,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    durationSec = 0L,
                )
            )
        }
    }

    fun endWorkout() {
        val activeWorkout = _activeWorkout.value ?: return
        val endTime = System.currentTimeMillis()
        val durationSec = ((endTime - activeWorkout.startTime) / 1000L).coerceAtLeast(0L)

        viewModelScope.launch {
            repository.updateWorkout(
                activeWorkout.copy(
                    endTime = endTime,
                    durationSec = durationSec,
                )
            )
        }
    }
}

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
