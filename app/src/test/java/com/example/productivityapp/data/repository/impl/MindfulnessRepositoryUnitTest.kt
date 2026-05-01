package com.example.productivityapp.data.repository.impl

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.productivityapp.data.AppDatabase
import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import com.example.productivityapp.data.model.MindfulnessSessionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MindfulnessRepositoryUnitTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: RoomMindfulnessRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomMindfulnessRepository(db.mindfulnessDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startSession_persistsActiveMindfulnessSession() = runTest {
        val id = repository.startSession(
            MindfulnessSessionEntity(
                sessionType = MindfulnessSessionType.BREATHING.storageValue,
                startTime = 1_700_000_000_000L,
                endTime = null,
                durationSec = 0L,
            )
        )

        val stored = repository.getSessionById(id)
        assertNotNull(stored)
        assertEquals(MindfulnessSessionType.BREATHING.storageValue, stored?.sessionType)
    }

    @Test
    fun updateSession_finishesSessionAndKeepsLogSupport() = runTest {
        val id = repository.startSession(
            MindfulnessSessionEntity(
                sessionType = MindfulnessSessionType.MEDITATION.storageValue,
                startTime = 1_700_000_000_000L,
                endTime = null,
                durationSec = 0L,
            )
        )
        val session = repository.getSessionById(id) ?: error("Expected stored session")

        repository.updateSession(
            session.copy(
                endTime = session.startTime + 600_000L,
                durationSec = 600L,
            )
        )
        repository.addLog(
            MindLogEntity(
                createdAt = session.startTime + 700_000L,
                content = "Mind feels clearer after this session.",
            )
        )

        val updated = repository.getSessionById(id)
        assertEquals(600L, updated?.durationSec)
        assertNull(repository.observeActiveSession().first())
    }
}
