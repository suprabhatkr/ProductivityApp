package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val displayName: String = "",
    val weightKg: String = "",
    val heightCm: String = "",
    val strideLengthMeters: String = "0.78",
    val preferredUnits: String = "metric",
    val dailyStepGoal: String = "10000",
    val dailyWaterGoalMl: String = "2000",
    val hasUnsavedChanges: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val repo: UserProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private var lastLoadedProfile: UserProfile = UserProfile()

    init {
        viewModelScope.launch {
            repo.observeUserProfile().collectLatest { profile ->
                lastLoadedProfile = profile
                val current = _uiState.value
                if (!current.hasUnsavedChanges || current.isLoading) {
                    _uiState.value = current.copy(
                        isLoading = false,
                        isSaving = false,
                        displayName = profile.displayName.orEmpty(),
                        weightKg = profile.weightKg?.stripTrailingZero()?.toString().orEmpty(),
                        heightCm = profile.heightCm?.toString().orEmpty(),
                        strideLengthMeters = profile.strideLengthMeters.stripTrailingZero().toString(),
                        preferredUnits = profile.preferredUnits,
                        dailyStepGoal = profile.dailyStepGoal.toString(),
                        dailyWaterGoalMl = profile.dailyWaterGoalMl.toString(),
                    )
                }
            }
        }
    }

    fun updateDisplayName(value: String) = updateField { copy(displayName = value, hasUnsavedChanges = true, message = null) }

    fun updateWeightKg(value: String) = updateField {
        copy(weightKg = value.filter { it.isDigit() || it == '.' }.normalizeDecimalInput(), hasUnsavedChanges = true, message = null)
    }

    fun updateHeightCm(value: String) = updateField {
        copy(heightCm = value.filter { it.isDigit() }.take(3), hasUnsavedChanges = true, message = null)
    }

    fun updateStrideLengthMeters(value: String) = updateField {
        copy(strideLengthMeters = value.filter { it.isDigit() || it == '.' }.normalizeDecimalInput(), hasUnsavedChanges = true, message = null)
    }

    fun updatePreferredUnits(value: String) = updateField {
        copy(preferredUnits = if (value == "imperial") "imperial" else "metric", hasUnsavedChanges = true, message = null)
    }

    fun updateDailyStepGoal(value: String) = updateField {
        copy(dailyStepGoal = value.filter { it.isDigit() }.take(6), hasUnsavedChanges = true, message = null)
    }

    fun updateDailyWaterGoalMl(value: String) = updateField {
        copy(dailyWaterGoalMl = value.filter { it.isDigit() }.take(5), hasUnsavedChanges = true, message = null)
    }

    fun saveProfile() {
        val current = _uiState.value
        val validated = validate(current) ?: return
        _uiState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch {
            repo.updateUserProfile(validated)
            lastLoadedProfile = validated
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isSaving = false,
                hasUnsavedChanges = false,
                message = "Settings saved"
            )
        }
    }

    fun resetProfile() {
        val defaults = UserProfile()
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            repo.updateUserProfile(defaults)
            lastLoadedProfile = defaults
            _uiState.value = SettingsUiState(
                isLoading = false,
                isSaving = false,
                displayName = "",
                weightKg = "",
                heightCm = "",
                strideLengthMeters = defaults.strideLengthMeters.stripTrailingZero().toString(),
                preferredUnits = defaults.preferredUnits,
                dailyStepGoal = defaults.dailyStepGoal.toString(),
                dailyWaterGoalMl = defaults.dailyWaterGoalMl.toString(),
                hasUnsavedChanges = false,
                message = "Profile reset to defaults"
            )
        }
    }

    private fun updateField(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun validate(state: SettingsUiState): UserProfile? {
        val weightText = state.weightKg.trim()
        val heightText = state.heightCm.trim()
        val strideText = state.strideLengthMeters.trim()
        val stepGoalText = state.dailyStepGoal.trim()
        val waterGoalText = state.dailyWaterGoalMl.trim()

        val weight = when {
            weightText.isBlank() -> null
            else -> weightText.toDoubleOrNull()?.takeIf { it > 0.0 }
        }
        if (weightText.isNotBlank() && weight == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid weight in kilograms")
            return null
        }

        val height = when {
            heightText.isBlank() -> null
            else -> heightText.toIntOrNull()?.takeIf { it > 0 }
        }
        if (heightText.isNotBlank() && height == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid height in centimeters")
            return null
        }

        val stride = strideText.toDoubleOrNull()?.takeIf { it > 0.0 }
        if (stride == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid stride length in meters")
            return null
        }

        val dailyStepGoal = stepGoalText.toIntOrNull()?.takeIf { it > 0 }
        if (dailyStepGoal == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid daily step goal")
            return null
        }

        val dailyWaterGoal = waterGoalText.toIntOrNull()?.takeIf { it > 0 }
        if (dailyWaterGoal == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid daily water goal in ml")
            return null
        }

        return UserProfile(
            displayName = state.displayName.trim().ifBlank { null },
            weightKg = weight,
            heightCm = height,
            strideLengthMeters = stride,
            preferredUnits = if (state.preferredUnits == "imperial") "imperial" else "metric",
            dailyStepGoal = dailyStepGoal,
            dailyWaterGoalMl = dailyWaterGoal,
        )
    }
}

private fun String.normalizeDecimalInput(): String {
    val firstDot = indexOf('.')
    return if (firstDot == -1) {
        this
    } else {
        substring(0, firstDot + 1) + substring(firstDot + 1).replace(".", "")
    }
}

private fun Double.stripTrailingZero(): Number {
    val longValue = toLong()
    return if (this == longValue.toDouble()) longValue else this
}

class SettingsViewModelFactory(
    private val repo: UserProfileRepository,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

