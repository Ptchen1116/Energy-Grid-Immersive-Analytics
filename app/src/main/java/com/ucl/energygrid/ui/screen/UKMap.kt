package com.ucl.energygrid.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ucl.energygrid.R


@Composable
fun UKMap(
    floodCenters: List<LatLng>,
    showMarkers: Boolean,
    renewableSites: List<LatLng> = emptyList()
) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (showMarkers) {
                floodCenters.forEach { center ->
                    Marker(
                        state = MarkerState(position = center),
                        title = "Flood Center",
                        snippet = "Flood risk here"
                    )
                }
            }

            renewableSites.forEach { site ->
                Marker(
                    state = MarkerState(position = site),
                    title = "Renewable Site",
                    snippet = "Renewable energy location"
                )
            }
        }

        FloatingActionButton(
            onClick = { /* resets the map */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 128.dp),
            containerColor = Color.White,
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
