package com.example.productivityapp.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.datastore.UserDataStore
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
// removed unused imports related to the previous midnight coroutine

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = UserDataStore(application)

    private val _todayData = kotlinx.coroutines.flow.MutableStateFlow(WaterDayData(date = "", goalMl = 2000))
    val todayData: StateFlow<WaterDayData> = _todayData

    private var observeJob: Job? = null

    private fun startObservingForDate(date: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                dataStore.observeEntriesForDate(date),
                dataStore.observeUserProfile()
            ) { entries, profile ->
                WaterDayData(date = date, entries = entries, goalMl = profile.dailyWaterGoalMl)
            }.collect { _todayData.value = it }
        }
    }

    init {
        // Start observing today's date
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        startObservingForDate(today)
    }

    /** Public refresh entry so UI or other callers can trigger a reload when the date changes. */
    fun refresh() {
        // Re-evaluate today's observations. Callers may call this when the UI detects
        // the system date changed while the process was alive.
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        startObservingForDate(today)
    }

    fun addWater(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            dataStore.addEntryReturnId(today, amountMl)
        }
    }

    /**
     * Add water and return the generated entry id. This is a suspend function so callers can
     * await the id and perform an undo if needed.
     */
    suspend fun addWaterAndGetId(amountMl: Int): Long {
        if (amountMl <= 0) return -1L
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        return dataStore.addEntryReturnId(today, amountMl)
    }

    fun removeEntry(id: Long) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            dataStore.removeEntry(today, id)
        }
    }

    // Goal update helper was removed from public API to avoid an unused-public warning.
    // If you need to expose changing the goal from UI later, reintroduce a method here.
}
