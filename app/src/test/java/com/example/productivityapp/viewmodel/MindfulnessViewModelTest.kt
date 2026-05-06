package com.example.productivityapp.viewmodel

import com.example.productivityapp.data.entities.MindLogEntity
import com.example.productivityapp.data.entities.MindfulnessSessionEntity
import com.example.productivityapp.data.model.MindfulnessSessionType
import com.example.productivityapp.data.repository.MindfulnessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MindfulnessViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startSession_usesSelectedPracticeType() = runTest(dispatcher) {
        val repository = FakeMindfulnessRepository()
        val viewModel = MindfulnessViewModel(repository)
        viewModel.selectSessionType(MindfulnessSessionType.MEDITATION)

        viewModel.startSession()
        advanceUntilIdle()

        val activeSession = repository.activeSessionFlow.value
        assertNotNull(activeSession)
        assertEquals(MindfulnessSessionType.MEDITATION.storageValue, activeSession?.sessionType)
    }

    @Test
    fun endSession_finishesActivePractice() = runTest(dispatcher) {
        val repository = FakeMindfulnessRepository(
            activeSession = MindfulnessSessionEntity(
                id = 7L,
                sessionType = MindfulnessSessionType.BREATHING.storageValue,
                startTime = System.currentTimeMillis() - 5_000L,
                endTime = null,
                durationSec = 0L,
            )
        )
        val viewModel = MindfulnessViewModel(repository)
        advanceUntilIdle()

        viewModel.endSession()
        advanceUntilIdle()

        val finished = repository.sessionsFlow.value.first()
        assertTrue(finished.endTime != null)
        assertTrue(finished.durationSec >= 0L)
    }

    @Test
    fun saveReflection_persistsMindLogAndClearsDraft() = runTest(dispatcher) {
        val repository = FakeMindfulnessRepository()
        val viewModel = MindfulnessViewModel(repository)
        viewModel.updateReflectionDraft("Needed a calm pause this afternoon.")

        viewModel.saveReflection()
        advanceUntilIdle()

        assertEquals("", viewModel.reflectionDraft.value)
        assertEquals("Needed a calm pause this afternoon.", repository.logsFlow.value.first().content)
    }
}

private class FakeMindfulnessRepository(
    activeSession: MindfulnessSessionEntity? = null,
    logs: List<MindLogEntity> = emptyList(),
) : MindfulnessRepository {
    val sessionsFlow = MutableStateFlow(activeSession?.let { listOf(it) } ?: emptyList())
    val logsFlow = MutableStateFlow(logs)
    val activeSessionFlow = MutableStateFlow(activeSession)

    override fun observeSessions(): Flow<List<MindfulnessSessionEntity>> = sessionsFlow

    override fun observeLogs(): Flow<List<MindLogEntity>> = logsFlow

    override fun observeActiveSession(): Flow<MindfulnessSessionEntity?> = activeSessionFlow

    override suspend fun getSessionById(id: Long): MindfulnessSessionEntity? {
        return sessionsFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun startSession(session: MindfulnessSessionEntity): Long {
        val stored = session.copy(id = if (session.id == 0L) 1L else session.id)
        sessionsFlow.value = listOf(stored) + sessionsFlow.value.filterNot { it.id == stored.id }
        activeSessionFlow.value = stored
        return stored.id
    }

    override suspend fun updateSession(session: MindfulnessSessionEntity) {
        sessionsFlow.value = sessionsFlow.value
            .map { existing -> if (existing.id == session.id) session else existing }
            .ifEmpty { listOf(session) }
        activeSessionFlow.value = session.takeIf { it.endTime == null }
    }

    override suspend fun addLog(log: MindLogEntity): Long {
        val stored = log.copy(id = if (log.id == 0L) 1L else log.id)
        logsFlow.value = listOf(stored) + logsFlow.value.filterNot { it.id == stored.id }
        return stored.id
    }
}
