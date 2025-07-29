package com.ucl.energygrid.data.repository

import android.util.Log
import com.ucl.energygrid.data.remote.apis.ForecastItem
import com.ucl.energygrid.data.remote.apis.ForecastRequest
import com.ucl.energygrid.data.remote.apis.RetrofitInstance


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