package com.ucl.energygrid.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL


suspend fun readAndExtractSitesByType(category: String): List<Triple<String, Double, Double>> {
    val apiUrl = "https://immersive-analytics-for-energy-grid-data.uksouth.cloudapp.azure.com/sites/$category"
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