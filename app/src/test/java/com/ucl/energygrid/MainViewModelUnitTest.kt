package com.ucl.energygrid

// Kotlin & Coroutines
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch

// JUnit
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

// Android
import android.app.Application
import android.util.Log
import com.google.android.gms.maps.model.LatLng

// MockK
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll

// Retrofit
import retrofit2.Response

// Your app classes
import com.ucl.energygrid.ui.screen.MainViewModel
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.BottomSheetContent
import com.ucl.energygrid.data.model.RegionFeature
import com.ucl.energygrid.data.model.PinResponse
import com.ucl.energygrid.data.remote.apis.RetrofitInstance
import com.ucl.energygrid.data.remote.apis.PinApi
import com.ucl.energygrid.data.repository.GeoJsonRepository
import com.ucl.energygrid.data.repository.getAllMines
import com.ucl.energygrid.data.repository.getInfoByReference
import com.ucl.energygrid.data.repository.readAndExtractSitesByType
import com.ucl.energygrid.data.repository.fetchAllFloodCenters
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import com.ucl.energygrid.ui.screen.MainRepository

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MainViewModel
    private lateinit var app: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Fake application context
        app = mockk(relaxed = true)

        // Mock PinApi
        val fakePinApi = mockk<PinApi>(relaxed = true)
        val fakePins = listOf(
            PinResponse(mine_id = 1, note = "Pin1", id = 101),
            PinResponse(mine_id = 2, note = "Pin2", id = 102)
        )
        coEvery { fakePinApi.getAllPins(any()) } returns Response.success(fakePins)

        // Fake repository
        val fakeRepository = mockk<MainRepository>(relaxed = true)

        coEvery { fakeRepository.getAllMines() } returns listOf(
            Mine(reference = "ref1", name = "Test Mine 1", status = "active",
                easting = 123.45, northing = 678.90, localAuthority = "A",
                note = "note", floodRiskLevel = "low", floodHistory = emptyList(),
                energyDemandHistory = emptyList(), forecastEnergyDemand = emptyList(),
                trend = null),
            Mine( reference = "ref2", name = "Test Mine 2", status = "closed", easting = 222.22, northing = 333.33, localAuthority = "B", note = "note", floodRiskLevel = "high", floodHistory = emptyList(), energyDemandHistory = emptyList(), forecastEnergyDemand = emptyList(), trend = null )
        )
        coEvery { fakeRepository.readAndExtractSitesByType("solar") } returns listOf(
            Triple("S1", 1.0, 2.0)
        )
        coEvery { fakeRepository.fetchAllFloodCenters(any()) } returns listOf(LatLng(0.0, 0.0))
        coEvery { fakeRepository.getInfoByReference("ref1") } returns null

        // Return our fake PinApi
        every { fakeRepository.pinApi } returns fakePinApi

        // Mock GeoJsonRepository
        mockkObject(GeoJsonRepository)
        every { GeoJsonRepository.loadGeoJsonFeatures(any()) } answers {
            firstArg<(List<RegionFeature>) -> Unit>().invoke(listOf())
        }

        // Inject fake repository
        viewModel = MainViewModel(app, repository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial data is loaded properly`() = runTest(testDispatcher) {
        val mines = viewModel.allMines.first()
        println("mines: $mines")
        assertEquals(2, mines.size)
        assertEquals("Test Mine 1", mines[0].name)

        val solarSites = viewModel.solarSites.value
        println("solarSites: $solarSites")
        assertEquals(1, solarSites.size)
        assertEquals("S1", solarSites[0].first)

        val floodCenters = viewModel.floodCenters.value
        println("floodCenters: $floodCenters")
        assertEquals(1, floodCenters.size)

        val regions = viewModel.regionFeatures.value
        assertTrue(regions.isEmpty())
    }

    @Test
    fun `selecting a mine updates selectedMine`() {
        val mine = Mine(
            reference = "ref1",
            name = "Test Mine",
            status = null,
            easting = 0.0,
            northing = 0.0,
            localAuthority = null,
            note = null,
            floodRiskLevel = null,
            floodHistory = emptyList(),
            energyDemandHistory = emptyList(),
            forecastEnergyDemand = emptyList(),
            trend = null
        )

        viewModel.onMineSelected(mine)

        val selected = viewModel.selectedMine.value
        println("Selected mine: $selected")
        assertNotNull(selected)
        assertEquals("ref1", selected?.reference)
        assertEquals("Test Mine", selected?.name)
    }

    @Test
    fun `toggle flood risk updates state`() {
        viewModel.toggleShowFloodRisk(true)
        assertTrue(viewModel.showFloodRisk.value)
    }

    @Test
    fun `toggle energy sources updates state`() {
        viewModel.toggleShowSolar(true)
        assertTrue(viewModel.showSolar.value)

        viewModel.toggleShowWind(true)
        assertTrue(viewModel.showWind.value)

        viewModel.toggleShowHydroelectric(true)
        assertTrue(viewModel.showHydroelectric.value)
    }

    @Test
    fun `bottom sheet content updates correctly`() {
        viewModel.onBottomSheetChange(BottomSheetContent.SiteInfo)
        assertEquals(BottomSheetContent.SiteInfo, viewModel.currentBottomSheet.value)
    }

    @Test
    fun `selected year can be changed`() {
        viewModel.changeSelectedYear(2030)
        assertEquals(2030, viewModel.selectedYear.value)
    }

    @Test
    fun `loadMyPins sets pins and shows markers`() = runTest {
        // Act
        viewModel.loadMyPins(userId = 123, isLoggedIn = true)
        // Advance coroutines
        testScheduler.advanceUntilIdle()

        // Assert
        val pins = viewModel.myPins.value
        val showMarkers = viewModel.showMyPinsMarkers.value

        assertEquals(2, pins.size)
        assertEquals("Pin1", pins[0].note)
        assertEquals("Pin2", pins[1].note)
        assertTrue(showMarkers)
    }

    @Test
    fun `loadMyPins does nothing when not logged in`() = runTest {
        viewModel.loadMyPins(userId = 123, isLoggedIn = false)
        advanceUntilIdle()

        assertTrue(viewModel.myPins.value.isEmpty())
        assertFalse(viewModel.showMyPinsMarkers.value)
    }

    @Test
    fun `clearMyPins clears pins and hides markers`() = runTest {
        viewModel.loadMyPins(userId = 123, isLoggedIn = true)
        advanceUntilIdle()

        assertEquals(2, viewModel.myPins.value.size)
        assertTrue(viewModel.showMyPinsMarkers.value)

        viewModel.clearMyPins()

        assertTrue(viewModel.myPins.value.isEmpty())
        assertFalse(viewModel.showMyPinsMarkers.value)
    }
}