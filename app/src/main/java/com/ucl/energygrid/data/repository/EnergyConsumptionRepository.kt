package com.ucl.energygrid.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class EnergyRepository {
    suspend fun fetchEnergyForecast(year: Int): Map<String, Pair<Double, String>> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, Pair<Double, String>>()
            val apiUrl = "http://10.0.2.2:8000/forecast"

            try {
                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val requestBody = JSONObject().put("year", year).toString()
                OutputStreamWriter(conn.outputStream).use { it.write(requestBody) }

                val responseCode = conn.responseCode
                Log.d("EnergyForecast", "Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                    val jsonResponse = JSONObject(response)

                    for (key in jsonResponse.keys()) {
                        val item = jsonResponse.getJSONObject(key)
                        val value = item.getDouble("value")
                        val source = item.getString("source")
                        result[key] = Pair(value, source)

                        Log.d("EnergyForecast", "$key: $value ($source)")
                    }
                } else {
                    Log.e("EnergyForecast", "Error response code: $responseCode")
                }

            } catch (e: Exception) {
                Log.e("EnergyForecast", "Error fetching forecast: ${e.message}", e)
            }

            result
        }
}