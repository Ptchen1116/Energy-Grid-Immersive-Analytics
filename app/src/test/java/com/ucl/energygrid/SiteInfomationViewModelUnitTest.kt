package com.ucl.energygrid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import retrofit2.Response
import com.ucl.energygrid.ui.layout.siteInformationPanel.SiteInformationViewModel
import com.ucl.energygrid.data.remote.apis.PinApi
import com.ucl.energygrid.data.model.PinResponse
import com.ucl.energygrid.data.model.PinRequest

import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.slot

@ExperimentalCoroutinesApi
class SiteInformationViewModelUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SiteInformationViewModel
    private val mockApi: PinApi = mockk()

    private val userId = 1
    private val mineId = 100

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadNoteAndPinStatus sets note when api returns success`() = runTest {
        // Arrange
        val fakePin = PinResponse(id = 1, mine_id = 123, note = "Test note")
        coEvery { mockApi.getPin(userId, mineId) } returns Response.success(fakePin)

        // Act
        viewModel = SiteInformationViewModel(userId, mineId, mockApi)
        testScheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("Test note", state.note)
        assertEquals("Test note", state.savedNote)
        assertTrue(state.isPinned)
        assertTrue(state.isNoteLoaded)
        assertNull(state.postResult)
    }

    @Test
    fun `updateNote updates note in uiState`() = runTest {
        // Arrange: Stub getPin
        val fakePin = PinResponse(id = 1, mine_id = 123, note = "Initial note")
        coEvery { mockApi.getPin(userId, mineId) } returns Response.success(fakePin)

        // Initialize ViewModel
        viewModel = SiteInformationViewModel(userId, mineId, mockApi)
        testScheduler.advanceUntilIdle()

        // Act
        viewModel.updateNote("New note")

        // Assert
        val state = viewModel.uiState.value
        assertEquals("New note", state.note)
    }

    @Test
    fun `saveNote posts note successfully`() = runTest {
        val requestSlot = slot<PinRequest>()

        // mock getPin，避免初始化時崩潰
        val fakePin = PinResponse(id = 1, mine_id = mineId, note = "Initial Note")
        coEvery { mockApi.getPin(userId, mineId) } returns Response.success(fakePin)

        // mock create_or_update_pin
        val fakeResponse = PinResponse(id = 1, mine_id = mineId, note = "My Note")
        coEvery { mockApi.create_or_update_pin(userId, capture(requestSlot)) } returns Response.success(fakeResponse)

        viewModel = SiteInformationViewModel(userId, mineId, mockApi)
        viewModel.updateNote("My Note")

        // Act
        viewModel.saveNote()
        testScheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("My Note", requestSlot.captured.note)
        assertEquals("My Note", state.savedNote)
        assertTrue(state.isPinned)
        assertEquals("Note saved successfully", state.postResult)
    }

    @Test
    fun `removePin clears note successfully`() = runTest {
        // Arrange
        coEvery { mockApi.getPin(userId, mineId) } returns Response.success(
            PinResponse(id = 1, mine_id = mineId, note = "Initial Note")
        )
        coEvery { mockApi.deletePin(userId, mineId) } returns Response.success(Unit)

        viewModel = SiteInformationViewModel(userId, mineId, mockApi)
        viewModel.updateNote("Some note")

        // Act
        viewModel.removePin()
        testScheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("", state.note)
        assertEquals("", state.savedNote)
        assertFalse(state.isPinned)
        assertEquals("Pin removed successfully", state.postResult)
    }

}