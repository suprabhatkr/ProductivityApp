package com.example.productivityapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.data.model.AppThemePreference
import com.example.productivityapp.data.model.AvatarCategory
import com.example.productivityapp.data.model.AvatarConfig
import com.example.productivityapp.data.model.AvatarDefaults
import com.example.productivityapp.data.model.AvatarPickerFilter
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.AppThemeRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

val SupportedGenderOptions = listOf(
    "Female",
    "Male",
    "Non-binary",
    "Prefer not to say",
)

data class ProfileSettingsSectionState(
    val displayName: String = "",
    val ageYears: String = "",
    val gender: String? = null,
    val heightCm: String = "",
    val weightKg: String = "",
    val avatar: AvatarConfig = AvatarConfig(),
    val displayNameError: String? = null,
    val ageYearsError: String? = null,
)

data class AvatarEditorState(
    val isVisible: Boolean = false,
    val draft: AvatarConfig = AvatarConfig(),
    val selectedFilter: AvatarPickerFilter = AvatarPickerFilter.CREATURE,
)

data class AppearanceSettingsSectionState(
    val themePreference: AppThemePreference = AppThemePreference.SYSTEM,
)

data class StepSettingsSectionState(
    val dailyStepGoal: String = "10000",
    val strideLengthMeters: String = "0.78",
)

data class RunSettingsSectionState(
    val preferredUnits: String = "metric",
)

data class WaterSettingsSectionState(
    val dailyWaterGoalMl: String = "2000",
)

data class SleepSettingsSectionState(
    val nightlySleepGoalMinutes: String = "480",
    val typicalBedtime: String = "22:00",
    val typicalWakeTime: String = "07:00",
    val sleepDetectionBufferMinutes: String = "30",
)

data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val message: String? = null,
    val profile: ProfileSettingsSectionState = ProfileSettingsSectionState(),
    val appearance: AppearanceSettingsSectionState = AppearanceSettingsSectionState(),
    val steps: StepSettingsSectionState = StepSettingsSectionState(),
    val run: RunSettingsSectionState = RunSettingsSectionState(),
    val water: WaterSettingsSectionState = WaterSettingsSectionState(),
    val sleep: SleepSettingsSectionState = SleepSettingsSectionState(),
    val avatarEditor: AvatarEditorState = AvatarEditorState(),
)

private data class ValidatedSettings(
    val profile: UserProfile,
    val themePreference: AppThemePreference,
)

class SettingsViewModel(
    private val profileRepository: UserProfileRepository,
    private val themeRepository: AppThemeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private var lastLoadedProfile: UserProfile = UserProfile()
    private var lastLoadedTheme: AppThemePreference = AppThemePreference.SYSTEM

    init {
        viewModelScope.launch {
            combine(
                profileRepository.observeUserProfile(),
                themeRepository.observeThemePreference(),
            ) { profile, themePreference ->
                profile to themePreference
            }.collectLatest { (profile, themePreference) ->
                lastLoadedProfile = profile
                lastLoadedTheme = themePreference
                val current = _uiState.value
                if (!current.hasUnsavedChanges || current.isLoading) {
                    _uiState.value = buildUiState(
                        profile = profile,
                        themePreference = themePreference,
                        isLoading = false,
                        isSaving = false,
                        hasUnsavedChanges = false,
                        message = current.message,
                    )
                }
            }
        }
    }

    fun updateDisplayName(value: String) = updateField {
        copy(
            profile = profile.copy(displayName = value, displayNameError = null),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateAgeYears(value: String) = updateField {
        copy(
            profile = profile.copy(ageYears = value.filter(Char::isDigit).take(3), ageYearsError = null),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateGender(value: String?) = updateField {
        copy(
            profile = profile.copy(gender = value?.takeIf { it in SupportedGenderOptions }),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateWeightKg(value: String) = updateField {
        copy(
            profile = profile.copy(weightKg = value.filter { it.isDigit() || it == '.' }.normalizeDecimalInput()),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateHeightCm(value: String) = updateField {
        copy(
            profile = profile.copy(heightCm = value.filter(Char::isDigit).take(3)),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateStrideLengthMeters(value: String) = updateField {
        copy(
            steps = steps.copy(strideLengthMeters = value.filter { it.isDigit() || it == '.' }.normalizeDecimalInput()),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updatePreferredUnits(value: String) = updateField {
        copy(
            run = run.copy(preferredUnits = if (value == "imperial") "imperial" else "metric"),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateThemePreference(value: AppThemePreference) = updateField {
        copy(
            appearance = appearance.copy(themePreference = value),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateDailyStepGoal(value: String) = updateField {
        copy(
            steps = steps.copy(dailyStepGoal = value.filter(Char::isDigit).take(6)),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateDailyWaterGoalMl(value: String) = updateField {
        copy(
            water = water.copy(dailyWaterGoalMl = value.filter(Char::isDigit).take(5)),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateNightlySleepGoalMinutes(value: String) = updateField {
        copy(
            sleep = sleep.copy(nightlySleepGoalMinutes = value.filter(Char::isDigit).take(4)),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateTypicalBedtime(value: String) = updateField {
        copy(
            sleep = sleep.copy(typicalBedtime = value.filter { it.isDigit() || it == ':' }.normalizeClockInput()),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateTypicalWakeTime(value: String) = updateField {
        copy(
            sleep = sleep.copy(typicalWakeTime = value.filter { it.isDigit() || it == ':' }.normalizeClockInput()),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun updateSleepDetectionBufferMinutes(value: String) = updateField {
        copy(
            sleep = sleep.copy(sleepDetectionBufferMinutes = value.filter(Char::isDigit).take(3)),
            hasUnsavedChanges = true,
            message = null,
        )
    }

    fun openAvatarEditor() = updateField {
        val currentAvatar = profile.avatar
        copy(
            avatarEditor = AvatarEditorState(
                isVisible = true,
                draft = currentAvatar,
                selectedFilter = currentAvatar.defaultPickerFilter(),
            ),
            message = null,
        )
    }

    fun dismissAvatarEditor() = updateField {
        copy(
            avatarEditor = avatarEditor.copy(
                isVisible = false,
                draft = profile.avatar,
                selectedFilter = profile.avatar.defaultPickerFilter(),
            ),
            message = null,
        )
    }

    fun resetAvatarDraftToSaved() = updateField {
        copy(
            avatarEditor = avatarEditor.copy(
                draft = profile.avatar,
                selectedFilter = profile.avatar.defaultPickerFilter(),
            ),
            message = null,
        )
    }

    fun updateAvatarFilter(value: AvatarPickerFilter) = updateField {
        copy(
            avatarEditor = avatarEditor.copy(selectedFilter = value),
            message = null,
        )
    }

    fun updateSelectedAvatar(value: String) = updateAvatarDraft {
        copy(avatarId = AvatarDefaults.normalizeAvatarId(value))
    }

    fun applyAvatarDraft() = updateField {
        val updatedAvatar = avatarEditor.draft
        copy(
            profile = profile.copy(avatar = updatedAvatar),
            avatarEditor = avatarEditor.copy(isVisible = false, draft = updatedAvatar),
            hasUnsavedChanges = hasUnsavedChanges || profile.avatar != updatedAvatar,
            message = null,
        )
    }

    fun saveSettings(): Boolean {
        val current = _uiState.value
        val validated = validate(current) ?: return false
        _uiState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch {
            profileRepository.updateUserProfile(validated.profile)
            themeRepository.updateThemePreference(validated.themePreference)
            lastLoadedProfile = validated.profile
            lastLoadedTheme = validated.themePreference
            _uiState.value = buildUiState(
                profile = validated.profile,
                themePreference = validated.themePreference,
                isLoading = false,
                isSaving = false,
                hasUnsavedChanges = false,
                message = "Settings saved",
            )
        }
        return true
    }

    fun resetSettings() {
        val defaults = UserProfile()
        _uiState.value = _uiState.value.copy(isSaving = true, message = null)
        viewModelScope.launch {
            profileRepository.updateUserProfile(defaults)
            themeRepository.updateThemePreference(AppThemePreference.SYSTEM)
            lastLoadedProfile = defaults
            lastLoadedTheme = AppThemePreference.SYSTEM
            _uiState.value = buildUiState(
                profile = defaults,
                themePreference = AppThemePreference.SYSTEM,
                isLoading = false,
                isSaving = false,
                hasUnsavedChanges = false,
                message = "Settings reset to defaults",
            )
        }
    }

    private fun buildUiState(
        profile: UserProfile,
        themePreference: AppThemePreference,
        isLoading: Boolean,
        isSaving: Boolean,
        hasUnsavedChanges: Boolean,
        message: String?,
    ): SettingsUiState {
        return SettingsUiState(
            isLoading = isLoading,
            isSaving = isSaving,
            hasUnsavedChanges = hasUnsavedChanges,
            message = message,
            profile = ProfileSettingsSectionState(
                displayName = profile.displayName.orEmpty(),
                ageYears = profile.ageYears?.toString().orEmpty(),
                gender = profile.gender,
                heightCm = profile.heightCm?.toString().orEmpty(),
                weightKg = profile.weightKg?.toEditableNumber().orEmpty(),
                avatar = profile.avatar,
                displayNameError = null,
                ageYearsError = null,
            ),
            appearance = AppearanceSettingsSectionState(
                themePreference = themePreference,
            ),
            steps = StepSettingsSectionState(
                dailyStepGoal = profile.dailyStepGoal.toString(),
                strideLengthMeters = profile.strideLengthMeters.toEditableNumber(),
            ),
            run = RunSettingsSectionState(
                preferredUnits = profile.preferredUnits,
            ),
            water = WaterSettingsSectionState(
                dailyWaterGoalMl = profile.dailyWaterGoalMl.toString(),
            ),
            sleep = SleepSettingsSectionState(
                nightlySleepGoalMinutes = profile.nightlySleepGoalMinutes.toString(),
                typicalBedtime = profile.typicalBedtimeMinutes.toClockString(),
                typicalWakeTime = profile.typicalWakeTimeMinutes.toClockString(),
                sleepDetectionBufferMinutes = profile.sleepDetectionBufferMinutes.toString(),
            ),
            avatarEditor = AvatarEditorState(
                isVisible = false,
                draft = profile.avatar,
                selectedFilter = profile.avatar.defaultPickerFilter(),
            ),
        )
    }

    private fun updateField(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun updateAvatarDraft(transform: AvatarConfig.() -> AvatarConfig) = updateField {
        copy(
            avatarEditor = avatarEditor.copy(
                draft = avatarEditor.draft.transform(),
            ),
            message = null,
        )
    }

    private fun validate(state: SettingsUiState): ValidatedSettings? {
        val defaults = UserProfile()
        val profileState = state.profile
        val sleepState = state.sleep
        val stepState = state.steps
        val runState = state.run
        val waterState = state.water

        val displayNameText = profileState.displayName.trim()
        val ageText = profileState.ageYears.trim()
        val weightText = profileState.weightKg.trim()
        val heightText = profileState.heightCm.trim()
        val strideText = stepState.strideLengthMeters.trim()
        val stepGoalText = stepState.dailyStepGoal.trim()
        val waterGoalText = waterState.dailyWaterGoalMl.trim()
        val nightlySleepGoalText = sleepState.nightlySleepGoalMinutes.trim()
        val bedtimeText = sleepState.typicalBedtime.trim()
        val wakeText = sleepState.typicalWakeTime.trim()
        val bufferText = sleepState.sleepDetectionBufferMinutes.trim()

        var displayNameError: String? = null
        var ageError: String? = null

        if (displayNameText.isBlank()) {
            displayNameError = "Name is mandatory"
        }

        val age = when {
            ageText.isBlank() -> null
            else -> ageText.toIntOrNull()?.takeIf { it in 1..120 }
        }
        if (ageText.isBlank()) {
            ageError = "Age is mandatory"
        } else if (age == null) {
            ageError = "Enter a valid age"
        }
        if (displayNameError != null || ageError != null) {
            _uiState.value = state.copy(
                isSaving = false,
                message = if (displayNameError != null && ageError != null) {
                    "Name and age are mandatory"
                } else {
                    displayNameError ?: ageError
                },
                profile = profileState.copy(
                    displayNameError = displayNameError,
                    ageYearsError = ageError,
                ),
            )
            return null
        }

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

        val stride = when {
            strideText.isBlank() -> defaults.strideLengthMeters
            else -> strideText.toDoubleOrNull()?.takeIf { it > 0.0 }
        }
        if (stride == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid stride length in meters")
            return null
        }

        val dailyStepGoal = when {
            stepGoalText.isBlank() -> defaults.dailyStepGoal
            else -> stepGoalText.toIntOrNull()?.takeIf { it > 0 }
        }
        if (dailyStepGoal == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid daily step goal")
            return null
        }

        val dailyWaterGoal = when {
            waterGoalText.isBlank() -> defaults.dailyWaterGoalMl
            else -> waterGoalText.toIntOrNull()?.takeIf { it > 0 }
        }
        if (dailyWaterGoal == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid daily water goal in ml")
            return null
        }

        val nightlySleepGoal = when {
            nightlySleepGoalText.isBlank() -> defaults.nightlySleepGoalMinutes
            else -> nightlySleepGoalText.toIntOrNull()?.takeIf { it in 180..720 }
        }
        if (nightlySleepGoal == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid nightly sleep goal in minutes")
            return null
        }

        val bedtime = when {
            bedtimeText.isBlank() -> defaults.typicalBedtimeMinutes
            else -> bedtimeText.parseClockMinutesOrNull()
        }
        if (bedtime == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter bedtime as HH:mm")
            return null
        }

        val wakeTime = when {
            wakeText.isBlank() -> defaults.typicalWakeTimeMinutes
            else -> wakeText.parseClockMinutesOrNull()
        }
        if (wakeTime == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter wake time as HH:mm")
            return null
        }

        val buffer = when {
            bufferText.isBlank() -> defaults.sleepDetectionBufferMinutes
            else -> bufferText.toIntOrNull()?.takeIf { it in 0..180 }
        }
        if (buffer == null) {
            _uiState.value = state.copy(isSaving = false, message = "Enter a valid sleep detection buffer in minutes")
            return null
        }

        return ValidatedSettings(
            profile = UserProfile(
                displayName = displayNameText,
                weightKg = weight,
                heightCm = height,
                strideLengthMeters = stride,
                preferredUnits = if (runState.preferredUnits == "imperial") "imperial" else "metric",
                dailyStepGoal = dailyStepGoal,
                dailyWaterGoalMl = dailyWaterGoal,
                nightlySleepGoalMinutes = nightlySleepGoal,
                typicalBedtimeMinutes = bedtime,
                typicalWakeTimeMinutes = wakeTime,
                sleepDetectionBufferMinutes = buffer,
                ageYears = age,
                gender = profileState.gender?.takeIf { it in SupportedGenderOptions },
                avatar = profileState.avatar,
            ),
            themePreference = state.appearance.themePreference,
        )
    }
}

private fun AvatarConfig.defaultPickerFilter(): AvatarPickerFilter = when (category) {
    AvatarCategory.MALE -> AvatarPickerFilter.MALE
    AvatarCategory.FEMALE -> AvatarPickerFilter.FEMALE
    AvatarCategory.CREATURE -> AvatarPickerFilter.CREATURE
}

private fun String.normalizeDecimalInput(): String {
    val firstDot = indexOf('.')
    return if (firstDot == -1) {
        this
    } else {
        substring(0, firstDot + 1) + substring(firstDot + 1).replace(".", "")
    }
}

private fun Double.toEditableNumber(): String {
    val longValue = toLong()
    return if (this == longValue.toDouble()) longValue.toString() else toString()
}

private fun Int.toClockString(): String {
    val hours = (this / 60).coerceIn(0, 23)
    val minutes = (this % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hours, minutes)
}

private fun String.normalizeClockInput(): String {
    val digits = filter(Char::isDigit)
    return when {
        contains(":") -> split(":").take(2).joinToString(":") { it.take(2) }
        digits.length <= 2 -> digits
        digits.length == 3 -> "0${digits[0]}:${digits.substring(1)}"
        else -> digits.take(2) + ":" + digits.substring(2, minOf(4, digits.length))
    }
}

private fun String.parseClockMinutesOrNull(): Int? {
    val trimmed = trim()
    if (trimmed.isBlank()) return null
    val parts = trimmed.split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}

class SettingsViewModelFactory(
    private val profileRepository: UserProfileRepository,
    private val themeRepository: AppThemeRepository,
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(profileRepository, themeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
