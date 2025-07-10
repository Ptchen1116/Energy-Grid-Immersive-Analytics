package com.ucl.energygrid.ui.screen

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
    onPinSelected: (Mine) -> Unit,
    showMyPinsMarkers: Boolean,
    onShowMyPinsClick: () -> Unit,
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

    val dynamicMarkerIcons = remember { mutableMapOf<PinType, BitmapDescriptor>() }
    var selectedRegion by remember { mutableStateOf<RegionFeature?>(null) }

    var energyConsumption by remember { mutableStateOf<Map<String, Pair<Double, String>>>(emptyMap()) }

    LaunchedEffect(year) {
        energyConsumption = fetchEnergyForecast(year)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { selectedRegion = null }
        ) {
            // Load icons once
            if (dynamicMarkerIcons.isEmpty()) {
                PinType.values().forEach { type ->
                    dynamicMarkerIcons[type] = BitmapDescriptorFactory.fromBitmap(createPinBitmap(context, type))
                }
            }

            MinesMarkers(
                closedMine = closedMine,
                closingMine = closingMine,
                markerIcons = dynamicMarkerIcons,
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
                        icon = dynamicMarkerIcons[PinType.USER_PIN] ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                        onClick = {
                            onPinSelected(mine)
                            true
                        }
                    )
                }
            }

            if (showMarkers) {
                floodCenters.forEach { center ->
                    Marker(
                        state = MarkerState(position = center),
                        title = "Flood Center",
                        snippet = "Flood risk here",
                        icon = dynamicMarkerIcons[PinType.FLOODING_RISK]
                    )
                }
            }

            renewableSites.forEach { site ->
                Marker(
                    state = MarkerState(position = site.location),
                    title = "${site.type.name} Site",
                    snippet = site.name,
                    icon = BitmapDescriptorFactory.fromBitmap(createPinBitmap(context, site.type))
                )
            }

            if (energyDemandHeatmap) {
                regionFeatures.forEach { region ->
                    val nutsCode = region.nutsCode
                    val demand = energyConsumption[nutsCode]?.first

                    val baseColor = when {
                        demand == null -> Color.Transparent
                        demand < 10000.0 -> Color(0xFF00FF00)
                        demand < 20000.0 -> Color(0xFFFFFF00)
                        else -> Color(0xFFFF0000)
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
                            onClick = { selectedRegion = region }
                        )
                    }
                }
            }

            selectedRegion?.let { region ->
                val center = region.getCenterLatLng()
                val code = region.nutsCode
                val consumptionPair = energyConsumption[code]
                val consumption = consumptionPair?.first
                val source = consumptionPair?.second

                val titleWithSource = source?.let { "${region.name} ($it)" } ?: region.name
                val snippet = consumption?.let { "Total Consumption: %.2f GWh".format(it) } ?: "No data available"

                Marker(
                    state = MarkerState(position = center),
                    title = titleWithSource,
                    snippet = snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FloatingActionButton(
                onClick = { /* TODO: Reset action */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 16.dp),
                containerColor = Color.White
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.floating_button_reset_map),
                    contentDescription = "Map Button",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }

            FloatingActionButton(
                onClick = { onShowMyPinsClick()},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 88.dp),
                containerColor = Color.White
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.floating_button_my_pin),
                    contentDescription = "My Pin",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

fun RegionFeature.getCenterLatLng(): LatLng {
    val allPoints = polygons.flatten()
    val avgLat = allPoints.map { it[1] }.average()
    val avgLng = allPoints.map { it[0] }.average()
    return LatLng(avgLat, avgLng)
}

