package com.example.productivityapp.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.sleep.SleepSignalProvider
import com.example.productivityapp.data.sleep.SleepSignalSnapshot
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class SleepMaintenanceWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SleepMaintenanceWorker.clearTestOverrides()
    }

    @After
    fun tearDown() {
        SleepMaintenanceWorker.clearTestOverrides()
    }

    @Test
    fun calculateInitialDelay_prefersWakeBoundaryDuringSleepWindow() {
        val profile = UserProfile(
            typicalBedtimeMinutes = 22 * 60,
            typicalWakeTimeMinutes = 7 * 60,
            sleepDetectionBufferMinutes = 30,
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 10),
            LocalTime.of(23, 30),
            ZoneId.of("Asia/Kolkata"),
        )

        val delay = SleepMaintenanceWorker.calculateInitialDelay(now, profile)

        assertEquals(450, delay.toMinutes())
    }

    @Test
    fun performMaintenance_finalizesActiveAutoSessionAfterWakeWindow() = runTest {
        val repo = FakeSleepRepository(
            initial = listOf(
                SleepEntity(
                    id = 1L,
                    date = "2026-04-10",
                    startTimestamp = 1_000L,
                    endTimestamp = 0L,
                    durationSec = 0L,
                    sleepQuality = null,
                    notes = null,
                    detectionSource = SleepDetectionSource.AUTO.storageValue,
                    confidenceScore = 0.88,
                    reviewState = SleepReviewState.PROVISIONAL.storageValue,
                )
            )
        )
        val profileRepo = FakeUserProfileRepository(
            UserProfile(
                typicalBedtimeMinutes = 22 * 60,
                typicalWakeTimeMinutes = 7 * 60,
                sleepDetectionBufferMinutes = 30,
            )
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 11),
            LocalTime.of(7, 45),
            ZoneId.of("Asia/Kolkata"),
        )

        SleepMaintenanceWorker.sleepRepositoryProvider = { repo }
        SleepMaintenanceWorker.userProfileRepositoryProvider = { profileRepo }

        SleepMaintenanceWorker.performMaintenance(context, now)

        val saved = repo.sessions.value.first()
        assertTrue(saved.endTimestamp > 0L)
        assertEquals(SleepReviewState.NEEDS_REVIEW.storageValue, saved.reviewState)
    }

    @Test
    fun performMaintenance_marksOldProvisionalAutoSessionNeedsReview() = runTest {
        val repo = FakeSleepRepository(
            initial = listOf(
                SleepEntity(
                    id = 2L,
                    date = "2026-04-11",
                    startTimestamp = 1_000L,
                    endTimestamp = 10_000L,
                    durationSec = 9_000L,
                    sleepQuality = null,
                    notes = null,
                    detectionSource = SleepDetectionSource.AUTO.storageValue,
                    confidenceScore = 0.8,
                    reviewState = SleepReviewState.PROVISIONAL.storageValue,
                )
            )
        )
        val profileRepo = FakeUserProfileRepository(
            UserProfile(
                sleepDetectionBufferMinutes = 30,
            )
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 11),
            LocalTime.of(8, 0),
            ZoneId.of("Asia/Kolkata"),
        )

        SleepMaintenanceWorker.sleepRepositoryProvider = { repo }
        SleepMaintenanceWorker.userProfileRepositoryProvider = { profileRepo }

        SleepMaintenanceWorker.performMaintenance(context, now)

        val saved = repo.sessions.value.first()
        assertEquals(SleepReviewState.NEEDS_REVIEW.storageValue, saved.reviewState)
    }

    @Test
    fun performMaintenance_detectsAndPersistsProvisionalSleepWhenSignalsAreStrong() = runTest {
        val repo = FakeSleepRepository()
        val profileRepo = FakeUserProfileRepository(
            UserProfile(
                typicalBedtimeMinutes = 22 * 60,
                typicalWakeTimeMinutes = 7 * 60,
                sleepDetectionBufferMinutes = 30,
            )
        )
        val now = ZonedDateTime.of(
            LocalDate.of(2026, 4, 11),
            LocalTime.of(23, 30),
            ZoneId.of("Asia/Kolkata"),
        )

        SleepMaintenanceWorker.sleepRepositoryProvider = { repo }
        SleepMaintenanceWorker.userProfileRepositoryProvider = { profileRepo }
        SleepMaintenanceWorker.sleepSignalProvider = object : SleepSignalProvider {
            override fun collect(
                context: Context,
                profile: UserProfile,
                now: ZonedDateTime,
            ): SleepSignalSnapshot? = SleepSignalSnapshot(
                idleMinutes = 360,
                lastInteractionMinutesAgo = 240,
                wakeInteractionMinutesAgo = null,
                isCharging = true,
                hasForegroundActivity = false,
                nowEpochMs = now.toInstant().toEpochMilli(),
            )
        }

        SleepMaintenanceWorker.performMaintenance(context, now)

        val saved = repo.sessions.value.first()
        assertEquals(SleepDetectionSource.AUTO.storageValue, saved.detectionSource)
        assertEquals(SleepReviewState.PROVISIONAL.storageValue, saved.reviewState)
        assertEquals("overnight,charging", saved.tagsCsv)
    }
}

private class FakeUserProfileRepository(initial: UserProfile) : UserProfileRepository {
    private val state = MutableStateFlow(initial)

    override fun observeUserProfile(): Flow<UserProfile> = state

    override fun getUserProfileBlocking(): UserProfile = state.value

    override suspend fun updateUserProfile(profile: UserProfile) {
        state.value = profile
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
