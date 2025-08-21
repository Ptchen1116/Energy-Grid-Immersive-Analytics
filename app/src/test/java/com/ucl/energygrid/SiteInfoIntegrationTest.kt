package com.ucl.energygrid


import com.ucl.energygrid.data.model.PinRequest
import com.ucl.energygrid.data.model.PinResponse
import com.ucl.energygrid.data.remote.apis.PinApi
import com.ucl.energygrid.ui.layout.siteInformationPanel.SiteInformationViewModel
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@OptIn(ExperimentalCoroutinesApi::class)
class SiteInformationViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var api: PinApi
    private lateinit var viewModel: SiteInformationViewModel

    private val userId = 1
    private val mineId = 100

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full pin lifecycle integration test`() = runTest {
        val fakePin = PinResponse(id = 1, mine_id = 123, note = "Test note")

        coEvery { api.getPin(userId, mineId) } returns Response.success(
            fakePin
        )

        coEvery { api.create_or_update_pin(userId, any()) } returns Response.success(PinResponse(id = 1, mine_id = 123, note = "Updated note"))

        coEvery { api.deletePin(userId, mineId) } returns Response.success(Unit)

        viewModel = SiteInformationViewModel(userId, mineId, api)

        testScheduler.advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals("Test note", state.note)
        assertEquals("Test note", state.savedNote)
        assertTrue(state.isPinned)
        assertTrue(state.isNoteLoaded)

        viewModel.updateNote("Updated note")
        state = viewModel.uiState.value
        assertEquals("Updated note", state.note)
        assertEquals("Test note", state.savedNote) // savedNote 未改变

        viewModel.saveNote()
        testScheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("Updated note", state.savedNote)
        assertTrue(state.isPinned)
        assertEquals("Note saved successfully", state.postResult)

        viewModel.removePin()
        testScheduler.advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals("", state.note)
        assertEquals("", state.savedNote)
        assertFalse(state.isPinned)
        assertEquals("Pin removed successfully", state.postResult)

        coVerifySequence {
            api.getPin(userId, mineId)
            api.create_or_update_pin(userId, PinRequest(mine_id = mineId, note = "Updated note"))
            api.deletePin(userId, mineId)
        }
    }
}