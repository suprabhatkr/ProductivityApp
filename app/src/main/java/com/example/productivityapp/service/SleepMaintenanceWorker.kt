package com.example.productivityapp.service

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.sleep.AndroidSleepSignalProvider
import com.example.productivityapp.data.sleep.HeuristicSleepDetectionCoordinator
import com.example.productivityapp.data.sleep.SleepSignalProvider
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SleepMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = nowProvider()
        return try {
            performMaintenance(applicationContext, now)
            schedule(applicationContext, now)
            Result.success()
        } catch (_: Throwable) {
            schedule(applicationContext, now)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "sleep_maintenance_worker"
        private const val MIN_DELAY_MINUTES = 15L

        @VisibleForTesting
        internal var nowProvider: () -> ZonedDateTime = { ZonedDateTime.now() }

        @VisibleForTesting
        internal var sleepRepositoryProvider: ((Context) -> SleepRepository)? = null

        @VisibleForTesting
        internal var userProfileRepositoryProvider: ((Context) -> UserProfileRepository)? = null

        @VisibleForTesting
        internal var sleepSignalProvider: SleepSignalProvider? = AndroidSleepSignalProvider

        fun schedule(context: Context, now: ZonedDateTime = nowProvider()) {
            val profile = userProfileRepositoryProvider?.invoke(context)?.getUserProfileBlocking()
                ?: RepositoryProvider.provideUserProfileRepository(context).getUserProfileBlocking()
            val request = OneTimeWorkRequestBuilder<SleepMaintenanceWorker>()
                .setInitialDelay(calculateInitialDelay(now, profile))
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        @VisibleForTesting
        internal fun calculateInitialDelay(now: ZonedDateTime, profile: UserProfile): Duration {
            val target = nextBoundary(now, profile)
            val delay = Duration.between(now, target)
            return if (delay.isNegative || delay.isZero) Duration.ofMinutes(MIN_DELAY_MINUTES) else delay
        }

        @VisibleForTesting
        internal suspend fun performMaintenance(context: Context, now: ZonedDateTime = nowProvider()) {
            val sleepRepo = sleepRepositoryProvider?.invoke(context)
                ?: RepositoryProvider.provideSleepRepository(context)
            val profileRepo = userProfileRepositoryProvider?.invoke(context)
                ?: RepositoryProvider.provideUserProfileRepository(context)
            val profile = profileRepo.getUserProfileBlocking()
            val activeBeforeMaintenance = sleepRepo.getActiveSleepSession()

            finalizeWakeIfNeeded(sleepRepo, profile, now)
            markProvisionalForReviewIfNeeded(sleepRepo, profile, now)
            detectSleepStartIfNeeded(context, sleepRepo, profile, now, activeBeforeMaintenance == null)
        }

        @VisibleForTesting
        internal fun clearTestOverrides() {
            nowProvider = { ZonedDateTime.now() }
            sleepRepositoryProvider = null
            userProfileRepositoryProvider = null
            sleepSignalProvider = AndroidSleepSignalProvider
        }

        private suspend fun finalizeWakeIfNeeded(
            sleepRepo: SleepRepository,
            profile: UserProfile,
            now: ZonedDateTime,
        ) {
            val active = sleepRepo.getActiveSleepSession() ?: return
            if (!shouldWake(active, profile, now)) return

            val end = now.toInstant().toEpochMilli()
            val durationSec = ((end - active.startTimestamp).coerceAtLeast(0L) / 1000L)
            val finalized = active.copy(
                endTimestamp = end,
                durationSec = durationSec,
                inferredEndTimestamp = end,
                reviewState = if (active.detectionSource == SleepDetectionSource.AUTO.storageValue) {
                    SleepReviewState.NEEDS_REVIEW.storageValue
                } else {
                    SleepReviewState.CONFIRMED.storageValue
                },
            )
            sleepRepo.stopSleep(finalized)
        }

        private suspend fun markProvisionalForReviewIfNeeded(
            sleepRepo: SleepRepository,
            profile: UserProfile,
            now: ZonedDateTime,
        ) {
            val date = now.toLocalDate().toString()
            val reviewCutoff = now.minusMinutes(profile.sleepDetectionBufferMinutes.toLong())
            val sessions = sleepRepo.observeSleepForDate(date).first()
            val provisional = sessions.firstOrNull { session ->
                session.detectionSource == SleepDetectionSource.AUTO.storageValue &&
                    session.reviewState == SleepReviewState.PROVISIONAL.storageValue &&
                    session.endTimestamp > 0L &&
                    ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(session.endTimestamp), ZoneId.systemDefault()).isBefore(reviewCutoff)
            } ?: return

            sleepRepo.updateSleep(
                provisional.copy(
                    reviewState = SleepReviewState.NEEDS_REVIEW.storageValue,
                )
            )
        }

        private suspend fun detectSleepStartIfNeeded(
            context: Context,
            sleepRepo: SleepRepository,
            profile: UserProfile,
            now: ZonedDateTime,
            allowDetection: Boolean,
        ) {
            if (!allowDetection) return
            val signals = sleepSignalProvider?.collect(context, profile, now) ?: return
            HeuristicSleepDetectionCoordinator(sleepRepo).detectAndPersist(profile, signals)
        }

        private fun shouldWake(session: SleepEntity, profile: UserProfile, now: ZonedDateTime): Boolean {
            if (session.endTimestamp != 0L) return false
            val wakeBoundary = boundaryTime(now, profile.typicalWakeTimeMinutes)
            val wakeCutoff = wakeBoundary.plusMinutes(profile.sleepDetectionBufferMinutes.toLong())
            return now.isAfter(wakeCutoff) || now.isEqual(wakeCutoff)
        }

        private fun nextBoundary(now: ZonedDateTime, profile: UserProfile): ZonedDateTime {
            val bedtime = boundaryTime(now, profile.typicalBedtimeMinutes)
            val wakeTime = boundaryTime(now, profile.typicalWakeTimeMinutes)
            val inSleepWindow = isInSleepWindow(now, profile)
            return when {
                inSleepWindow && now.isBefore(wakeTime) -> wakeTime
                inSleepWindow -> wakeTime.plusDays(1)
                now.isBefore(bedtime) -> bedtime
                else -> bedtime.plusDays(1)
            }
        }

        private fun isInSleepWindow(now: ZonedDateTime, profile: UserProfile): Boolean {
            val minutes = now.hour * 60 + now.minute
            val bedtime = profile.typicalBedtimeMinutes
            val wake = profile.typicalWakeTimeMinutes
            return if (bedtime <= wake) {
                minutes in bedtime..wake
            } else {
                minutes >= bedtime || minutes <= wake
            }
        }

        private fun boundaryTime(now: ZonedDateTime, minutesOfDay: Int): ZonedDateTime {
            val hours = (minutesOfDay / 60).coerceIn(0, 23)
            val minutes = (minutesOfDay % 60).coerceIn(0, 59)
            return now.toLocalDate().atTime(hours, minutes).atZone(now.zone)
        }
    }
}
