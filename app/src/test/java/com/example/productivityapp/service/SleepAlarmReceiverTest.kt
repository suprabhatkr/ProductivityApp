package com.example.productivityapp.service

import android.content.Context
import android.content.Intent
import com.example.productivityapp.data.entities.SleepDetectionSource
import com.example.productivityapp.data.entities.SleepEntity
import com.example.productivityapp.data.entities.SleepReviewState
import com.example.productivityapp.data.repository.SleepRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SleepAlarmReceiverTest {

    @Test
    fun confirmWake_finishesActiveNapSession() {
        val active = SleepEntity(
            id = 7L,
            date = "2026-04-15",
            startTimestamp = 1_000L,
            endTimestamp = 0L,
            durationSec = 0L,
            sleepQuality = null,
            notes = null,
            detectionSource = SleepDetectionSource.NAP.storageValue,
            confidenceScore = 1.0,
            inferredStartTimestamp = 1_000L,
            inferredEndTimestamp = null,
            reviewState = SleepReviewState.CONFIRMED.storageValue,
            tagsCsv = "nap",
        )
        val repo = NapAlarmSleepRepository(active)
        val expectedEnd = ZonedDateTime.parse("2026-04-15T10:30:00Z").toInstant().toEpochMilli()
        val receiver = SleepAlarmReceiver().apply {
            sleepRepositoryProvider = { repo }
            nowProvider = { ZonedDateTime.parse("2026-04-15T10:30:00Z") }
            cancelNapReminderAction = { }
            cancelWakeAlarmAction = { }
        }

        receiver.onReceive(
            ApplicationProvider.getApplicationContext<Context>(),
            Intent(SleepAlarmReceiver.ACTION_CONFIRM_WAKE),
        )

        val stopped = repo.sessions.value.first()
        assertNotNull(stopped)
        assertEquals(expectedEnd, stopped.endTimestamp)
        assertEquals((expectedEnd - 1_000L) / 1000L, stopped.durationSec)
        assertEquals(SleepReviewState.CONFIRMED.storageValue, stopped.reviewState)
    }
}

private class NapAlarmSleepRepository(initial: SleepEntity) : SleepRepository {
    val sessions = MutableStateFlow(listOf(initial))

    override fun observeSleepForDate(date: String): Flow<List<SleepEntity>> =
        sessions.map { list -> list.filter { it.date == date } }

    override fun observeSleepForRange(startDate: String, endDate: String): Flow<List<SleepEntity>> = sessions

    override suspend fun getActiveSleepSession(): SleepEntity? = sessions.value.firstOrNull { it.endTimestamp == 0L }

    override suspend fun getSleepById(id: Long): SleepEntity? = sessions.value.firstOrNull { it.id == id }

    override suspend fun startSleep(session: SleepEntity): Long {
        sessions.value = sessions.value + session.copy(id = 99L)
        return 99L
    }

    override suspend fun stopSleep(session: SleepEntity): SleepEntity {
        sessions.value = sessions.value.map { existing ->
            if (existing.id == session.id) session else existing
        }
        return session
    }

    override suspend fun updateSleep(session: SleepEntity) {
        sessions.value = sessions.value.map { existing ->
            if (existing.id == session.id) session else existing
        }
    }

    override suspend fun deleteSleep(id: Long) {
        sessions.value = sessions.value.filterNot { it.id == id }
    }
}
