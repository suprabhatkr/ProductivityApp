package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.repository.StepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StepViewModel(private val repo: StepRepository) : ViewModel() {
    private val _steps = MutableStateFlow(0)
    val steps: StateFlow<Int> = _steps

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters

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
    }

    fun addManualSteps(delta: Int) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            repo.incrementSteps(today, delta, "manual")
        }
    }
}

class StepViewModelFactory(private val repo: StepRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StepViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

