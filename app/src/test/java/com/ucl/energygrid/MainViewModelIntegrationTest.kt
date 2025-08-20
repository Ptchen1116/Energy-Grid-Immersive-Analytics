package com.ucl.energygrid

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import com.ucl.energygrid.ui.screen.MainViewModel
import com.ucl.energygrid.ui.screen.MainRepository
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.RegionFeature
import com.ucl.energygrid.data.model.PinResponse
import com.ucl.energygrid.data.remote.apis.PinApi
import com.ucl.energygrid.data.repository.GeoJsonRepository


@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var viewModel: MainViewModel
    private lateinit var fakeRepository: MainRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = mockk(relaxed = true)

        // Fake PinApi
        val fakePinApi = mockk<PinApi>(relaxed = true)
        coEvery { fakePinApi.getAllPins(1) } returns Response.success(
            listOf(
                PinResponse(mine_id = 1, note = "Pin1", id = 101),
                PinResponse(mine_id = 2, note = "Pin2", id = 102)
            )
        )

        // Fake Repository
        fakeRepository = mockk(relaxed = true) {
            coEvery { getAllMines() } returns listOf(
                Mine("ref1", "Mine 1", "active", 0.0, 0.0, "A", "note", "low", emptyList(), emptyList(), emptyList(), null),
                Mine("ref2", "Mine 2", "closed", 0.0, 0.0, "B", "note", "high", emptyList(), emptyList(), emptyList(), null)
            )
            coEvery { readAndExtractSitesByType(any()) } returns emptyList()
            coEvery { fetchAllFloodCenters(any()) } returns listOf(LatLng(0.0, 0.0))
            coEvery { getInfoByReference("ref1") } returns Mine(
                "ref1", "Detailed Mine", "closed", 0.0, 0.0, "A", "Some note", null,
                emptyList(), emptyList(), emptyList(), null
            )
            every { pinApi } returns fakePinApi
        }

        // Mock GeoJsonRepository
        mockkObject(GeoJsonRepository)
        every { GeoJsonRepository.loadGeoJsonFeatures(any()) } answers {
            firstArg<(List<RegionFeature>) -> Unit>().invoke(listOf())
        }

        viewModel = MainViewModel(app, repository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial data is loaded correctly`() = runTest {
        // 等待 ViewModel 初始化
        advanceUntilIdle()

        val mines = viewModel.allMines.value
        val floodCenters = viewModel.floodCenters.value
        val regionFeatures = viewModel.regionFeatures.value

        assertEquals(2, mines.size)
        assertEquals(1, floodCenters.size)
        assertEquals(0, regionFeatures.size) // 因為我們 mock 返回空 list
    }

    @Test
    fun `selecting a mine updates selectedMine`() = runTest {
        advanceUntilIdle()
        val mine = viewModel.allMines.value.first()
        viewModel.onMineSelected(mine)
        advanceUntilIdle()

        val selected = viewModel.selectedMine.value
        assertEquals("Detailed Mine", selected?.name)
    }

    @Test
    fun `loading pins updates myPins and showMyPinsMarkers`() = runTest {
        advanceUntilIdle()
        viewModel.loadMyPins(userId = 1, isLoggedIn = true)
        advanceUntilIdle()

        val pins = viewModel.myPins.value
        val showMarkers = viewModel.showMyPinsMarkers.value

        assertEquals(2, pins.size)
        assertTrue(showMarkers)
    }
}