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
import com.ucl.energygrid.data.RegionFeature
import com.ucl.energygrid.data.loadEnergyConsumption
import com.ucl.energygrid.ui.component.PinType
import com.ucl.energygrid.ui.component.createPinBitmap

@Composable
fun UKMap(
    floodCenters: List<LatLng>,
    showMarkers: Boolean,
    renewableSites: List<RenewableSite> = emptyList(),
    regionFeatures: List<RegionFeature> = emptyList(),
    energyDemandHeatmap: Boolean = false,
    year: Int
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
    var energyConsumption by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    // Load energy data once
    LaunchedEffect(year) {
        energyConsumption = loadEnergyConsumption(context, year)
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
                    region.polygons.forEach { polygon ->
                        Polygon(
                            points = polygon.map { LatLng(it[1], it[0]) },
                            strokeColor = Color.Blue,
                            fillColor = Color(0x550000FF),
                            strokeWidth = 3f,
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

                Log.d("MapMarker", "Selected region code: $code")

                val consumption = energyConsumption[code]

                if (consumption != null) {
                    Log.d("MapMarker", "Found energy consumption: $consumption GWh for code: $code")
                } else {
                    Log.w("MapMarker", "No energy consumption found for code: $code. Available keys: ${energyConsumption.keys}")
                }

                val snippet = if (consumption != null) {
                    "Total Consumption: %.2f GWh".format(consumption)
                } else {
                    "No data available"
                }

                Marker(
                    state = MarkerState(position = center),
                    title = region.name,
                    snippet = snippet,
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

