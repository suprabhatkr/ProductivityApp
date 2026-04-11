package com.example.productivityapp.service

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.productivityapp.data.RepositoryProvider
import com.example.productivityapp.datastore.UserDataStore
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Schedules a lightweight daily rollover task aligned to the next local midnight.
 *
 * The worker is intentionally conservative: destructive reset work only runs during a small
 * window just after midnight so that delayed execution does not wipe a user's newly-entered data.
 * Data is date-keyed elsewhere in the app, so the worker mainly provides predictable reset and
 * service-baseline housekeeping for water/steps while always re-scheduling the next midnight run.
 */
class MidnightResetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = nowProvider()
        return try {
            if (shouldPerformReset(now)) {
                val resetAction = resetActionOverride ?: ::performResetOperations
                resetAction(applicationContext, now.toLocalDate())
            }
            schedule(applicationContext, now)
            Result.success()
        } catch (_: Throwable) {
            schedule(applicationContext, now)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "midnight_reset_worker"
        private const val RESET_WINDOW_MINUTES = 5L
        private const val LEGACY_WATER_PREFS = "water_data_prefs"

        @VisibleForTesting
        internal var nowProvider: () -> ZonedDateTime = { ZonedDateTime.now() }

        @VisibleForTesting
        internal var resetActionOverride: (suspend (Context, LocalDate) -> Unit)? = null

        fun schedule(context: Context, now: ZonedDateTime = nowProvider()) {
            val request = OneTimeWorkRequestBuilder<MidnightResetWorker>()
                .setInitialDelay(calculateInitialDelay(now))
                .addTag(UNIQUE_WORK_NAME)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        @VisibleForTesting
        internal fun calculateInitialDelay(now: ZonedDateTime): Duration {
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            val delay = Duration.between(now, nextMidnight)
            return if (delay.isNegative || delay.isZero) Duration.ofMinutes(1) else delay
        }

        @VisibleForTesting
        internal fun shouldPerformReset(now: ZonedDateTime): Boolean {
            val resetDeadline = LocalTime.MIDNIGHT.plusMinutes(RESET_WINDOW_MINUTES)
            return !now.toLocalTime().isAfter(resetDeadline)
        }

        @VisibleForTesting
        internal suspend fun performResetOperations(context: Context, date: LocalDate) {
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Water data currently exists in both a legacy SharedPreferences log store and a small
            // DataStore compatibility layer; clear both for the new day when the worker runs on time.
            context.getSharedPreferences(LEGACY_WATER_PREFS, Context.MODE_PRIVATE).edit {
                remove("entries_$dateString")
            }
            runCatching {
                UserDataStore(context.applicationContext).resetWaterForDate(dateString)
            }

            // Steps are day-keyed, but clearing an accidental stale row keeps midnight UX consistent.
            runCatching {
                RepositoryProvider.provideStepRepository(context.applicationContext)
                    .resetStepsForDate(dateString)
            }
            StepCounterService.markNewDay(context.applicationContext, dateString)
        }

        @VisibleForTesting
        internal fun clearTestOverrides() {
            nowProvider = { ZonedDateTime.now() }
            resetActionOverride = null
        }
    }
}

