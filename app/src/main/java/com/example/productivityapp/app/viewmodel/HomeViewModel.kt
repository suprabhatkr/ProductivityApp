package com.example.productivityapp.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.data.entities.RunEntity
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.RunRepository
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.repository.WaterRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val waterRepository: WaterRepository,
    private val stepRepository: StepRepository,
    private val runRepository: RunRepository,
    private val sleepRepository: SleepRepository,
    private val userProfileRepository: UserProfileRepository,
    private val currentDateProvider: () -> LocalDate = { LocalDate.now() },
    private val currentTimeProvider: () -> LocalTime = { LocalTime.now() },
) : ViewModel() {

    private val selectedDate = MutableStateFlow(currentDateProvider())

    val uiState: StateFlow<HomeDashboardUiState> = selectedDate
        .flatMapLatest { date ->
            val dateText = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            combine(
                waterRepository.observeDay(dateText),
                stepRepository.observeStepsForDate(dateText),
                runRepository.observeRuns(),
                sleepRepository.observeSleepForDate(dateText),
                userProfileRepository.observeUserProfile(),
            ) { waterDay, stepEntity, runs, sleeps, profile ->
                buildHomeDashboardUiState(
                    date = date,
                    currentTime = currentTimeProvider(),
                    waterDay = waterDay,
                    stepEntity = stepEntity,
                    runs = runs,
                    sleeps = sleeps,
                    profile = profile,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = buildHomeDashboardUiState(
                date = currentDateProvider(),
                currentTime = currentTimeProvider(),
                waterDay = WaterDayData(date = currentDateProvider().format(DateTimeFormatter.ISO_LOCAL_DATE)),
                stepEntity = null,
                runs = emptyList(),
                sleeps = emptyList(),
                profile = UserProfile(),
            ),
        )

    fun refresh() {
        selectedDate.update { currentDateProvider() }
    }
}

class HomeViewModelFactory(
    private val waterRepository: WaterRepository,
    private val stepRepository: StepRepository,
    private val runRepository: RunRepository,
    private val sleepRepository: SleepRepository,
    private val userProfileRepository: UserProfileRepository,
    private val currentDateProvider: () -> LocalDate = { LocalDate.now() },
    private val currentTimeProvider: () -> LocalTime = { LocalTime.now() },
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                waterRepository = waterRepository,
                stepRepository = stepRepository,
                runRepository = runRepository,
                sleepRepository = sleepRepository,
                userProfileRepository = userProfileRepository,
                currentDateProvider = currentDateProvider,
                currentTimeProvider = currentTimeProvider,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class HomeDashboardUiState(
    val dateLabel: String,
    val greetingTitle: String,
    val greetingMessage: String,
    val heroChips: List<String>,
    val waterSummary: HomeFeatureSummary,
    val stepsSummary: HomeFeatureSummary,
    val runSummary: HomeFeatureSummary,
    val sleepSummary: HomeFeatureSummary,
    val settingsSummary: HomeFeatureSummary,
)

data class HomeFeatureSummary(
    val title: String,
    val headline: String,
    val supporting: String,
    val secondary: String,
    val progressFraction: Float? = null,
)

internal fun buildHomeDashboardUiState(
    date: LocalDate,
    currentTime: LocalTime,
    waterDay: WaterDayData,
    stepEntity: StepEntity?,
    runs: List<RunEntity>,
    sleeps: List<SleepEntity>,
    profile: UserProfile,
): HomeDashboardUiState {
    val greetingTitle = when {
        currentTime.hour < 12 -> "Good morning"
        currentTime.hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
    val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    val waterRemaining = (waterDay.goalMl - waterDay.totalMl).coerceAtLeast(0)
    val waterSummary = HomeFeatureSummary(
        title = "Water intake",
        headline = "${waterDay.totalMl} ml",
        supporting = "${waterDay.progressPercent}% of ${waterDay.goalMl} ml goal",
        secondary = if (waterDay.totalMl >= waterDay.goalMl) "Goal reached" else "$waterRemaining ml remaining",
        progressFraction = waterDay.progressFraction,
    )

    val steps = stepEntity?.steps ?: 0
    val stepsGoal = profile.dailyStepGoal.coerceAtLeast(1)
    val stepsFraction = (steps / stepsGoal.toFloat()).coerceIn(0f, 1f)
    val stepsSummary = HomeFeatureSummary(
        title = "Steps",
        headline = "${formatCount(steps)} steps",
        supporting = "${(stepsFraction * 100).toInt()}% of $stepsGoal goal",
        secondary = formatDistanceMeters(stepEntity?.distanceMeters ?: 0.0),
        progressFraction = stepsFraction,
    )

    val todayRuns = runs.filter { runDate(it.startTime) == date }
    val latestRun = runs.firstOrNull()
    val todayDistance = todayRuns.sumOf { it.distanceMeters }
    val runSummary = HomeFeatureSummary(
        title = "Run",
        headline = formatDistanceMeters(todayDistance),
        supporting = if (todayRuns.isEmpty()) "Ready to start" else "${todayRuns.size} run${if (todayRuns.size == 1) "" else "s"} today",
        secondary = latestRun?.let {
            if (it.endTime == null) "Run in progress"
            else "Latest ${formatDistanceMeters(it.distanceMeters)}"
        } ?: "No runs yet",
        progressFraction = (todayDistance / 5_000.0).toFloat().coerceIn(0f, 1f),
    )

    val totalSleepSeconds = sleeps.sumOf { it.durationSec }
    val sleepGoalMinutes = profile.nightlySleepGoalMinutes.coerceAtLeast(1)
    val sleepSummary = HomeFeatureSummary(
        title = "Sleep",
        headline = formatSleepDuration(totalSleepSeconds),
        supporting = if (sleeps.isEmpty()) "Ready to log sleep" else "${sleeps.size} session${if (sleeps.size == 1) "" else "s"} today",
        secondary = "Goal ${formatSleepDuration(sleepGoalMinutes * 60L)}",
        progressFraction = (totalSleepSeconds / (sleepGoalMinutes * 60f)).coerceIn(0f, 1f),
    )

    val settingsSummary = HomeFeatureSummary(
        title = "Settings",
        headline = "${formatCount(profile.dailyStepGoal)} steps • ${profile.dailyWaterGoalMl} ml",
        supporting = "${formatSleepDuration(profile.nightlySleepGoalMinutes * 60L)} sleep goal",
        secondary = if (profile.preferredUnits.equals("imperial", ignoreCase = true)) "Imperial units" else "Metric units",
    )

    val heroChips = listOf(
        "Water ${waterDay.progressPercent}%",
        "Steps ${(stepsFraction * 100).toInt()}%",
        "Sleep ${formatSleepDuration(totalSleepSeconds)}",
    )

    return HomeDashboardUiState(
        dateLabel = dateLabel,
        greetingTitle = greetingTitle,
        greetingMessage = "Your daily snapshot across steps, runs, sleep, water, and preferences.",
        heroChips = heroChips,
        waterSummary = waterSummary,
        stepsSummary = stepsSummary,
        runSummary = runSummary,
        sleepSummary = sleepSummary,
        settingsSummary = settingsSummary,
    )
}

private fun runDate(timestamp: Long): LocalDate {
    return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatDistanceMeters(distanceMeters: Double): String {
    return String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)
}

private fun formatCount(value: Int): String {
    return "%,d".format(Locale.US, value)
}

private fun formatSleepDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "%dh %02dm".format(hours, minutes) else "%dm".format(minutes)
}
