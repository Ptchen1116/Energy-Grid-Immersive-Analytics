package com.ucl.energygrid.ui.layout.ukMap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ucl.energygrid.data.repository.EnergyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class UKMapViewModel(
    private val energyRepository: EnergyRepository = EnergyRepository() // 手動建立 Repository
) : ViewModel() {

    private val _energyConsumption = MutableStateFlow<Map<String, Pair<Double, String>>>(emptyMap())
    val energyConsumption: StateFlow<Map<String, Pair<Double, String>>> = _energyConsumption

    fun loadForecast(year: Int) {
        viewModelScope.launch {
            _energyConsumption.value = energyRepository.fetchEnergyForecast(year)
        }
    }
}