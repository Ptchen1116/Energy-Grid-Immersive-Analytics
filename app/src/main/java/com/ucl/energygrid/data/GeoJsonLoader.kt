package com.ucl.energygrid.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import com.ucl.energygrid.data.model.RegionFeature

object GeoJsonLoader {
    fun loadGeoJsonFeatures(onResult: (List<RegionFeature>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url =
                    "https://services1.arcgis.com/ESMARspQHYMw9BZ9/arcgis/rest/services/NUTS1_Jan_2018_UGCB_in_the_UK_2022/FeatureServer/0/query?outFields=*&where=1%3D1&f=geojson"
                val json = URL(url).readText()
                val geoJson = JSONObject(json)
                val featureList = mutableListOf<RegionFeature>()
                val features = geoJson.getJSONArray("features")

                for (i in 0 until features.length()) {
                    try {
                        val feature = features.getJSONObject(i)
                        val props = feature.getJSONObject("properties")
                        val geometry = feature.getJSONObject("geometry")
                        val geometryType = geometry.getString("type")
                        val coords = geometry.getJSONArray("coordinates")

                        val nutsCode = props.optString("nuts118cd", "")
                        val name = props.optString("nuts118nm", "Unknown")

                        val polygon = mutableListOf<List<List<Double>>>()

                        if (geometryType == "Polygon") {
                            for (ringIndex in 0 until coords.length()) {
                                val ring = coords.getJSONArray(ringIndex)
                                val path = mutableListOf<List<Double>>()
                                for (ptIndex in 0 until ring.length()) {
                                    val point = ring.getJSONArray(ptIndex)
                                    val lng = point.getDouble(0)
                                    val lat = point.getDouble(1)
                                    path.add(listOf(lng, lat))
                                }
                                polygon.add(path)
                            }
                        } else if (geometryType == "MultiPolygon") {
                            for (polyIndex in 0 until coords.length()) {
                                val poly = coords.getJSONArray(polyIndex)
                                for (ringIndex in 0 until poly.length()) {
                                    val ring = poly.getJSONArray(ringIndex)
                                    val path = mutableListOf<List<Double>>()
                                    for (ptIndex in 0 until ring.length()) {
                                        val point = ring.getJSONArray(ptIndex)
                                        val lng = point.getDouble(0)
                                        val lat = point.getDouble(1)
                                        path.add(listOf(lng, lat))
                                    }
                                    polygon.add(path)
                                }
                            }
                        } else {
                            Log.w("GeoJsonLoader", "Unsupported geometry type: $geometryType")
                        }

                        featureList.add(RegionFeature(name, nutsCode, polygon))
                    } catch (e: Exception) {
                        Log.e("GeoJsonLoader", "Error parsing feature $i: ${e.message}", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(featureList)
                }

            } catch (e: Exception) {
                Log.e("GeoJsonLoader", "Failed to load GeoJSON: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }
}
