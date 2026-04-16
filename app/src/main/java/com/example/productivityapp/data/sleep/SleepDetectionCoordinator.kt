package com.example.productivityapp.data.sleep

import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.entities.toSleepTagsStorage
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SleepSignalSnapshot(
    val idleMinutes: Int,
    val lastInteractionMinutesAgo: Int,
    val wakeInteractionMinutesAgo: Int? = null,
    val isCharging: Boolean = false,
    val hasForegroundActivity: Boolean = false,
    val nowEpochMs: Long = System.currentTimeMillis(),
)

data class SleepDetectionCandidate(
    val date: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSec: Long,
    val confidenceScore: Double,
    val detectionSource: SleepDetectionSource,
    val reviewState: SleepReviewState,
    val inferredStartTimestamp: Long,
    val inferredEndTimestamp: Long,
    val tags: List<String>,
)

sealed class SleepDetectionOutcome {
    data class Candidate(val candidate: SleepDetectionCandidate) : SleepDetectionOutcome()
    data object NoMatch : SleepDetectionOutcome()
    data object Suppressed : SleepDetectionOutcome()
}

sealed class SleepPersistenceOutcome {
    data class Persisted(val id: Long, val session: SleepEntity) : SleepPersistenceOutcome()
    data object SkippedActiveSession : SleepPersistenceOutcome()
    data object SkippedDuplicate : SleepPersistenceOutcome()
    data object Rejected : SleepPersistenceOutcome()
}

class HeuristicSleepDetectionCoordinator(
    private val repo: SleepRepository,
) {
    suspend fun detect(profile: UserProfile, signals: SleepSignalSnapshot): SleepDetectionOutcome {
        val confidence = score(profile, signals)
        if (confidence < 0.7) return SleepDetectionOutcome.NoMatch

        val bedtime = profile.typicalBedtimeMinutes
        val wakeTime = profile.typicalWakeTimeMinutes
        val minuteOfDay = minuteOfDay(signals.nowEpochMs)
        val overnight = isWithinSleepWindow(minuteOfDay, bedtime, wakeTime)
        val durationMinutes = max(signals.idleMinutes, profile.nightlySleepGoalMinutes / 2)
        val inferredStart = signals.nowEpochMs - durationMinutes.coerceAtLeast(0) * 60_000L
        val inferredEnd = signals.nowEpochMs
        val durationSec = ((inferredEnd - inferredStart).coerceAtLeast(0L) / 1000L)
        val tags = buildList {
            add(if (overnight) "overnight" else "nap")
            if (signals.isCharging) add("charging")
            if (signals.hasForegroundActivity) add("foreground_idle")
        }

        return SleepDetectionOutcome.Candidate(
            SleepDetectionCandidate(
                date = epochToDate(signals.nowEpochMs),
                startTimestamp = inferredStart,
                endTimestamp = inferredEnd,
                durationSec = durationSec,
                confidenceScore = confidence,
                detectionSource = SleepDetectionSource.AUTO,
                reviewState = SleepReviewState.PROVISIONAL,
                inferredStartTimestamp = inferredStart,
                inferredEndTimestamp = inferredEnd,
                tags = tags,
            )
        )
    }

    suspend fun detectAndPersist(profile: UserProfile, signals: SleepSignalSnapshot): SleepPersistenceOutcome {
        val outcome = detect(profile, signals)
        val candidate = (outcome as? SleepDetectionOutcome.Candidate)?.candidate ?: return when (outcome) {
            SleepDetectionOutcome.NoMatch -> SleepPersistenceOutcome.Rejected
            SleepDetectionOutcome.Suppressed -> SleepPersistenceOutcome.Rejected
            is SleepDetectionOutcome.Candidate -> error("unreachable")
        }
        return persist(candidate)
    }

    suspend fun persist(candidate: SleepDetectionCandidate): SleepPersistenceOutcome {
        val active = repo.getActiveSleepSession()
        if (active != null) return SleepPersistenceOutcome.SkippedActiveSession

        val existingSessions = repo.observeSleepForDate(candidate.date).first()
        val duplicate = existingSessions.any { existing ->
            existing.detectionSource == SleepDetectionSource.AUTO.storageValue &&
                existing.reviewState == SleepReviewState.PROVISIONAL.storageValue &&
                abs(existing.startTimestamp - candidate.startTimestamp) < 15 * 60_000L
        }
        if (duplicate) return SleepPersistenceOutcome.SkippedDuplicate

        val entity = candidate.toEntity()
        val id = repo.startSleep(entity)
        val persisted = repo.getSleepById(id) ?: entity.copy(id = id)
        return SleepPersistenceOutcome.Persisted(id = id, session = persisted)
    }

    private fun score(profile: UserProfile, signals: SleepSignalSnapshot): Double {
        if (signals.lastInteractionMinutesAgo < 5) return 0.0

        val idleScore = when {
            signals.idleMinutes >= max(profile.nightlySleepGoalMinutes / 2, 240) -> 0.55
            signals.idleMinutes >= 180 -> 0.42
            signals.idleMinutes >= 90 -> 0.28
            else -> 0.0
        }

        val windowScore = when {
            isWithinSleepWindow(minuteOfDay(signals.nowEpochMs), profile.typicalBedtimeMinutes, profile.typicalWakeTimeMinutes) -> 0.2
            isNearWindowEdge(minuteOfDay(signals.nowEpochMs), profile.typicalBedtimeMinutes, profile.sleepDetectionBufferMinutes) -> 0.1
            else -> 0.0
        }

        val chargingScore = if (signals.isCharging) 0.12 else 0.0
        val wakePenalty = when {
            signals.wakeInteractionMinutesAgo != null && signals.wakeInteractionMinutesAgo <= 15 -> -0.25
            signals.wakeInteractionMinutesAgo != null && signals.wakeInteractionMinutesAgo <= 45 -> -0.1
            else -> 0.0
        }
        val foregroundPenalty = if (signals.hasForegroundActivity) -0.15 else 0.0
        return (idleScore + windowScore + chargingScore + wakePenalty + foregroundPenalty).coerceIn(0.0, 1.0)
    }

    private fun isWithinSleepWindow(minuteOfDay: Int, bedtimeMinutes: Int, wakeMinutes: Int): Boolean {
        return if (bedtimeMinutes <= wakeMinutes) {
            minuteOfDay in bedtimeMinutes..wakeMinutes
        } else {
            minuteOfDay >= bedtimeMinutes || minuteOfDay <= wakeMinutes
        }
    }

    private fun isNearWindowEdge(minuteOfDay: Int, bedtimeMinutes: Int, bufferMinutes: Int): Boolean {
        val bedtimeDistance = circularDistance(minuteOfDay, bedtimeMinutes)
        return bedtimeDistance <= bufferMinutes
    }

    private fun circularDistance(a: Int, b: Int): Int {
        val raw = abs(a - b)
        return min(raw, 1440 - raw)
    }

    private fun SleepDetectionCandidate.toEntity(): SleepEntity {
        return SleepEntity(
            date = date,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            durationSec = durationSec,
            sleepQuality = null,
            notes = null,
            detectionSource = detectionSource.storageValue,
            confidenceScore = confidenceScore,
            inferredStartTimestamp = inferredStartTimestamp,
            inferredEndTimestamp = inferredEndTimestamp,
            reviewState = reviewState.storageValue,
            tagsCsv = tags.toSleepTagsStorage(),
        )
    }

    private fun minuteOfDay(epochMs: Long): Int {
        val zone = java.time.ZoneId.systemDefault()
        val instant = java.time.Instant.ofEpochMilli(epochMs)
        val localTime = instant.atZone(zone).toLocalTime()
        return localTime.hour * 60 + localTime.minute
    }

    private fun epochToDate(epochMs: Long): String {
        return java.time.Instant.ofEpochMilli(epochMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }
}
