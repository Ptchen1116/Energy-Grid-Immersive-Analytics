package com.ucl.energygrid.data


import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.compose.MapEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.ucl.energygrid.AppEnvironment
import java.net.URL


data class FloodPolygonMeta(
    val areaId: String,
    val polygonUrl: String,
    val severityLevel: String
)

suspend fun fetchFloodPolygons(context: Context): List<FloodPolygonMeta> = withContext(Dispatchers.IO) {
    val jsonText = if (AppEnvironment.isDebug) {
        context.assets.open("fake_flood_data.json").bufferedReader().use { it.readText() }
    } else {
        URL("https://environment.data.gov.uk/flood-monitoring/id/floods").readText()
    }

    val root = JSONObject(jsonText)
    val items = root.getJSONArray("items")
    val list = mutableListOf<FloodPolygonMeta>()

    for (i in 0 until items.length()) {
        val item = items.getJSONObject(i)
        val floodArea = item.getJSONObject("floodArea")
        val severityLevel = item.optString("severityLevel", "unknown")
        val notation = floodArea.getString("notation")
        val areaId = "https://environment.data.gov.uk/flood-monitoring/id/floodAreas/$notation"
        val polygonUrl = "https://environment.data.gov.uk/flood-monitoring/id/floodAreas/$notation/polygon"

        list.add(FloodPolygonMeta(areaId, polygonUrl, severityLevel))
    }

    return@withContext list
}



suspend fun loadPolygonPoints(polygonUrl: String): List<LatLng> = withContext(Dispatchers.IO) {
    val url = polygonUrl
    val json = URL(url).readText()
    val root = JSONObject(json)
    val features = root.getJSONArray("features")
    if (features.length() == 0) return@withContext emptyList()

    val geometry = features.getJSONObject(0).getJSONObject("geometry")
    val type = geometry.getString("type")

    val points = mutableListOf<LatLng>()

    when (type) {
        "Polygon" -> {
            val coords = geometry.getJSONArray("coordinates").getJSONArray(0)
            for (i in 0 until coords.length()) {
                val point = coords.getJSONArray(i)
                val lng = point.getDouble(0)
                val lat = point.getDouble(1)
                points.add(LatLng(lat, lng))
            }
        }
        "MultiPolygon" -> {
            val multiCoords = geometry.getJSONArray("coordinates")
            for (i in 0 until multiCoords.length()) {
                val polygon = multiCoords.getJSONArray(i)
                val ring = polygon.getJSONArray(0)
                for (j in 0 until ring.length()) {
                    val point = ring.getJSONArray(j)
                    val lng = point.getDouble(0)
                    val lat = point.getDouble(1)
                    points.add(LatLng(lat, lng))
                }
            }
        }
        else -> throw IllegalArgumentException("Unsupported geometry type: $type")
    }

    return@withContext points
}

suspend fun loadCenterPoint(areaUrl: String): LatLng? = withContext(Dispatchers.IO) {
    try {
        val json = URL(areaUrl).readText()
        val root = JSONObject(json)
        val items = root.getJSONObject("items")

        val lat = items.getDouble("lat")
        val lng = items.getDouble("long")

        LatLng(lat, lng)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun severityToColor(severity: String): Int {
    return when (severity.lowercase()) {
        "severe flood warning" -> 0x66FF0000
        "flood warning" -> 0x66FFA500
        "flood alert" -> 0x66FFFF00
        else -> 0x66444444
    }
}

fun drawFloodPolygons(context: Context, map: GoogleMap) {
    CoroutineScope(Dispatchers.IO).launch {
        val floodPolygons = fetchFloodPolygons(context)

        for (meta in floodPolygons) {
            val points = loadPolygonPoints(meta.polygonUrl)

            withContext(Dispatchers.Main) {
                map.addPolygon(
                    PolygonOptions()
                        .addAll(points)
                        .fillColor(severityToColor(meta.severityLevel))
                        .strokeColor(Color.BLACK)
                        .strokeWidth(1f)
                )
            }
        }
    }
}

@Composable
fun FloodPolygonsOverlay() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    MapEffect { map ->
        coroutineScope.launch {
            val floodPolygons = fetchFloodPolygons(context)
            for (meta in floodPolygons) {
                try {
                    val points = loadPolygonPoints(meta.polygonUrl)
                    map.addPolygon(
                        PolygonOptions()
                            .addAll(points)
                            .fillColor(severityToColor(meta.severityLevel))
                            .strokeColor(Color.BLACK)
                            .strokeWidth(1f)
                    )
                } catch (e: Exception) {
                    Log.e("FloodPolygon", "Failed to load polygon: ${e.message}")
                }
            }
        }
    }
}

suspend fun fetchAllFloodCenters(context: Context): List<LatLng> {
    val floodPolygons = fetchFloodPolygons(context)
    val centers = mutableListOf<LatLng>()
    for (meta in floodPolygons) {
        val center = loadCenterPoint(meta.areaId)
        if (center != null) {
            centers.add(center)
        }
    }
    return centers
}