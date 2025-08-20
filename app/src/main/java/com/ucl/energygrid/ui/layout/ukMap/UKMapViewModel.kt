package com.ucl.energygrid.ui.layout.ukMap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.ui.screen.MainRepository
import com.ucl.energygrid.ui.screen.MainRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// Keep your interface
interface EnergyRepository {
    suspend fun fetchEnergyForecast(year: Int): Map<String, ForecastItem>
}

// Make a separate Impl that wraps the real remote class
class EnergyRepositoryImpl(
    private val remote: com.ucl.energygrid.data.repository.EnergyRepository
) : EnergyRepository {

    override suspend fun fetchEnergyForecast(year: Int): Map<String, ForecastItem> {
        return remote.fetchEnergyForecast(year) // just call it
    }
}

class UKMapViewModel(
    private val energyRepository: EnergyRepository =  EnergyRepositoryImpl(com.ucl.energygrid.data.repository.EnergyRepository())

) : ViewModel() {

    private val _energyConsumption = MutableStateFlow<Map<String, ForecastItem>>(emptyMap())
    val energyConsumption: StateFlow<Map<String, ForecastItem>> = _energyConsumption

    private val _resetCameraEvent = MutableSharedFlow<Unit>()
    val resetCameraEvent: SharedFlow<Unit> = _resetCameraEvent

    fun loadForecast(year: Int) {
        viewModelScope.launch {
            _energyConsumption.value = energyRepository.fetchEnergyForecast(year)
        }
    }

    fun resetCamera() {
        viewModelScope.launch {
            _resetCameraEvent.emit(Unit)
        }
    }
}
