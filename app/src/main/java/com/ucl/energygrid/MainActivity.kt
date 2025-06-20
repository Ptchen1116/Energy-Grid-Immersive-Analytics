package com.ucl.energygrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ucl.energygrid.ui.screen.MainScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

/*
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Hello Flood Test")
        }

        lifecycleScope.launch {
            try {
                val floodPolygons = fetchFloodPolygons(this@MainActivity)
                floodPolygons.forEach { meta ->
                    Log.d("FloodTest", "AreaId: ${meta.areaId}, Severity: ${meta.severityLevel}, PolygonUrl: ${meta.polygonUrl}")
                    try {
                        val points = loadPolygonPoints(meta.polygonUrl)
                        Log.d("PolygonPoints", "Points for ${meta.areaId}: ${points.size} points")
                        points.take(5).forEach { point ->
                            Log.d("PolygonPoints", " - LatLng: ${point.latitude}, ${point.longitude}")
                        }
                        val center = loadCenterPoint(meta.areaId)
                        if (center != null) {
                            Log.d("center", "Center for ${meta.areaId}: ${center.latitude}, ${center.longitude}")
                        } else {
                            Log.d("center", "No center for ${meta.areaId}")
                        }
                    } catch (e: Exception) {
                        Log.e("PolygonPoints", "Failed to load points for ${meta.areaId}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("FloodTest", "Fetch failed: ${e.message}")
            }
        }
    }
}
*/




