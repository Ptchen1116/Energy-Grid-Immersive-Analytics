package com.ucl.energygrid.data.remote.apis

import retrofit2.http.Body
import retrofit2.http.POST

data class ForecastRequest(val year: Int)

data class ForecastItem(
    val value: Double,
    val source: String
)

typealias ForecastResponse = Map<String, ForecastItem>

interface EnergyApi {
    @POST("forecast")
    suspend fun getForecast(@Body request: ForecastRequest): ForecastResponse
}