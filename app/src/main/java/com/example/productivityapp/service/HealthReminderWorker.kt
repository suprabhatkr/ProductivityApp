package com.example.productivityapp.service

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.repository.WaterRepository
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class HealthReminderWorker(
    appContext: Context,
    params: androidx.work.WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = nowProvider()
        return try {
            performReminderCheck(applicationContext, now)
            scheduleNext(applicationContext, DEFAULT_REPEAT_DELAY, replace = true)
            Result.success()
        } catch (_: Throwable) {
            scheduleNext(applicationContext, DEFAULT_REPEAT_DELAY, replace = true)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "health_reminder_worker"
        private val DEFAULT_REPEAT_DELAY: Duration = Duration.ofMinutes(15)
        private val WATER_WINDOW_START: LocalTime = LocalTime.of(9, 0)
        private val WATER_WINDOW_END: LocalTime = LocalTime.of(21, 0)

        @VisibleForTesting
        internal var nowProvider: () -> ZonedDateTime = { ZonedDateTime.now() }

        @VisibleForTesting
        internal var waterRepositoryProvider: ((Context) -> WaterRepository)? = null

        @VisibleForTesting
        internal var stepRepositoryProvider: ((Context) -> StepRepository)? = null

        @VisibleForTesting
        internal var sleepRepositoryProvider: ((Context) -> SleepRepository)? = null

        @VisibleForTesting
        internal var userProfileRepositoryProvider: ((Context) -> UserProfileRepository)? = null

        @VisibleForTesting
        internal var stateStoreFactory: ((Context) -> HealthReminderStateStore)? = null

        @VisibleForTesting
        internal var waterFirstNotifier: (Context, LocalTime) -> Unit = { context, time ->
            HealthReminderNotifier.sendWaterFirstDrinkReminder(context, time)
        }

        @VisibleForTesting
        internal var waterIdleNotifier: (Context, LocalTime) -> Unit = { context, time ->
            HealthReminderNotifier.sendWaterIdleReminder(context, time)
        }

        @VisibleForTesting
        internal var stepHalfNotifier: (Context, LocalTime) -> Unit = { context, time ->
            HealthReminderNotifier.sendStepHalfReminder(context, time)
        }

        @VisibleForTesting
        internal var stepNinetyNotifier: (Context, LocalTime) -> Unit = { context, time ->
            HealthReminderNotifier.sendStepNinetyReminder(context, time)
        }

        @VisibleForTesting
        internal var stepEveningNotifier: (Context, Int, LocalTime) -> Unit = { context, remaining, time ->
            HealthReminderNotifier.sendStepEveningReminder(context, remaining, time)
        }

        @VisibleForTesting
        internal var bedtimeNotifier: (Context, String, LocalTime) -> Unit = { context, bedtimeLabel, time ->
            HealthReminderNotifier.sendSleepBedtimeReminder(context, bedtimeLabel, time)
        }

        fun ensureScheduled(context: Context) {
            scheduleNext(context.applicationContext, DEFAULT_REPEAT_DELAY, replace = false)
        }

        @VisibleForTesting
        internal suspend fun performReminderCheck(
            context: Context,
            now: ZonedDateTime = nowProvider(),
        ) {
            val appContext = context.applicationContext
            val waterRepository = waterRepositoryProvider?.invoke(appContext)
                ?: RepositoryProvider.provideWaterRepository(appContext)
            val stepRepository = stepRepositoryProvider?.invoke(appContext)
                ?: RepositoryProvider.provideStepRepository(appContext)
            val sleepRepository = sleepRepositoryProvider?.invoke(appContext)
                ?: RepositoryProvider.provideSleepRepository(appContext)
            val profileRepository = userProfileRepositoryProvider?.invoke(appContext)
                ?: RepositoryProvider.provideUserProfileRepository(appContext)
            val stateStore = stateStoreFactory?.invoke(appContext)
                ?: HealthReminderStateStore(appContext)
            val profile = profileRepository.getUserProfileBlocking()
            val today = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

            maybeNotifyWater(appContext, now, waterRepository.getDay(today), stateStore)
            maybeNotifySteps(appContext, now, stepRepository.getStepsForDate(today), profile, stateStore)
            maybeNotifySleepBedtime(appContext, now, profile, sleepRepository.getActiveSleepSession(), stateStore)
        }

        @VisibleForTesting
        internal fun clearTestOverrides() {
            nowProvider = { ZonedDateTime.now() }
            waterRepositoryProvider = null
            stepRepositoryProvider = null
            sleepRepositoryProvider = null
            userProfileRepositoryProvider = null
            stateStoreFactory = null
            waterFirstNotifier = { context, time -> HealthReminderNotifier.sendWaterFirstDrinkReminder(context, time) }
            waterIdleNotifier = { context, time -> HealthReminderNotifier.sendWaterIdleReminder(context, time) }
            stepHalfNotifier = { context, time -> HealthReminderNotifier.sendStepHalfReminder(context, time) }
            stepNinetyNotifier = { context, time -> HealthReminderNotifier.sendStepNinetyReminder(context, time) }
            stepEveningNotifier = { context, remaining, time ->
                HealthReminderNotifier.sendStepEveningReminder(context, remaining, time)
            }
            bedtimeNotifier = { context, bedtimeLabel, time ->
                HealthReminderNotifier.sendSleepBedtimeReminder(context, bedtimeLabel, time)
            }
        }

        private fun scheduleNext(context: Context, delay: Duration, replace: Boolean) {
            val request = OneTimeWorkRequestBuilder<HealthReminderWorker>()
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .addTag(UNIQUE_WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }

        private fun maybeNotifyWater(
            context: Context,
            now: ZonedDateTime,
            day: WaterDayData,
            stateStore: HealthReminderStateStore,
        ) {
            val localTime = now.toLocalTime()
            if (localTime.isBefore(WATER_WINDOW_START) || !localTime.isBefore(WATER_WINDOW_END)) return
            val date = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val latestEntry = day.entries.maxByOrNull { it.timestamp }
            when {
                latestEntry == null && stateStore.shouldNotifyWaterFirst(date) -> {
                    waterFirstNotifier(context, localTime)
                    stateStore.markWaterFirst(date)
                }

                latestEntry != null -> {
                    val latestZoned = latestEntry.timestamp.atZone(ZoneId.systemDefault())
                    val minutesSinceLatest = Duration.between(latestZoned, now).toMinutes()
                    if (minutesSinceLatest >= 90 && stateStore.shouldNotifyWaterIdle(date, latestEntry.id)) {
                        waterIdleNotifier(context, localTime)
                        stateStore.markWaterIdle(date, latestEntry.id)
                    }
                }
            }
        }

        private fun maybeNotifySteps(
            context: Context,
            now: ZonedDateTime,
            todaySteps: StepEntity?,
            profile: UserProfile,
            stateStore: HealthReminderStateStore,
        ) {
            val date = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val steps = todaySteps?.steps ?: 0
            val goal = profile.dailyStepGoal.coerceAtLeast(1)
            val localTime = now.toLocalTime()
            when {
                steps >= (goal * 0.9f) && stateStore.shouldNotifyStepNinety(date) -> {
                    stepNinetyNotifier(context, localTime)
                    stateStore.markStepNinety(date)
                    stateStore.markStepHalf(date)
                }

                steps >= (goal * 0.5f) && stateStore.shouldNotifyStepHalf(date) -> {
                    stepHalfNotifier(context, localTime)
                    stateStore.markStepHalf(date)
                }
            }

            if (localTime.hour >= 20 && steps < goal && stateStore.shouldNotifyStepEvening(date)) {
                stepEveningNotifier(context, (goal - steps).coerceAtLeast(0), localTime)
                stateStore.markStepEvening(date)
            }
        }

        private fun maybeNotifySleepBedtime(
            context: Context,
            now: ZonedDateTime,
            profile: UserProfile,
            activeSleep: SleepEntity?,
            stateStore: HealthReminderStateStore,
        ) {
            if (activeSleep != null) return
            val upcomingBedtime = nextBedtime(now, profile.typicalBedtimeMinutes)
            val minutesUntilBedtime = Duration.between(now, upcomingBedtime).toMinutes()
            val targetDate = upcomingBedtime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (minutesUntilBedtime in 0..30 && stateStore.shouldNotifySleepBedtime(targetDate)) {
                bedtimeNotifier(
                    context,
                    formatClockMinutes(profile.typicalBedtimeMinutes),
                    now.toLocalTime(),
                )
                stateStore.markSleepBedtime(targetDate)
            }
        }

        private fun nextBedtime(now: ZonedDateTime, minutesOfDay: Int): ZonedDateTime {
            val hours = (minutesOfDay / 60).coerceIn(0, 23)
            val minutes = (minutesOfDay % 60).coerceIn(0, 59)
            val candidate = now.toLocalDate().atTime(hours, minutes).atZone(now.zone)
            return if (candidate.isBefore(now)) candidate.plusDays(1) else candidate
        }

        private fun formatClockMinutes(minutesOfDay: Int): String {
            val hours = (minutesOfDay / 60).coerceIn(0, 23)
            val minutes = (minutesOfDay % 60).coerceIn(0, 59)
            return LocalTime.of(hours, minutes).format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    }
}
