package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.datastore.UserDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WaterViewModel(private val dataStore: UserDataStore) : ViewModel() {
    private val _waterMl = MutableStateFlow(0)
    val waterMl: StateFlow<Int> = _waterMl

    init {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            dataStore.observeWaterForDate(today).collectLatest { ml -> _waterMl.value = ml }
        }
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            dataStore.incrementWater(today, ml)
        }
    }
}

class WaterViewModelFactory(private val dataStore: UserDataStore) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

