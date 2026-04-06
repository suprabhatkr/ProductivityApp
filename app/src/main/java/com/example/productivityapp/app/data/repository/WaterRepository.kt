package com.example.productivityapp.app.data.repository

import android.content.Context
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lightweight repository implementation using SharedPreferences + org.json so we don't need
 * to add DataStore or Gson dependencies.
 */
class WaterRepository(context: Context) {

    private val prefs = context.getSharedPreferences("water_data_prefs", Context.MODE_PRIVATE)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _todayData = MutableStateFlow(WaterDayData(date = "", goalMl = 2500))

    init {
        loadToday()
    }

    private fun entriesPrefKey(date: String) = "entries_$date"
    private val goalPrefKey = "daily_goal_ml"

    private fun loadToday() {
        val today = LocalDate.now().format(dateFormatter)
        val goal = prefs.getInt(goalPrefKey, 2500)
        val json = prefs.getString(entriesPrefKey(today), "[]") ?: "[]"

        val entries = mutableListOf<WaterEntry>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optLong("id", System.currentTimeMillis())
                val amount = obj.optInt("amountMl", 0)
                val ts = obj.optString("timestamp", LocalDateTime.now().toString())
                val timestamp = try { LocalDateTime.parse(ts) } catch (_: Exception) { LocalDateTime.now() }
                entries.add(WaterEntry(id = id, amountMl = amount, timestamp = timestamp))
            }
        } catch (_: Exception) {
            // ignore and use empty list
        }

        _todayData.value = WaterDayData(date = today, entries = entries, goalMl = goal)
    }

    /** Public refresh so callers (e.g. a ViewModel) can force reload when the date changes. */
    fun refresh() { loadToday() }

    fun getTodayData(): Flow<WaterDayData> = _todayData.asStateFlow()

    /**
     * Add an entry and return its generated id. This allows callers to undo the addition by id.
     */
    suspend fun addEntryReturnId(amountMl: Int): Long = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val json = prefs.getString(entriesPrefKey(today), "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        val id = System.currentTimeMillis()
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("amountMl", amountMl)
        obj.put("timestamp", LocalDateTime.now().toString())
        arr.put(obj)
        prefs.edit { putString(entriesPrefKey(today), arr.toString()) }
        loadToday()
        id
    }

    // Backwards-compatible helper
    suspend fun addEntry(amountMl: Int) {
        addEntryReturnId(amountMl)
    }

    suspend fun removeEntry(id: Long) = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        val json = prefs.getString(entriesPrefKey(today), "[]") ?: "[]"
        val arr = try { JSONArray(json) } catch (_: Exception) { JSONArray() }
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            if (obj.optLong("id", -1) != id) newArr.put(obj)
        }
        prefs.edit { putString(entriesPrefKey(today), newArr.toString()) }
        loadToday()
    }

    suspend fun setGoal(goalMl: Int) = withContext(Dispatchers.IO) {
        prefs.edit { putInt(goalPrefKey, goalMl) }
        loadToday()
    }
}
