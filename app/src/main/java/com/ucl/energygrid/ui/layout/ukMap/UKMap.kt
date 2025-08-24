package com.ucl.energygrid.ui.layout.ukMap

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.ucl.energygrid.data.model.Mine
import com.ucl.energygrid.data.model.PinResponse
import com.ucl.energygrid.data.model.PinType
import com.ucl.energygrid.data.model.RegionFeature
import com.ucl.energygrid.data.model.RenewableSite
import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.data.remote.apis.RetrofitInstance
import com.ucl.energygrid.data.repository.convertOSGB36ToWGS84
import com.ucl.energygrid.ui.component.createPinBitmap
import androidx.compose.runtime.rememberCoroutineScope
import com.ucl.energygrid.ui.screen.rememberResetMap
import com.ucl.energygrid.ui.screen.MainViewModel
import com.ucl.energygrid.ui.screen.MineMarker

@Composable
fun UKMap(
    mainViewModel: MainViewModel,
    ukMapViewModel: UKMapViewModel,
    floodCenters: List<LatLng>,
    renewableSites: List<RenewableSite> = emptyList(),
    regionFeatures: List<RegionFeature> = emptyList(),
    energyDemandHeatmap: Boolean = false,
    year: Int,
    mineMarkers: List<MineMarker> = emptyList(),
    myPins: List<PinResponse> = emptyList(),
    allMines: List<Mine> = emptyList(),
    onPinSelected: (Mine) -> Unit,
    onShowMyPinsClick: () -> Unit,
) {
    val context = LocalContext.current
    val energyConsumption by ukMapViewModel.energyConsumption.collectAsState()

    LaunchedEffect(year) { ukMapViewModel.loadForecast(year) }

    val ukBounds = LatLngBounds(
        LatLng(50.0, -9.5),
        LatLng(58.0, 1.0)
    )
    val cameraPositionState = rememberCameraPositionState()

    LaunchedEffect(Unit) {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(ukBounds, 100),
            durationMs = 1000
        )
    }

    LaunchedEffect(Unit) {
        ukMapViewModel.resetCameraEvent.collect {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(ukBounds, 100),
                durationMs = 500
            )
        }
    }

    var dynamicMarkerIcons by remember { mutableStateOf<Map<PinType, BitmapDescriptor>>(emptyMap()) }
    var selectedRegion by remember { mutableStateOf<RegionFeature?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { selectedRegion = null },
            onMapLoaded = {
                val icons = mutableMapOf<PinType, BitmapDescriptor>()
                PinType.values().forEach { type ->
                    icons[type] = BitmapDescriptorFactory.fromBitmap(createPinBitmap(context, type))
                }
                dynamicMarkerIcons = icons
            }
        ) {
            MineMarkers(mineMarkers = mineMarkers)

            MyPinsMarkers(
                myPins = myPins,
                allMines = allMines,
                dynamicMarkerIcons = dynamicMarkerIcons,
                onPinSelected = onPinSelected
            )


            FloodMarkers(
                floodCenters = floodCenters,
                dynamicMarkerIcons = dynamicMarkerIcons
            )

            RenewableMarkers(renewableSites)

            if (energyDemandHeatmap) {
                RegionHeatmapPolygons(
                    regionFeatures = regionFeatures,
                    energyConsumption = energyConsumption,
                    selectedRegion = selectedRegion,
                    onRegionSelected = { selectedRegion = it }
                )
            }

            selectedRegion?.let {
                SelectedRegionMarker(
                    region = it,
                    energyConsumption = energyConsumption
                )
            }
        }

        val resetMap = rememberResetMap(mainViewModel, ukMapViewModel)

        MapControlButtons(
            onResetClick = resetMap,
            onShowMyPinsClick = onShowMyPinsClick
        )
    }
}

@Composable
fun MineMarkers(
    mineMarkers: List<MineMarker>
) {
    val context = LocalContext.current

    mineMarkers.forEach { marker ->
        val latLng = remember(marker.mine.easting, marker.mine.northing) {
            convertOSGB36ToWGS84(marker.mine.easting, marker.mine.northing)
        }

        val pinType = when (marker.mine.status) {
            "C" -> PinType.CLOSED_MINE
            "I" -> PinType.CLOSING_MINE
            else -> PinType.CLOSED_MINE
        }

        val iconBitmap = remember(marker.mine.trend, pinType) {
            createPinBitmap(context, pinType, marker.mine.trend)
        }

        val iconDescriptor = remember(iconBitmap) {
            BitmapDescriptorFactory.fromBitmap(iconBitmap)
        }

        Marker(
            state = MarkerState(position = latLng),
            title = marker.mine.name,
            snippet = when (marker.mine.status) {
                "C" -> "Closed Mine"
                "I" -> "Closing Mine"
                else -> "Mine"
            },
            icon = iconDescriptor,
            onClick = {
                marker.onClick()
                true
            }
        )
    }
}

@Composable
fun MyPinsMarkers(
    myPins: List<PinResponse>,
    allMines: List<Mine>,
    dynamicMarkerIcons: Map<PinType, BitmapDescriptor>,
    onPinSelected: (Mine) -> Unit,
) {
    myPins.forEach { pin ->
        val mine = allMines.find { it.reference == pin.mine_id.toString() }
        if (mine != null) {
            val position = convertOSGB36ToWGS84(mine.easting, mine.northing)
            Marker(
                state = MarkerState(position = position),
                title = mine.name ?: "My Pin",
                snippet = pin.note ?: "",
                icon = dynamicMarkerIcons[PinType.USER_PIN]
                    ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                onClick = {
                    onPinSelected(mine)
                    true
                }
            )
        }
    }
}

@Composable
fun FloodMarkers(
    floodCenters: List<LatLng>,
    dynamicMarkerIcons: Map<PinType, BitmapDescriptor>,
) {
    floodCenters.forEach { center ->
        Marker(
            state = MarkerState(position = center),
            title = "Flood Center",
            snippet = "Flood risk here",
            icon = dynamicMarkerIcons[PinType.FLOODING_RISK]
        )
    }
}

@Composable
fun RenewableMarkers(renewableSites: List<RenewableSite>) {
    val context = LocalContext.current
    renewableSites.forEach { site ->
        Marker(
            state = MarkerState(position = site.location),
            title = "${site.type.name} Site",
            snippet = site.name,
            icon = BitmapDescriptorFactory.fromBitmap(createPinBitmap(context, site.type))
        )
    }
}

@Composable
fun RegionHeatmapPolygons(
    regionFeatures: List<RegionFeature>,
    energyConsumption: Map<String, ForecastItem>,
    selectedRegion: RegionFeature?,
    onRegionSelected: (RegionFeature) -> Unit
) {
    regionFeatures.forEach { region ->
        val nutsCode = region.nutsCode
        val demand = energyConsumption[nutsCode]?.value

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
                onClick = { onRegionSelected(region) }
            )
        }
    }
}

@Composable
fun SelectedRegionMarker(
    region: RegionFeature,
    energyConsumption: Map<String, ForecastItem>,
) {
    val center = region.getCenterLatLng()
    val code = region.nutsCode

    val forecast = energyConsumption[code]
    val consumption = forecast?.value
    val source = forecast?.source

    val titleWithSource = source?.let { "${region.name} ($it)" } ?: region.name
    val snippet = consumption?.let { "Total Consumption: %.2f GWh".format(it) } ?: "No data available"

    Marker(
        state = MarkerState(position = center),
        title = titleWithSource,
        snippet = snippet,
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
    )
}

@Composable
fun MapControlButtons(
    onResetClick: () -> Unit,
    onShowMyPinsClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = onResetClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp),
            containerColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = R.drawable.floating_button_reset_map),
                contentDescription = "Reset Map",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }

        FloatingActionButton(
            onClick = onShowMyPinsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 88.dp),
            containerColor = Color.White
        ) {
            Icon(
                painter = painterResource(id = R.drawable.floating_button_my_pin),
                contentDescription = "My Pins",
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

