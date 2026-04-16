package com.example.productivityapp.data.sleep

import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class HeuristicSleepDetectionCoordinatorTest {
    @Test
    fun detectAndPersist_strongOvernightSignalCreatesProvisionalAutoSession() = runTest {
        val repo = FakeSleepRepository()
        val coordinator = HeuristicSleepDetectionCoordinator(repo)
        val profile = UserProfile(
            nightlySleepGoalMinutes = 480,
            typicalBedtimeMinutes = 22 * 60,
            typicalWakeTimeMinutes = 7 * 60,
            sleepDetectionBufferMinutes = 30,
        )
        val now = LocalDateTime.of(2026, 4, 10, 23, 30)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val outcome = coordinator.detectAndPersist(
            profile = profile,
            signals = SleepSignalSnapshot(
                idleMinutes = 360,
                lastInteractionMinutesAgo = 240,
                wakeInteractionMinutesAgo = null,
                isCharging = true,
                hasForegroundActivity = false,
                nowEpochMs = now,
            )
        )

        assertTrue(outcome is SleepPersistenceOutcome.Persisted)
        val persisted = repo.sessions.value.first()
        assertEquals(SleepDetectionSource.AUTO.storageValue, persisted.detectionSource)
        assertEquals(SleepReviewState.PROVISIONAL.storageValue, persisted.reviewState)
        assertEquals("overnight,charging", persisted.tagsCsv)
        assertTrue(persisted.confidenceScore >= 0.7)
    }

    @Test
    fun detectAndPersist_weakSignalIsRejected() = runTest {
        val repo = FakeSleepRepository()
        val coordinator = HeuristicSleepDetectionCoordinator(repo)
        val profile = UserProfile()
        val now = LocalDateTime.of(2026, 4, 10, 14, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val outcome = coordinator.detectAndPersist(
            profile = profile,
            signals = SleepSignalSnapshot(
                idleMinutes = 20,
                lastInteractionMinutesAgo = 2,
                wakeInteractionMinutesAgo = null,
                isCharging = false,
                hasForegroundActivity = true,
                nowEpochMs = now,
            )
        )

        assertEquals(SleepPersistenceOutcome.Rejected, outcome)
        assertTrue(repo.sessions.value.isEmpty())
    }

    @Test
    fun persist_duplicateAutoCandidate_isSkipped() = runTest {
        val existing = SleepEntity(
            date = "2026-04-10",
            startTimestamp = 1_000L,
            endTimestamp = 2_000L,
            durationSec = 1_000L,
            sleepQuality = null,
            notes = null,
            detectionSource = SleepDetectionSource.AUTO.storageValue,
            confidenceScore = 0.8,
            reviewState = SleepReviewState.PROVISIONAL.storageValue,
            tagsCsv = "overnight",
        )
        val repo = FakeSleepRepository(initial = listOf(existing))
        val coordinator = HeuristicSleepDetectionCoordinator(repo)

        val outcome = coordinator.persist(
            SleepDetectionCandidate(
                date = "2026-04-10",
                startTimestamp = 1_200L,
                endTimestamp = 2_200L,
                durationSec = 1_000L,
                confidenceScore = 0.85,
                detectionSource = SleepDetectionSource.AUTO,
                reviewState = SleepReviewState.PROVISIONAL,
                inferredStartTimestamp = 1_200L,
                inferredEndTimestamp = 2_200L,
                tags = listOf("overnight"),
            )
        )

        assertEquals(SleepPersistenceOutcome.SkippedDuplicate, outcome)
    }
}

private class FakeSleepRepository(initial: List<SleepEntity> = emptyList()) : SleepRepository {
    val sessions = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observeSleepForDate(date: String): Flow<List<SleepEntity>> =
        sessions.map { list -> list.filter { it.date == date }.sortedByDescending { it.startTimestamp } }

    override fun observeSleepForRange(startDate: String, endDate: String): Flow<List<SleepEntity>> =
        sessions.map { list ->
            list.filter { it.date >= startDate && it.date <= endDate }.sortedByDescending { it.startTimestamp }
        }

    override suspend fun getActiveSleepSession(): SleepEntity? =
        sessions.value.firstOrNull { it.endTimestamp == 0L }

    override suspend fun getSleepById(id: Long): SleepEntity? =
        sessions.value.firstOrNull { it.id == id }

    override suspend fun startSleep(session: SleepEntity): Long {
        val active = getActiveSleepSession()
        if (active != null) return active.id
        val persisted = session.copy(id = nextId++)
        sessions.value = listOf(persisted) + sessions.value
        return persisted.id
    }

    override suspend fun stopSleep(session: SleepEntity): SleepEntity {
        updateSleep(session)
        return session
    }

    override suspend fun updateSleep(session: SleepEntity) {
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun deleteSleep(id: Long) {
        sessions.value = sessions.value.filterNot { it.id == id }
    }
}
