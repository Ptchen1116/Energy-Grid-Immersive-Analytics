package com.ucl.energygrid.ui.screen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.ucl.energygrid.data.model.BottomSheetContent
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.PinResponse
import com.ucl.energygrid.data.model.RegionFeature
import com.ucl.energygrid.data.remote.apis.RetrofitInstance
import com.ucl.energygrid.data.repository.GeoJsonRepository
import com.ucl.energygrid.data.repository.fetchAllFloodCenters
import com.ucl.energygrid.data.repository.getAllMines
import com.ucl.energygrid.data.repository.getInfoByReference
import com.ucl.energygrid.data.repository.readAndExtractSitesByType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>()

    private val _currentBottomSheet = MutableStateFlow(BottomSheetContent.None)
    val currentBottomSheet: StateFlow<BottomSheetContent> = _currentBottomSheet

    private val _closedMine = MutableStateFlow(false)
    val closedMine: StateFlow<Boolean> = _closedMine

    private val _closingMine = MutableStateFlow(false)
    val closingMine: StateFlow<Boolean> = _closingMine

    private val _selectedMine = MutableStateFlow<Mine?>(null)
    val selectedMine: StateFlow<Mine?> = _selectedMine

    private val _showFloodRisk = MutableStateFlow(false)
    val showFloodRisk: StateFlow<Boolean> = _showFloodRisk

    private val _floodCenters = MutableStateFlow<List<LatLng>>(emptyList())
    val floodCenters: StateFlow<List<LatLng>> = _floodCenters

    private val _showSolar = MutableStateFlow(false)
    val showSolar: StateFlow<Boolean> = _showSolar

    private val _showWind = MutableStateFlow(false)
    val showWind: StateFlow<Boolean> = _showWind

    private val _showHydroelectric = MutableStateFlow(false)
    val showHydroelectric: StateFlow<Boolean> = _showHydroelectric

    private val _energyDemandHeatmap = MutableStateFlow(false)
    val energyDemandHeatmap: StateFlow<Boolean> = _energyDemandHeatmap

    private val _selectedYear = MutableStateFlow(2025)
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _solarSites = MutableStateFlow<List<Triple<String, Double, Double>>>(emptyList())
    val solarSites: StateFlow<List<Triple<String, Double, Double>>> = _solarSites

    private val _windSites = MutableStateFlow<List<Triple<String, Double, Double>>>(emptyList())
    val windSites: StateFlow<List<Triple<String, Double, Double>>> = _windSites

    private val _hydroelectricSites = MutableStateFlow<List<Triple<String, Double, Double>>>(emptyList())
    val hydroelectricSites: StateFlow<List<Triple<String, Double, Double>>> = _hydroelectricSites

    private val _regionFeatures = MutableStateFlow<List<RegionFeature>>(emptyList())
    val regionFeatures: StateFlow<List<RegionFeature>> = _regionFeatures

    private val _allMines = MutableStateFlow<List<Mine>>(emptyList())
    val allMines: StateFlow<List<Mine>> = _allMines

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _allMines.value = getAllMines()
            _solarSites.value = readAndExtractSitesByType( category = "solar")
            _windSites.value = readAndExtractSitesByType(category ="wind")
            _hydroelectricSites.value = readAndExtractSitesByType( category ="hydroelectric")
            _floodCenters.value = fetchAllFloodCenters(appContext)
            GeoJsonRepository.loadGeoJsonFeatures { features ->
                _regionFeatures.value = features
            }
        }
    }

    fun onMineSelected(mine: Mine) {
        viewModelScope.launch {
            val detailedMine = getInfoByReference(mine.reference)
            if (detailedMine != null) {
                _selectedMine.value = detailedMine
            } else {
                _selectedMine.value = mine
            }
        }
    }

    fun toggleShowFloodRisk(value: Boolean) {
        _showFloodRisk.value = value
    }

    fun toggleShowSolar(value: Boolean) {
        _showSolar.value = value
    }

    fun toggleShowWind(value: Boolean) {
        _showWind.value = value
    }

    fun toggleShowHydroelectric(value: Boolean) {
        _showHydroelectric.value = value
    }

    fun toggleEnergyDemandHeatmap(value: Boolean) {
        _energyDemandHeatmap.value = value
    }

    fun changeSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    fun onSelectedMineChange(mine: Mine?) {
        _selectedMine.value = mine
    }

    fun onBottomSheetChange(content: BottomSheetContent) {
        _currentBottomSheet.value = content
    }

    fun updateClosedMine(mine: Boolean) {
        _closedMine.value = mine
    }

    fun updateClosingMine(mine: Boolean) {
        _closingMine.value = mine
    }

    private val _myPins = MutableStateFlow<List<PinResponse>>(emptyList())
    val myPins: StateFlow<List<PinResponse>> = _myPins.asStateFlow()

    private val _showMyPinsMarkers = MutableStateFlow(false)
    val showMyPinsMarkers: StateFlow<Boolean> = _showMyPinsMarkers.asStateFlow()

    fun loadMyPins(userId: Int, isLoggedIn: Boolean) {
        if (!isLoggedIn) {
            return
        }

        viewModelScope.launch {
            try {
                val api = RetrofitInstance.pinApi
                val response = api.getAllPins(userId)
                if (response.isSuccessful) {
                    _myPins.value = response.body() ?: emptyList()
                    _showMyPinsMarkers.value = true
                } else {
                    Log.e("MainViewModel", "Failed to load pins: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Exception loading pins: ${e.message}")
            }
        }
    }

    fun clearMyPins() {
        _showMyPinsMarkers.value = false
        _myPins.value = emptyList()
    }
}