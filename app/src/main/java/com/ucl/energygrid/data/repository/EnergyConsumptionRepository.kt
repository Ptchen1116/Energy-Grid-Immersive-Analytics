package com.ucl.energygrid.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.ucl.energygrid.data.remote.apis.RetrofitInstance
import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.data.remote.apis.ForecastRequest


class EnergyRepository {
    suspend fun fetchEnergyForecast(year: Int): Map<String, ForecastItem> {
        return try {
            RetrofitInstance.energyApi.getForecast(ForecastRequest(year))
        } catch (e: Exception) {
            Log.e("EnergyForecast", "Error fetching forecast: ${e.message}", e)
            emptyMap()
        }
    }
}