package com.ucl.energygrid.data.model

import com.google.android.gms.maps.model.LatLng

enum class PinType {
    CLOSED_MINE,
    CLOSING_MINE,
    SOLAR,
    WIND,
    HYDROELECTRIC,
    FLOODING_RISK,
    USER_PIN
}

enum class BottomSheetContent {
    None,
    SiteInfo,
    MapControl,
    TimeSimulation
}

data class RenewableSite(
    val name: String,
    val location: LatLng,
    val type: PinType
)

data class EnergyDemand(
    val year: Int,
    val value: Double
)

enum class Trend { INCREASING, DECREASING, STABLE }

data class FloodEvent(val year: Int, val events: Int)

data class Mine(
    val reference: String,
    val name: String,
    val status: String?,
    val easting: Double,
    val northing: Double,
    val localAuthority: String?,
    val note: String?,
    val floodRiskLevel: String?,
    val floodHistory: List<FloodEvent>?,
    val energyDemandHistory: List<EnergyDemand>?,
    val forecastEnergyDemand: List<EnergyDemand>?,
    val trend: Trend? = null
)

data class RegionFeature(
    val name: String,
    val nutsCode: String,
    val polygons: List<List<List<Double>>>
)