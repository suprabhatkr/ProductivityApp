package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import com.example.productivityapp.data.entities.type
import com.example.productivityapp.data.model.MindfulnessSessionType
import com.example.productivityapp.data.repository.MindfulnessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MindfulnessViewModel(
    private val repository: MindfulnessRepository,
) : ViewModel() {
    private val _sessions = MutableStateFlow<List<MindfulnessSessionEntity>>(emptyList())
    val sessions: StateFlow<List<MindfulnessSessionEntity>> = _sessions

    private val _logs = MutableStateFlow<List<MindLogEntity>>(emptyList())
    val logs: StateFlow<List<MindLogEntity>> = _logs

    private val _activeSession = MutableStateFlow<MindfulnessSessionEntity?>(null)
    val activeSession: StateFlow<MindfulnessSessionEntity?> = _activeSession

    private val _selectedSessionType = MutableStateFlow(MindfulnessSessionType.BREATHING)
    val selectedSessionType: StateFlow<MindfulnessSessionType> = _selectedSessionType

    private val _reflectionDraft = MutableStateFlow("")
    val reflectionDraft: StateFlow<String> = _reflectionDraft

    init {
        viewModelScope.launch {
            repository.observeSessions().collectLatest { sessions ->
                _sessions.value = sessions
            }
        }
        viewModelScope.launch {
            repository.observeLogs().collectLatest { logs ->
                _logs.value = logs
            }
        }
        viewModelScope.launch {
            repository.observeActiveSession().collectLatest { session ->
                _activeSession.value = session
                if (session != null) {
                    _selectedSessionType.value = session.type
                }
            }
        }
    }

    fun selectSessionType(type: MindfulnessSessionType) {
        _selectedSessionType.value = type
    }

    fun updateReflectionDraft(value: String) {
        _reflectionDraft.value = value
    }

    fun startSession() {
        if (_activeSession.value != null) return

        viewModelScope.launch {
            repository.startSession(
                MindfulnessSessionEntity(
                    sessionType = _selectedSessionType.value.storageValue,
                    startTime = System.currentTimeMillis(),
                    endTime = null,
                    durationSec = 0L,
                )
            )
        }
    }

    fun endSession() {
        val activeSession = _activeSession.value ?: return
        val endTime = System.currentTimeMillis()
        val durationSec = ((endTime - activeSession.startTime) / 1000L).coerceAtLeast(0L)

        viewModelScope.launch {
            repository.updateSession(
                activeSession.copy(
                    endTime = endTime,
                    durationSec = durationSec,
                )
            )
        }
    }

    fun saveReflection() {
        val reflection = _reflectionDraft.value.trim()
        if (reflection.isBlank()) return

        viewModelScope.launch {
            repository.addLog(
                MindLogEntity(
                    createdAt = System.currentTimeMillis(),
                    content = reflection,
                )
            )
            _reflectionDraft.value = ""
        }
    }
}

class MindfulnessViewModelFactory(
    private val repository: MindfulnessRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MindfulnessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MindfulnessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
