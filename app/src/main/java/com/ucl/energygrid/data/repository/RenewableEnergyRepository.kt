package com.ucl.energygrid.data.repository

import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


suspend fun readAndExtractSitesByType(category: String): List<Triple<String, Double, Double>> {
    val apiUrl = "http://10.0.2.2:8000/sites/$category"
    val result = mutableListOf<Triple<String, Double, Double>>()

    return withContext(Dispatchers.IO) {
        try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONArray(response)

            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                result.add(
                    Triple(
                        obj.getString("name"),
                        obj.getDouble("lat"),
                        obj.getDouble("lon")
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e("FetchSites", "Error: ${e.message}", e)
            emptyList()
        }
    }
}