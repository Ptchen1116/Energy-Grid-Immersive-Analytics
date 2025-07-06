package com.ucl.energygrid.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.ucl.energygrid.R
import com.ucl.energygrid.data.MinesMarkers
import com.ucl.energygrid.data.RegionFeature
import com.ucl.energygrid.data.fetchEnergyForecast
import com.ucl.energygrid.ui.component.PinType
import com.ucl.energygrid.ui.component.createPinBitmap
import com.ucl.energygrid.data.API.PinResponse
import com.ucl.energygrid.data.convertOSGB36ToWGS84


data class SelectedPinInfo(val mine: Mine, val pinNote: String)


@Composable
fun UKMap(
    floodCenters: List<LatLng>,
    showMarkers: Boolean,
    renewableSites: List<RenewableSite> = emptyList(),
    regionFeatures: List<RegionFeature> = emptyList(),
    energyDemandHeatmap: Boolean = false,
    year: Int,
    closedMine: Boolean,
    closingMine: Boolean,
    markerIcons: Map<PinType, BitmapDescriptor>,
    onSiteSelected: (Mine) -> Unit,
    myPins: List<PinResponse> = emptyList(),
    allMines: List<Mine> = emptyList(),
    onPinSelected: (Mine) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val ukBounds = LatLngBounds(
        LatLng(49.9, -8.6),
        LatLng(60.9, 1.8)
    )

    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(ukBounds, 100),
            durationMs = 1000
        )
    }

    val markerIcons = remember { mutableMapOf<PinType, BitmapDescriptor>() }
    var selectedRegion by remember { mutableStateOf<RegionFeature?>(null) }

    // State to store energy consumption data
    var energyConsumption by remember { mutableStateOf<Map<String, Pair<Double, String>>>(emptyMap()) }

    // Load energy data once
    LaunchedEffect(year) {
        energyConsumption = fetchEnergyForecast(year)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = {
                selectedRegion = null
            }
        ) {
            // Load custom marker icons
            if (markerIcons.isEmpty()) {
                PinType.values().forEach { type ->
                    markerIcons[type] = BitmapDescriptorFactory.fromBitmap(createPinBitmap(context, type))
                }
            }

            MinesMarkers(
                closedMine = closedMine,
                closingMine = closingMine,
                markerIcons = markerIcons,
                onSiteSelected = onSiteSelected
            )

            myPins.forEach { pin ->
                val mine = allMines.find { it.reference == pin.mine_id.toString() }
                if (mine != null) {
                    val position = convertOSGB36ToWGS84(mine.easting, mine.northing)

                    Marker(
                        state = MarkerState(position = position),
                        title = mine.name ?: "My Pin",
                        snippet = pin.note ?: "",
                        icon = markerIcons[PinType.USER_PIN] ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        onClick = {
                            onPinSelected(mine)
                            true
                        }
                    )
                }
            }

            // Flood markers
            if (showMarkers) {
                floodCenters.forEach { center ->
                    Marker(
                        state = MarkerState(position = center),
                        title = "Flood Center",
                        snippet = "Flood risk here",
                        icon = markerIcons[PinType.FLOODING_RISK]
                    )
                }
            }


            // Renewable site markers
            renewableSites.forEach { site ->
                Marker(
                    state = MarkerState(position = site.location),
                    title = "${site.type.name} Site",
                    snippet = site.name,
                    icon = BitmapDescriptorFactory.fromBitmap(createPinBitmap(context, site.type))
                )
            }

            // Heatmap
            if (energyDemandHeatmap) {
                regionFeatures.forEach { region ->
                    val nutsCode = region.nutsCode
                    val demand = energyConsumption[nutsCode]?.first

                    val lowThreshold = 10000.0
                    val highThreshold = 20000.0

                    val baseColor = when {
                        demand == null -> Color.Transparent
                        demand < lowThreshold -> Color(0xFF00FF00)
                        demand >= lowThreshold && demand < highThreshold -> Color(0xFFFFFF00)
                        demand >= highThreshold -> Color(0xFFFF0000)
                        else -> Color.Gray
                    }


                    val isSelected = selectedRegion?.nutsCode == nutsCode
                    val fillColor = if (isSelected) Color(0x550000FF) else baseColor

                    region.polygons.forEach { polygon ->
                        Polygon(
                            points = polygon.map { LatLng(it[1], it[0]) },
                            strokeColor = if (isSelected) Color.Blue else Color.Black,
                            fillColor = fillColor,
                            strokeWidth = if (isSelected) 3f else 1f,
                            clickable = true,
                            onClick = {
                                selectedRegion = region
                            }
                        )
                    }
                }
            }

            // Tooltip marker for selected region
            selectedRegion?.let { region ->
                val center = region.getCenterLatLng()
                val code = region.nutsCode

                val consumptionPair = energyConsumption[code]
                val consumption = consumptionPair?.first
                val source = consumptionPair?.second

                val titleWithSource = if (source != null) {
                    "${region.name} ($source)"
                } else {
                    region.name
                }

                val snippet = if (consumption != null) {
                    "Total Consumption: %.2f GWh\n(Source: %s)".format(consumption, source)
                } else {
                    "No data available"
                }

                Marker(
                    state = MarkerState(position = center),
                    title = titleWithSource,
                    snippet = if (consumption != null) {
                        "Total Consumption: %.2f GWh".format(consumption)
                    } else {
                        "No data available"
                    },
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )


            }
        }

        FloatingActionButton(
            onClick = { /* Add reset logic */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 128.dp),
            containerColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = R.drawable.map_btn),
                contentDescription = "Map Button",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun RegionFeature.getCenterLatLng(): LatLng {
    val allPoints = polygons.flatten()
    val avgLat = allPoints.map { it[1] }.average()
    val avgLng = allPoints.map { it[0] }.average()
    return LatLng(avgLat, avgLng)
}

