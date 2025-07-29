package com.ucl.energygrid.data.remote.apis

import retrofit2.http.GET
import retrofit2.http.Path

data class SiteResponse(
    val name: String,
    val lat: Double,
    val lon: Double
)

interface SiteApi {
    @GET("sites/{category}")
    suspend fun getSitesByCategory(@Path("category") category: String): List<SiteResponse>
}