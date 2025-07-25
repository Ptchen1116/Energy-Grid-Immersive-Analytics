package com.ucl.energygrid.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.LatLng
import com.ucl.energygrid.data.GeoJsonLoader
import com.ucl.energygrid.data.fetchAllFloodCenters
import com.ucl.energygrid.data.loadMinesFromJson
import com.ucl.energygrid.data.readAndExtractSitesByType
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.BottomSheetContent
import com.ucl.energygrid.data.model.RegionFeature


class MainViewModel(private val context: Context) : ViewModel() {

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
            _allMines.value = loadMinesFromJson(context)
            _solarSites.value = readAndExtractSitesByType(context, category = "solar")
            _windSites.value = readAndExtractSitesByType(context, category ="wind")
            _hydroelectricSites.value = readAndExtractSitesByType(context, category ="hydroelectric")
            _floodCenters.value = fetchAllFloodCenters(context)
            GeoJsonLoader.loadGeoJsonFeatures { features ->
                _regionFeatures.value = features
            }
        }
    }

    fun selectMine(mine: Mine) {
        _selectedMine.value = mine
        _currentBottomSheet.value = BottomSheetContent.SiteInfo
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
}