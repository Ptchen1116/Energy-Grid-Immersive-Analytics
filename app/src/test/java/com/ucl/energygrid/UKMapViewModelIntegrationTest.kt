package com.ucl.energygrid

import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.ui.layout.ukMap.EnergyRepository
import com.ucl.energygrid.ui.layout.ukMap.UKMapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UKMapViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: UKMapViewModel
    private lateinit var fakeRepo: FakeEnergyRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeEnergyRepository()
        viewModel = UKMapViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadForecast updates energyConsumption`() = runTest {
        // Arrange
        val year = 2030

        // Act
        viewModel.loadForecast(year)
        advanceUntilIdle()

        // Assert
        val result = viewModel.energyConsumption.value
        assertTrue(result.isNotEmpty())
        assertEquals("2030-forecast", result["UK"]?.source)
        assertEquals(2030.0, result["UK"]?.value)
    }

    @Test
    fun `resetCamera emits event`() = runTest {
        var eventTriggered = false

        // 監聽事件
        val job = launch {
            viewModel.resetCameraEvent.collect {
                eventTriggered = true
            }
        }

        // Act
        viewModel.resetCamera()
        advanceUntilIdle()

        // Assert
        assertTrue(eventTriggered)

        job.cancel()
    }
}


class FakeEnergyRepository : EnergyRepository {
    override suspend fun fetchEnergyForecast(year: Int): Map<String, ForecastItem> {
        return mapOf(
            "UK" to ForecastItem(
                value = year.toDouble(),
                source = "$year-forecast"
            )
        )
    }
}