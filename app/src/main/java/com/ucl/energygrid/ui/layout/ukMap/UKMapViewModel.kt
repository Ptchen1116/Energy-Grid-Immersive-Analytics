package com.ucl.energygrid.ui.layout.ukMap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.data.repository.EnergyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UKMapViewModel(
    private val energyRepository: EnergyRepository = EnergyRepository()
) : ViewModel() {

    private val _energyConsumption = MutableStateFlow<Map<String, ForecastItem>>(emptyMap())
    val energyConsumption: StateFlow<Map<String, ForecastItem>> = _energyConsumption

    fun loadForecast(year: Int) {
        viewModelScope.launch {
            _energyConsumption.value = energyRepository.fetchEnergyForecast(year)
        }
    }
}