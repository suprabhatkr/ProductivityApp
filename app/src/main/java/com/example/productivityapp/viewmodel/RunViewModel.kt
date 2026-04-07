package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.repository.RunRepository
import com.example.productivityapp.data.entities.RunEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RunViewModel(private val repo: RunRepository) : ViewModel() {
    private val _runs = MutableStateFlow<List<RunEntity>>(emptyList())
    val runs: StateFlow<List<RunEntity>> = _runs

    init {
        viewModelScope.launch {
            repo.observeRuns().collectLatest { list -> _runs.value = list }
        }
    }

    suspend fun startRun(run: RunEntity): Long = repo.startRun(run)
    suspend fun updateRun(run: RunEntity) = repo.updateRun(run)

}

class RunViewModelFactory(private val repo: RunRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RunViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RunViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

