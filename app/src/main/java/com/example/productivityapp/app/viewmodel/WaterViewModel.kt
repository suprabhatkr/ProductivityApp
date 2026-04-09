package com.example.productivityapp.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.repository.WaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
// removed unused imports related to the previous midnight coroutine

class WaterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WaterRepository(application)

    val todayData: StateFlow<WaterDayData> = repository.getTodayData()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterDayData(date = "", goalMl = 2000)
        )

    init {
        // Intentionally do not schedule a long-running background job here.
        // We'll refresh when the UI detects a date change (either on open or via
        // ACTION_DATE_CHANGED broadcast) so nothing runs while the process is not
        // active and we avoid potential leaks or long delays in the ViewModel.
    }

    /** Public refresh entry so UI or other callers can trigger a reload when the date changes. */
    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
        }
    }

    fun addWater(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch {
            repository.addEntry(amountMl)
        }
    }

    /**
     * Add water and return the generated entry id. This is a suspend function so callers can
     * await the id and perform an undo if needed.
     */
    suspend fun addWaterAndGetId(amountMl: Int): Long {
        if (amountMl <= 0) return -1L
        return repository.addEntryReturnId(amountMl)
    }

    fun removeEntry(id: Long) {
        viewModelScope.launch {
            repository.removeEntry(id)
        }
    }

    // Goal update helper was removed from public API to avoid an unused-public warning.
    // If you need to expose changing the goal from UI later, reintroduce a method here.
}
