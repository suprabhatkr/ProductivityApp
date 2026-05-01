package com.example.productivityapp.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.app.data.model.WaterDayData
import com.example.productivityapp.app.data.model.WaterEntry
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.StepEntity
import com.example.productivityapp.data.entities.StepSampleEntity
import com.example.productivityapp.data.model.UserProfile
import com.example.productivityapp.data.repository.SleepRepository
import com.example.productivityapp.data.repository.StepRepository
import com.example.productivityapp.data.repository.UserProfileRepository
import com.example.productivityapp.data.repository.WaterRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HealthReminderWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("health_reminder_state", Context.MODE_PRIVATE).edit().clear().commit()
        HealthReminderWorker.clearTestOverrides()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("health_reminder_state", Context.MODE_PRIVATE).edit().clear().commit()
        HealthReminderWorker.clearTestOverrides()
    }

    @Test
    fun performReminderCheck_sendsFirstWaterReminderDuringWakeHours() = runTest {
        val events = mutableListOf<String>()
        val date = "2026-05-01"
        val now = ZonedDateTime.of(LocalDate.of(2026, 5, 1), LocalTime.of(10, 0), ZoneId.of("Asia/Kolkata"))
        installFakes(
            waterRepository = ReminderFakeWaterRepository(
                mutableMapOf(date to WaterDayData(date = date, goalMl = 2200))
            ),
            stepRepository = ReminderFakeStepRepository(),
            sleepRepository = ReminderFakeSleepRepository(),
            profileRepository = ReminderFakeUserProfileRepository(UserProfile()),
            events = events,
        )

        HealthReminderWorker.performReminderCheck(context, now)

        assertEquals(listOf("water-first"), events)
    }

    @Test
    fun performReminderCheck_sendsWaterIdleReminderWhenLastDrinkIsOld() = runTest {
        val events = mutableListOf<String>()
        val date = "2026-05-01"
        val now = ZonedDateTime.of(LocalDate.of(2026, 5, 1), LocalTime.of(14, 0), ZoneId.of("Asia/Kolkata"))
        installFakes(
            waterRepository = ReminderFakeWaterRepository(
                mutableMapOf(
                    date to WaterDayData(
                        date = date,
                        goalMl = 2200,
                        entries = listOf(
                            WaterEntry(
                                id = 7L,
                                amountMl = 300,
                                timestamp = LocalDateTime.of(2026, 5, 1, 12, 0),
                            )
                        ),
                    )
                )
            ),
            stepRepository = ReminderFakeStepRepository(),
            sleepRepository = ReminderFakeSleepRepository(),
            profileRepository = ReminderFakeUserProfileRepository(UserProfile()),
            events = events,
        )

        HealthReminderWorker.performReminderCheck(context, now)

        assertEquals(listOf("water-idle"), events)
    }

    @Test
    fun performReminderCheck_prefersNinetyPercentStepReminderOverHalfway() = runTest {
        val events = mutableListOf<String>()
        val date = "2026-05-01"
        val now = ZonedDateTime.of(LocalDate.of(2026, 5, 1), LocalTime.of(16, 0), ZoneId.of("Asia/Kolkata"))
        installFakes(
            waterRepository = ReminderFakeWaterRepository(
                mutableMapOf(
                    date to WaterDayData(
                        date = date,
                        entries = listOf(
                            WaterEntry(
                                id = 9L,
                                amountMl = 250,
                                timestamp = LocalDateTime.of(2026, 5, 1, 15, 30),
                            )
                        ),
                    )
                )
            ),
            stepRepository = ReminderFakeStepRepository(
                mapOf(
                    date to StepEntity(
                        date = date,
                        steps = 9_100,
                        distanceMeters = 0.0,
                        calories = 0.0,
                        source = "test",
                        lastUpdatedAt = 0L,
                    )
                )
            ),
            sleepRepository = ReminderFakeSleepRepository(),
            profileRepository = ReminderFakeUserProfileRepository(UserProfile(dailyStepGoal = 10_000)),
            events = events,
        )

        HealthReminderWorker.performReminderCheck(context, now)

        assertEquals(listOf("step-ninety"), events)
    }

    @Test
    fun performReminderCheck_sendsBedtimeReminderThirtyMinutesBeforeTarget() = runTest {
        val events = mutableListOf<String>()
        val date = "2026-05-01"
        val now = ZonedDateTime.of(LocalDate.of(2026, 5, 1), LocalTime.of(21, 35), ZoneId.of("Asia/Kolkata"))
        installFakes(
            waterRepository = ReminderFakeWaterRepository(
                mutableMapOf(
                    date to WaterDayData(
                        date = date,
                        entries = listOf(
                            WaterEntry(
                                id = 11L,
                                amountMl = 250,
                                timestamp = LocalDateTime.of(2026, 5, 1, 20, 45),
                            )
                        ),
                    )
                )
            ),
            stepRepository = ReminderFakeStepRepository(
                mapOf(
                    date to StepEntity(
                        date = date,
                        steps = 10_000,
                        distanceMeters = 0.0,
                        calories = 0.0,
                        source = "test",
                        lastUpdatedAt = 0L,
                    )
                )
            ),
            sleepRepository = ReminderFakeSleepRepository(),
            profileRepository = ReminderFakeUserProfileRepository(UserProfile(typicalBedtimeMinutes = 22 * 60)),
            events = events,
        )
        HealthReminderStateStore(context).apply {
            markStepHalf(date)
            markStepNinety(date)
            markStepEvening(date)
        }

        HealthReminderWorker.performReminderCheck(context, now)

        assertEquals(listOf("bedtime"), events)
    }

    private fun installFakes(
        waterRepository: WaterRepository,
        stepRepository: StepRepository,
        sleepRepository: SleepRepository,
        profileRepository: UserProfileRepository,
        events: MutableList<String>,
    ) {
        HealthReminderWorker.waterRepositoryProvider = { waterRepository }
        HealthReminderWorker.stepRepositoryProvider = { stepRepository }
        HealthReminderWorker.sleepRepositoryProvider = { sleepRepository }
        HealthReminderWorker.userProfileRepositoryProvider = { profileRepository }
        HealthReminderWorker.waterFirstNotifier = { _, _ -> events += "water-first" }
        HealthReminderWorker.waterIdleNotifier = { _, _ -> events += "water-idle" }
        HealthReminderWorker.stepHalfNotifier = { _, _ -> events += "step-half" }
        HealthReminderWorker.stepNinetyNotifier = { _, _ -> events += "step-ninety" }
        HealthReminderWorker.stepEveningNotifier = { _, _, _ -> events += "step-evening" }
        HealthReminderWorker.bedtimeNotifier = { _, _, _ -> events += "bedtime" }
    }
}

private class ReminderFakeWaterRepository(
    private val days: MutableMap<String, WaterDayData> = mutableMapOf(),
) : WaterRepository {
    override fun observeDay(date: String): Flow<WaterDayData> = flowOf(days[date] ?: WaterDayData(date = date))

    override suspend fun getDay(date: String): WaterDayData = days[date] ?: WaterDayData(date = date)

    override suspend fun addEntry(date: String, amountMl: Int): Long = 0L

    override suspend fun removeEntry(date: String, id: Long) = Unit
}

private class ReminderFakeStepRepository(
    private val days: Map<String, StepEntity> = emptyMap(),
) : StepRepository {
    override fun observeStepsForDate(date: String): Flow<StepEntity?> = flowOf(days[date])

    override suspend fun getStepsForDate(date: String): StepEntity? = days[date]

    override suspend fun getStepsForRange(startDate: String, endDate: String): List<StepEntity> = emptyList()

    override fun observeStepsForRange(startDate: String, endDate: String): Flow<List<StepEntity>> = flowOf(emptyList())

    override suspend fun upsertSteps(step: StepEntity) = Unit

    override suspend fun incrementSteps(date: String, delta: Int, source: String) = Unit

    override suspend fun resetStepsForDate(date: String) = Unit

    override fun observeSamplesForDate(date: String): Flow<List<StepSampleEntity>> = flowOf(emptyList())

    override suspend fun getSamplesForDate(date: String): List<StepSampleEntity> = emptyList()

    override suspend fun insertSample(sample: StepSampleEntity) = Unit
}

private class ReminderFakeUserProfileRepository(initial: UserProfile) : UserProfileRepository {
    private val state = MutableStateFlow(initial)

    override fun observeUserProfile(): Flow<UserProfile> = state

    override fun getUserProfileBlocking(): UserProfile = state.value

    override suspend fun updateUserProfile(profile: UserProfile) {
        state.value = profile
    }
}

private class ReminderFakeSleepRepository(
    private val active: SleepEntity? = null,
) : SleepRepository {
    override fun observeSleepForDate(date: String): Flow<List<SleepEntity>> = flowOf(emptyList())

    override fun observeSleepForRange(startDate: String, endDate: String): Flow<List<SleepEntity>> = flowOf(emptyList())

    override suspend fun getActiveSleepSession(): SleepEntity? = active

    override suspend fun getSleepById(id: Long): SleepEntity? = null

    override suspend fun startSleep(session: SleepEntity): Long = 0L

    override suspend fun stopSleep(session: SleepEntity): SleepEntity = session

    override suspend fun updateSleep(session: SleepEntity) = Unit

    override suspend fun deleteSleep(id: Long) = Unit
}
