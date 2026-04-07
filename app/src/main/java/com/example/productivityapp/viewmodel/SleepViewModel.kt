package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.entities.SleepEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SleepViewModel(private val repo: SleepRepository) : ViewModel() {
    private val _sessions = MutableStateFlow<List<SleepEntity>>(emptyList())
    val sessions: StateFlow<List<SleepEntity>> = _sessions

    init {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            repo.observeSleepForDate(today).collectLatest { list -> _sessions.value = list }
        }
    }

    suspend fun startSleep(session: SleepEntity): Long = repo.startSleep(session)
    suspend fun stopSleep(session: SleepEntity): SleepEntity = repo.stopSleep(session)
}

class SleepViewModelFactory(private val repo: SleepRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SleepViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SleepViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

