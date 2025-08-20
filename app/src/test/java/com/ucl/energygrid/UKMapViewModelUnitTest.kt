package com.ucl.energygrid

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.coroutines.cancel
import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.ui.layout.ukMap.EnergyRepository

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

import com.ucl.energygrid.ui.layout.ukMap.UKMapViewModel


@ExperimentalCoroutinesApi
class UKMapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: UKMapViewModel
    private lateinit var mockRepository: EnergyRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mock()
        viewModel = UKMapViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadForecast updates energyConsumption`() = runTest {
        // Arrange
        val year = 2025
        val fakeData = mapOf(
            "London" to ForecastItem(value = 123.0, source = "Test"),
            "Scotland" to ForecastItem(value = 456.0, source = "Test")
        )
        whenever(mockRepository.fetchEnergyForecast(year)).thenReturn(fakeData)

        // Act
        viewModel.loadForecast(year)
        testScheduler.advanceUntilIdle()

        // Assert
        val result = viewModel.energyConsumption.first()
        assertEquals(fakeData, result)
        verify(mockRepository).fetchEnergyForecast(year)
    }

    @Test
    fun `resetCamera emits event without changing ViewModel`() = runTest {
        var eventReceived: Unit? = null

        val job = launch {
            viewModel.resetCameraEvent.collect { event ->
                eventReceived = event
                this.cancel()
            }
        }

        // Act
        viewModel.resetCamera()
        testScheduler.advanceUntilIdle()

        // Assert
        assertEquals(Unit, eventReceived)
        job.cancel()
    }
}